package com.qiujie.leave.approval;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.attendance.entity.Attendance;
import com.qiujie.attendance.enums.AttendanceStatusEnum;
import com.qiujie.attendance.service.AttendanceService;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.leave.entity.StaffLeave;
import com.qiujie.leave.enums.LeaveEnum;
import com.qiujie.overtime.entity.StaffOvertime;
import com.qiujie.overtime.enums.OvertimeStatusEnum;
import com.qiujie.overtime.mapper.StaffOvertimeMapper;
import com.qiujie.security.ServiceException;
import com.qiujie.leave.service.StaffLeaveService;
import com.qiujie.util.DatetimeUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;

/**
 * 经理审批执行监听器——流程转译 + 考勤/调休同步副作用。
 * <p>
 * 审批通过后逐日遍历请假区间：
 * <ul>
 *   <li>非工作日（周末/节假日）跳过，补班日照常记录</li>
 *   <li>{@code TIME_OFF}：考勤状态为调休，并删除该员工最早的一条调休记录</li>
 *   <li>其他类型：考勤状态为休假</li>
 *   <li>已有考勤记录则更新，否则新增</li>
 * </ul>
 * </p>
 */
@Component
public class ManagerApproveListener implements ExecutionListener {

    @Autowired
    private StaffLeaveService staffLeaveService;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private StaffOvertimeMapper staffOvertimeMapper;
    @Autowired
    private DatetimeUtil datetimeUtil;

    @Override
    @Transactional
    public void notify(DelegateExecution execution) {
        StaffLeave staffLeave = staffLeaveService.getOne(new QueryWrapper<StaffLeave>()
                .eq("id", Integer.valueOf(execution.getProcessInstanceBusinessKey())));
        for (int i = 0; i < staffLeave.getDays(); i++) {
            Date attendanceDate = DateUtil.offsetDay(staffLeave.getStartDate(), i).toSqlDate();
            // 工作日判定统一走 isWorkday（补班日算工作日；周末本就休息、节假日无需考勤）
            if (!datetimeUtil.isWorkday(attendanceDate)) {
                continue;
            }
            Attendance attendance = new Attendance()
                    .setAttendanceDate(attendanceDate)
                    .setStaffId(staffLeave.getStaffId());
            if (staffLeave.getTypeNum() == LeaveEnum.TIME_OFF) {
                // 调休：考勤状态为调休，并扣减员工一条调休记录
                attendance.setStatus(AttendanceStatusEnum.TIME_OFF);
                staffOvertimeMapper.delete(new QueryWrapper<StaffOvertime>()
                        .eq("staff_id", staffLeave.getStaffId())
                        .eq("status", OvertimeStatusEnum.TIME_OFF)
                        .orderByAsc("overtime_date").last("limit 1"));
            } else {
                attendance.setStatus(AttendanceStatusEnum.LEAVE);
            }
            Attendance existing = attendanceService.getOne(new QueryWrapper<Attendance>()
                    .eq("staff_id", attendance.getStaffId())
                    .eq("attendance_date", attendance.getAttendanceDate()));
            if (existing != null) {
                attendance.setId(existing.getId());
                if (!attendanceService.updateById(attendance)) {
                    throw new ServiceException(BusinessStatusEnum.ERROR);
                }
            } else {
                if (!attendanceService.save(attendance)) {
                    throw new ServiceException(BusinessStatusEnum.ERROR);
                }
            }
        }
    }
}
