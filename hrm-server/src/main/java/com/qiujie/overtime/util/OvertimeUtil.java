package com.qiujie.overtime.util;

import cn.hutool.core.date.DateUtil;
import com.qiujie.overtime.entity.Overtime;
import com.qiujie.overtime.entity.StaffOvertime;
import com.qiujie.overtime.enums.OvertimeEnum;
import com.qiujie.overtime.enums.OvertimeStatusEnum;
import com.qiujie.salary.entity.Salary;
import com.qiujie.staff.entity.Staff;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 加班计算工具类——收敛加班规则的纯静态计算，输入即输出，无任何 I/O。
 * <p>
 * 引用数据（员工 / 部门加班配置 / 最近薪资）由调用方加载后传入，
 * 兜底语义：配置缺失、无薪资记录或日薪时薪字段为空 → 不崩，{@code overtimeSalary} 记 null。
 * <p>
 * 隐藏的复杂性：
 * <ul>
 *   <li><b>时长</b>：每天上午/下午四个时间点 → 小时数</li>
 *   <li><b>日期分类</b>：节假日 / 周末 / 工作日 → {@link OvertimeEnum}</li>
 *   <li><b>调休判定</b>：休息日 + 配置允许调休 + 时长 ≥ 8h → {@link OvertimeStatusEnum#TIME_OFF}</li>
 *   <li><b>计薪规则</b>：按小时（≥2h）或按天（≥8h），时薪/日薪 × 倍数 + 奖金</li>
 * </ul>
 * 输出结果为只读 {@code Map<String, Object>}，key 为 {@code typeNum} / {@code status} /
 * {@code totalOvertime} / {@code overtimeSalary}。零 mock 可直接单测。
 */
public final class OvertimeUtil {

    private static final BigDecimal MIN_HOURLY_OVERTIME = new BigDecimal("2");
    private static final BigDecimal MIN_DAILY_OVERTIME = new BigDecimal("8");

    private OvertimeUtil() {
    }

    /**
     * 纯计算——仅依赖传入的引用数据，可直接零 mock 单测。
     * <p>
     * 规则（与历史实现保持一致）：
     * <ol>
     *   <li>时长 = (morEnd-morStart) + (aftEnd-aftStart)，按小时</li>
     *   <li>类型：src 已带类型则信任之；否则按日期分类（节假日→HOLIDAY；周末→DAY_OFF；否则→WORKDAY）</li>
     *   <li>调休：DAY_OFF 且配置 timeOffFlag==1 且时长≥8h → TIME_OFF（不计薪）</li>
     *   <li>计薪：countType==0 且时长≥2h → 时薪×倍数×时长+奖金；countType==1 且时长≥8h → 日薪×倍数+奖金；否则 0</li>
     * </ol>
     *
     * @param isHoliday 节假日判定谓词（非空）
     */
    public static Map<String, Object> calculate(StaffOvertime src, Staff staff,
                                                Overtime config, Salary latestSalary,
                                                Predicate<Date> isHoliday) {
        if (src == null || src.getOvertimeDate() == null) {
            return empty();
        }
        BigDecimal total = totalOvertime(src);
        OvertimeEnum type = resolveType(src, isHoliday);
        if (config != null && isTimeOff(type, config, total)) {
            return result(type, OvertimeStatusEnum.TIME_OFF, total, null);
        }
        BigDecimal salary = config != null && latestSalary != null
                ? calculateSalary(total, config, latestSalary) : null;
        OvertimeStatusEnum status = salary == null && config == null
                ? OvertimeStatusEnum.NORMAL
                : OvertimeStatusEnum.OVERTIME;
        return result(type, status, total, salary);
    }

    /**
     * 记录适用的加班类型：src 已明确类型则信任（单条设置场景手动指定）；否则按日期分类。
     * <p>
     * 加班配置按「部门 + 类型」查找，调用方加载配置前需先用本方法定类型，保证与计算口径一致。
     */
    public static OvertimeEnum resolveType(StaffOvertime src, Predicate<Date> isHoliday) {
        if (src.getTypeNum() != null) {
            return src.getTypeNum();
        }
        return classifyType(src.getOvertimeDate(), isHoliday);
    }

    /**
     * 日期分类：节假日 → HOLIDAY；周末 → DAY_OFF；否则 → WORKDAY。
     */
    public static OvertimeEnum classifyType(Date date, Predicate<Date> isHoliday) {
        if (isHoliday.test(date)) {
            return OvertimeEnum.HOLIDAY_OVERTIME;
        }
        if (DateUtil.isWeekend(date)) {
            return OvertimeEnum.DAY_OFF_OVERTIME;
        }
        return OvertimeEnum.WORKDAY_OVERTIME;
    }

    // ==================== 私有 ====================

    private static boolean isTimeOff(OvertimeEnum type, Overtime config, BigDecimal total) {
        return type == OvertimeEnum.DAY_OFF_OVERTIME
                && config.getTimeOffFlag() != null && config.getTimeOffFlag() == 1
                && total.compareTo(MIN_DAILY_OVERTIME) >= 0;
    }

    private static BigDecimal calculateSalary(BigDecimal total, Overtime config, Salary salary) {
        if (config.getCountType() != null && config.getCountType() == 1) {
            // 按天：当日加班时长 ≥ 8h 才有
            if (total.compareTo(MIN_DAILY_OVERTIME) < 0) {
                return BigDecimal.ZERO;
            }
            // 日薪缺失与无薪资记录同一兜底：工资记 null
            BigDecimal daySalary = salary.getDaySalary();
            if (daySalary == null) {
                return null;
            }
            return daySalary.multiply(config.getSalaryMultiple()).add(config.getBonus());
        }
        // 按小时：当日加班时长 ≥ 2h 才有
        if (total.compareTo(MIN_HOURLY_OVERTIME) < 0) {
            return BigDecimal.ZERO;
        }
        // 时薪缺失与无薪资记录同一兜底：工资记 null
        BigDecimal hourSalary = salary.getHourSalary();
        if (hourSalary == null) {
            return null;
        }
        return hourSalary.multiply(config.getSalaryMultiple()).multiply(total).add(config.getBonus());
    }

    private static BigDecimal totalOvertime(StaffOvertime src) {
        if (src.getMorStartTime() == null || src.getMorEndTime() == null
                || src.getAftStartTime() == null || src.getAftEndTime() == null) {
            return BigDecimal.ZERO;
        }
        long morDiff = src.getMorEndTime().getTime() - src.getMorStartTime().getTime();
        long aftDiff = src.getAftEndTime().getTime() - src.getAftStartTime().getTime();
        return BigDecimal.valueOf((morDiff + aftDiff) / (1000 * 60 * 60.0));
    }

    private static Map<String, Object> empty() {
        return result(null, OvertimeStatusEnum.NORMAL, BigDecimal.ZERO, null);
    }

    /** 组装计算结果——不用 {@code Map.of}，value 允许为 {@code null} */
    private static Map<String, Object> result(OvertimeEnum type, OvertimeStatusEnum status,
                                              BigDecimal total, BigDecimal salary) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("typeNum", type);
        map.put("status", status);
        map.put("totalOvertime", total);
        map.put("overtimeSalary", salary);
        return Collections.unmodifiableMap(map);
    }
}
