package org.octopusden.octopus.lifecycle;

import org.octopusden.octopus.lifecycle.db.RuleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CLSApplication {
    public CLSApplication(RuleRepository userRepository) {}

    public static void main(String[] args) {
		SpringApplication.run(CLSApplication.class, args);
	}
}
