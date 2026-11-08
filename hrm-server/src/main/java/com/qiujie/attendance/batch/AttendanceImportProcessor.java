package com.qiujie.attendance.batch;

import com.qiujie.attendance.dto.AttendanceImportRow;
import com.qiujie.attendance.entity.Attendance;
import com.qiujie.dept.entity.Dept;
import com.qiujie.filetask.entity.FileTaskError;
import com.qiujie.filetask.spi.ImportProcessor;
import com.qiujie.staff.entity.Staff;
import com.qiujie.attendance.enums.AttendanceStatusEnum;
import com.qiujie.attendance.mapper.AttendanceMapper;
import com.qiujie.dept.mapper.DeptMapper;
import com.qiujie.staff.mapper.StaffMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cn.hutool.core.date.DateUtil;

/**
 * 考勤导入处理器（ImportProcessor 实现）。
 * <p>
 * 负责导入行的预加载、校验、状态判定与幂等批量写入。
 * 不负责文件读取、任务状态或错误记录持久化。
 * </p>
 */
@Component
public class AttendanceImportProcessor implements ImportProcessor<AttendanceImportRow> {

    /**
     * 单条批量 upsert SQL 的最大行数。
     * 与引擎的读取/事务批次（FileTaskEngine.IMPORT_BATCH_SIZE = 500）是两回事：
     * processBatch 是 SPI，不能假设调用方永远传 500 行。
     */
    private static final int UPSERT_BATCH_SIZE = 500;

    @Autowired
    private AttendanceMapper attendanceMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Class<AttendanceImportRow> getRowClass() {
        return AttendanceImportRow.class;
    }

