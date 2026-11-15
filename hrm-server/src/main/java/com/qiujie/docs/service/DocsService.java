package com.qiujie.docs.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.common.storage.MinioStorageService;
import com.qiujie.util.ObjectDownloadWriter;
import com.qiujie.docs.entity.Docs;
import com.qiujie.docs.mapper.DocsMapper;
import com.qiujie.docs.vo.StaffDocsVO;
import com.qiujie.staff.entity.Staff;
import com.qiujie.staff.mapper.StaffMapper;
import com.qiujie.util.HutoolExcelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件中心服务（普通附件）。
 *
 * @Author qiujie
 * @Date 2022/2/24
 * @Version 1.0
 */
@Service
public class DocsService extends ServiceImpl<DocsMapper, Docs> {

    private static final Logger log = LoggerFactory.getLogger(DocsService.class);

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private DocsMapper docsMapper;

    @Autowired
    private StaffMapper staffMapper;

    /**
     * 由独立任务按各自保留期清理的对象前缀，孤儿清理不得越界删除。
     * task-source / task-result / task-error 由 FileTaskCleanupJob 按 file-task.retention-days 处理，
     * 早于该保留期删除会让用户在任务有效期内无法下载；kb/ 由文档模块自行清理。
     */
    private static final List<String> MANAGED_ELSEWHERE_PREFIXES = List.of(
            "task-source/", "task-result/", "task-error/", "kb/");

    /**
     * 在文件下载以及数据导出时，响应对象是可以不用作为方法返回值返回的，其在方法执行时已经开始输出，
     * 且其无法与@RestController配合，以JSON格式返回给前端；如果返回响应对象，后端会抛出异常
     */
    public void download(String filename, HttpServletResponse response) throws IOException {
        ObjectDownloadWriter.write(storageService, filename, response);
    }

    public ResponseDTO add(Docs docs) {
        if (save(docs)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 物理删除：行与物理文件一起消失。
     * <p>
     * 物理文件在事务提交后删除（{@code name} 有唯一约束，不存在被其他行共享的可能）。
     * 行已不存在（重复删除、并发删除）一律返回成功；不存在的 id 与已删 id 在物理删除后
     * 无法区分，按调用方参数错误处理没有价值。
     * </p>
     */
    public ResponseDTO delete(Integer id) {
        Docs doc = docsMapper.selectById(id);
        if (doc == null) {
            return Response.success();
        }
        // 并发删除：行已被别的请求删掉，removeById 影响行数为 0，此时对对象再删一次是幂等的
        removeById(id);
        deleteObjectAfterCommit(doc.getName());
        return Response.success();
    }

    /**
     * 物理文件删除保持在事务提交后执行：提交前删对象，一旦回滚就留下「行在、文件没了」的坏行。
     */
    private void deleteObjectAfterCommit(String storageKey) {
        if (storageKey == null) {
            return;
        }
        Runnable task = () -> {
            try {
                storageService.delete(storageKey);
            } catch (Exception e) {
                log.warn("删除物理文件失败（对象将由孤儿清理兜底）: {}", storageKey, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            delete(id);
        }
        return Response.success();
    }

    public ResponseDTO edit(Docs docs) {
        if (updateById(docs)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO query(Integer id) {
        Docs docs = getById(id);
        if (docs != null) {
            return Response.success(docs);
        }
        return Response.error();
    }

    public ResponseDTO list(Integer current, Integer size, String oldName, String staffName) {
        if (oldName == null) {
            oldName = "";
        }
        if (staffName == null) {
            staffName = "";
        }
        IPage<StaffDocsVO> config = new Page<>(current, size);
        IPage<StaffDocsVO> page = this.docsMapper.listStaffDocsVO(config, oldName, staffName);
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    public void export(HttpServletResponse response, String filename) throws IOException {
        List<Docs> list = list();
        HutoolExcelUtil.writeExcel(response, list, filename, Docs.class);
    }

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp");

    /**
     * 头像上传（小文件直传 MinIO），复用文件名前缀兼容旧逻辑
     */
    public ResponseDTO upload(MultipartFile file, Integer staffId) throws IOException {
        if (file.isEmpty()) {
            return Response.error(BusinessStatusEnum.FILE_NOT_EXIST);
        }
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extName.toLowerCase())) {
            return Response.error("不支持的文件类型: " + extName);
        }
        String filename = cn.hutool.core.util.IdUtil.fastSimpleUUID().substring(2, 22) + "." + extName;
        storageService.put(filename, file.getBytes());
        Map<String, Object> result = new HashMap<>();
        result.put("name", filename);
        return Response.success(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        List<Docs> list = HutoolExcelUtil.readExcel(inputStream, 1, Docs.class);
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 每日凌晨 4:00 清理无 DB 引用的孤儿文件。
     *
     * <p>「有引用」指对象键出现在 sys_docs.name 或 sys_staff.avatar（头像直传只写对象、
     * 不建 sys_docs 行）中。知识库文档（kb/ 前缀）、分片上传目录与异步任务产物各有独立清理任务，
     * 本任务不越界处理——新增模块的存储前缀需同样登记到 {@code MANAGED_ELSEWHERE_PREFIXES}。</p>
     *
     * <p>文件需满足：最后修改时间超过 24 小时且对象键不在上述引用集合中。</p>
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanOrphanDocsFiles() {
        Set<String> referencedKeys = list().stream()
                .map(Docs::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        staffMapper.selectList(new QueryWrapper<Staff>().select("avatar")
                        .isNotNull("avatar").ne("avatar", ""))
                .stream().map(Staff::getAvatar).forEach(referencedKeys::add);

        long cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000;

        for (String key : storageService.listKeys()) {
            if (referencedKeys.contains(key) || isManagedElsewhere(key)) {
                continue;
            }
            Date lastModified = storageService.getLastModified(key);
            if (lastModified != null && lastModified.getTime() > cutoff) {
                continue;
            }
            storageService.delete(key);
            log.info("清理孤儿文件: {}", key);
        }
    }

    /** 该前缀下对象由其它清理任务负责，孤儿清理跳过 */
    private boolean isManagedElsewhere(String key) {
        for (String prefix : MANAGED_ELSEWHERE_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
