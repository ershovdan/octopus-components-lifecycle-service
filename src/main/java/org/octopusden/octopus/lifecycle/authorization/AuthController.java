package org.octopusden.octopus.lifecycle.authorization;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.octopusden.cloud.commons.security.dto.User;
import org.octopusden.octopus.lifecycle.config.SecurityServiceConfig.SimpleSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@Tag(name = "Auth Controller")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final SimpleSecurityService securityService;

    public AuthController(SimpleSecurityService securityService) {
        this.securityService = securityService;
    }

    @GetMapping("me")
    public User getUserInfo() {
        User user = securityService.getCurrentUser();
        if (log.isTraceEnabled()) {
            log.trace("Logged User: {}", user.getUsername());
        }
        return user;
    }
}
