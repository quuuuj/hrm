package com.qiujie.knowledge.service;

import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.lifecycle.DocumentLifecycleService;
import com.qiujie.knowledge.mapper.DocsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库全量回填重建 Runner：
 * 当配置 chat.knowledge.rebuild-on-start=true 时，开机扫描所有 kb_status IS NOT NULL 的 sys_docs，
 * 通过 DocumentLifecycleService.retry() 触发完整 ETL（支持 zstd 解压重读），重建 PG 切片与向量。
 * 默认 false（按需手动开启一次性回填）。
 */
@Component
public class KnowledgeReindexRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeReindexRunner.class);

    @Value("${chat.knowledge.rebuild-on-start:false}")
    private boolean rebuildOnStart;

    @Autowired
    private DocsMapper docsMapper;

    @Autowired(required = false)
    private DocumentLifecycleService lifecycle;

    @Override
    public void run(String... args) {
        if (!rebuildOnStart) {
            return;
        }
        if (lifecycle == null) {
            log.warn("KnowledgeReindexRunner: DocumentLifecycleService not available, skipping rebuild.");
            return;
        }
        log.info("KnowledgeReindexRunner: starting knowledge base reindexing...");
        List<Docs> docsList = docsMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Docs>()
                        .isNotNull("kb_status")
                        .eq("is_deleted", 0));
        int triggered = 0;
        for (Docs doc : docsList) {
            // 重置为 UPLOADED 允许重试
            docsMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Docs>()
                            .set("kb_status", "UPLOADED")
                            .set("failure_reason", null)
                            .eq("id", doc.getId()));
            DocumentLifecycleService.RetryResult r =
                    lifecycle.retry(new DocumentLifecycleService.RetryCommand(doc.getId().longValue()));
            if (r.accepted()) {
                triggered++;
            }
        }
        log.info("KnowledgeReindexRunner: triggered reindex for {} documents.", triggered);
    }
}
