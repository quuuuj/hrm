package com.qiujie.filetask.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.filetask.entity.FileUploadChunk;
import com.qiujie.filetask.entity.FileUploadSession;
import com.qiujie.filetask.enums.UploadSessionStatusEnum;
import com.qiujie.filetask.mapper.FileUploadChunkMapper;
import com.qiujie.filetask.mapper.FileUploadSessionMapper;
import com.qiujie.filetask.spi.UploadCompletionHandler;
import com.qiujie.filetask.spi.UploadSessionInfo;
import com.qiujie.common.storage.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 三阶段分片上传服务。
 *
 * 协议流程：
 *   ① init   → 创建上传会话（秒传/断点续传/新建）
 *   ② chunks → 逐片上传分片到 MinIO（幂等，已传分片自动跳过）
 *   ③ complete → 合并分片为完整文件，回调业务 handler 执行后续逻辑
 *
 * 支持秒传（同 hash 文件已存在时跳过上传）和断点续传（未过期会话可续传未完成分片）。
 */
@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    /** 单文件最大 256MB */
    private static final long MAX_FILE_SIZE = 256 * 1024 * 1024L;
    /** 单分片最大 10MB */
    private static final long MAX_CHUNK_SIZE = 10 * 1024 * 1024L;
    /** MinIO composeObject 要求除最后一片外每片不小于 5MiB，小于该值且多分片必然合并失败 */
    private static final long MIN_CHUNK_SIZE = 5 * 1024 * 1024L;
    /** 上传会话有效期 24 小时，超时后定时清理 */
    private static final int SESSION_TTL_HOURS = 24;
    /** 清理任务单批处理条数，避免一次拉全表 */
    private static final int CLEAN_BATCH_SIZE = 200;
    /** 会话不存在与归属不符统一文案，不暴露会话是否存在 */
    private static final String SESSION_MISSING = "上传会话不存在或已过期";
    private static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-fA-F]{64}$");

    @Autowired
    private FileUploadSessionMapper sessionMapper;

    @Autowired
    private FileUploadChunkMapper chunkMapper;

    @Autowired
    private MinioStorageService storageService;

    /**
     * 阶段1：初始化上传会话。
     *
     * 三级优先级决策：
     *   1. 秒传 — dedupHandler.checkDedup(fileHash, staffId) 命中本人名下同 hash 文档 → 直接返回，跳过上传
     *   2. 断点续传 — 同 staffId + fileHash 存在未过期且未结束的会话 → 返回已有 uploadId 和已传分片列表
     *   3. 新建会话 — 写入 file_upload_session，返回 uploadId、chunkSize、chunkCount
     *
     * @param fileName     原始文件名，如 "员工手册.pdf"
     * @param fileExt      文件扩展名（不含点），如 "pdf"
     * @param fileSize     文件总大小（字节）
     * @param fileHash     文件 SHA-256 哈希，用于秒传去重和断点续传匹配
     * @param chunkSize    单个分片大小（字节），前端默认 5MB
     * @param dedupHandler 秒传检测策略，文件中心传入 DocsUploadCompletionHandler；导入任务传 null 跳过秒传
     * @param staffId      当前操作员工 ID
     * @return uploadId + 可选 chunkSize/chunkCount/instantUpload/resumed/uploadedChunks
     */
    public ResponseDTO initUpload(String fileName, String fileExt, Long fileSize,
                                   String fileHash, Long chunkSize,
                                   UploadCompletionHandler dedupHandler, Integer staffId) {
        // 入参校验放在控制器入口，缺参、零值、非法哈希一律返回业务错误
        String invalid = validateInitArgs(fileName, fileSize, fileHash, chunkSize);
        if (invalid != null) {
            return Response.error(invalid);
        }

        // —— 优先级 1: 秒传 ——
        // dedupHandler 为 null 时跳过（通用导入场景无需去重）
        if (dedupHandler != null) {
            Map<String, Object> dedup = dedupHandler.checkDedup(fileHash, staffId);
            if (dedup != null && !dedup.isEmpty()) {
                dedup.put("instantUpload", true);
                return Response.success("秒传成功，文件已存在", dedup);
            }
        }

        // —— 优先级 2: 断点续传 ——
        // 只复用未过期、未结束的会话：过期会话状态不会变化，不加过期条件会被当成可续传
        List<FileUploadSession> sessions = sessionMapper.selectList(
                new QueryWrapper<FileUploadSession>()
                        .eq("staff_id", staffId)
                        .eq("file_hash", fileHash)
                        .in("status", UploadSessionStatusEnum.INIT, UploadSessionStatusEnum.UPLOADING)
                        .gt("expire_time", LocalDateTime.now())
                        .orderByDesc("create_time")
                        .last("LIMIT 1"));
        if (!sessions.isEmpty()) {
            FileUploadSession session = sessions.get(0);
            // 返回已上传的分片索引列表，前端据此跳过已传分片
            List<Integer> uploadedChunks = chunkMapper.selectList(
                    new QueryWrapper<FileUploadChunk>().eq("upload_id", session.getId()))
                    .stream().map(FileUploadChunk::getChunkIndex).collect(Collectors.toList());
            Map<String, Object> result = new HashMap<>();
            result.put("instantUpload", false);
            result.put("resumed", true);
            result.put("uploadId", session.getId());
            result.put("uploadedChunks", uploadedChunks);
            // 以会话记录为准：续传时前端必须按首次上传的切片大小继续切分
            result.put("chunkSize", session.getChunkSize());
            result.put("chunkCount", session.getChunkCount());
            return Response.success("恢复上传会话", result);
        }

        // —— 优先级 3: 新建会话 ——
        // 只有这一条 insert，无需事务包裹
        int chunkCount = (int) Math.ceil((double) fileSize / chunkSize);
        String uploadId = IdUtil.fastSimpleUUID();
        FileUploadSession session = new FileUploadSession()
                .setId(uploadId)
                .setStaffId(staffId)
                .setFileName(fileName)
                .setFileExt(fileExt)
                .setFileSize(fileSize)
                .setFileHash(fileHash)
                .setChunkSize(chunkSize)
                .setChunkCount(chunkCount)
                .setStatus(UploadSessionStatusEnum.INIT)
                // 24 小时后过期，定时任务 cleanExpiredSessions() 清理
                .setExpireTime(LocalDateTime.now().plusHours(SESSION_TTL_HOURS));
        sessionMapper.insert(session);

        Map<String, Object> result = new HashMap<>();
        result.put("instantUpload", false);
        result.put("resumed", false);
        result.put("uploadId", uploadId);
        result.put("chunkSize", chunkSize);
        result.put("chunkCount", chunkCount);
        return Response.success(result);
    }

    /**
     * 阶段2：上传单个分片（幂等），校验分片序号与分片哈希。
     *
     * @param chunkIndex 分片序号，从 0 开始
     * @param chunkHash  客户端算出的该分片 SHA-256，可为空
     * @param chunk      该分片的内容，不是完整文件
     * @param staffId    当前操作员工 ID，用于校验会话归属
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO uploadChunk(String uploadId, Integer chunkIndex, String chunkHash,
                                    MultipartFile chunk, Integer staffId) {
        FileUploadSession session = sessionMapper.selectById(uploadId);
        if (!ownedBy(session, staffId)) {
            return Response.error(SESSION_MISSING);
        }
        // 已合并的会话不再接受新分片，否则会出现有记录但不重新合并的不一致状态
        if (session.getStatus() == UploadSessionStatusEnum.COMPLETED
                || session.getStatus() == UploadSessionStatusEnum.COMPLETING) {
            return Response.error("上传会话已结束，请重新上传");
        }
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= session.getChunkCount()) {
            return Response.error("分片序号超出范围: " + chunkIndex);
        }

        // 幂等：不重复上传已有分片
        if (chunkMapper.selectCount(new QueryWrapper<FileUploadChunk>()
                .eq("upload_id", uploadId).eq("chunk_index", chunkIndex)) > 0) {
            return Response.success("分片已存在");
        }

        String storagePath = String.format("uploads/%s/chunks/%d", uploadId, chunkIndex);
        try {
            byte[] bytes = chunk.getBytes();
            String serverHash = SecureUtil.sha256(new ByteArrayInputStream(bytes));
            if (chunkHash != null && !chunkHash.isEmpty() && !serverHash.equalsIgnoreCase(chunkHash)) {
                log.warn("分片哈希不匹配: uploadId={}, chunkIndex={}, client={}, server={}",
                        uploadId, chunkIndex, chunkHash, serverHash);
                return Response.error("分片校验失败，请重试");
            }
            storageService.put(storagePath, bytes);
            FileUploadChunk chunkRecord = new FileUploadChunk()
                    .setUploadId(uploadId)
                    .setChunkIndex(chunkIndex)
                    .setChunkSize((long) bytes.length)
                    .setChunkHash(serverHash)
                    .setStoragePath(storagePath);
            // 事务内的写全部圈进补偿范围：任何一步失败都会回滚分片记录，MinIO 对象则不随事务回滚，须就地删除
            try {
                chunkMapper.insert(chunkRecord);
                // 首个分片时更新状态
                if (session.getStatus() != UploadSessionStatusEnum.UPLOADING) {
                    session.setStatus(UploadSessionStatusEnum.UPLOADING);
                    sessionMapper.updateById(session);
                }
            } catch (DuplicateKeyException e) {
                // 并发穿过上面的预查后，由 uk_session_chunk 兜底；对象键固定，重复写入的是同一份对象
                log.info("并发重复上传同一分片，按幂等成功返回: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
                return Response.success("分片已存在");
            } catch (RuntimeException e) {
                // 入库失败而事务尚未提交，MinIO 里刚写入的对象不会被任何记录引用，就地补偿删除
                deleteQuietly(storagePath);
                throw e;
            }

            return Response.success(uploadId, Map.of("chunkIndex", chunkIndex));
        } catch (IOException e) {
            log.error("分片上传失败: uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            return Response.error("分片上传失败");
        }
    }

    /**
     * 合并分片（公共逻辑）。检查分片完整性 → composeObject → 更新会话状态。
     * 会话由调用方读取并校验归属后传入，状态与 mergedObjectKey 就地写回同一实例，
     * 调用方拿到后可直接继续 updateById，不需要重新查库。
     *
     * @param session   已通过归属校验的上传会话
     * @param keyPrefix mergedKey 路径前缀（如 "task-source" 或 handler.getStoragePrefix()）
     * @return 合并后的文件 key（MinIO 对象路径）
     */
    private String mergeChunks(FileUploadSession session, String keyPrefix) {
        String uploadId = session.getId();
        List<FileUploadChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<FileUploadChunk>().eq("upload_id", uploadId).orderByAsc("chunk_index"));
        if (chunks.size() != session.getChunkCount()) {
            throw new RuntimeException("分片不完整，已上传 " + chunks.size() + "/" + session.getChunkCount()
                    + missingHint(chunks, session.getChunkCount()));
        }
        // 数量相等不代表序号连续：漏传中间片、补传越界序号时数量依然可能相等
        for (int i = 0; i < chunks.size(); i++) {
            if (chunks.get(i).getChunkIndex() != i) {
                throw new RuntimeException("分片序号不连续，缺失序号 " + i);
            }
        }

        session.setStatus(UploadSessionStatusEnum.COMPLETING);
        sessionMapper.updateById(session);

        String mergedKey = String.format("%s/%d/%s/%s.%s",
                keyPrefix, session.getStaffId(), uploadId,
                IdUtil.fastSimpleUUID().substring(2, 22),
                session.getFileExt());
        List<String> sourceKeys = chunks.stream()
                .map(FileUploadChunk::getStoragePath).collect(Collectors.toList());
        storageService.composeObject(sourceKeys, mergedKey, "application/octet-stream");

        session.setMergedObjectKey(mergedKey);
        session.setStatus(UploadSessionStatusEnum.COMPLETED);
        sessionMapper.updateById(session);
        return mergedKey;
    }

    /**
     * 阶段3（无 handler）：仅合并分片，返回 MinIO key。
     * 供导入任务等自行创建 FileTask 的场景使用。
     *
     * @param staffId 当前操作员工 ID，用于校验会话归属
     */
    @Transactional(rollbackFor = Exception.class)
    public String completeUpload(String uploadId, Integer staffId) {
        FileUploadSession session = sessionMapper.selectById(uploadId);
        if (!ownedBy(session, staffId)) {
            throw new RuntimeException(SESSION_MISSING);
        }
        if (session.getStatus() == UploadSessionStatusEnum.COMPLETED && session.getMergedObjectKey() != null) {
            return session.getMergedObjectKey();
        }
        return mergeChunks(session, "task-source");
    }

    /**
     * 阶段3：完成上传，合并分片并通过 handler 执行业务逻辑。
     * ingest 参数透传给 handler（决定是否触发知识库 ETL）。
     * 重复调用时返回首次执行的结果快照，不重复合并、不重复入库。
     *
     * @param staffId 当前操作员工 ID，用于校验会话归属
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO completeUpload(String uploadId, UploadCompletionHandler handler, boolean ingest, Integer staffId) {
        FileUploadSession session = sessionMapper.selectById(uploadId);
        if (!ownedBy(session, staffId)) {
            return Response.error(SESSION_MISSING);
        }
        // 会话已完成且结果已落库：直接重放，避免重复插 sys_docs 与重复触发 ETL
        if (session.getStatus() == UploadSessionStatusEnum.COMPLETED && session.getMergedObjectKey() != null) {
            return Response.success("上传完成", replayResult(session));
        }

        String mergedKey = mergeChunks(session, handler.getStoragePrefix());
        UploadSessionInfo info = new UploadSessionInfo(
                uploadId, session.getFileName(), session.getFileExt(),
                session.getFileSize(), session.getFileHash(),
                session.getStaffId(), session.getChunkCount(), ingest);
        Map<String, Object> extra = handler.onComplete(mergedKey, info);
        if (extra == null) extra = new HashMap<>();
        session.setCompleteResult(JSON.toJSONString(extra));
        sessionMapper.updateById(session);
        return Response.success("上传完成", extra);
    }

    /**
     * 每小时清理过期上传会话及其分片文件。
     * 按过期时间升序分批处理，避免一次拉全表；单个对象删除失败只记日志，记录留到下一轮重试。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredSessions() {
        int cleaned = 0;
        while (true) {
            List<FileUploadSession> expired = sessionMapper.selectList(
                    new QueryWrapper<FileUploadSession>()
                            .lt("expire_time", LocalDateTime.now())
                            .orderByAsc("expire_time")
                            .last("LIMIT " + CLEAN_BATCH_SIZE));
            if (expired.isEmpty()) {
                break;
            }
            int removedThisBatch = 0;
            for (FileUploadSession session : expired) {
                if (cleanSession(session)) {
                    removedThisBatch++;
                }
            }
            cleaned += removedThisBatch;
            // 整批都没能删掉（对象存储持续不可用）时退出，等下一轮再试，避免死循环
            if (removedThisBatch == 0) {
                log.warn("过期上传会话清理失败，本轮保留 {} 条待下次重试", expired.size());
                break;
            }
        }
        if (cleaned > 0) {
            log.info("Cleaned {} expired upload sessions", cleaned);
        }
    }

    /**
     * 清理单个过期会话：删分片对象 → 删分片记录 → 删会话行。
     * 任一对象删除失败则保留会话行，下一轮继续重试，避免数据库记录先消失、对象彻底失去线索。
     */
    private boolean cleanSession(FileUploadSession session) {
        List<FileUploadChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<FileUploadChunk>().eq("upload_id", session.getId()));
        for (FileUploadChunk chunk : chunks) {
            try {
                storageService.delete(chunk.getStoragePath());
            } catch (Exception e) {
                log.warn("分片对象删除失败，会话保留待重试: uploadId={}, path={}",
                        session.getId(), chunk.getStoragePath(), e);
                return false;
            }
        }
        try {
            chunkMapper.delete(new QueryWrapper<FileUploadChunk>().eq("upload_id", session.getId()));
            sessionMapper.deleteById(session.getId());
            return true;
        } catch (Exception e) {
            log.warn("过期会话记录删除失败，下一轮重试: uploadId={}", session.getId(), e);
            return false;
        }
    }

    /** init 入参校验，返回 null 表示通过 */
    private String validateInitArgs(String fileName, Long fileSize, String fileHash, Long chunkSize) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "文件名不能为空";
        }
        if (fileSize == null || fileSize <= 0) {
            return "文件大小必须为正数";
        }
        if (fileSize > MAX_FILE_SIZE) {
            return "文件大小超过限制，最大256MB";
        }
        if (chunkSize == null || chunkSize < MIN_CHUNK_SIZE) {
            return "分片大小不能小于5MB";
        }
        if (chunkSize > MAX_CHUNK_SIZE) {
            return "分片大小超过限制，最大10MB";
        }
        if (fileHash == null || !SHA256_HEX.matcher(fileHash).matches()) {
            return "文件哈希格式错误，需为 64 位十六进制 SHA-256";
        }
        return null;
    }

    /** 会话存在且属于当前操作人 */
    private boolean ownedBy(FileUploadSession session, Integer staffId) {
        return session != null
                && session.getExpireTime() != null
                && !session.getExpireTime().isBefore(LocalDateTime.now())
                && staffId != null
                && staffId.equals(session.getStaffId());
    }

    /** 分片不完整时补充缺失序号提示 */
    private String missingHint(List<FileUploadChunk> chunks, int chunkCount) {
        Set<Integer> uploaded = chunks.stream()
                .map(FileUploadChunk::getChunkIndex).collect(Collectors.toSet());
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            if (!uploaded.contains(i)) {
                missing.add(i);
            }
        }
        return missing.isEmpty() ? "" : "，缺失序号 " + missing;
    }

    /** 幂等返回：优先读会话上的结果快照，历史会话没有快照时按 mergedKey 兜底 */
    private Map<String, Object> replayResult(FileUploadSession session) {
        if (session.getCompleteResult() != null) {
            try {
                return JSON.parseObject(session.getCompleteResult(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("complete_result 解析失败，返回空结果: uploadId={}", session.getId(), e);
            }
        }
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("mergedKey", session.getMergedObjectKey());
        return fallback;
    }

    /** 补偿删除，失败只记日志（对象已在事务外写入，删不掉只能留给孤儿清理任务） */
    private void deleteQuietly(String path) {
        try {
            storageService.delete(path);
        } catch (Exception e) {
            log.warn("补偿删除分片对象失败: {}", path, e);
        }
    }
}
