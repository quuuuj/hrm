package com.qiujie.leave.service;
import com.qiujie.leave.entity.Leave;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.staff.entity.Staff;
import com.qiujie.leave.entity.Leave;
import com.qiujie.leave.entity.StaffLeave;
import com.qiujie.leave.enums.AuditStatusEnum;
import com.qiujie.leave.enums.LeaveEnum;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.leave.approval.LeaveNotifier;
import com.qiujie.leave.mapper.LeaveMapper;
import com.qiujie.leave.mapper.StaffLeaveMapper;
import com.qiujie.overtime.service.StaffOvertimeService;
import com.qiujie.staff.mapper.StaffMapper;
import com.qiujie.util.EnumUtil;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.staff.service.SecurityUtil;
import com.qiujie.leave.vo.StaffLeaveVO;
import com.qiujie.util.DatetimeUtil;
import cn.hutool.core.date.DateUtil;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-04-05
 */
@Service
public class StaffLeaveService extends ServiceImpl<StaffLeaveMapper, StaffLeave> {

    private static final Logger log = LoggerFactory.getLogger(StaffLeaveService.class);

    @Autowired
    private StaffLeaveMapper staffLeaveMapper;

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private StaffOvertimeService staffOvertimeService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private LeaveNotifier leaveNotifier;

    @Autowired
    private DatetimeUtil datetimeUtil;

    /**
     * 获取当前登录用户的工号
     *
     * @return 工号，如果未登录或用户不存在则返回 null
     */
    private String getCurrentStaffCode() {
        Integer currentStaffId = securityUtil.getCurrentOperatorId();
        if (currentStaffId == null) {
            return null;
        }
        Staff currentStaff = this.staffMapper.selectById(currentStaffId);
        return currentStaff != null ? currentStaff.getCode() : null;
    }

    /**
     * 复核部门假期额度与调休余额（与前端 checkDays 保持同口径）
     *
     * @param staff      请假发起人
     * @param staffLeave 请假信息
     * @return 校验不通过的错误信息，通过则返回 null
     */
    private String checkQuota(Staff staff, StaffLeave staffLeave) {
        if (staffLeave.getTypeNum() == null || staffLeave.getDays() == null || staffLeave.getStartDate() == null) {
            return "请假类型、天数与起始日期不能为空！";
        }
        if (staffLeave.getDays() < 1) {
            return "请假天数必须大于0！";
        }

        // 部门额度校验
        Leave leave = this.leaveMapper.selectOne(new QueryWrapper<Leave>()
                .eq("dept_id", staff.getDeptId())
                .eq("type_num", staffLeave.getTypeNum()));
        if (leave == null) {
            return "所在部门未配置该假期类型的额度！";
        }
        if (Integer.valueOf(0).equals(leave.getStatus())) {
            return "该假期类型已停用！";
        }
        if (staffLeave.getDays() > leave.getDays()) {
            return "部门规定，" + leave.getTypeNum().getMessage() + "休假天数不超过" + leave.getDays() + "天!";
        }

        // 调休余额校验：按工作日折算（补班日/非周末且非节假日），与前端 checkDays 及审批侧考勤写入同口径
        if (staffLeave.getTypeNum() == LeaveEnum.TIME_OFF) {
            int required = 0;
            for (int i = 0; i < staffLeave.getDays(); i++) {
                if (datetimeUtil.isWorkday(DateUtil.offsetDay(staffLeave.getStartDate(), i).toSqlDate())) {
                    required++;
                }
            }
            long balance = this.staffOvertimeService.countTimeOffDays(staff.getId());
            if (required > balance) {
                return "所需调休天数为" + required + "，而你仅仅只有" + balance + "天的调休余额！";
            }
        }
        return null;
    }

