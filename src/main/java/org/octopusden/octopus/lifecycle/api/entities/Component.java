package org.octopusden.octopus.lifecycle.api.entities;

import java.io.Serializable;
import java.util.List;

public class Component implements Serializable {
    public String id;
    public String name;

    public boolean isArchived;

    public String componentOwner;
    public String securityChampion;
    public String releaseManager;

    public boolean distributionExplicit;
    public boolean distributionExternal;
    public List<String> distributionDocker;
    public List<String> distributionGAV;

    public List<String> artifact;
}
