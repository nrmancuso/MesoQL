package com.mesoql.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs each HTTP request and response with ANSI color codes for readability.
 * Request lines are cyan; response status is green (2xx), yellow (3xx), or red (4xx/5xx).
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String RESET  = "\033[0m";
    private static final String BOLD   = "\033[1m";
    private static final String CYAN   = "\033[36m";
    private static final String GREEN  = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String RED    = "\033[31m";

    private static final int HTTP_5XX_MIN = 500;
    private static final int HTTP_4XX_MIN = 400;
    private static final int HTTP_3XX_MIN = 300;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String method = request.getMethod();
        final String uri = buildUri(request);
        final long start = System.currentTimeMillis();

        log.info("{}{}{} {}{}", BOLD + CYAN, method, RESET + CYAN, uri, RESET);

        chain.doFilter(request, response);

        final int status = response.getStatus();
        final long elapsed = System.currentTimeMillis() - start;
        final String color = statusColor(status);

        log.info("{}{}{} {} {} {}ms{}",
            BOLD, color, status, RESET + color, uri, elapsed, RESET);
    }

    private static String buildUri(HttpServletRequest request) {
        final String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private static String statusColor(int status) {
        if (status >= HTTP_5XX_MIN) return RED;
        if (status >= HTTP_4XX_MIN) return RED;
        if (status >= HTTP_3XX_MIN) return YELLOW;
        return GREEN;
    }
}
