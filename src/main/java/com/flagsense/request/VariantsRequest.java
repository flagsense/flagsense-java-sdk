package com.flagsense.request;

import com.flagsense.enums.Environment;

import java.util.concurrent.ConcurrentMap;

public class VariantsRequest {
    private String machineId;
    private String sdkType;
    private Environment environment;
    private ConcurrentMap<String, ConcurrentMap<String, Long>> data;
    private ConcurrentMap<String, Long> errors;
    private Long time;

    public VariantsRequest(String machineId, Environment environment, ConcurrentMap<String, ConcurrentMap<String, Long>> data, ConcurrentMap<String, Long> errors, Long time) {
        this.machineId = machineId;
        this.sdkType = "java";
        this.environment = environment;
        this.data = data;
        this.errors = errors;
        this.time = time;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getSdkType() {
        return sdkType;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public ConcurrentMap<String, ConcurrentMap<String, Long>> getData() {
        return data;
    }

    public void setData(ConcurrentMap<String, ConcurrentMap<String, Long>> data) {
        this.data = data;
    }

    public ConcurrentMap<String, Long> getErrors() {
        return errors;
    }

    public void setErrors(ConcurrentMap<String, Long> errors) {
        this.errors = errors;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
