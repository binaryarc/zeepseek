package com.zeepseek.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestInfo = String.format("[Request] %s %s from %s", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        logger.info(requestInfo);

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;
        String responseInfo = String.format("[Response] %s %s -> %d [%dms]", request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        logger.info(responseInfo);
    }
}
