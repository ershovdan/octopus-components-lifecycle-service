package org.octopusden.octopus.lifecycle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that creates authentication from headers forwarded by the API Gateway.
 * The gateway authenticates users with Keycloak and forwards user information via headers.
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Check if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try to extract user from gateway headers
        String username = extractUsername(request);

        if (username != null && !username.isEmpty()) {
            // Extract groups/roles from headers
            List<SimpleGrantedAuthority> authorities = extractAuthorities(request);

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

            // Set in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractUsername(HttpServletRequest request) {
        // Try different header names that gateways commonly use
        String username = request.getHeader("X-Auth-Username");
        if (username == null) {
            username = request.getHeader("X-Forwarded-User");
        }
        if (username == null) {
            username = request.getHeader("X-User");
        }
        if (username == null) {
            username = request.getHeader("SM_USER");
        }
        // Try to extract from Authorization header (Bearer token with username claim)
        if (username == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // If there's a bearer token but no username header, we might be in JWT mode
                // In that case, return null and let OAuth2 Resource Server handle it
                return null;
            }
        }
        // Fallback for development/testing
        if (username == null) {
            username = request.getRemoteUser();
        }
        return username;
    }

    private List<SimpleGrantedAuthority> extractAuthorities(HttpServletRequest request) {
        String rolesHeader = request.getHeader("X-Auth-Roles");
        if (rolesHeader == null) {
            rolesHeader = request.getHeader("X-Forwarded-Roles");
        }
        if (rolesHeader == null) {
            rolesHeader = request.getHeader("X-User-Roles");
        }

        if (rolesHeader != null && !rolesHeader.isEmpty()) {
            return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
