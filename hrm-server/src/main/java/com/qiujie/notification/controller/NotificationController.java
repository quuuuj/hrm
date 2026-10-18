package com.qiujie.notification.controller;

import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.common.sse.SseService;
import com.qiujie.notification.service.NotificationService;
import com.qiujie.staff.service.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 站内通知：SSE 订阅与通知箱查询。
 *
 * @author qiujie
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private SseService sseService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "订阅通知（SSE）")
    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return sseService.subscribe(staffId);
    }

    @Operation(summary = "当前用户通知分页")
    @GetMapping("/page")
    public ResponseDTO page(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size) {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error("未登录");
        }
        return Response.success(notificationService.pageByStaffId(staffId, current, size));
    }

    @Operation(summary = "当前用户未读通知数")
    @GetMapping("/unread-count")
    public ResponseDTO unreadCount() {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error("未登录");
        }
        return Response.success(notificationService.countUnread(staffId));
    }

    @Operation(summary = "标记单条通知已读")
    @PutMapping("/read/{id}")
    public ResponseDTO read(@PathVariable Integer id) {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error("未登录");
        }
        notificationService.markRead(staffId, id);
        return Response.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public ResponseDTO readAll() {
        Integer staffId = securityUtil.getCurrentOperatorId();
        if (staffId == null) {
            return Response.error("未登录");
        }
        notificationService.markAllRead(staffId);
        return Response.success();
    }
}
