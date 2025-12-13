package org.octopusden.octopus.lifecycle.authorization;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SecurityManager {
    private final Environment environment;

    public SecurityManager(Environment environment) {
        this.environment = environment;
    }

    public String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getPreferredUsername();
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("preferred_username");
        }

        return authentication.getName();
    }

    public List<String> getRoles() {
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        OidcIdToken idToken = oidcUser.getIdToken();

        ArrayList<String> roles = (ArrayList<String>) idToken.getClaims().get("roles");

        if (roles != null) {
            return roles;
        }
        return new ArrayList<>();
    }

    public boolean getCanEdit() {
        if (environment.acceptsProfiles("test")) return true; // able to edit during testing

        return getRoles().contains("canEdit");
    }

}
