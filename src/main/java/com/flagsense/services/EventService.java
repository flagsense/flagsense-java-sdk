package com.flagsense.services;

public interface EventService {
    void start();
    void addEvaluationCount(String flagId, String variantKey);
    void addErrorsCount(String flagId);
    void addCodeBugsCount(String flagId, String variantKey);
    void recordExperimentEvent(String flagId, String eventName, String variantKey, double value);
}
