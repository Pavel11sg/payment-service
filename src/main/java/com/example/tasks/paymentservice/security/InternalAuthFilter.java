package com.example.tasks.paymentservice.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${gateway.internal.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String receivedSecret = request.getHeader("X-Internal-Secret");
        if (!gatewaySecret.equals(receivedSecret)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Direct access not allowed");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
