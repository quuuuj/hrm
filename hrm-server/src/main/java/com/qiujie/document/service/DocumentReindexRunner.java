package com.qiujie.document.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.qiujie.document.entity.Document;
import com.qiujie.document.enums.DocumentStatusEnum;
import com.qiujie.document.lifecycle.DocumentLifecycleService;
import com.qiujie.document.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库全量回填重建 Runner：
 * 当配置 knowledge.rebuild-on-start=true 时，开机扫描 kb_document 全表，
 * 通过 DocumentLifecycleService.retry() 触发完整 ETL（支持 zstd 解压重读），重建 PG 切片与向量。
 * 默认 false（按需手动开启一次性回填）。
 */
@Component
public class DocumentReindexRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentReindexRunner.class);

    @Value("${knowledge.rebuild-on-start}")
    private boolean rebuildOnStart;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentLifecycleService lifecycle;

    @Override
    public void run(String... args) {
        if (!rebuildOnStart) {
            return;
        }
        log.info("DocumentReindexRunner: starting knowledge base reindexing...");
        List<Document> documents = documentMapper.selectList(null);
        int triggered = 0;
        for (Document document : documents) {
            // 重置为 UPLOADED 允许重试
            documentMapper.update(null,
                    new UpdateWrapper<Document>()
                            .set("status", DocumentStatusEnum.UPLOADED.name())
                            .set("failure_reason", null)
                            .eq("id", document.getId()));
            DocumentLifecycleService.RetryResult r =
                    lifecycle.retry(new DocumentLifecycleService.RetryCommand(document.getId()));
            if (r.accepted()) {
                triggered++;
            }
        }
        log.info("DocumentReindexRunner: triggered reindex for {} documents.", triggered);
    }
}
