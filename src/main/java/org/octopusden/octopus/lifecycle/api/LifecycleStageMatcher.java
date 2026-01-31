package org.octopusden.octopus.lifecycle.api;

import org.octopusden.octopus.lifecycle.api.entities.Build;
import org.octopusden.octopus.lifecycle.db.ComponentsRulesRepository;
import org.octopusden.octopus.lifecycle.db.Rule;
import org.octopusden.octopus.lifecycle.db.RuleRepository;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LifecycleStageMatcher {
    private final DateTimeFormatter buildDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private ComponentsRulesRepository componentsRulesRepository;

    @Autowired
    private BuildsManager buildsManager;

    private boolean checkForAbsoluteTime(Rule rule, Build build) {
        LocalDate buildDate = LocalDateTime.parse(build.buildDate, buildDateTimeFormatter).toLocalDate();

        if (rule.minDate != null && rule.maxDate == null) {
            return rule.minDate.isBefore(buildDate);
        } else if (rule.minDate == null && rule.maxDate != null) {
            return rule.maxDate.isAfter(buildDate);
        } else if (rule.minDate != null) {
            return (rule.minDate.isBefore(buildDate)) && (rule.maxDate.isAfter(buildDate));
        }

        return false;
    }

    private boolean checkForRelativeTime(Rule rule, Build build) {
        LocalDate buildDate = LocalDateTime.parse(build.buildDate, buildDateTimeFormatter).toLocalDate();
        return ChronoUnit.DAYS.between(buildDate, LocalDateTime.now()) <= rule.timeGap;
    }

    private boolean checkForVersionRange(Rule rule, String version) throws InvalidVersionSpecificationException {
        for (String vr : rule.versionRange) {
            VersionRange versionRange = VersionRange.createFromVersionSpec(vr);
            DefaultArtifactVersion v = new DefaultArtifactVersion(version);
            if (versionRange.containsVersion(v)) return true;
        }
        return false;
    }

    private boolean checkRule(Rule rule, Build build, String version) throws InvalidVersionSpecificationException {
        if (rule.dateFormat != null) {
            if (rule.dateFormat.equals("abs")) {
                return checkForAbsoluteTime(rule, build);
            } else if (rule.dateFormat.equals("rel")) {
                return checkForRelativeTime(rule, build);
            }
        } else {
            if (rule.versionRange != null) {
                return checkForVersionRange(rule, version);
            }
        }
        return false;
    }

    public String getLifecycleStageByBuild(String componentId, String buildId) throws IOException, InvalidVersionSpecificationException {
        String version = buildsManager.getVersionByBuild(buildId);
        return getLifecycleStage(componentId, version);
    }

    public String getLifecycleStage(String componentId, String version) throws IOException, InvalidVersionSpecificationException {
        List<Rule> componentRules = componentsRulesRepository.findByComponentId(componentId).rules;

        Build build = buildsManager.getBuildByVersion(componentId, version);

        if (build == null) return "unsupported";

        for (Rule rule : componentRules) {
            if (checkRule(rule, build, version)) {
                return rule.putLifecycleStage;
            }
        }

        Rule globalRuleActive = ruleRepository.findByName("globalActive");
        Rule globalRuleMaintenance = ruleRepository.findByName("globalMaintenance");

        if (checkRule(globalRuleActive, build, version)) {
            return "active";
        } else if (checkRule(globalRuleMaintenance, build, version)) {
            return "maintenance";
        }
        return "unsupported";
    }
}
