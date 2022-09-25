package com.flagsense.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentDTO implements Serializable {
    private Set<String> eventNames;

    public Set<String> getEventNames() {
        return eventNames;
    }
}
