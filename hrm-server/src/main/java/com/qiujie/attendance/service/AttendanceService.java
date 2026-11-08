package com.qiujie.attendance.service;
import com.qiujie.filetask.service.FileUploadService;
import com.qiujie.filetask.spi.ExportProcessor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.filetask.enums.TaskModuleEnum;
import com.qiujie.attendance.batch.AttendanceImportProcessor;
import com.qiujie.filetask.entity.FileTask;
import com.qiujie.filetask.service.FileTaskService;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.attendance.entity.Attendance;
import com.qiujie.attendance.enums.AttendanceStatusEnum;
import com.qiujie.attendance.mapper.AttendanceMapper;
import com.qiujie.util.DatetimeUtil;
import com.qiujie.util.EnumUtil;
import com.qiujie.staff.service.SecurityUtil;
import com.qiujie.attendance.vo.AttendanceMonthSummaryVO;
import com.qiujie.attendance.vo.AttendanceMonthVO;
import com.qiujie.attendance.vo.StaffAttendanceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

@Service
public class AttendanceService extends ServiceImpl<AttendanceMapper, Attendance> {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private DatetimeUtil datetimeUtil;

    @Autowired
    private FileTaskService fileTaskService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private AttendanceImportProcessor importProcessor;

    public ResponseDTO add(Attendance attendance) {
        if (save(attendance)) {
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

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO edit(Attendance attendance) {
        if (updateById(attendance)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO query(Integer id) {
        Attendance attendance = getById(id);
        if (attendance != null) {
            return Response.success(attendance);
        }
        return Response.error();
    }

    public ResponseDTO list(Integer current, Integer size, String name, Integer deptId, String month) {
        IPage<StaffAttendanceVO> config = new Page<>(current, size);
        if (name == null) {
            name = "";
        }
        IPage<StaffAttendanceVO> page;
        if (deptId == null) {
            page = this.attendanceMapper.listStaffAttendanceVO(config, name);
        } else {
            page = this.attendanceMapper.listStaffDeptAttendanceVO(config, name, deptId);
        }
        List<StaffAttendanceVO> staffDeptVOList = page.getRecords();
        if (month == null) {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        String[] monthDayList = this.datetimeUtil.getMonthDayList(month);

        // 批量加载：一次 SQL 查所有员工 x 所有日期
        List<Integer> staffIds = staffDeptVOList.stream().map(StaffAttendanceVO::getStaffId).collect(Collectors.toList());
        List<Date> dates = new ArrayList<>();
        for (String day : monthDayList) {
            dates.add(DateUtil.parse(day, "yyyyMMdd").toSqlDate());
        }
        Map<String, Attendance> attendanceMap = new HashMap<>();
        if (!staffIds.isEmpty() && !dates.isEmpty()) {
            List<Attendance> attendances = this.attendanceMapper.queryByStaffIdsAndDates(staffIds, dates);
            for (Attendance a : attendances) {
                attendanceMap.put(a.getStaffId() + "_" + DateUtil.format(a.getAttendanceDate(), "yyyyMMdd"), a);
            }
        }

        for (StaffAttendanceVO staffDeptVO : staffDeptVOList) {
            List<HashMap<String, Object>> list = new ArrayList<>();
            for (String day : monthDayList) {
                HashMap<String, Object> map = new HashMap<>();
                Attendance attendance = attendanceMap.get(staffDeptVO.getStaffId() + "_" + day);
                if (attendance == null) {
                    Date date = DateUtil.parse(day, "yyyyMMdd").toSqlDate();
                    if (DateUtil.isWeekend(date) || this.datetimeUtil.isHoliday(date)) {
                        map.put("message", AttendanceStatusEnum.LEAVE.getMessage());
                        map.put("tagType", AttendanceStatusEnum.LEAVE.getTagType());
                    } else {
                        map.put("message", AttendanceStatusEnum.NORMAL.getMessage());
                        map.put("tagType", AttendanceStatusEnum.NORMAL.getTagType());
                    }
                    map.put("attendanceDate", date);
                } else {
                    map.put("message", attendance.getStatus().getMessage());
                    map.put("tagType", attendance.getStatus().getTagType());
                    map.put("attendanceDate", attendance.getAttendanceDate());
                }
                list.add(map);
            }
            staffDeptVO.setAttendanceList(list);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffDeptVOList);
        map.put("dayNum", monthDayList.length);
        map.put("month", month);
        return Response.success(map);
    }

    public void export(HttpServletResponse response, String month, String filename) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8) + ".xlsx");
        DateTime dt = DateUtil.parse(month, "yyyyMM");
        Date exportStart = dt.toSqlDate();
        Date exportEnd = DateUtil.offsetMonth(dt, 1).toSqlDate();
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), AttendanceMonthVO.class).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("attendance").build();
        try {
            int pageSize = 500;
            int current = 1;
            IPage<AttendanceMonthVO> page;
            do {
                page = new Page<>(current, pageSize);
                page = this.attendanceMapper.queryAttendanceMonthVOPage(page);
                List<AttendanceMonthVO> list = page.getRecords();
                if (list.isEmpty()) {
                    break;
                }
                List<Integer> staffIds = list.stream()
                        .map(AttendanceMonthVO::getStaffId)
                        .collect(Collectors.toList());
                Map<Integer, AttendanceMonthSummaryVO> summaryMap = new HashMap<>();
                if (!staffIds.isEmpty()) {
                    summaryMap = this.attendanceMapper.queryMonthSummaryByStaffIds(exportStart, exportEnd, staffIds).stream()
                            .collect(Collectors.toMap(AttendanceMonthSummaryVO::getStaffId, item -> item));
                }
                for (AttendanceMonthVO vo : list) {
                    AttendanceMonthSummaryVO summary = summaryMap.get(vo.getStaffId());
                    vo.setLateTimes(summary == null ? 0 : valueOrZero(summary.getLateTimes()));
                    vo.setLeaveEarlyTimes(summary == null ? 0 : valueOrZero(summary.getLeaveEarlyTimes()));
                    vo.setAbsenteeismTimes(summary == null ? 0 : valueOrZero(summary.getAbsenteeismTimes()));
                    vo.setLeaveDays(summary == null ? 0 : valueOrZero(summary.getLeaveDays()));
                    vo.setTimeOffDays(summary == null ? 0 : valueOrZero(summary.getTimeOffDays()));
                }
                excelWriter.write(list, writeSheet);
                current++;
            } while (current <= page.getPages());
        } finally {
            excelWriter.finish();
        }
    }

