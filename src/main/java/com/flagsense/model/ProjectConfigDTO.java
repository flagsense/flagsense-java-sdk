package com.flagsense.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectConfigDTO {
    private Boolean captureDeviceEvents;

    public Boolean getCaptureDeviceEvents() {
        return captureDeviceEvents;
    }
}
