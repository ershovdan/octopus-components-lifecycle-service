package org.octopusden.octopus.lifecycle.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.octopusden.octopus.lifecycle.api.entities.Build;
import org.octopusden.octopus.lifecycle.api.entities.Component;
import org.octopusden.octopus.lifecycle.config.ComponentsRegistryServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ComponentsManager {

    private static final Logger log = LoggerFactory.getLogger(ComponentsManager.class);

    private final String componentsRegistryServiceUrl;

    @Autowired
    private Gson gson;

    @Autowired
    private BuildsManager buildsManager;

    public ComponentsManager(ComponentsRegistryServiceProperties properties) {
        String baseUrl = properties.getUrl();
        String path = "rest/api/2/components";

        // Ensure URL ends with / for proper path construction
        this.componentsRegistryServiceUrl = ( baseUrl.endsWith("/") ? baseUrl : baseUrl + "/" ) + path;

        log.info("ComponentsManager initialized with registry URL: {}", componentsRegistryServiceUrl);
    }

    private String getStringOrNullFromJson(JsonElement jsonElement) {
        if (!jsonElement.isJsonNull()) return jsonElement.getAsString();
        return null;
    }

    private boolean isArchived(String componentName) {
        if (componentName == null) return false;
        return componentName.indexOf("(archived)") == componentName.length() - 10;
    }

    private String getRequest(String url) throws IOException {
        log.debug("Making request to component registry: {}", url);
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            log.debug("Received response from {}: {} bytes", url, response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch data from component registry at {}: {}", url, e.getMessage(), e);
            throw new IOException("Failed to connect to component registry at " + url + ": " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "getVersionsByComponent", key = "#componentId")
    public List<String> getVersionsByComponent(String componentId) throws IOException {
        List<Build> builds = buildsManager.getBuildsByComponent(componentId);
        List<String> versions = new ArrayList<>();

        for (Build build : builds) {
            String version = buildsManager.getVersionByBuild(build.id);

            if (version != null) { // occurs during wiremock testing
                versions.add(version);
            }
        }

        return versions;
    }

    @Cacheable(value = "getComponent", key = "#componentId")
    public Component getComponent(String componentId) throws IOException {
        JsonObject fullJson = gson.fromJson(getRequest(componentsRegistryServiceUrl + componentId), JsonObject.class);

        Component component = new Component();
        component.id = fullJson.get("id").getAsString();
        component.name = getStringOrNullFromJson(fullJson.get("name"));
        component.componentOwner = fullJson.get("componentOwner").getAsString();
        component.securityChampion = getStringOrNullFromJson(fullJson.get("securityChampion"));
        component.releaseManager = getStringOrNullFromJson(fullJson.get("releaseManager"));

        component.isArchived = isArchived(component.name);

        component.distributionExplicit = fullJson.get("distribution").getAsJsonObject().get("explicit").getAsBoolean();
        component.distributionExternal = fullJson.get("distribution").getAsJsonObject().get("external").getAsBoolean();

        if (fullJson.get("distribution").getAsJsonObject().get("docker") != null) {
            component.distributionDocker = Arrays.stream(fullJson.get("distribution").getAsJsonObject().get("docker").getAsString().split(",")).toList();
        } else {
            component.distributionDocker = null;
        }

        if (fullJson.get("distribution").getAsJsonObject().get("GAV") != null) {
            component.distributionGAV = List.of(fullJson.get("distribution").getAsJsonObject().get("GAV").getAsString().split(","));
        } else {
            component.distributionGAV = null;
        }

        List<Build> builds = buildsManager.getBuildsByComponent(component.id);

        if (!builds.isEmpty()) {
            List<String> artifactsList = new ArrayList<>();
            JsonObject artifacts = gson.fromJson(getRequest(componentsRegistryServiceUrl + componentId + "/maven-artifacts"), JsonObject.class);
            if (artifacts != null) {
                Set<String> keys = artifacts.keySet();

                for (String key : keys) {
                    artifactsList.add(artifacts.get(key).getAsJsonObject().get("groupPattern").getAsString());
                }
            }

            component.artifact = artifactsList;

        }

        return component;
    }


    @Cacheable(value = "getComponents", key = "{#showArchived, #owner}")
    public List<String> getComponents(Optional<String> showArchived, Optional<String> owner) throws IOException {
        List<String> components = new ArrayList<>();

        try {
            log.debug("Fetching components list from registry. ShowArchived: {}, Owner: {}", showArchived, owner);
            String response = getRequest(componentsRegistryServiceUrl);

            if (response == null || response.trim().isEmpty()) {
                log.error("Received empty response from component registry at {}", componentsRegistryServiceUrl);
                return components;
            }

            log.debug("Received response from registry (first 200 chars): {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);

            JsonObject rootObject = gson.fromJson(response, JsonObject.class);
            if (rootObject == null) {
                log.error("Failed to parse JSON response from component registry. Response was: {}", response);
                return components;
            }

            log.debug("Root object keys: {}", rootObject.keySet());

            JsonElement componentsElement = rootObject.get("components");
            if (componentsElement == null || !componentsElement.isJsonArray()) {
                log.error("Response from component registry does not contain 'components' array. Root keys: {}, Response: {}",
                    rootObject.keySet(), response);
                return components;
            }

            JsonArray fullJson = componentsElement.getAsJsonArray();
            log.debug("Retrieved {} components from registry", fullJson.size());

            boolean showArch = false;
            if (showArchived.isPresent()) {
                showArch = Boolean.parseBoolean(showArchived.get());
            }

            for (int i = 0; i < fullJson.size(); i++) {
                JsonElement element = fullJson.get(i);
                if (element == null || !element.isJsonObject()) {
                    log.warn("Skipping null or non-object element at index {}", i);
                    continue;
                }

                JsonObject jsonComponent = element.getAsJsonObject();

                // Check for required fields
                JsonElement nameElement = jsonComponent.get("name");
                JsonElement ownerElement = jsonComponent.get("componentOwner");
                JsonElement idElement = jsonComponent.get("id");

                if (idElement == null || idElement.isJsonNull()) {
                    log.warn("Skipping component at index {} - missing 'id' field", i);
                    continue;
                }

                // Check if archived (skip if showArchived is false)
                if (nameElement != null && !nameElement.isJsonNull()) {
                    String name = getStringOrNullFromJson(nameElement);
                    if (isArchived(name) && !showArch) {
                        log.debug("Skipping archived component: {}", name);
                        continue;
                    }
                }

                // Check owner filter
                if (owner.isPresent() && ownerElement != null && !ownerElement.isJsonNull()) {
                    String componentOwner = ownerElement.getAsString();
                    if (!componentOwner.equals(owner.get())) {
                        log.debug("Skipping component with different owner: {}", componentOwner);
                        continue;
                    }
                } else if (owner.isPresent()) {
                    log.debug("Skipping component with missing owner field");
                    continue;
                }

                components.add(idElement.getAsString());
            }

            log.debug("Filtered to {} components based on criteria", components.size());
            return components;

        } catch (IOException e) {
            log.error("IOException while fetching components from registry: {}", e.getMessage(), e);
            // Return empty list instead of throwing - allows app to continue
            return components;
        } catch (Exception e) {
            log.error("Unexpected error while processing components from registry: {}", e.getMessage(), e);
            // Return empty list instead of throwing - allows app to continue
            return components;
        }
    }

}