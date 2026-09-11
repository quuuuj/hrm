package com.qiujie.attendance.vo;

import lombok.Data;

@Data
public class AttendanceMonthSummaryVO {

    private Integer staffId;

    private Integer lateTimes;

    private Integer leaveEarlyTimes;

    private Integer absenteeismTimes;

    private Integer leaveDays;

    private Integer timeOffDays;
}
