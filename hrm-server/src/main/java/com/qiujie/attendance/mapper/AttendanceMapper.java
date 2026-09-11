package com.qiujie.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.attendance.entity.Attendance;
import com.qiujie.attendance.vo.AttendanceMonthSummaryVO;
import com.qiujie.attendance.vo.AttendanceMonthVO;
import com.qiujie.attendance.vo.StaffAttendanceVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Date;
import java.util.Collection;
import java.util.List;

public interface AttendanceMapper extends BaseMapper<Attendance> {

    @Select("select * from att_attendance where is_deleted = 0 and staff_id = #{id} and attendance_date = #{day}")
    Attendance queryByStaffIdAndDate(@Param("id") Integer id, @Param("day") Date day);

    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id " +
            "where ss.is_deleted = 0 and ss.name like concat('%',#{name},'%')")
    IPage<StaffAttendanceVO> listStaffAttendanceVO(IPage<StaffAttendanceVO> config, @Param("name") String name);

    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id " +
            "where ss.is_deleted = 0 and ss.dept_id = #{deptId} and ss.name like concat('%',#{name},'%')")
    IPage<StaffAttendanceVO> listStaffDeptAttendanceVO(IPage<StaffAttendanceVO> config, @Param("name") String name, @Param("deptId") Integer deptId);

    @Select("select ss.id staff_id,ss.dept_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name from sys_staff ss inner join sys_dept sd on ss.dept_id = sd.id where ss.is_deleted = 0")
    IPage<AttendanceMonthVO> queryAttendanceMonthVOPage(IPage<AttendanceMonthVO> page);

    @Select("<script><![CDATA[select count(*) from att_attendance where is_deleted = 0 and staff_id = #{id} and status = #{status} and attendance_date >= #{start} and attendance_date < #{end}]]></script>")
    Integer countTimes(@Param("id") Integer id, @Param("status") Integer status,
                       @Param("start") Date start, @Param("end") Date end);

    @Select("<script><![CDATA[select attendance_date from att_attendance where is_deleted = 0 and staff_id = #{id} and status=#{status} and attendance_date >= #{start} and attendance_date < #{end}]]></script>")
    List<Date> queryLeaveDate(@Param("id") Integer id, @Param("status") Integer status,
                              @Param("start") Date start, @Param("end") Date end);

    @Select({
            "<script>",
            "select * from att_attendance",
            "where is_deleted = 0",
            "and staff_id in",
            "<foreach collection='staffIds' item='staffId' open='(' separator=',' close=')'>",
            "#{staffId}",
            "</foreach>",
            "and attendance_date in",
            "<foreach collection='dates' item='date' open='(' separator=',' close=')'>",
            "#{date}",
            "</foreach>",
            "</script>"
    })
    List<Attendance> queryByStaffIdsAndDates(@Param("staffIds") Collection<Integer> staffIds,
                                             @Param("dates") Collection<Date> dates);

    @Select("<script><![CDATA[select staff_id as staffId, " +
            "sum(case when status = 1 then 1 else 0 end) as lateTimes, " +
            "sum(case when status = 2 then 1 else 0 end) as leaveEarlyTimes, " +
            "sum(case when status = 3 then 1 else 0 end) as absenteeismTimes, " +
            "sum(case when status = 4 and dayofweek(attendance_date) not in (1,7) then 1 else 0 end) as leaveDays, " +
            "sum(case when status = 5 then 1 else 0 end) as timeOffDays " +
            "from att_attendance " +
            "where is_deleted = 0 and attendance_date >= #{start} and attendance_date < #{end} " +
            "group by staff_id]]></script>")
    List<AttendanceMonthSummaryVO> queryMonthSummary(@Param("start") Date start, @Param("end") Date end);

    @Select({
            "<script>",
            "<![CDATA[select staff_id as staffId, " +
            "sum(case when status = 1 then 1 else 0 end) as lateTimes, " +
            "sum(case when status = 2 then 1 else 0 end) as leaveEarlyTimes, " +
            "sum(case when status = 3 then 1 else 0 end) as absenteeismTimes, " +
            "sum(case when status = 4 and dayofweek(attendance_date) not in (1,7) then 1 else 0 end) as leaveDays, " +
            "sum(case when status = 5 then 1 else 0 end) as timeOffDays " +
            "from att_attendance " +
            "where is_deleted = 0 and attendance_date >= #{start} and attendance_date < #{end} ]]>",
            "and staff_id in ",
            "<foreach collection='staffIds' item='staffId' open='(' separator=',' close=')'>",
            "#{staffId}",
            "</foreach> " +
            "group by staff_id",
            "</script>"
    })
    List<AttendanceMonthSummaryVO> queryMonthSummaryByStaffIds(@Param("start") Date start, @Param("end") Date end,
                                                                @Param("staffIds") Collection<Integer> staffIds);
}
