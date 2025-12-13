package org.octopusden.octopus.lifecycle.db;

import org.octopusden.octopus.lifecycle.api.ComponentsManager;
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
    public void scanComponentRule() throws IOException {
        List<String> components = componentsManager.getComponents(Optional.empty(), Optional.empty());
        for (String component : components) {
            if (!componentsRulesRepository.existsByComponentId(component)) {
                ComponentsRules componentsRules = new ComponentsRules(component);
                componentsRulesRepository.save(componentsRules);
            }
        }
    }
}