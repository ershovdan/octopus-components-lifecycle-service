package org.octopusden.octopus.lifecycle.api;

import org.octopusden.octopus.lifecycle.api.entities.Build;
import org.octopusden.octopus.lifecycle.api.entities.Component;
import org.octopusden.octopus.lifecycle.authorization.SecurityManager;
import org.octopusden.octopus.lifecycle.db.Rule;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;


@RestController
@RequestMapping(path = "api")
public class ApiController {
    @Autowired
    private ComponentsManager componentsManager;

    @Autowired
    private BuildsManager buildsManager;

    @Autowired
    private RulesManager rulesManager;

    @Autowired
    private LifecycleStageMatcher lifecycleStageMatcher;

    @Autowired
    private SecurityManager securityManager;

    public ApiController() {}

    @GetMapping("/components")
    @Operation(
            summary = "Get components"
    )
    public List<String> getComponents(@RequestParam(name = "showArchived") Optional<String> showArchived,
                                      @RequestParam(name = "owner") Optional<String> owner) throws IOException {
        return componentsManager.getComponents(showArchived, owner);
    }

    @GetMapping("/components/{componentId}")
    @Operation(
            summary = "Get component"
    )
    public Component getComponent(@PathVariable String componentId) throws IOException {
        return componentsManager.getComponent(componentId);
    }

    @GetMapping("/components/{componentId}/builds")
    @Operation(
            summary = "Get builds of component"
    )
    public List<Build> getBuildsByComponent(@PathVariable String componentId) throws IOException {
        return buildsManager.getBuildsByComponent(componentId);
    }

    @GetMapping("/components/{componentId}/versions")
    @Operation(
            summary = "Get versions of component"
    )
    public List<String> getVersionsByComponent(@PathVariable String componentId) throws IOException {
        return componentsManager.getVersionsByComponent(componentId);
    }

    @GetMapping("/components/{componentId}/builds/{buildId}")
    @Operation(
            summary = "Get build of component"
    )
    public Build getBuild(@PathVariable String componentId, @PathVariable String buildId) throws IOException {
        return buildsManager.getBuild(componentId, buildId);
    }

    @GetMapping("/components/{componentId}/builds/{buildId}/version")
    @Operation(
            summary = "Get version of the build"
    )
    public String getVersion(@PathVariable String componentId, @PathVariable String buildId) {
        return "\"" + buildsManager.getVersionByBuild(buildId) + "\"";
    }

    @GetMapping("/components/{componentId}/versions/{version}/lifecycle-stage")
    @Operation(
            summary = "Get lifecycle stage by version"
    )
    public String getLifecycleStage(@PathVariable String componentId, @PathVariable String version) throws IOException, InvalidVersionSpecificationException {
        return "\"" + lifecycleStageMatcher.getLifecycleStage(componentId, version) + "\"";
    }

    @GetMapping("/components/{componentId}/builds/{buildId}/lifecycle-stage")
    @Operation(
            summary = "Get lifecycle stage by build"
    )
    public String getLifecycleStageByBuild(@PathVariable String componentId, @PathVariable String buildId) throws IOException, InvalidVersionSpecificationException {
        return "\"" + lifecycleStageMatcher.getLifecycleStageByBuild(componentId, buildId) + "\"";
    }

    @GetMapping("/rules")
    @Operation(
            summary = "Get rules"
    )
    public List<Rule> getRules(@RequestParam(name = "componentId") Optional<String> componentId) {
        return rulesManager.getRules(componentId);
    }

    @GetMapping("/rules/{ruleName}")
    @Operation(
            summary = "Get rule"
    )
    public Rule getRule(@PathVariable String ruleName) {
        return rulesManager.getRule(ruleName);
    }

    @PostMapping("/rules")
    @Operation(
            summary = "Add rule"
    )
    public ResponseEntity addRule(@RequestParam(name = "componentId") String componentId,
                                  @RequestParam(name = "name") String name,
                                  @RequestParam(name = "type") String type,
                                  @RequestParam(name = "putLifecycleStage") String putLifecycleStage,
                                  @RequestParam(name = "dateFormat") Optional<String> dateFormat,
                                  @RequestParam(name = "minDate") Optional<String> minDate,
                                  @RequestParam(name = "maxDate") Optional<String> maxDate,
                                  @RequestParam(name = "timeGap") Optional<String> timeGap,
                                  @RequestParam(name = "versionRange") Optional<String> versionRange) throws IOException {

        if (securityManager.getCanEdit()) {
            return rulesManager.addRule(componentId, name, type, putLifecycleStage, dateFormat, minDate, maxDate, timeGap, versionRange);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PutMapping("/rules/{ruleName}")
    @Operation(
            summary = "Update rule"
    )
    public ResponseEntity changeRule(@PathVariable String ruleName, @RequestParam(name = "newName") Optional<String> newName,
                           @RequestParam(name = "newPutLifecycleStage") Optional<String> newPutLifecycleStage,
                           @RequestParam(name = "newDateFormat") Optional<String> newDateFormat,
                           @RequestParam(name = "newMinDate") Optional<String> newMinDate,
                           @RequestParam(name = "newMaxDate") Optional<String> newMaxDate,
                           @RequestParam(name = "newTimeGap") Optional<String> newTimeGap,
                           @RequestParam(name = "newVersionRange") Optional<String> newVersionRange) {

        if (securityManager.getCanEdit()) {
            return rulesManager.changeRule(ruleName, newName, newPutLifecycleStage, newDateFormat, newMinDate, newMaxDate, newTimeGap, newVersionRange);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PostMapping("/rules/set-order")
    @Operation(
            summary = "Set order of rules for the component"
    )
    public ResponseEntity setRuleOrder(@RequestParam(name = "componentId") String componentId,
                                       @RequestParam(name = "rules") String rules) {
        if (securityManager.getCanEdit()) {
            return rulesManager.setRuleOrder(componentId, rules);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @DeleteMapping("/rules/{ruleName}")
    @Operation(
            summary = "Delete rule"
    )
    public ResponseEntity deleteRule(@PathVariable String ruleName, @RequestParam(name = "componentId") String componentId) {
        if (securityManager.getCanEdit()) {
            return rulesManager.deleteRule(ruleName, componentId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
