package com.qiujie.leave.approval;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.qiujie.leave.entity.StaffLeave;
import com.qiujie.leave.enums.AuditStatusEnum;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.security.ServiceException;
import com.qiujie.leave.service.StaffLeaveService;
import com.qiujie.staff.entity.Staff;
import com.qiujie.staff.mapper.StaffMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HR 审批执行监听器——流程转译层。
 * <p>
 * 请假状态置为待审 + 装配经理候选人变量（角色成员工号逗号串）。
 * </p>
 */
@Component
public class HrApproveListener implements ExecutionListener {

    @Autowired
    private StaffLeaveService staffLeaveService;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private RuntimeService runtimeService;

    @Override
    @Transactional
    public void notify(DelegateExecution execution) {
        UpdateWrapper<StaffLeave> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", AuditStatusEnum.UNAUDITED).eq("id", Integer.valueOf(execution.getProcessInstanceBusinessKey()));
        if (!this.staffLeaveService.update(updateWrapper)) {
            throw new ServiceException(BusinessStatusEnum.ERROR);
        }
        String managerCodes = staffMapper.queryByRole("manager").stream()
                .map(Staff::getCode).collect(Collectors.joining(","));
        Map<String, Object> variables = new HashMap<>();
        variables.put("manager", managerCodes);
        runtimeService.setVariables(execution.getId(), variables);
    }
}