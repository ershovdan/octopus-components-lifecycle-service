package org.octopusden.octopus.lifecycle;

import org.octopusden.octopus.lifecycle.config.ComponentsRegistryServiceProperties;
import org.octopusden.octopus.lifecycle.config.RelengJiraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({
    ComponentsRegistryServiceProperties.class,
    RelengJiraProperties.class
})
public class ComponentsLifecycleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComponentsLifecycleServiceApplication.class, args);
    }
}
