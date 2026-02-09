package org.octopusden.octopus.lifecycle.api;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient;
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider;
import org.octopusden.octopus.components.registry.core.dto.ComponentV1;
import org.octopusden.octopus.components.registry.core.dto.ComponentV3;
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
    @Value("${app.componentsRegistryService.baseUrl}")
    public String componentsRegistryServiceUrl;

    @Autowired
    private Gson gson;

    @Autowired
    private BuildsManager buildsManager;

    public ComponentsManager() {}

    private ClassicComponentsRegistryServiceClient componentsRegistryServiceClient;

    @PostConstruct
    public void initComponentsManager() {
        componentsRegistryServiceClient = new ClassicComponentsRegistryServiceClient(new ClassicComponentsRegistryServiceClientUrlProvider() {
            @NotNull
            @Override
            public String getApiUrl() {
                return componentsRegistryServiceUrl;
            }
        });
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

    @Cacheable(value = "getComponent", key = "#componentId")
    public Component getComponent(String componentId) throws IOException {
        System.out.println(componentsRegistryServiceClient.getById(componentId));

        Component component = new Component();
        component.id = componentsRegistryServiceClient.getById(componentId).getId();
        component.name = componentsRegistryServiceClient.getById(componentId).getName();
        component.componentOwner = componentsRegistryServiceClient.getById(componentId).getComponentOwner();
        component.securityChampion = componentsRegistryServiceClient.getById(componentId).getSecurityChampion();
        component.releaseManager = componentsRegistryServiceClient.getById(componentId).getReleaseManager();
        component.isArchived = componentsRegistryServiceClient.getById(componentId).getArchived();

        component.distributionExplicit = componentsRegistryServiceClient.getById(componentId).getDistribution().getExplicit();
        component.distributionExternal = componentsRegistryServiceClient.getById(componentId).getDistribution().getExternal();

        component.distributionDocker = Collections.singletonList(componentsRegistryServiceClient.getById(componentId).getDistribution().getDocker());

        component.distributionGAV = Collections.singletonList(componentsRegistryServiceClient.getById(componentId).getDistribution().getGav());

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
    public List<String> getComponents(Optional<String> showArchived, Optional<String> owner) {
        ArrayList<String> componentIds = new ArrayList<>();

        boolean showArch = showArchived.isPresent() && showArchived.get().equals("true");

        for (ComponentV3 component : componentsRegistryServiceClient.getComponents()) {
            if (component.getComponent().getArchived() && !showArch) continue;
            if (owner.isPresent() && !component.getComponent().getComponentOwner().equals(owner.get())) continue;
            componentIds.add(component.getComponent().getId());
        }

        return componentIds;
    }

}