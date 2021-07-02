package com.flagsense.services;

public interface EventService {
    void start();
    void addEvaluationCount(String flagId, String variantKey);
    void addErrorsCount(String flagId);
}