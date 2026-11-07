package com.qiujie.salary.service;
import com.qiujie.filetask.service.FileUploadService;
import com.qiujie.filetask.spi.ExportProcessor;
import com.qiujie.filetask.spi.ImportProcessor;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.salary.dto.SalaryImportRow;
import com.qiujie.salary.entity.Salary;
import com.qiujie.salary.entity.SalaryDeduct;
import com.qiujie.attendance.enums.AttendanceStatusEnum;
import com.qiujie.salary.enums.DeductEnum;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.entity.FileTaskError;
import com.qiujie.filetask.service.FileTaskService;
import com.qiujie.attendance.mapper.AttendanceMapper;
import com.qiujie.salary.mapper.SalaryMapper;
import com.qiujie.staff.entity.Staff;
import com.qiujie.staff.mapper.StaffMapper;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.salary.util.SalaryUtil;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.staff.service.SecurityUtil;
import com.qiujie.salary.vo.StaffSalaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-04-06
 */
@Service
public class SalaryService extends ServiceImpl<SalaryMapper, Salary> {

    /** 导入月份格式 yyyyMM */
    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{6}$");

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private SalaryDeductService salaryDeductService;


    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private StaffMapper staffMapper;


    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private SecurityUtil securityUtil;

    public ResponseDTO add(Salary salary) {
        if (save(salary)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO delete(Integer id) {
        if (removeById(id)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO edit(Salary salary) {
        if (updateById(salary)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        Salary salary = getById(id);
        if (salary != null) {
            return Response.success(salary);
        }
        return Response.error();
    }


    public ResponseDTO list(Integer current, Integer size, String name, Integer deptId, String month) {
        IPage<StaffSalaryVO> config = new Page<>(current, size);
        // 解决当搜索条件为空时，默认查询所有数据
        if (name == null) {
            name = "";
        }
        IPage<StaffSalaryVO> page;
        if (deptId == null) {
            page = this.salaryMapper.listStaffSalaryVO(config, name);
        } else {
            page = this.salaryMapper.listStaffDeptSalaryVO(config, name, deptId);
        }
        // 如果没有指明月份，就默认显示当前月份
        if (month == null) {
            Date datetime = new Date(System.currentTimeMillis());
            month = DateUtil.format(datetime, "yyyyMM");
        }
        List<StaffSalaryVO> staffSalaryVOList = page.getRecords();
        setSalaryInfo(month, staffSalaryVOList);
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffSalaryVOList);
        map.put("month", month);
        return Response.success(map);
    }

    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response, String month,String filename) throws IOException {
        List<StaffSalaryVO> list = this.salaryMapper.queryStaffSalaryVO();
        setSalaryInfo(month, list);
        EasyExcelUtil.write(response, list, filename, StaffSalaryVO.class);
    }

    /**
     * 设置工资的详细信息
     *
     * @param month
     * @param list
     */
    private void setSalaryInfo(String month, List<StaffSalaryVO> list) {
        DateTime dt = DateUtil.parse(month, "yyyyMM");
        Date startDate = dt.toSqlDate();
        Date endDate = DateUtil.offsetMonth(dt, 1).toSqlDate();
        for (StaffSalaryVO staffSalaryVO : list) {
            // 考勤次数（迟到/早退/旷工）
            Map<Integer, Integer> attendanceCounts = new HashMap<>();
            attendanceCounts.put(AttendanceStatusEnum.LATE.getCode(),
                    this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                            AttendanceStatusEnum.LATE.getCode(), startDate, endDate));
            attendanceCounts.put(AttendanceStatusEnum.LEAVE_EARLY.getCode(),
                    this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                            AttendanceStatusEnum.LEAVE_EARLY.getCode(), startDate, endDate));
            attendanceCounts.put(AttendanceStatusEnum.ABSENTEEISM.getCode(),
                    this.attendanceMapper.countTimes(staffSalaryVO.getStaffId(),
                            AttendanceStatusEnum.ABSENTEEISM.getCode(), startDate, endDate));
            // 休假扣款仅计工作日（跳过周末）
            int leaveWorkdayCount = 0;
            List<Date> leaveDateList = this.attendanceMapper.queryLeaveDate(staffSalaryVO.getStaffId(),
                    AttendanceStatusEnum.LEAVE.getCode(), startDate, endDate);
            for (Date date : leaveDateList) {
                if (!DateUtil.isWeekend(date)) {
                    leaveWorkdayCount++;
                }
            }
            // 扣款配置（部门维度）
            Map<Integer, Integer> deductRates = new HashMap<>();
            for (DeductEnum type : DeductEnum.values()) {
                deductRates.put(type.getCode(), queryDeductRate(staffSalaryVO.getDeptId(), type));
            }
            // 当月薪资记录 + 加班费汇总
            Salary monthSalary = getOne(new QueryWrapper<Salary>()
                    .eq("staff_id", staffSalaryVO.getStaffId()).eq("month", month));
            BigDecimal monthOvertime = this.salaryMapper.sumMonthOvertimeSalary(
                    staffSalaryVO.getStaffId(), month);
            // 纯计算委托
            SalaryUtil.calculate(staffSalaryVO, attendanceCounts, leaveWorkdayCount,
                    monthOvertime, monthSalary, deductRates);
        }
    }

    /** 扣款配置：按部门 + 类型查询，缺失回退默认值。 */
    private Integer queryDeductRate(Integer deptId, DeductEnum type) {
        if (deptId == null) {
            return type.getDefaultValue();
        }
        SalaryDeduct salaryDeduct = this.salaryDeductService.getOne(new QueryWrapper<SalaryDeduct>()
                .eq("dept_id", deptId).eq("type_num", type));
        return salaryDeduct != null ? salaryDeduct.getDeduct() : type.getDefaultValue();
    }

    public ResponseDTO setSalary(Salary salary) {
        QueryWrapper<Salary> query = new QueryWrapper<>();
        applyDerivedSalary(salary);
        query.eq("month", salary.getMonth()).eq("staff_id", salary.getStaffId());
        if (update(salary, query) || save(salary)) {
            return Response.success();
        }
        return Response.error();
    }

    /** 由基础工资折算日薪（月计薪天数 21.75）与时薪（21.75 × 8 小时） */
    private void applyDerivedSalary(Salary salary) {
        salary.setDaySalary(salary.getBaseSalary().divide(new BigDecimal("21.75"), 3, RoundingMode.HALF_UP));
        salary.setHourSalary(salary.getBaseSalary().divide(new BigDecimal(174), 3, RoundingMode.HALF_UP));
    }


    /**
     * 通过三阶段上传完成后创建异步导入任务。
     */
    public ResponseDTO createImportTask(String uploadId) {
        String mergedKey = fileUploadService.completeUpload(uploadId, getCurrentOperatorId());
        FileTask task = fileTaskService.submitImport(TaskModuleEnum.SALARY, "salary_import.xlsx",
                mergedKey, null, getCurrentOperatorId(), new SalaryImportHandler(this));
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        return Response.success(result);
    }

    /**
     * 创建异步导出任务（支持大文件）
     */
    public ResponseDTO createExportTask(String month, String filename) {
        FileTask task = fileTaskService.submitExport(TaskModuleEnum.SALARY, filename, month,
                getCurrentOperatorId(), new SalaryExportHandler(this));
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        return Response.success(result);
    }

    private static final class SalaryImportHandler implements ImportProcessor<SalaryImportRow> {

        private final SalaryService service;

        private SalaryImportHandler(SalaryService service) {
            this.service = service;
        }

        @Override
        public Class<SalaryImportRow> getRowClass() {
            return SalaryImportRow.class;
        }

        @Override
        public void processBatch(List<SalaryImportRow> rows, Long taskId,
                                 Consumer<FileTaskError> errorCollector) {
            Map<Integer, Staff> staffMap = new HashMap<>();
            for (SalaryImportRow row : rows) {
                String rowData = JSON.toJSONString(row);
                int rowNum = row.getRowNum() == null ? 0 : row.getRowNum();
                if (row.getStaffId() == null) {
                    errorCollector.accept(buildError(taskId, rowNum, rowData, "员工id不能为空"));
                    continue;
                }
                if (row.getMonth() == null || !MONTH_PATTERN.matcher(row.getMonth().trim()).matches()) {
                    errorCollector.accept(buildError(taskId, rowNum, rowData, "月份格式应为 yyyyMM"));
                    continue;
                }
                if (row.getBaseSalary() == null) {
                    errorCollector.accept(buildError(taskId, rowNum, rowData, "基础工资不能为空"));
                    continue;
                }
                if (staffMap.computeIfAbsent(row.getStaffId(), id -> service.staffMapper.selectById(id)) == null) {
                    errorCollector.accept(buildError(taskId, rowNum, rowData, "员工不存在"));
                    continue;
                }
                // 走与手工设置相同的入口：折算日薪/时薪，同员工同月按更新处理
                if (service.setSalary(toSalary(row)).getCode() != BusinessStatusEnum.SUCCESS.getCode()) {
                    errorCollector.accept(buildError(taskId, rowNum, rowData, "薪资数据保存失败"));
                }
            }
        }

        private Salary toSalary(SalaryImportRow row) {
            Salary salary = new Salary();
            salary.setStaffId(row.getStaffId());
            salary.setMonth(row.getMonth().trim());
            salary.setBaseSalary(row.getBaseSalary());
            salary.setSubsidy(row.getSubsidy() == null ? BigDecimal.ZERO : row.getSubsidy());
            salary.setBonus(row.getBonus() == null ? BigDecimal.ZERO : row.getBonus());
            salary.setRemark(row.getRemark());
            return salary;
        }

        private FileTaskError buildError(Long taskId, int rowNum, String rawData, String message) {
            return new FileTaskError()
                    .setTaskId(taskId).setRowNum(rowNum)
                    .setRawData(rawData).setErrorMessage(message);
        }
    }

    private static final class SalaryExportHandler implements ExportProcessor<StaffSalaryVO> {

        private final SalaryService service;

        private SalaryExportHandler(SalaryService service) {
            this.service = service;
        }

        @Override
        public Class<StaffSalaryVO> getRowClass() {
            return StaffSalaryVO.class;
        }

        @Override
        public IPage<StaffSalaryVO> queryPage(int current, int pageSize, String queryParamsJson) {
            String month = queryParamsJson;
            List<StaffSalaryVO> all = service.salaryMapper.queryStaffSalaryVO();
            service.setSalaryInfo(month, all);
            IPage<StaffSalaryVO> page = new Page<>(current, pageSize);
            int from = (current - 1) * pageSize;
            int to = Math.min(from + pageSize, all.size());
            page.setRecords(from >= all.size() ? java.util.Collections.emptyList() : all.subList(from, to));
            page.setTotal(all.size());
            return page;
        }
    }

    private Integer getCurrentOperatorId() {
        try {
            return securityUtil != null ? securityUtil.getCurrentOperatorId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}




