package com.flagsense.request;

import com.flagsense.enums.Environment;
import com.flagsense.model.Metrics;

import java.util.concurrent.ConcurrentMap;

public class ExperimentEventsRequest {
    private String machineId;
    private String sdkType;
    private Environment environment;
    private ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, Metrics>>> experimentEvents;
    private Long time;

    public ExperimentEventsRequest(String machineId, Environment environment, ConcurrentMap<String,
            ConcurrentMap<String, ConcurrentMap<String, Metrics>>> experimentEvents, Long time) {
        this.machineId = machineId;
        this.sdkType = "java";
        this.environment = environment;
        this.experimentEvents = experimentEvents;
        this.time = time;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getSdkType() {
        return sdkType;
    }

    public void setSdkType(String sdkType) {
        this.sdkType = sdkType;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, Metrics>>> getExperimentEvents() {
        return experimentEvents;
    }

    public void setExperimentEvents(ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, Metrics>>> experimentEvents) {
        this.experimentEvents = experimentEvents;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
