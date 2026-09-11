package com.qiujie.overtime.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.overtime.entity.StaffOvertime;
import com.qiujie.overtime.vo.OvertimeMonthVO;
import com.qiujie.overtime.vo.StaffOvertimeVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 员工加班表 Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2024-03-20
 */
public interface StaffOvertimeMapper extends BaseMapper<StaffOvertime> {

    @Select("select * from att_staff_overtime where is_deleted = 0 and staff_id = #{id} and date_format(overtime_date,'%Y%m%d') = #{day}")
    StaffOvertime queryByStaffIdAndDate(@Param("id") Integer id, @Param("day") String day);

    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id " +
            "where ss.is_deleted = 0 and ss.name like concat('%',#{name},'%')")
    IPage<StaffOvertimeVO> listStaffOvertimeVO(IPage<StaffOvertimeVO> config, @Param("name") String name);

    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id " +
            "where ss.is_deleted = 0 and ss.dept_id = #{deptId} and ss.name like concat('%',#{name},'%')")
    IPage<StaffOvertimeVO> listStaffDeptOvertimeVO(IPage<StaffOvertimeVO> config, @Param("name") String name, @Param("deptId") Integer deptId);

    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id where ss.is_deleted = 0")
    List<OvertimeMonthVO> queryOvertimeMonthVO();


    /**
     * 统计员工加班次数、获得的调休天数
     *
     * @param id     员工id
     * @param status 状态
     * @param month  月份
     */
    @Select("select count(*) from att_staff_overtime where is_deleted = 0 and staff_id = #{id} and status = #{status} and date_format(overtime_date,'%Y%m') = #{month} ")
    Integer countTimes(@Param("id") Integer id, @Param("status") Integer status, @Param("month") String month);

}
