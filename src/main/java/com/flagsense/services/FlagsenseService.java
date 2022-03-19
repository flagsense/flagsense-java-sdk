package com.flagsense.services;

import com.flagsense.model.FSFlag;
import com.flagsense.model.FSUser;
import com.flagsense.model.FSVariation;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface FlagsenseService {
    boolean initializationComplete();
    void waitForInitializationComplete();
    FSVariation<Boolean> booleanVariation(FSFlag<Boolean> fsFlag, FSUser fsUser);
    FSVariation<Integer> integerVariation(FSFlag<Integer> fsFlag, FSUser fsUser);
    FSVariation<Double> decimalVariation(FSFlag<Double> fsFlag, FSUser fsUser);
    FSVariation<String> stringVariation(FSFlag<String> fsFlag, FSUser fsUser);
    FSVariation<JsonNode> jsonVariation(FSFlag<JsonNode> fsFlag, FSUser fsUser);
    FSVariation<Map<String, Object>> mapVariation(FSFlag<Map<String, Object>> fsFlag, FSUser fsUser);
    void recordCodeError(FSFlag<?> fsFlag, FSUser fsUser);
    void recordEvent(FSUser fsUser, String eventName, String flagId);
    void recordEvent(FSUser fsUser, String eventName, double value, String flagId);
}
