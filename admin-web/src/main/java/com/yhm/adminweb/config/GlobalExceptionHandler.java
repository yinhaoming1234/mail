package com.yhm.adminweb.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理器
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, Model model, HttpServletRequest request) {
        log.warn("参数异常: {}", e.getMessage());
        
        // 如果是 HTMX 请求，返回错误片段
        if (isHtmxRequest(request)) {
            model.addAttribute("error", e.getMessage());
            return "common/fragments/error :: error-toast";
        }
        
        model.addAttribute("error", e.getMessage());
        return "error/400";
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model, HttpServletRequest request) {
        log.error("系统异常", e);
        
        // 如果是 HTMX 请求，返回错误片段
        if (isHtmxRequest(request)) {
            model.addAttribute("error", "系统错误，请稍后重试");
            return "common/fragments/error :: error-toast";
        }
        
        model.addAttribute("error", "系统错误，请稍后重试");
        return "error/500";
    }

    /**
     * 判断是否为 HTMX 请求
     */
    private boolean isHtmxRequest(HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }
}

