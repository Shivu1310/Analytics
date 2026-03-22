package com.liflab.analytics.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EventIngestionRateLimitFilter extends OncePerRequestFilter {

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile long windowStartNanos = System.nanoTime();

    @Value("${analytics.rate-limit.events-per-second:100}")
    private int maxEventsPerSecond;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isEventsIngestionRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!tryConsume()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"Rate limit exceeded for /api/events\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isEventsIngestionRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return "/api/events".equals(path) || path.endsWith("/api/events");
    }

    private boolean tryConsume() {
        long now = System.nanoTime();
        synchronized (this) {
            if (now - windowStartNanos >= 1_000_000_000L) {
                windowStartNanos = now;
                requestCount.set(0);
            }
            int current = requestCount.incrementAndGet();
            return current <= maxEventsPerSecond;
        }
    }
}
