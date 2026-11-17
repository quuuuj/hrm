package com.qiujie.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.document.entity.Document;
import com.qiujie.document.vo.DocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * CAS 认领：把 UPLOADED/READY/FAILED 置 PROCESSING，并清掉上一轮的失败原因与切片快照。
     * <p>
     * 唯一的并发互斥原语——「进入 PROCESSING 的唯一入口是一条要求起始状态非 PROCESSING
     * 的原子 UPDATE」，并发触发只有一个能通过（影响 1 行），其余影响 0 行即退出。
     * PROCESSING 不在允许集合内：在途管道持有该文档，不能借此抢走所有权。
     * <p>
     * 初始提交与重试共用这一条：前者认领刚插入的 UPLOADED 行，后者顺带把终态行退回可跑状态。
     */
    @Update("UPDATE kb_document SET status = 'PROCESSING', failure_reason = NULL, "
          + "chunk_count = NULL, preview_text = NULL, update_time = NOW() "
          + "WHERE id = #{id} AND status IN ('UPLOADED', 'READY', 'FAILED')")
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
