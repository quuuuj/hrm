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

    @Select("select sd.*,ss.name staff_name from sys_staff ss left join sys_docs sd on ss.id = sd.staff_id " +
            "where sd.is_deleted = 0 and sd.old_name like concat('%',#{oldName},'%') and ss.name like concat('%',#{staffName},'%')")
    IPage<StaffDocsVO> listStaffDocsVO(IPage<StaffDocsVO> config, @Param("oldName") String oldName, @Param("staffName") String staffName);

    // ===== 知识库生命周期 SQL（sys_docs 吸收 kb_document）=====

    /** 启动恢复：执行中崩溃的 PROCESSING 文档 → FAILED（UPLOADED 由恢复器续跑，不在此处理）。 */
    @Update("UPDATE sys_docs SET kb_status = 'FAILED', failure_reason = #{reason}, "
          + "update_time = NOW() WHERE kb_status = 'PROCESSING' AND is_deleted = 0")
    int markStaleProcessingAsFailed(String reason);

    /** 启动恢复：未启动的 UPLOADED 文档（供续跑 ETL）。 */
    @Select("SELECT * FROM sys_docs WHERE kb_status = 'UPLOADED' AND is_deleted = 0")
    List<Docs> selectLiveUploaded();

    /** 启动恢复：已逻辑删除的文档（供重跑物理清理）。 */
    @Select("SELECT * FROM sys_docs WHERE is_deleted = 1 AND kb_status IS NOT NULL")
    List<Docs> selectDeleted();

    /**
     * CAS 认领：仅当状态为 UPLOADED/FAILED 且未删除时置 PROCESSING。
     * 唯一的并发互斥原语——PROCESSING ⟺ 恰好一个管道拥有该文档。
     */
    @Update("UPDATE sys_docs SET kb_status = 'PROCESSING', update_time = NOW() "
          + "WHERE id = #{id} AND kb_status IN ('UPLOADED', 'FAILED') AND is_deleted = 0")
    int claimForProcessing(Long id);

    /** CAS 结算：PROCESSING → READY，一并写 chunk_count/preview_text/process_time 并清除失败原因；已删除则失败（防复活）。 */
    @Update("UPDATE sys_docs SET kb_status = 'READY', preview_text = #{previewText}, "
          + "chunk_count = #{chunkCount}, failure_reason = NULL, process_time = NOW(), update_time = NOW() "
          + "WHERE id = #{id} AND kb_status = 'PROCESSING' AND is_deleted = 0")
    int completeProcessing(@Param("id") Long id, @Param("previewText") String previewText,
                           @Param("chunkCount") int chunkCount);

    /** 状态机补偿：PROCESSING → FAILED + failure_reason（已删除则跳过）。 */
    @Update("UPDATE sys_docs SET kb_status = 'FAILED', failure_reason = #{reason}, update_time = NOW() "
          + "WHERE id = #{id} AND kb_status = 'PROCESSING' AND is_deleted = 0")
    int markFailed(@Param("id") Long id, @Param("reason") String reason);

    /** 存活文档引用计数：同物理文件（name）且未删除、排除自身。 */
    @Select("SELECT COUNT(*) FROM sys_docs WHERE name = #{name} AND is_deleted = 0 AND id != #{excludeId}")
    long countLiveByFileName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /** CAS 逻辑删除（幂等：已删除时影响行数为 0）。 */
    @Update("UPDATE sys_docs SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int markDeleted(Long id);
}