    // ==================== 异步导入入口 ====================
    // 通过三阶段上传完成后创建异步导入任务
    public ResponseDTO createImportTask(String uploadId) {
        String mergedKey = fileUploadService.completeUpload(uploadId, getCurrentOperatorId());
        FileTask task = fileTaskService.submitImport(TaskModuleEnum.ATTENDANCE, "attendance_import.xlsx",
                mergedKey, null, getCurrentOperatorId(), importProcessor);
        return Response.success("导入任务已创建", task);
    }

    // ==================== 异步导出入口 ====================
    public ResponseDTO createExportTask(String month, String filename) {
        if (!StrUtil.isNotBlank(month)) {
            month = DateUtil.format(new java.util.Date(), "yyyyMM");
        }
        String exportName = StrUtil.isNotBlank(filename) ? filename : month + "_attendance_report.xlsx";
        if (!exportName.toLowerCase().endsWith(".xlsx")) {
            exportName = exportName + ".xlsx";
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("month", month);
        FileTask task = fileTaskService.submitExport(TaskModuleEnum.ATTENDANCE, exportName,
                JSON.toJSONString(queryParams), getCurrentOperatorId(), new AttendanceExportProcessor());
        return Response.success("导出任务已创建", task);
    }

    public ResponseDTO setAttendance(Attendance attendance) {
        QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("staff_id", attendance.getStaffId()).eq("attendance_date", attendance.getAttendanceDate());
        if (update(attendance, queryWrapper) || save(attendance)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO queryAll() {
        List<Map<String, Object>> enumList = EnumUtil.getEnumList(AttendanceStatusEnum.class);
        for (Map<String, Object> map : enumList) {
            for (AttendanceStatusEnum attendanceStatusEnum : AttendanceStatusEnum.values()) {
                if (map.get("code") == attendanceStatusEnum.getCode()) {
                    map.put("tagType", attendanceStatusEnum.getTagType());
                }
            }
        }
        return Response.success(enumList);
    }

    public ResponseDTO queryByStaffIdAndDate(Integer id, String date) {
        Date sqlDate = DateUtil.parse(date.replace("-", ""), "yyyyMMdd").toSqlDate();
        Attendance attendance = this.attendanceMapper.queryByStaffIdAndDate(id, sqlDate);
        if (attendance != null) {
            return Response.success(attendance);
        }
        return Response.error();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer getCurrentOperatorId() {
        return securityUtil.getCurrentOperatorId();
    }

    // ==================== 异步导出处理器（供 FileTaskEngine 回调） ====================
    private class AttendanceExportProcessor implements ExportProcessor<AttendanceMonthVO> {

        @Override
        public Class<AttendanceMonthVO> getRowClass() {
            return AttendanceMonthVO.class;
        }

        @Override
        public IPage<AttendanceMonthVO> queryPage(int current, int pageSize, String queryParamsJson) {
            String month = parseMonth(queryParamsJson);
            DateTime dt = DateUtil.parse(month, "yyyyMM");
            Date rangeStart = dt.toSqlDate();
            Date rangeEnd = DateUtil.offsetMonth(dt, 1).toSqlDate();
            IPage<AttendanceMonthVO> page = new Page<>(current, pageSize);
            page = AttendanceService.this.attendanceMapper.queryAttendanceMonthVOPage(page);
            List<AttendanceMonthVO> list = page.getRecords();
            if (!list.isEmpty()) {
                List<Integer> staffIds = list.stream()
                        .map(AttendanceMonthVO::getStaffId)
                        .collect(Collectors.toList());
                Map<Integer, AttendanceMonthSummaryVO> summaryMap = AttendanceService.this.attendanceMapper
                        .queryMonthSummaryByStaffIds(rangeStart, rangeEnd, staffIds).stream()
                        .collect(Collectors.toMap(AttendanceMonthSummaryVO::getStaffId, item -> item));
                for (AttendanceMonthVO vo : list) {
                    AttendanceMonthSummaryVO summary = summaryMap.get(vo.getStaffId());
                    vo.setLateTimes(summary == null ? 0 : valueOrZero(summary.getLateTimes()));
                    vo.setLeaveEarlyTimes(summary == null ? 0 : valueOrZero(summary.getLeaveEarlyTimes()));
                    vo.setAbsenteeismTimes(summary == null ? 0 : valueOrZero(summary.getAbsenteeismTimes()));
                    vo.setLeaveDays(summary == null ? 0 : valueOrZero(summary.getLeaveDays()));
                    vo.setTimeOffDays(summary == null ? 0 : valueOrZero(summary.getTimeOffDays()));
                }
            }
            return page;
        }

        private String parseMonth(String queryParamsJson) {
            if (queryParamsJson == null || queryParamsJson.isEmpty()) {
                return DateUtil.format(new java.util.Date(), "yyyyMM");
            }
            try {
                Map<String, Object> params = JSON.parseObject(queryParamsJson, Map.class);
                return params.getOrDefault("month", DateUtil.format(new java.util.Date(), "yyyyMM")).toString();
            } catch (Exception e) {
                return DateUtil.format(new java.util.Date(), "yyyyMM");
            }
        }
    }
}
