package com.qiujie.security;

import com.alibaba.fastjson.JSON;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.common.enums.BusinessStatusEnum;
import com.qiujie.util.WebUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
public class AccessDeniedExceptionHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        String str = JSON.toJSONString(new ResponseDTO(BusinessStatusEnum.FORBIDDEN));
        // 给出异常提示信息
        WebUtil.renderString(response, str);
    }
}
