package com.yourorg.eventdashboard.shared;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("method", httpRequest.getMethod());
        MDC.put("path", httpRequest.getRequestURI());

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            MDC.put("duration", String.valueOf(duration));
            MDC.put("status", String.valueOf(httpResponse.getStatus()));

            int status = httpResponse.getStatus();
            if (status >= 500) {
                log.error("Request completed");
            } else if (status >= 400) {
                log.warn("Request completed");
            } else {
                log.info("Request completed");
            }

            MDC.clear();
        }
    }
}
