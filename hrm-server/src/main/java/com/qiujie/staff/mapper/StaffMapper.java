package com.qiujie.staff.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.staff.entity.Staff;
import com.qiujie.staff.vo.StaffDeptVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-01-27
 */


public interface StaffMapper extends BaseMapper<Staff> {

    @Select("select ss.id , ss.code, ss.name, ss.gender, ss.pwd password, ss.avatar, ss.birthday, ss.phone, ss.address, ss.remark,ss.status, ss.dept_id,sd.name dept_name from sys_staff ss left join sys_dept sd on ss.dept_id = sd.id where ss.is_deleted = 0 and ss.code = #{code}")
    StaffDeptVO queryByCode(@Param("code") String code);

    @Select("select ss.id , ss.code, ss.name, ss.gender, ss.avatar, ss.birthday, ss.phone, ss.address, ss.remark,ss.status, ss.dept_id,sd.name dept_name from sys_staff ss left join sys_dept sd on ss.dept_id = sd.id where ss.is_deleted = 0 and ss.id = #{id}")
    StaffDeptVO queryInfo(@Param("id") Integer id);

    @Select("select ss.id , ss.code, ss.name, ss.gender, ss.pwd password, ss.avatar, ss.birthday, ss.phone, ss.address, ss.remark,ss.status, ss.dept_id,sd.name dept_name from sys_staff ss left join sys_dept sd on ss.dept_id = sd.id where ss.is_deleted = 0")
    List<StaffDeptVO> queryStaffDeptVO();

    @Select("select ss.* from sys_staff ss inner join per_staff_role psr on ss.id = psr.staff_id inner join per_role pr on psr.role_id = pr.id where ss.is_deleted = 0 and pr.code = #{code}")
    List<Staff> queryByRole(@Param("code") String code);

}
