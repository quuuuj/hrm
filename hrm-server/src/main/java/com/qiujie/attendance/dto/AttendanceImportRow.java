package com.qiujie.attendance.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.qiujie.filetask.spi.ImportRow;
import lombok.Data;

import java.util.Date;

@Data
public class AttendanceImportRow implements ImportRow {

    /** Excel 行号，由引擎回填，仅用于错误定位 */
    @ExcelIgnore
    private Integer rowNum;

    @ExcelProperty("员工id")
    private Integer staffId;

    @ExcelProperty("上午上班时间")
    private Date morStartTime;

    @ExcelProperty("上午下班时间")
    private Date morEndTime;

    @ExcelProperty("下午上班时间")
    private Date aftStartTime;

    @ExcelProperty("下午下班时间")
    private Date aftEndTime;

    @ExcelProperty("考勤日期")
    private Date attendanceDate;
}
