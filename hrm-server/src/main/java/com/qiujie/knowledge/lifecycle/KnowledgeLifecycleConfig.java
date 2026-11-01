package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.mapper.KnowledgeIngestionTaskMapper;
import com.qiujie.knowledge.mapper.DocsMapper;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DashScopeEmbeddingClient;
import com.qiujie.knowledge.service.KnowledgeParserService;
import com.qiujie.knowledge.service.TextCleanupService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executor;

/**
 * 文档生命周期模块装配：门面 + 管道 + 清理 + 启动恢复。
 * 两个端口（ChunkVectorStore/ObjectStore）的生产适配器由组件扫描注册；嵌入直接使用 DashScopeEmbeddingClient。
 */
@Configuration
public class KnowledgeLifecycleConfig {

    @Bean
    public IngestionPipeline ingestionPipeline(DocsMapper docsMapper,
                                               KnowledgeIngestionTaskMapper taskMapper,
                                               ChunkVectorStore chunkVectorStore,
                                               ObjectStore objectStore,
                                               DashScopeEmbeddingClient embeddingClient,
                                               KnowledgeParserService parserService,
                                               TextCleanupService textCleanupService,
                                               ChunkService chunkService) {
        return new IngestionPipeline(docsMapper, taskMapper, chunkVectorStore, objectStore,
                embeddingClient, parserService, textCleanupService, chunkService);
    }

    @Bean
    public KnowledgePurgeHandler docsPurgeHandler(DocsMapper docsMapper,
                                                     ChunkVectorStore chunkVectorStore,
                                                     ObjectStore objectStore) {
        return new KnowledgePurgeHandler(docsMapper, chunkVectorStore, objectStore);
    }

    @Bean
    public KnowledgeLifecycleService docsLifecycleService(DocsMapper docsMapper,
                                                             KnowledgeIngestionTaskMapper taskMapper,
                                                             TransactionTemplate transactionTemplate,
                                                             @Qualifier("fileTaskExecutor") Executor ingestExecutor,
                                                             IngestionPipeline pipeline,
                                                             KnowledgePurgeHandler purgeHandler) {
        return new KnowledgeLifecycleService(docsMapper, taskMapper, transactionTemplate,
                ingestExecutor, pipeline, purgeHandler);
    }

    /** 启动恢复：仅在知识库启用时注册（四步崩溃窗口 + 孤儿索引核对）。 */
    @Bean
    @ConditionalOnExpression("${knowledge.enabled:false}")
    public StartupRecovery startupRecovery(DocsMapper docsMapper,
                                           KnowledgeIngestionTaskMapper taskMapper,
                                           IngestionPipeline pipeline,
                                           KnowledgePurgeHandler purgeHandler,
                                           ChunkVectorStore chunkVectorStore,
                                           @Qualifier("fileTaskExecutor") Executor executor) {
        return new StartupRecovery(docsMapper, taskMapper, pipeline, purgeHandler,
                chunkVectorStore, executor);
    }
}
