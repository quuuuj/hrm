package com.qiujie.leave.approval;

import com.qiujie.leave.entity.StaffLeave;
import com.qiujie.leave.enums.AuditStatusEnum;
import com.qiujie.notification.constant.NotificationBizType;
import com.qiujie.notification.dto.NotificationMessage;
import com.qiujie.notification.enums.NotificationCategoryEnum;
import com.qiujie.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 请假通知——把请假业务语义翻译为通用通知消息。
 * <p>
 * 由 {@code StaffLeaveService} 与 Flowable 监听器调用。事件类型：
 * 提交时通知申请人({@code LEAVE_SUBMITTED}) + 通知 HR({@code LEAVE_PENDING})；
 * 完成时按结果通知申请人({@code LEAVE_APPROVED} / {@code LEAVE_REJECTED})。
 * 落库与推送失败由 {@link NotificationService} 统一吞掉，不阻断审批流程。
 * </p>
 *
 * @author qiujie
 */
@Component
public class LeaveNotifier {

    private static final String BIZ_TYPE = NotificationBizType.LEAVE;

    @Autowired
    private NotificationService notificationService;

    /**
     * 请假申请已提交：通知申请人"已提交"，通知 HR 有新的待审申请。
     *
     * @param leave 已提交的请假
     * @param hrIds 待审 HR 员工 id 列表
     */
    public void onLeaveSubmitted(StaffLeave leave, List<Integer> hrIds) {
        String desc = leave.getDays() + "天请假，等待审批";
        notificationService.send(NotificationMessage.to(leave.getStaffId(), "LEAVE_SUBMITTED",
                NotificationCategoryEnum.INFO, "请假申请已提交", desc, BIZ_TYPE, bizId(leave)));

        if (hrIds != null && !hrIds.isEmpty()) {
            notificationService.send(NotificationMessage.to(hrIds, "LEAVE_PENDING",
                    NotificationCategoryEnum.INFO, "新的请假申请",
                    "员工提交了" + leave.getDays() + "天请假", BIZ_TYPE, bizId(leave)));
        }
    }

    /**
     * HR 通过、流程转入经理审批：通知经理有新的待办。
     *
     * @param leave      已通过人事审批的请假
     * @param managerIds 经理角色员工 id 列表
     */
    public void onManagerPending(StaffLeave leave, List<Integer> managerIds) {
        if (managerIds == null || managerIds.isEmpty()) {
            return;
        }
        notificationService.send(NotificationMessage.to(managerIds, "LEAVE_MANAGER_PENDING",
                NotificationCategoryEnum.INFO, "请假待您审批",
                "员工提交了" + leave.getDays() + "天请假，人事已通过", BIZ_TYPE, bizId(leave)));
    }

    /**
     * 请假审批完成（通过/拒绝）：通知申请人结果。
     *
     * @param leave 已完成的请假（status 为 APPROVE 或 REJECT）
     */
    public void onLeaveCompleted(StaffLeave leave) {
        boolean approved = AuditStatusEnum.APPROVE.equals(leave.getStatus());
        String type = approved ? "LEAVE_APPROVED" : "LEAVE_REJECTED";
        NotificationCategoryEnum category = approved
                ? NotificationCategoryEnum.SUCCESS : NotificationCategoryEnum.ERROR;
        String title = "请假" + (approved ? "已通过" : "已拒绝");
        String body = leave.getDays() + "天请假" + (approved ? "已通过" : "已拒绝");
        notificationService.send(NotificationMessage.to(leave.getStaffId(), type,
                category, title, body, BIZ_TYPE, bizId(leave)));
    }

    private Long bizId(StaffLeave leave) {
        return leave.getId() == null ? null : leave.getId().longValue();
    }
}
