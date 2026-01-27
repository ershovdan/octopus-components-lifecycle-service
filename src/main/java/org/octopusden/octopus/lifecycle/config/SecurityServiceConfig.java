package org.octopusden.octopus.lifecycle.config;

import org.octopusden.cloud.commons.security.dto.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.List;

/**
 * Configuration to provide a simple SecurityService implementation.
 * This service operates behind an API Gateway, so we extract user info
 * from the security context populated by the gateway.
 **/
@Configuration
public class SecurityServiceConfig {

    @Bean
    public SimpleSecurityService securityService() {
        return new SimpleSecurityService();
    }

    /**
     * Simple Java-based SecurityService implementation.
     * Extracts user information from Spring Security context.
     */
    public static class SimpleSecurityService {

        public User getCurrentUser() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                // Return anonymous user for non-authenticated requests
                return createAnonymousUser();
            }

            // Handle JWT authentication (if gateway forwards JWT)
            if (authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
                Jwt jwt = jwtAuth.getToken();

                String username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getClaimAsString("sub");
                }
                if (username == null) {
                    username = jwt.getSubject();
                }

                List<String> groups = jwt.getClaimAsStringList("groups");
                if (groups == null) {
                    groups = Collections.emptyList();
                }

                return new User(username != null ? username : "unknown", Collections.emptyList(), groups);
            }

            // Handle simple authentication
            String username = authentication.getName();
            if (username == null || username.isEmpty()) {
                return createAnonymousUser();
            }

            return new User(username, Collections.emptyList(), Collections.emptyList());
        }

        private User createAnonymousUser() {
            return new User("anonymous", Collections.emptyList(), Collections.emptyList());
        }
    }
}
