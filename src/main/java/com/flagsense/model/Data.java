package com.flagsense.model;

import java.util.Map;

public class Data {
    private long lastUpdatedOn;
    private Map<String, SegmentDTO> segments;
    private Map<String, FlagDTO> flags;
    private Map<String, ExperimentDTO> experiments;

    public Data() {
        this.lastUpdatedOn = 0;
        this.segments = null;
        this.flags = null;
    }

    public Long getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public void setLastUpdatedOn(Long lastUpdatedOn) {
        this.lastUpdatedOn = lastUpdatedOn;
    }

    public Map<String, SegmentDTO> getSegments() {
        return segments;
    }

    public void setSegments(Map<String, SegmentDTO> segments) {
        this.segments = segments;
    }

    public Map<String, FlagDTO> getFlags() {
        return flags;
    }

    public void setFlags(Map<String, FlagDTO> flags) {
        this.flags = flags;
    }

    public Map<String, ExperimentDTO> getExperiments() {
        return experiments;
    }

    public void setExperiments(Map<String, ExperimentDTO> experiments) {
        this.experiments = experiments;
    }
}
