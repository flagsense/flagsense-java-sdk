package com.flagsense;

import com.fasterxml.jackson.databind.JsonNode;
import com.flagsense.enums.Environment;
import com.flagsense.model.FSFlag;
import com.flagsense.model.FSUser;
import com.flagsense.services.FlagsenseService;
import com.flagsense.services.impl.FlagsenseServiceImpl;
import com.flagsense.util.FlagsenseException;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Flagsense {
    private static final ConcurrentMap<String, FlagsenseService> flagsenseServiceMap = new ConcurrentHashMap<>();

    private Flagsense() {
    }

    public static FlagsenseService createService(String sdkId, String sdkSecret, String env) {
        if (StringUtils.isBlank(sdkId) || StringUtils.isBlank(sdkSecret))
            throw new FlagsenseException("Empty sdk params not allowed");

        if (!flagsenseServiceMap.containsKey(sdkId)) {
            Environment environment = Environment.isValid(env) ? Environment.valueOf(env) : Environment.PROD;
            synchronized (Flagsense.class) {
                if (!flagsenseServiceMap.containsKey(sdkId))
                    flagsenseServiceMap.put(sdkId, new FlagsenseServiceImpl(sdkId, sdkSecret, environment));
            }
        }

        return flagsenseServiceMap.get(sdkId);
    }

    public static FSUser user(String userId) {
        return new FSUser(userId, null);
    }

    public static FSUser user(String userId, Map<String, Object> attributes) {
        return new FSUser(userId, attributes);
    }

    public static FSFlag<Boolean> booleanFlag(String flagId, String defaultKey, boolean defaultValue) {
        return new FSFlag<>(flagId, defaultKey, defaultValue);
    }

    public static FSFlag<Integer> integerFlag(String flagId, String defaultKey, int defaultValue) {
        return new FSFlag<>(flagId, defaultKey, defaultValue);
    }

    public static FSFlag<Double> decimalFlag(String flagId, String defaultKey, double defaultValue) {
        return new FSFlag<>(flagId, defaultKey, defaultValue);
    }

    public static FSFlag<String> stringFlag(String flagId, String defaultKey, String defaultValue) {
        return new FSFlag<>(flagId, defaultKey, defaultValue);
    }

    public static FSFlag<JsonNode> jsonFlag(String flagId, String defaultKey, JsonNode defaultValue) {
        return new FSFlag<>(flagId, defaultKey, defaultValue);
    }

    public static FSFlag<Map<String, Object>> mapFlag(String flagId, String defaultKey, Map<String, Object> defaultValue) {
        return new FSFlag<>(flagId, defaultKey, defaultValue);
    }
}
