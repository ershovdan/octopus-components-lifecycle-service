package org.octopusden.octopus.lifecycle.api;

import org.octopusden.octopus.lifecycle.api.entities.Build;
import org.octopusden.octopus.lifecycle.api.entities.Component;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ComponentsManager {
    @Value("${app.componentsRegistryService.baseUrl}${app.componentsRegistryService.path}")
    public String componentsRegistryServiceUrl;

    @Autowired
    private Gson gson;

    @Autowired
    private BuildsManager buildsManager;

    public ComponentsManager() {}

    private String getStringOrNullFromJson(JsonElement jsonElement) {
        if (!jsonElement.isJsonNull()) return jsonElement.getAsString();
        return null;
    }

    private boolean isArchived(String componentName) {
        if (componentName == null) return false;
        return componentName.indexOf("(archived)") == componentName.length() - 10;
    }

    private String getRequest(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        try {
            return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
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


//    @Cacheable(value = "getComponent2", key = "#componentId")
//    public Component getComponent2(String componentId) throws IOException {
//
//    }



    @Cacheable(value = "getComponent", key = "#componentId")
    public Component getComponent(String componentId) throws IOException {
        JsonObject fullJson = gson.fromJson(getRequest(componentsRegistryServiceUrl + componentId), JsonObject.class).getAsJsonObject();

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
        JsonArray fullJson = gson.fromJson(getRequest(componentsRegistryServiceUrl), JsonObject.class).getAsJsonObject().get("components").getAsJsonArray();
        List<String> components = new ArrayList<>();

        boolean showArch = false;
        if (showArchived.isPresent()) {
            showArch = Boolean.parseBoolean(showArchived.get());
        }

        for (int i = 0; i < fullJson.size(); i++) {
            JsonObject jsonComponent = fullJson.get(i).getAsJsonObject().getAsJsonObject();

            if (isArchived(getStringOrNullFromJson(jsonComponent.get("name"))) && !showArch) continue;
            if (owner.isPresent() && !jsonComponent.get("componentOwner").getAsString().equals(owner.get())) continue;

            components.add(jsonComponent.get("id").getAsString());
        }

        return components;
    }

}