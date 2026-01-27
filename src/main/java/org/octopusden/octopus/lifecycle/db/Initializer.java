package org.octopusden.octopus.lifecycle.db;

import org.octopusden.octopus.lifecycle.api.ComponentsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class Initializer {

    private static final Logger log = LoggerFactory.getLogger(Initializer.class);

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private ComponentsRulesRepository componentsRulesRepository;

    @Autowired
    private ComponentsManager componentsManager;

    @EventListener(ApplicationReadyEvent.class)
    public void initGlobalRule() {
        if (ruleRepository.findAll().isEmpty()) {
            Rule rule = new Rule();
            rule.type = "global";
            rule.putLifecycleStage = "active";
            rule.name = "globalActive";
            ruleRepository.save(rule);

            rule = new Rule();
            rule.type = "global";
            rule.putLifecycleStage = "maintenance";
            rule.name = "globalMaintenance";
            ruleRepository.save(rule);
        }
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void scanComponentRule() {
        try {
            log.debug("Starting scheduled component scan");
            List<String> components = componentsManager.getComponents(Optional.empty(), Optional.empty());
            log.debug("Found {} components to process", components.size());

            for (String component : components) {
                if (!componentsRulesRepository.existsByComponentId(component)) {
                    ComponentsRules componentsRules = new ComponentsRules(component);
                    componentsRulesRepository.save(componentsRules);
                    log.debug("Created new component rule for: {}", component);
                }
            }
            log.debug("Completed scheduled component scan");
        } catch (IOException e) {
            log.error("Failed to fetch components during scheduled scan: {}", e.getMessage(), e);
            // Don't rethrow - allow the scheduled task to continue on next execution
        } catch (Exception e) {
            log.error("Unexpected error during scheduled component scan: {}", e.getMessage(), e);
            // Don't rethrow - allow the scheduled task to continue on next execution
        }
    }
}