package com.seckill.service.config;

import com.seckill.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从 Header 中获取 Gateway 传过来的 userId
        String userIdStr = request.getHeader("userId");

        // 2. 如果 Header 里有 ID，说明是网关验过票的，存入 ThreadLocal
        if (StringUtils.hasText(userIdStr)) {
            UserContext.setUserId(Long.valueOf(userIdStr));
        }
        // 如果没有 userId，可能是测试接口或无需登录的接口，直接放行即可
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 3. 用完必须清理，防止内存泄漏 (ThreadLocal 的经典坑)
        UserContext.remove();
    }
}