package com.flagsense.model;

import java.io.Serializable;
import java.util.Set;

public class ExperimentDTO implements Serializable {
    private Set<String> eventNames;

    public Set<String> getEventNames() {
        return eventNames;
    }
}
