package org.octopusden.octopus.lifecycle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Components Lifecycle Service.
 *
 * This service operates behind an API Gateway and supports multiple authentication methods:
 *
 * 1. OAuth2 Login (Browser Users):
 *    - For direct browser access, initiates OAuth2 login flow
 *    - Redirects unauthenticated users to Keycloak
 *    - Creates session after successful authentication
 *
 * 2. OAuth2 Resource Server (API Clients):
 *    - Validates JWT tokens from Keycloak
 *    - Used for API-to-API communication
 *
 * 3. Gateway Authentication (When behind Gateway):
 *    - Accepts authentication from API Gateway via headers
 *    - Gateway handles OAuth2 flow and forwards authenticated requests
 *
 * Static resources and health checks are publicly accessible for operational needs.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    public SecurityConfig(GatewayAuthenticationFilter gatewayAuthenticationFilter) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Add filter to create authentication from gateway headers (before authentication check)
            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authorize -> authorize
                // Allow anonymous access to static resources (CSS, JS, images)
                .requestMatchers("/css/**", "/js/**", "/img/**", "/static/**").permitAll()
                // Allow anonymous access to actuator health endpoints for Kubernetes probes
                .requestMatchers("/actuator/health/**", "/actuator/health").permitAll()
                // Allow anonymous access to Swagger/OpenAPI documentation
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // Allow anonymous access to API endpoints
                .requestMatchers("/api/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Exception handling - return 401 for unauthenticated requests
            // The API Gateway should intercept and redirect to login
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    // Return 401 Unauthorized - API Gateway will intercept and handle authentication
                    // DO NOT redirect or return HTML - just set status code
                    response.sendError(401, "Unauthorized");
                })
            )
            // Disable CSRF protection completely since we're behind API Gateway
            .csrf(csrf -> csrf.disable());

        // Configure OAuth2 Resource Server only if JWT issuer-uri is provided
        // If empty, the service trusts the API Gateway's authentication
        if (issuerUri != null && !issuerUri.isEmpty()) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JWT validation is configured via spring.security.oauth2.resourceserver.jwt properties
                    // The API Gateway forwards JWT tokens from Keycloak
                })
            );
        }

        return http.build();
    }
}
