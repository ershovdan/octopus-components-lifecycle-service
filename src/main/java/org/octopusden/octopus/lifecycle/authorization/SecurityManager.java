package org.octopusden.octopus.lifecycle.authorization;

import org.octopusden.cloud.commons.security.dto.User;
import org.octopusden.octopus.lifecycle.config.SecurityServiceConfig.SimpleSecurityService;
import org.springframework.stereotype.Service;

@Service
public class SecurityManager {

    private final SimpleSecurityService securityService;

    public SecurityManager(SimpleSecurityService securityService) {
        this.securityService = securityService;
    }

    public String getUsername() {
        User user = securityService.getCurrentUser();
        return user.getUsername();
    }

    public boolean getCanEdit() {
        return true;
    }

}
