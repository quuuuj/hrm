package com.qiujie.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.salary.entity.Salary;
import com.qiujie.salary.vo.StaffSalaryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-04-06
 */
public interface SalaryMapper extends BaseMapper<Salary> {


    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name,si.per_social_pay social_pay,si.per_house_pay house_pay " +
            "from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id left join soc_insurance si on ss.id = si.staff_id where ss.is_deleted = 0")
    List<StaffSalaryVO> queryStaffSalaryVO();


    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name,si.per_social_pay social_pay,si.per_house_pay house_pay " +
            "from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id left join soc_insurance si on ss.id = si.staff_id where ss.is_deleted = 0 and ss.name like concat('%',#{name},'%')")
        IPage<StaffSalaryVO> listStaffSalaryVO(IPage<StaffSalaryVO> config, @Param("name") String name);

    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name,si.per_social_pay social_pay,si.per_house_pay house_pay " +
            "from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id left join soc_insurance si on ss.id = si.staff_id where ss.is_deleted = 0 and ss.dept_id = #{deptId} and ss.name like concat('%',#{name},'%')")
    IPage<StaffSalaryVO> listStaffDeptSalaryVO(IPage<StaffSalaryVO> config, @Param("name") String name, @Param("deptId")Integer deptId);

    /**
     * 合计员工当前月的加班工资（薪资域自持查询，避免反向依赖 overtime 模块）。
     */
    @Select("select sum(overtime_salary) from att_staff_overtime where is_deleted = 0 and staff_id = #{id} and date_format(overtime_date,'%Y%m') = #{month}")
    BigDecimal sumMonthOvertimeSalary(@Param("id") Integer id, @Param("month") String month);

}
