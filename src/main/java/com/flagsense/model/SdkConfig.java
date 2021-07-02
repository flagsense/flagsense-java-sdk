package com.flagsense.model;

import com.flagsense.enums.Environment;

public class SdkConfig {
    private String sdkId;
    private String sdkSecret;
    private Environment environment;

    public SdkConfig(String sdkId, String sdkSecret, Environment environment) {
        this.sdkId = sdkId;
        this.sdkSecret = sdkSecret;
        this.environment = environment;
    }

    public String getSdkId() {
        return sdkId;
    }

    public String getSdkSecret() {
        return sdkSecret;
    }

    public Environment getEnvironment() {
        return environment;
    }
}
