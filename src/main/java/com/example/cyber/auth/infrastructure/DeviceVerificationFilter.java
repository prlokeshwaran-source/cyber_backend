package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.application.DeviceApprovalService;
import com.example.cyber.auth.application.DeviceApprovalDeliveryException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.dao.DataAccessException;

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
            writeProblem(response, HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                    "X-Device-Id must be a persistent random value between 32 and 128 characters");
            return;
        }
        String email = authentication.getName();
        boolean trusted;
        try {
            trusted = service.trusted(email, deviceId);
        } catch (DataAccessException exception) {
            writeProblem(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable",
                    "Account storage is temporarily unavailable");
            return;
        }
        if (trusted) {
            chain.doFilter(request, response);
            return;
        }
        try {
            service.requestApproval(email, deviceId, request.getRemoteAddr());
            writeProblem(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                    "Device approval required. Check the registered email.");
        } catch (DeviceApprovalDeliveryException exception) {
            writeProblem(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable",
                    "Device verification email could not be sent");
        } catch (DataAccessException exception) {
            writeProblem(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable",
                    "Device verification storage is temporarily unavailable");
        }
    }

    private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title
                + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
