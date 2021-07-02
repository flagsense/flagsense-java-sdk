package com.flagsense.request;

import com.flagsense.enums.Environment;

public class FetchLatestRequest {
    private Environment environment;
    private Long lastUpdatedOn;

    public FetchLatestRequest(Environment environment, Long lastUpdatedOn) {
        this.environment = environment;
        this.lastUpdatedOn = lastUpdatedOn;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Long getLastUpdatedOn() {
        return lastUpdatedOn;
    }
}
