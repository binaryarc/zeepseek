package com.zeepseek.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.io.IOException;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 요청을 ContentCachingRequestWrapper로 감싸서 바디를 캐싱할 수 있도록 함.
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        long startTime = System.currentTimeMillis();
        String requestInfo = String.format("[Request] %s %s from %s",
                wrappedRequest.getMethod(),
                wrappedRequest.getRequestURI(),
                wrappedRequest.getRemoteAddr());
        logger.info(requestInfo);

        // 필터 체인을 통해 다음 필터/서블릿으로 요청 전달
        filterChain.doFilter(wrappedRequest, response);

        // 요청 처리 후, 캐시된 요청 바디를 가져와서 로깅 (바디가 있을 경우)
        byte[] buf = wrappedRequest.getContentAsByteArray();
        if (buf.length > 0) {
            String payload = new String(buf, wrappedRequest.getCharacterEncoding());
            logger.info("[Request Body] {}", payload);
        }

        long duration = System.currentTimeMillis() - startTime;
        String responseInfo = String.format("[Response] %s %s -> %d [%dms]",
                wrappedRequest.getMethod(),
                wrappedRequest.getRequestURI(),
                response.getStatus(),
                duration);
        logger.info(responseInfo);
    }
}
