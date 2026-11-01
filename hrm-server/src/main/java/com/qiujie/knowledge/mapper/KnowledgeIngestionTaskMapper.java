package com.qiujie.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.knowledge.entity.KnowledgeIngestionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文档摄入异步任务 Mapper
 *
 * @author quuj
 */
@Mapper
public interface KnowledgeIngestionTaskMapper extends BaseMapper<KnowledgeIngestionTask> {

    /** 作废文档的在途任务（删除文档时防 worker 继续写 PG）。 */
    @Update("UPDATE kb_ingestion_task SET status = 'CANCELLED', finish_time = NOW() "
          + "WHERE docs_id = #{docsId} AND status IN ('PENDING', 'RUNNING')")
    int cancelActiveTasks(@Param("docsId") Long docsId);

    /** 启动回收：遗留 RUNNING 任务 → FAILED。 */
    @Update("UPDATE kb_ingestion_task SET status = 'FAILED', last_error = #{error}, finish_time = NOW() "
          + "WHERE status = 'RUNNING'")
    int markStaleRunningAsFailed(@Param("error") String error);
}
