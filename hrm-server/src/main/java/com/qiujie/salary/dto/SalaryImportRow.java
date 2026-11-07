package com.qiujie.salary.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.qiujie.filetask.spi.ImportRow;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资导入 Excel 行 DTO。
 * <p>
 * 只收人工录入项（基础工资、补贴、奖金、备注），日薪/时薪由服务端按基础工资折算，
 * 扣款与总工资在查询时按考勤与加班实时计算，不参与导入。
 *
 * @author qiujie
 */
@Data
public class SalaryImportRow implements ImportRow {

    /** Excel 行号，由引擎回填，仅用于错误定位 */
    @ExcelIgnore
    private Integer rowNum;

    @ExcelProperty("员工id")
    private Integer staffId;

    @ExcelProperty("月份")
    private String month;

    @ExcelProperty("基础工资")
    private BigDecimal baseSalary;

    @ExcelProperty("生活补贴")
    private BigDecimal subsidy;

    @ExcelProperty("奖金")
    private BigDecimal bonus;

    @ExcelProperty("备注")
    private String remark;
}
