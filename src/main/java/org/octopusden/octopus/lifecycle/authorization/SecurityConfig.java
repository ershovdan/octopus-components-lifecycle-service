package org.octopusden.octopus.lifecycle.authorization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
public class SecurityConfig {
    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2.loginPage("/oauth2/authorization/keycloak"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt()) // JWT validation for APIs
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler(keycloakLogoutHandler())
                )
                .build();
    }


    private LogoutSuccessHandler keycloakLogoutHandler() {
        return (request, response, authentication) -> response.sendRedirect(issuerUri + "/protocol/openid-connect/logout");
    }
}
