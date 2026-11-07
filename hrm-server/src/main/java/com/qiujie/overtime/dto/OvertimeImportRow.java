package com.qiujie.overtime.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.qiujie.filetask.spi.ImportRow;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 加班导入 Excel 行 DTO。
 *
 * @author qiujie
 */
@Data
public class OvertimeImportRow implements ImportRow {

    /** Excel 行号，由引擎回填，仅用于错误定位 */
    @ExcelIgnore
    private Integer rowNum;

    @ExcelProperty("员工id")
    private Integer staffId;

    @ExcelProperty("上午上班时间")
    private Timestamp morStartTime;

    @ExcelProperty("上午下班时间")
    private Timestamp morEndTime;

    @ExcelProperty("下午上班时间")
    private Timestamp aftStartTime;

    @ExcelProperty("下午下班时间")
    private Timestamp aftEndTime;

    @ExcelProperty("加班日期")
    private Date overtimeDate;
}
