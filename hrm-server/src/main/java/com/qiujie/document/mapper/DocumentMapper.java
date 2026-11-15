package com.qiujie.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.document.entity.Document;
import com.qiujie.document.vo.DocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 知识库文档 Mapper（MySQL，kb_document）。
 * <p>
 * 状态迁移一律用 CAS：{@code PROCESSING} 表示恰好一个管道拥有该文档，
 * 并发触发与崩溃恢复重发都靠这一条件互斥。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /** 列表查询：按原文件名模糊匹配、按状态过滤，带上上传者姓名。 */
    @Select("<script>" +
            "select d.*, ss.name staff_name from kb_document d " +
            "left join sys_staff ss on ss.id = d.staff_id " +
            "where d.old_name like concat('%',#{oldName},'%') " +
            "<if test='status != null and status != \"\"'> and d.status = #{status} </if>" +
            "order by d.id desc" +
            "</script>")
    IPage<DocumentVO> listDocumentVO(IPage<DocumentVO> config,
                                     @Param("oldName") String oldName,
                                     @Param("status") String status);

    /** 启动恢复：执行中崩溃的 PROCESSING 文档 → FAILED（UPLOADED 由恢复器续跑，不在此处理）。 */
    @Update("UPDATE kb_document SET status = 'FAILED', failure_reason = #{reason}, "
          + "update_time = NOW() WHERE status = 'PROCESSING'")
    int markStaleProcessingAsFailed(String reason);

    /** 启动恢复：未启动的 UPLOADED 文档（供续跑 ETL）。 */
    @Select("SELECT * FROM kb_document WHERE status = 'UPLOADED'")
    List<Document> selectLiveUploaded();

    /** 启动恢复：全部文档 id，用于识别 PG 侧失去主表行引用的孤儿索引。 */
    @Select("SELECT id FROM kb_document")
    List<Long> selectAllIds();

    /**
     * CAS 认领：仅当状态为 UPLOADED/FAILED 时置 PROCESSING。
     * 唯一的并发互斥原语——PROCESSING ⟺ 恰好一个管道拥有该文档。
     */
    @Update("UPDATE kb_document SET status = 'PROCESSING', update_time = NOW() "
          + "WHERE id = #{id} AND status IN ('UPLOADED', 'FAILED')")
    int claimForProcessing(Long id);

    /** CAS 结算：PROCESSING → READY，一并写 chunk_count/preview_text/process_time 并清除失败原因；已删除（行不存在）则失败。 */
    @Update("UPDATE kb_document SET status = 'READY', preview_text = #{previewText}, "
          + "chunk_count = #{chunkCount}, failure_reason = NULL, process_time = NOW(), update_time = NOW() "
          + "WHERE id = #{id} AND status = 'PROCESSING'")
    int completeProcessing(@Param("id") Long id,
                           @Param("previewText") String previewText,
                           @Param("chunkCount") Integer chunkCount);

    /** CAS 失败结算：PROCESSING → FAILED + 原因；行已删除或无在途处理则无影响。 */
    @Update("UPDATE kb_document SET status = 'FAILED', failure_reason = #{reason}, update_time = NOW() "
          + "WHERE id = #{id} AND status = 'PROCESSING'")
    int markFailed(@Param("id") Long id, @Param("reason") String reason);
}
