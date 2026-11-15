package com.qiujie.docs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.docs.entity.Docs;
import com.qiujie.docs.vo.StaffDocsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件中心 Mapper（MySQL，sys_docs）。
 * <p>
 * 只承载普通附件；知识库文档在 {@code kb_document}，由 document 模块的 DocumentMapper 负责。
 */
public interface DocsMapper extends BaseMapper<Docs> {

    @Select("<script>" +
            "select sd.*,ss.name staff_name from sys_staff ss left join sys_docs sd on ss.id = sd.staff_id " +
            "where sd.old_name like concat('%',#{oldName},'%') and ss.name like concat('%',#{staffName},'%') " +
            "</script>")
    IPage<StaffDocsVO> listStaffDocsVO(IPage<StaffDocsVO> config,
                                       @Param("oldName") String oldName,
                                       @Param("staffName") String staffName);
}
