package org.octopusden.octopus.lifecycle.ui;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.octopusden.octopus.lifecycle.authorization.SecurityManager;
import org.octopusden.octopus.lifecycle.db.ComponentsRulesRepository;
import org.octopusden.octopus.lifecycle.db.Rule;
import org.octopusden.octopus.lifecycle.db.RuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class UiController {
    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private ComponentsRulesRepository componentsRulesRepository;

    @Autowired
    private SecurityManager securityManager;

    @GetMapping("/")
    public String mainPage(Model model, HttpServletRequest request) {
        model.addAttribute("contextPath", request.getContextPath());
        model.addAttribute("username", securityManager.getUsername());
        model.addAttribute("canEdit", securityManager.getCanEdit());
        return "index";
    }

    @GetMapping("/globalRules")
    public String globalRules(Model model, HttpServletRequest request) {
        model.addAttribute("contextPath", request.getContextPath());
        model.addAttribute("username", securityManager.getUsername());
        model.addAttribute("canEdit", securityManager.getCanEdit());
        return "global_rules";
    }

    @GetMapping("/globalRules/edit")
    public String editGlobalRules(Model model, HttpServletRequest request) {
        if (securityManager.getCanEdit()) {
            model.addAttribute("contextPath", request.getContextPath());
            model.addAttribute("username", securityManager.getUsername());
            model.addAttribute("canEdit", securityManager.getCanEdit());
            return "global_rules_edit";
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @GetMapping("/generate-rule-name/{componentId}")
    @Operation(
            summary = "Generate available name for new rule"
    )
    @ResponseBody
    public String generateRuleName(@PathVariable String componentId) {
        if (securityManager.getCanEdit()) {
            List<Rule> componentRules = componentsRulesRepository.findByComponentId(componentId).rules;

            int index = 0;
            if (!componentRules.isEmpty()) {
                while (ruleRepository.existsByName(componentId + "-" + index)) index++;
            }

            return componentId + "-" + index;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
