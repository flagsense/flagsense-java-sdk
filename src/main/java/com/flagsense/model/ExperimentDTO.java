package com.flagsense.model;

import java.io.Serializable;
import java.util.Set;

public class ExperimentDTO implements Serializable {
    private String id;
    private String flagId;
    private Set<String> eventNames;

    public String getId() {
        return id;
    }

    public String getFlagId() {
        return flagId;
    }

    public Set<String> getEventNames() {
        return eventNames;
    }
}
