package com.qiujie.document.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.common.storage.MinioStorageService;
import com.qiujie.document.entity.Document;
import com.qiujie.document.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库对象孤儿清理：删除 kb/ 前缀下无 kb_document 引用的对象。
 * <p>
 * 只扫本模块自己的前缀——文件中心的孤儿清理（{@code DocsService.cleanOrphanDocsFiles}）
 * 已把 kb/ 列入跳过清单，两边互不越界。
 * <p>
 * 对象键在删除流程中随行一起消失，删除时对象删除失败会留下无主对象；这里按 24 小时
 * 的宽限期兜底回收。
 */
@Component
public class DocumentCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DocumentCleanupJob.class);

    private static final String PREFIX = "kb/";

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private DocumentMapper documentMapper;

    @Scheduled(cron = "0 30 4 * * ?")
    public void cleanOrphanObjects() {
        Set<String> referencedKeys = documentMapper.selectList(
                        new QueryWrapper<Document>().select("name"))
                .stream()
                .map(Document::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        long cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000;

        for (String key : storageService.listKeys()) {
            if (!key.startsWith(PREFIX) || referencedKeys.contains(key)) {
                continue;
            }
            Date lastModified = storageService.getLastModified(key);
            if (lastModified != null && lastModified.getTime() > cutoff) {
                continue;
            }
            storageService.delete(key);
            log.info("清理知识库孤儿文件: {}", key);
        }
    }
}
