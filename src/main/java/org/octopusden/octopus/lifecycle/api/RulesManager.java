package org.octopusden.octopus.lifecycle.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minidev.json.JSONObject;
import org.octopusden.octopus.lifecycle.db.ComponentsRules;
import org.octopusden.octopus.lifecycle.db.ComponentsRulesRepository;
import org.octopusden.octopus.lifecycle.db.Rule;
import org.octopusden.octopus.lifecycle.db.RuleRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RulesManager {
    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private ComponentsRulesRepository componentsRulesRepository;

    @Autowired
    private ComponentsManager componentsManager;

    @Autowired
    private BuildsManager buildsManager;

    private boolean checkVersionRange(String versionRange) {
        try {
            VersionRange.createFromVersionSpec(versionRange);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Rule> getRules(Optional<String> componentId) {
        if (componentId.isPresent()) {
            return componentsRulesRepository.findByComponentId(componentId.get()).rules;
        }
        return ruleRepository.findAll();
    }

    public Rule getRule(String ruleName) {
        return ruleRepository.findByName(ruleName);
    }

    public ResponseEntity addRule(String componentId, String name, String type, String putLifecycleStage,
                                  Optional<String> dateFormat, Optional<String> minDate, Optional<String> maxDate,
                                  Optional<String> timeGap, Optional<List<String>> versionRange) throws IOException {

        if (!componentsManager.getComponents(Optional.empty(), Optional.empty()).contains(componentId)) {
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }
        if (ruleRepository.existsByName(name)) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        if (!(type.equals("global") || type.equals("component") || type.equals("build"))) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        if (!(putLifecycleStage.equals("active") || putLifecycleStage.equals("maintenance") || putLifecycleStage.equals("unsupported"))) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        if ((dateFormat.isEmpty() || dateFormat.get().equals("null")) && versionRange.isEmpty()) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);

        if (versionRange.isEmpty()) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        for (String vr : versionRange.get()) {
            if (!checkVersionRange(vr)) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }

        Rule rule = new Rule(name, type, putLifecycleStage);

        if (dateFormat.isEmpty() || dateFormat.get().equals("null")) {
            rule.dateFormat = null;
        } else if (dateFormat.get().equals("abs")) {
            rule.dateFormat = "abs";
            if (minDate.isEmpty() && maxDate.isEmpty()) {
                return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
            }

            try {
                if (minDate.isPresent()) rule.minDate = LocalDate.parse(minDate.get());
                if (maxDate.isPresent()) rule.maxDate = LocalDate.parse(maxDate.get());
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
            }
        } else if (dateFormat.get().equals("rel")) {
            rule.dateFormat = "rel";
            if (timeGap.isEmpty()) {
                return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
            }

            try {
                rule.timeGap = Integer.parseInt(timeGap.get());
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }

        if (versionRange.isPresent()) {
            rule.versionRange = versionRange.get();
        }

        ruleRepository.save(rule);

        ComponentsRules componentsRules = componentsRulesRepository.findByComponentId(componentId);
        componentsRules.rules.addFirst(rule);
        componentsRulesRepository.save(componentsRules);

        return new ResponseEntity(HttpStatus.CREATED);
    }

    public ResponseEntity changeRule(String ruleName, Optional<String> newName, Optional<String> newPutLifecycleStage,
                                     Optional<String> newDateFormat, Optional<String> newMinDate, Optional<String> newMaxDate,
                                     Optional<String> newTimeGap, Optional<List<String>> newVersionRange) {

        if (newName.isPresent() && ruleRepository.existsByName(newName.get())) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        if (newDateFormat.isPresent() && !(newDateFormat.get().equals("null") || newDateFormat.get().equals("abs") || newDateFormat.get().equals("rel"))) {
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }

        if (newVersionRange.isEmpty()) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        for (String vr : newVersionRange.get()) {
            if (!checkVersionRange(vr)) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }

        Rule rule = ruleRepository.findByName(ruleName);

        if (newName.isPresent()) {
            rule.name = newName.get();
        }

        if (newPutLifecycleStage.isPresent() && (newPutLifecycleStage.get().equals("active") || newPutLifecycleStage.get().equals("maintenance") || newPutLifecycleStage.get().equals("unsupported"))) {
            rule.putLifecycleStage = newPutLifecycleStage.get();
        }

        if (newDateFormat.isPresent()) {
            if (newDateFormat.get().equals("abs") || newDateFormat.get().equals("rel")) {
                rule.dateFormat = newDateFormat.get();
            } else if (newDateFormat.get().equals("null")) {
                rule.dateFormat = null;
            }
        }

        if (newMinDate.isPresent()) {
            if (newMinDate.get().equals("null")) {
                rule.minDate = null;
            } else {
                try {
                    rule.minDate = LocalDate.parse(newMinDate.get());
                } catch (Exception ignored) {
                    return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
                }
            }
        }

        if (newMaxDate.isPresent()) {
            if (newMaxDate.get().equals("null")) {
                rule.maxDate = null;
            } else {
                try {
                    rule.maxDate = LocalDate.parse(newMaxDate.get());
                } catch (Exception ignored) {
                    return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
                }
            }
        }

        if (newTimeGap.isPresent()) {
            if (newTimeGap.get().equals("null") || newTimeGap.get().equals("none")) {
                rule.timeGap = null;
            } else {
                try {
                    rule.timeGap = Integer.valueOf(newTimeGap.get());
                } catch (Exception ignored) {
                    return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
                }
            }
        }

        if (newVersionRange.isPresent()) {
            rule.versionRange = newVersionRange.get();
        }

        ruleRepository.save(rule);

        return new ResponseEntity(HttpStatus.CREATED);
    }

    public ResponseEntity setRuleOrder(String componentId, String rules) {
        if (!componentsRulesRepository.existsByComponentId(componentId)) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);

        List<Rule> rulesList = new ArrayList<>();

        for (String ruleName : new ArrayList<>(Arrays.asList(rules.split(",")))) {
            if (!ruleRepository.existsByName(ruleName)) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
            rulesList.add(ruleRepository.findByName(ruleName));
        }

        ComponentsRules componentRule = componentsRulesRepository.findByComponentId(componentId);
        componentRule.rules = rulesList;
        componentsRulesRepository.save(componentRule);

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    public ResponseEntity deleteRule(String ruleName, String componentId) {
        if (!ruleRepository.existsByName(ruleName)) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        if (!componentsRulesRepository.existsByComponentId(componentId)) return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);

        ComponentsRules componentRule = componentsRulesRepository.findByComponentId(componentId);
        Rule rule = ruleRepository.findByName(ruleName);

        List<Rule> rulesList = new ArrayList<>();
        for (Rule r : componentRule.rules) {
            if (!r.name.equals(ruleName)) rulesList.add(r);
        }
        componentRule.rules = rulesList;

        componentsRulesRepository.save(componentRule);
        ruleRepository.delete(rule);

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    private String versionRangeLineBuilder(String buildId) {
        StringBuilder lineBuilder = new StringBuilder();
        lineBuilder.append("[");
        lineBuilder.append(buildsManager.getVersionByBuild(buildId));
        lineBuilder.append(",");
        lineBuilder.append(buildsManager.getVersionByBuild(buildId, true));
        lineBuilder.append(")");
        return lineBuilder.toString();
    }

    public ResponseEntity addRulesFromFile(MultipartFile file) throws IOException {
        String jsonString = new String(file.getBytes(), StandardCharsets.UTF_8);
        JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();

        for (JsonElement jsonElement : jsonArray) {
            String componentId = jsonElement.getAsJsonObject().get("component").getAsString();

            try {
                String versionRangesActiveString = jsonElement.getAsJsonObject().get("active").getAsString();
                List<String> versionRangesActive = Arrays.stream(versionRangesActiveString.substring(1, versionRangesActiveString.length() - 1)
                        .split(","))
                        .map(String::trim)
                        .toList();

                List<String> versionRanges = new ArrayList<>();
                for (String versionRange : versionRangesActive) {
                    versionRanges.add(versionRangeLineBuilder(versionRange));
                }

                addRule(componentId, componentId + "-from-file-active", "component",
                        "active", Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.of(versionRanges));

            } catch (NullPointerException exception) {}

            try {
                String versionRangesMaintenanceString = jsonElement.getAsJsonObject().get("maintenance").getAsString();
                List<String> versionRangesMaintenance = Arrays.stream(versionRangesMaintenanceString.substring(1, versionRangesMaintenanceString.length() - 1)
                        .split(","))
                        .map(String::trim)
                        .toList();

                List<String> versionRanges = new ArrayList<>();
                for (String versionRange : versionRangesMaintenance) {
                    versionRanges.add(versionRangeLineBuilder(versionRange));
                }

                ResponseEntity e = addRule(componentId, componentId + "-from-file-maintenance", "component",
                        "maintenance", Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.of(versionRanges));

                System.out.println(e.getBody());
            } catch (NullPointerException exception) {}
        }

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }
}