    @Override
    public void processBatch(List<AttendanceImportRow> rows, Long taskId,
                             Consumer<FileTaskError> errorCollector) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        Set<Integer> staffIds = rows.stream()
                .map(AttendanceImportRow::getStaffId)
                .filter(item -> item != null)
                .collect(Collectors.toSet());
        Map<Integer, Staff> staffMap = staffIds.isEmpty()
                ? new HashMap<>()
                : staffMapper.selectBatchIds(staffIds).stream()
                .collect(Collectors.toMap(Staff::getId, item -> item));
        Set<Integer> deptIds = staffMap.values().stream()
                .map(Staff::getDeptId)
                .filter(item -> item != null)
                .collect(Collectors.toSet());
        Map<Integer, Dept> deptMap = deptIds.isEmpty()
                ? new HashMap<>()
                : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Dept::getId, item -> item));
        Set<Date> attendanceDates = rows.stream()
                .map(AttendanceImportRow::getAttendanceDate)
                .filter(item -> item != null)
                .map(this::toSqlDate)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, Attendance> existingMap = new HashMap<>();
        if (!staffIds.isEmpty() && !attendanceDates.isEmpty()) {
            existingMap = attendanceMapper.queryByStaffIdsAndDates(staffIds, attendanceDates).stream()
                    .collect(Collectors.toMap(this::buildAttendanceKey, item -> item, (left, right) -> left));
        }

        List<Attendance> saveList = new ArrayList<>();
        for (AttendanceImportRow row : rows) {
            String rowData = buildRowData(row);
            int rowNum = row.getRowNum() == null ? 0 : row.getRowNum();
            if (row.getStaffId() == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "员工id不能为空"));
                continue;
            }
            if (row.getAttendanceDate() == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "考勤日期不能为空"));
                continue;
            }
            Staff staff = staffMap.get(row.getStaffId());
            if (staff == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "员工不存在"));
                continue;
            }
            Dept dept = deptMap.get(staff.getDeptId());
            if (dept == null) {
                errorCollector.accept(buildError(taskId, rowNum, rowData, "员工部门不存在"));
                continue;
            }

            Attendance attendance = convertAttendance(row);
            if (DateUtil.isWeekend(attendance.getAttendanceDate())) {
                continue;
            }
            Attendance existing = existingMap.get(buildAttendanceKey(attendance));
            if (existing != null && (existing.getStatus() == AttendanceStatusEnum.LEAVE
                    || existing.getStatus() == AttendanceStatusEnum.TIME_OFF)) {
                continue;
            }
            if (isAbsenteeism(attendance, dept)) {
                attendance.setStatus(AttendanceStatusEnum.ABSENTEEISM);
            } else if (isLate(attendance, dept)) {
                attendance.setStatus(AttendanceStatusEnum.LATE);
            } else if (isLeaveEarly(attendance, dept)) {
                attendance.setStatus(AttendanceStatusEnum.LEAVE_EARLY);
            } else {
                attendance.setStatus(AttendanceStatusEnum.NORMAL);
            }
            saveList.add(attendance);
        }

        if (!saveList.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> saveOrUpdate(saveList));
        }
    }

    /**
     * 依赖 uk_attendance_staff_date 幂等写入：同键行后写覆盖先写，逻辑删除的行恢复为未删除。
     */
    private void saveOrUpdate(List<Attendance> attendances) {
        for (int from = 0; from < attendances.size(); from += UPSERT_BATCH_SIZE) {
            int to = Math.min(from + UPSERT_BATCH_SIZE, attendances.size());
            attendanceMapper.upsertBatch(attendances.subList(from, to));
        }
    }

    public boolean isLate(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getAftStartTime() == null) {
            return false;
        }
        return DateUtil.compare(attendance.getMorStartTime(), dept.getMorStartTime(), "HH:mm:ss") > 0
                || DateUtil.compare(attendance.getAftStartTime(), dept.getAftStartTime(), "HH:mm:ss") > 0;
    }

    public boolean isLeaveEarly(Attendance attendance, Dept dept) {
        if (attendance.getMorEndTime() == null || attendance.getAftEndTime() == null) {
            return false;
        }
        return DateUtil.compare(attendance.getMorEndTime(), dept.getMorEndTime(), "HH:mm:ss") < 0
                || DateUtil.compare(attendance.getAftEndTime(), dept.getAftEndTime(), "HH:mm:ss") < 0;
    }

    public boolean isAbsenteeism(Attendance attendance, Dept dept) {
        if (attendance.getMorStartTime() == null || attendance.getMorEndTime() == null
                || attendance.getAftStartTime() == null || attendance.getAftEndTime() == null) {
            return true;
        }
        return isLate(attendance, dept) && isLeaveEarly(attendance, dept);
    }

    private Attendance convertAttendance(AttendanceImportRow row) {
        Attendance attendance = new Attendance();
        attendance.setStaffId(row.getStaffId());
        attendance.setMorStartTime(toSqlTimestamp(row.getMorStartTime()));
        attendance.setMorEndTime(toSqlTimestamp(row.getMorEndTime()));
        attendance.setAftStartTime(toSqlTimestamp(row.getAftStartTime()));
        attendance.setAftEndTime(toSqlTimestamp(row.getAftEndTime()));
        attendance.setAttendanceDate(toSqlDate(row.getAttendanceDate()));
        return attendance;
    }

    private FileTaskError buildError(Long taskId, int rowNum, String rawData, String message) {
        FileTaskError error = new FileTaskError();
        error.setTaskId(taskId);
        error.setRowNum(rowNum);
        error.setRawData(rawData);
        error.setErrorMessage(message);
        return error;
    }

    private String buildRowData(AttendanceImportRow row) {
        String date = row.getAttendanceDate() == null ? "" : DateUtil.formatDate(row.getAttendanceDate());
        return "staffId=" + row.getStaffId() + ", attendanceDate=" + date;
    }

    private String buildAttendanceKey(Attendance attendance) {
        return attendance.getStaffId() + "_" + attendance.getAttendanceDate();
    }

    private Timestamp toSqlTimestamp(java.util.Date date) {
        if (date == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return Timestamp.valueOf(LocalDateTime.of(1970, 1, 1,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
    }

    private Date toSqlDate(java.util.Date date) {
        return date == null ? null : new Date(date.getTime());
    }
}
