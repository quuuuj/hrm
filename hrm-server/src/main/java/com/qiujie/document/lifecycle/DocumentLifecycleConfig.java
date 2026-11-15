package com.qiujie.document.lifecycle;

import com.qiujie.document.lifecycle.port.ChunkVectorStore;
import com.qiujie.document.lifecycle.port.ObjectStore;
import com.qiujie.document.mapper.DocumentMapper;
import com.qiujie.document.service.ChunkService;
import com.qiujie.document.service.DashScopeEmbeddingClient;
import com.qiujie.document.service.DocumentParserService;
import com.qiujie.document.service.TextCleanupService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

/**
 * 知识库文档生命周期模块装配：管道 + 清理 + 启动恢复。
 * 门面 DocumentLifecycleService 由组件扫描注册（@Component + @Transactional）；
 * 两个端口（ChunkVectorStore/ObjectStore）的生产适配器同样由组件扫描注册。
 */
@Configuration
public class DocumentLifecycleConfig {

    @Bean
    public IngestionPipeline ingestionPipeline(DocumentMapper documentMapper,
                                               ChunkVectorStore chunkVectorStore,
                                               ObjectStore objectStore,
                                               DashScopeEmbeddingClient embeddingClient,
                                               DocumentParserService parserService,
                                               TextCleanupService textCleanupService,
                                               ChunkService chunkService) {
        return new IngestionPipeline(documentMapper, chunkVectorStore, objectStore,
                embeddingClient, parserService, textCleanupService, chunkService);
    }

    @Bean
    public DocumentPurgeHandler documentPurgeHandler(ChunkVectorStore chunkVectorStore,
                                                     ObjectStore objectStore) {
        return new DocumentPurgeHandler(chunkVectorStore, objectStore);
    }

    /** 启动恢复：仅在知识库启用时注册（两个崩溃窗口 + 孤儿索引核对）。 */
    @Bean
    @ConditionalOnExpression("${knowledge.enabled:false}")
    public StartupRecovery startupRecovery(DocumentMapper documentMapper,
                                           IngestionPipeline pipeline,
                                           ChunkVectorStore chunkVectorStore,
                                           @Qualifier("fileTaskExecutor") Executor executor) {
        return new StartupRecovery(documentMapper, pipeline, chunkVectorStore, executor);
    }
}