    public ResponseDTO add(StaffLeave staffLeave) {
        if (save(staffLeave)) {
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


    @Transactional
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * @param staffLeave
     * @return
     */
    public ResponseDTO edit(StaffLeave staffLeave) {
        if (updateById(staffLeave)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        StaffLeave staffLeave = getById(id);
        if (staffLeave != null) {
            return Response.success(staffLeave);
        }
        return Response.error();
    }


    public ResponseDTO list(Integer current, Integer size, String name, Integer deptId, String code) {
        IPage<StaffLeaveVO> config = new Page<>(current, size);
        // 查询当前用户的组任务以及个人任务
        List<Task> taskList = this.taskService.createTaskQuery().processDefinitionKey("leave").taskCandidateOrAssigned(code).list();
        List<Integer> ids = new ArrayList<>();
        for (Task task : taskList) {
            if (task != null) {
                ProcessInstance instance = this.runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                ids.add(Integer.valueOf(instance.getBusinessKey()));
            }
        }
        IPage<StaffLeaveVO> page = this.staffLeaveMapper.listStaffLeaveVO(config, name, deptId, ids);
        List<StaffLeaveVO> staffLeaveVOList = page.getRecords();
        List<HashMap<String, Object>> list = new ArrayList<>();
        for (StaffLeaveVO staffLeaveVO : staffLeaveVOList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("staffLeave", staffLeaveVO);
            map.put("tagType", staffLeaveVO.getStatus().getTagType());
            map.put("approve", AuditStatusEnum.APPROVE);
            map.put("reject", AuditStatusEnum.REJECT);
            map.put("unaudited", AuditStatusEnum.UNAUDITED);
            map.put("auditing", AuditStatusEnum.AUDITING);
            list.add(map);
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", list);
        return Response.success(map);
    }

    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response, String filename) throws IOException {
        List<StaffLeave> list = list();
        EasyExcelUtil.write(response, list, filename, StaffLeave.class);
    }

    /**
     * 数据导入
     *
     * @param file
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        List<StaffLeave> list = EasyExcelUtil.read(inputStream, 1, StaffLeave.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO queryByStaffId(Integer current, Integer size, Integer id) {
        IPage<StaffLeave> config = new Page<>(current, size);
        IPage<StaffLeave> page = this.staffLeaveMapper.listStaffLeaveByStaffId(config, id);
        List<StaffLeave> records = page.getRecords();
        List<HashMap<String, Object>> list = new ArrayList<>();
        for (StaffLeave staffLeave : records) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("staffLeave", staffLeave);
            map.put("tagType", staffLeave.getStatus().getTagType());
            map.put("unaudited", AuditStatusEnum.UNAUDITED);
            map.put("approve", AuditStatusEnum.APPROVE);
            map.put("reject", AuditStatusEnum.REJECT);
            map.put("cancel", AuditStatusEnum.CANCEL);
            list.add(map);
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", list);
        return Response.success(map);
    }

    public ResponseDTO queryAll() {
        List<Map<String, Object>> enumList = EnumUtil.getEnumList(AuditStatusEnum.class);
        for (Map<String, Object> map : enumList) {
            for (AuditStatusEnum auditStatusEnum : AuditStatusEnum.values()) {
                if (map.get("code") == auditStatusEnum.getCode()) {
                    map.put("tagType", auditStatusEnum.getTagType());
                }
            }
        }
        return Response.success(enumList);
    }

    /**
     * 请假申请
     *
     * @param staffLeave 请假信息
     * @return 操作结果
     */
    @Transactional
    public ResponseDTO apply(StaffLeave staffLeave) {
        // 验证当前用户并获取员工信息
        Integer currentStaffId = securityUtil.getCurrentOperatorId();
        if (currentStaffId == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }
        Staff currentStaff = this.staffMapper.selectById(currentStaffId);
        if (currentStaff == null) {
            return Response.error("用户信息不存在");
        }
        String code = currentStaff.getCode();

        // 验证请假归属
        if (!staffLeave.getStaffId().equals(currentStaffId)) {
            log.warn("Unauthorized leave application: currentStaffId={}, targetStaffId={}",
                     currentStaffId, staffLeave.getStaffId());
            return Response.error(BusinessStatusEnum.FORBIDDEN);
        }

        // 复核部门额度与调休余额（与前端 checkDays 同口径，防止绕过前端直接调接口）
        String quotaError = checkQuota(currentStaff, staffLeave);
        if (quotaError != null) {
            return Response.error(quotaError);
        }

        // 检查是否有待审核的请假
        List<StaffLeave> staffLeaveList = this.staffLeaveMapper.selectList(new QueryWrapper<StaffLeave>().eq("staff_id", staffLeave.getStaffId())
                .and(i -> i
                        .eq("status", AuditStatusEnum.UNAUDITED).or()
                        .eq("status", AuditStatusEnum.REJECT).or()
                        .eq("status", AuditStatusEnum.AUDITING))
        );
        if (!staffLeaveList.isEmpty()) {
            return Response.error("你有待审核、被驳回、正在审核中的请假申请！");
        }

        // 保存请假记录
        if (!save(staffLeave)) {
            return Response.error("提交失败！");
        }

        try {
            // 启动流程实例
            Map<String, Object> map = new HashMap<>();
            map.put("staff", code);
            ProcessInstance instance = this.runtimeService.startProcessInstanceByKey("leave", String.valueOf(staffLeave.getId()), map);

            log.info("Started leave process: instanceId={}, businessKey={}, staff={}",
                     instance.getId(), staffLeave.getId(), code);

            // 自动完成第一个任务(请假申请节点)
            Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                    .processInstanceBusinessKey(String.valueOf(staffLeave.getId()))
                    .taskAssignee(code).singleResult();
            List<Staff> staffList = null;
            if (task != null) {
                staffList = this.staffMapper.queryByRole("hr");
                if (staffList.isEmpty()) {
                    log.warn("No HR staff found for approval");
                    return Response.error("未找到人事审批人员，请联系管理员");
                }

                Map<String, Object> map1 = new HashMap<>();
                map1.put("hr", staffList.stream().map(Staff::getCode).collect(Collectors.joining(",")));
                taskService.complete(task.getId(), map1);

                log.info("Completed leave apply task: taskId={}, hrCount={}", task.getId(), staffList.size());
            }

            // 推送通知（不阻塞主流程）
            if (staffList != null) {
                leaveNotifier.onLeaveSubmitted(staffLeave, staffList.stream().map(Staff::getId).collect(Collectors.toList()));
            }

            return Response.success();
        } catch (Exception e) {
            log.error("Failed to start leave process: staffLeaveId={}", staffLeave.getId(), e);
            // 回滚请假记录
            removeById(staffLeave.getId());
            return Response.error("流程启动失败，请稍后再试");
        }
    }

    /**
     * 拾取请假申请任务
     *
     * @param staffLeave 请假信息
     * @return 操作结果
     */
    @Transactional
    public ResponseDTO claim(StaffLeave staffLeave) {
        String code = getCurrentStaffCode();
        if (code == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }

        staffLeave.setStatus(AuditStatusEnum.AUDITING);
        if (!updateById(staffLeave)) {
            return Response.error();
        }

        Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString())
                .taskCandidateUser(code).singleResult();
        if (task == null) {
            return Response.error("任务不存在或无权拾取");
        }

        this.taskService.claim(task.getId(), code);
        log.info("Claimed leave task: taskId={}, staff={}", task.getId(), code);

        return Response.success();
    }


    /**
     * 归还请假任务
     *
     * @param staffLeave 请假信息
     * @return 操作结果
     */
    @Transactional
    public ResponseDTO revert(StaffLeave staffLeave) {
        String code = getCurrentStaffCode();
        if (code == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }

        staffLeave.setStatus(AuditStatusEnum.UNAUDITED);
        if (!updateById(staffLeave)) {
            return Response.error();
        }

        Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString())
                .taskAssignee(code).singleResult();
        if (task == null) {
            return Response.error("任务不存在或未分配给你");
        }

        this.taskService.setAssignee(task.getId(), null);
        log.info("Reverted leave task: taskId={}, staff={}", task.getId(), code);

        return Response.success();
    }


    /**
     * 完成审批任务
     *
     * @param staffLeave 请假信息
     * @return 操作结果
     */
    @Transactional
    public ResponseDTO complete(StaffLeave staffLeave) {
        String code = getCurrentStaffCode();
        if (code == null) {
            return Response.error(BusinessStatusEnum.UNAUTHORIZED);
        }

        // 先校验任务归属：重复提交/任务已被他人处理时直接返回，避免业务状态被误更新
        Task task = this.taskService.createTaskQuery().processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString())
                .taskAssignee(code).singleResult();

        if (task == null) {
            log.warn("Leave audit task not found: leaveId={}, assignee={}", staffLeave.getId(), code);
            return Response.error("任务不存在或未分配给你（可能已被归还或他人已处理）");
        }

        if (!updateById(staffLeave)) {
            return Response.error();
        }

        // 请求体通常只带 id + status，通知所需的 staffId/days 从库中重新加载
        StaffLeave persisted = this.getById(staffLeave.getId());
        if (persisted == null) {
            return Response.error("请假记录不存在");
        }

        Map<String, Object> map = new HashMap<>();
        if (Objects.equals(task.getTaskDefinitionKey(), "hr_audit")) {
            map.put("hrAuditStatus", persisted.getStatus().getCode());
        } else if(Objects.equals(task.getTaskDefinitionKey(), "manager_audit")) {
            map.put("managerAuditStatus", persisted.getStatus().getCode());
        }

        boolean hrStage = Objects.equals(task.getTaskDefinitionKey(), "hr_audit");
        boolean approved = AuditStatusEnum.APPROVE.equals(persisted.getStatus());

        // 经理候选人需在流程推进前查询：Flowable 与业务库事务独立，
        // taskService.complete 提交后若再抛异常，流程状态会与业务状态不一致
        List<Integer> managerIds = null;
        if (hrStage && approved) {
            managerIds = staffMapper.queryByRole("manager").stream()
                    .map(Staff::getId).collect(Collectors.toList());
        }

        taskService.complete(task.getId(), map);
        log.info("Completed leave audit task: taskId={}, staff={}, status={}",
                 task.getId(), code, persisted.getStatus());

        if (hrStage && approved) {
            // HR 通过后流程进入经理审批：通知经理待办，申请人结果等经理审批结束再发
            leaveNotifier.onManagerPending(persisted, managerIds);
        } else {
            // HR 驳回、经理通过/驳回：流程结束或回到申请人，通知申请人结果
            leaveNotifier.onLeaveCompleted(persisted);
        }

        return Response.success();
    }

    /**
     * 撤销请假申请
     *
     * @param staffLeave
     * @return
     */
    @Transactional
    public ResponseDTO cancel(StaffLeave staffLeave) {
        staffLeave.setStatus(AuditStatusEnum.CANCEL);
        if (!updateById(staffLeave)) {
            return Response.error();
        }
        ProcessInstance instance = this.runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("leave")
                .processInstanceBusinessKey(staffLeave.getId().toString()).singleResult();
        if (instance != null) {
            runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), staffLeave.getStatus().getMessage());
            return Response.success();
        }
        return Response.error();
    }

}




