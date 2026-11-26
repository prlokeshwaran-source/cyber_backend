package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.application.DeviceApprovalService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DeviceVerificationFilter extends OncePerRequestFilter {
    private final DeviceApprovalService service;

    public DeviceVerificationFilter(DeviceApprovalService service) { this.service = service; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/v1/accounts") || path.startsWith("/api/v1/transactions"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            chain.doFilter(request, response);
            return;
        }
        String deviceId = request.getHeader("X-Device-Id");
        if (deviceId == null || deviceId.length() < 32 || deviceId.length() > 128) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "X-Device-Id must be a persistent random value between 32 and 128 characters");
            return;
        }
        String email = authentication.getName();
        try {
            if (service.trusted(email, deviceId)) {
                chain.doFilter(request, response);
                return;
            }
            service.requestApproval(email, deviceId, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Device approval required. Check the registered email.\"}");
        } catch (RuntimeException exception) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Device verification email is temporarily unavailable");
        }
    }
}
