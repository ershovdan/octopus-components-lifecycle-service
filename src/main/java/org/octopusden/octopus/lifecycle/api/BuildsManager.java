package org.octopusden.octopus.lifecycle.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.octopusden.octopus.lifecycle.api.entities.Build;
import org.octopusden.octopus.lifecycle.config.RelengJiraProperties;
import org.octopusden.releng.versions.IVersionInfo;
import org.octopusden.releng.versions.NumericVersionFactory;
import org.octopusden.releng.versions.VersionNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BuildsManager {

    private final String releaseEngineeringUrl;

    @Autowired
    private BuildParser buildParser;

    @Autowired
    private Gson gson;

    private final VersionNames VERSION_NAMES = new VersionNames("serviceBranch", "service", "minor");
    private static final Logger log = LoggerFactory.getLogger(ComponentsManager.class);

    public BuildsManager(RelengJiraProperties properties) {
        String baseUrl = properties.getHost();
        String path = "rest/release-engineering/3/admin/component/";
        this.releaseEngineeringUrl = ( baseUrl.endsWith("/") ? baseUrl : baseUrl + "/" ) + path;

        log.info("BuildsManager initialized with rest URL: {}", releaseEngineeringUrl);
    }

    private String getRequest(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        try {
            return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("request to releng " + url);
            log.error(e.getMessage());
            return null;
        }
    }

    public Build getBuildByVersion(String componentId, String version) throws IOException {
        for (Build build : getBuildsByComponent(componentId)) {
            if (getVersionByBuild(build.id).equals(version)) return build;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @Cacheable(value = "getBuildsByComponent", key = "#componentId")
    public List<Build> getBuildsByComponent(String componentId) throws IOException {
        return buildParser.parse(getRequest(releaseEngineeringUrl + componentId + "/builds"));
    }

    @Cacheable(value = "getBuild", key = "{#componentId, #buildId}")
    public Build getBuild(@RequestParam(name = "componentId") String componentId, @RequestParam(name = "buildId") String buildId) throws IOException {
        List<Build> builds = getBuildsByComponent(componentId);

        for (Build build : builds) {
            if (buildId.equals(build.id)) {
                String message;
                try {
                    message = gson.fromJson(getRequest(releaseEngineeringUrl + componentId + "/build/" + buildId), JsonObject.class).getAsJsonObject().get("message").getAsString();
                } catch (NullPointerException e) { // occurs during wiremock testing
                    return null;
                }

                List<String> dependencies;

                try {
                    Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(message);

                    String depsString = "";
                    if (matcher.find()) {
                        depsString = matcher.group(1).replace(" ", "");
                    }

                    dependencies = Arrays.stream(depsString.split(","))
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                build.dependencies = dependencies;

                return build;
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @Cacheable(value = "getVersionByBuild", key = "#buildId")
    public String getVersionByBuild(String buildId) {
        NumericVersionFactory numericVersionFactory = new NumericVersionFactory(VERSION_NAMES);
        IVersionInfo iVersionInfo = numericVersionFactory.create(buildId);

        List<String> verItems = new ArrayList<>();
        for (int i = 0; i < iVersionInfo.getItemsCount(); i++) {
            verItems.add(String.valueOf(iVersionInfo.getItem(i)));
        }

        return String.join(".", verItems);
    }

}
