package com.kidslearn.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 请求/响应日志过滤器
 * 记录每个 HTTP 请求的方法、URI、状态码和耗时
 */
@Component
@Order(2)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = wrappedResponse.getStatus();

            if (uri.startsWith("/actuator")) {
                // 不记录 actuator 请求
            } else if (status >= 400) {
                log.warn("HTTP_REQ | {} {} {} | status={} | {}ms",
                    method, uri, query != null ? "?" + query : "", status, duration);
            } else {
                log.info("HTTP_REQ | {} {} | status={} | {}ms",
                    method, uri, status, duration);
            }

            wrappedResponse.copyBodyToResponse();
        }
    }
}
