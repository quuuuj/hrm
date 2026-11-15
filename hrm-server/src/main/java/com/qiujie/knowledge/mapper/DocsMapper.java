package com.qiujie.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.vo.StaffDocsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-02-24
 */
public interface DocsMapper extends BaseMapper<Docs> {

    @Select("<script>" +
            "select sd.*,ss.name staff_name from sys_staff ss left join sys_docs sd on ss.id = sd.staff_id " +
            "where sd.old_name like concat('%',#{oldName},'%') and ss.name like concat('%',#{staffName},'%') " +
            "<if test='onlyKb != null and onlyKb == true'> and sd.kb_status is not null </if>" +
            "<if test='kbStatus != null and kbStatus != \"\"'> and sd.kb_status = #{kbStatus} </if>" +
            "</script>")
    IPage<StaffDocsVO> listStaffDocsVO(IPage<StaffDocsVO> config,
                                       @Param("oldName") String oldName,
                                       @Param("staffName") String staffName,
                                       @Param("onlyKb") Boolean onlyKb,
                                       @Param("kbStatus") String kbStatus);

    // ===== 知识库生命周期 SQL（sys_docs 吸收 kb_document）=====

    /** 启动恢复：执行中崩溃的 PROCESSING 文档 → FAILED（UPLOADED 由恢复器续跑，不在此处理）。 */
    @Update("UPDATE sys_docs SET kb_status = 'FAILED', failure_reason = #{reason}, "
          + "update_time = NOW() WHERE kb_status = 'PROCESSING'")
    int markStaleProcessingAsFailed(String reason);

    /** 启动恢复：未启动的 UPLOADED 文档（供续跑 ETL）。 */
    @Select("SELECT * FROM sys_docs WHERE kb_status = 'UPLOADED'")
    List<Docs> selectLiveUploaded();

    /** 启动恢复：全部文档 id，用于识别 PG 侧失去主表行引用的孤儿索引。 */
    @Select("SELECT id FROM sys_docs")
    List<Long> selectAllIds();

    /**
     * CAS 认领：仅当状态为 UPLOADED/FAILED 时置 PROCESSING。
     * 唯一的并发互斥原语——PROCESSING ⟺ 恰好一个管道拥有该文档。
     */
    @Update("UPDATE sys_docs SET kb_status = 'PROCESSING', update_time = NOW() "
          + "WHERE id = #{id} AND kb_status IN ('UPLOADED', 'FAILED')")
    int claimForProcessing(Long id);

    /** CAS 结算：PROCESSING → READY，一并写 chunk_count/preview_text/process_time 并清除失败原因；已删除（行不存在）则失败。 */
    @Update("UPDATE sys_docs SET kb_status = 'READY', preview_text = #{previewText}, "
          + "chunk_count = #{chunkCount}, failure_reason = NULL, process_time = NOW(), update_time = NOW() "
          + "WHERE id = #{id} AND kb_status = 'PROCESSING'")
    int completeProcessing(@Param("id") Long id, @Param("previewText") String previewText,
                           @Param("chunkCount") int chunkCount);

    /** 状态机补偿：PROCESSING → FAILED + failure_reason（已删除则跳过）。 */
    @Update("UPDATE sys_docs SET kb_status = 'FAILED', failure_reason = #{reason}, update_time = NOW() "
          + "WHERE id = #{id} AND kb_status = 'PROCESSING'")
    int markFailed(@Param("id") Long id, @Param("reason") String reason);
}
