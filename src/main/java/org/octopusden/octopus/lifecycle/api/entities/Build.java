package org.octopusden.octopus.lifecycle.api.entities;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_EMPTY)
public class Build implements Serializable {
    public String id;
    public String buildDate;
    public String rcDate;
    public String status;

    public List<String> dependencies;
}
