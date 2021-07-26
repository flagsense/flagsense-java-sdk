package com.flagsense.services.impl;

import com.flagsense.enums.Environment;
import com.flagsense.enums.VariantType;
import com.flagsense.model.*;
import com.flagsense.services.DataPollerService;
import com.flagsense.services.EventService;
import com.flagsense.services.FlagsenseService;
import com.flagsense.services.UserVariantService;
import com.flagsense.util.FlagsenseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class FlagsenseServiceImpl implements FlagsenseService {

    private final ObjectMapper objectMapper;
    private final Data data;
    private final SdkConfig sdkConfig;
    private final UserVariantService userVariantService;
    private final DataPollerService dataPollerService;
    private final EventService eventService;

    public FlagsenseServiceImpl(String sdkId, String sdkSecret, Environment environment) {
        this.objectMapper = new ObjectMapper();
        this.sdkConfig = new SdkConfig(sdkId, sdkSecret, environment);
        this.data = new Data();
        this.userVariantService = new UserVariantServiceImpl(this.data);
        this.dataPollerService = new DataPollerServiceImpl(this.data, this.sdkConfig);
        this.eventService = new EventServiceImpl(this.sdkConfig);
        this.dataPollerService.start();
        this.eventService.start();
    }

    @Override
    public boolean initializationComplete() {
        return this.data.getLastUpdatedOn() > 0;
    }

    @Override
    public void waitForInitializationComplete() {
        try {
            synchronized (this.data) {
                while (!this.initializationComplete())
                    this.data.wait();
            }
        }
        catch (InterruptedException e) {
//             System.out.println(e.toString());
        }
    }

    @Override
    public FSVariation<Boolean> booleanVariation(FSFlag<Boolean> fsFlag, FSUser fsUser) {
        try {
            return (FSVariation<Boolean>) this.evaluate(fsFlag, fsUser, VariantType.BOOL);
        }
        catch (Exception e) {
            return new FSVariation<>(fsFlag.getDefaultKey(), fsFlag.getDefaultValue());
        }
    }

    @Override
    public FSVariation<Integer> integerVariation(FSFlag<Integer> fsFlag, FSUser fsUser) {
        try {
            return (FSVariation<Integer>) this.evaluate(fsFlag, fsUser, VariantType.INT);
        }
        catch (Exception e) {
            return new FSVariation<>(fsFlag.getDefaultKey(), fsFlag.getDefaultValue());
        }
    }

    @Override
    public FSVariation<Double> decimalVariation(FSFlag<Double> fsFlag, FSUser fsUser) {
        try {
            return (FSVariation<Double>) this.evaluate(fsFlag, fsUser, VariantType.DOUBLE);
        }
        catch (Exception e) {
            return new FSVariation<>(fsFlag.getDefaultKey(), fsFlag.getDefaultValue());
        }
    }

    @Override
    public FSVariation<String> stringVariation(FSFlag<String> fsFlag, FSUser fsUser) {
        try {
            return (FSVariation<String>) this.evaluate(fsFlag, fsUser, VariantType.STRING);
        }
        catch (Exception e) {
            return new FSVariation<>(fsFlag.getDefaultKey(), fsFlag.getDefaultValue());
        }
    }

    @Override
    public FSVariation<JsonNode> jsonVariation(FSFlag<JsonNode> fsFlag, FSUser fsUser) {
        try {
            FSVariation<Map<String, Object>> fsVariation = (FSVariation<Map<String, Object>>) this.evaluate(fsFlag, fsUser, VariantType.JSON);
            return new FSVariation<>(fsVariation.getKey(), objectMapper.valueToTree(fsVariation.getValue()));
        }
        catch (Exception e) {
            return new FSVariation<>(fsFlag.getDefaultKey(), fsFlag.getDefaultValue());
        }
    }

    @Override
    public FSVariation<Map<String, Object>> mapVariation(FSFlag<Map<String, Object>> fsFlag, FSUser fsUser) {
        try {
            return (FSVariation<Map<String, Object>>) this.evaluate(fsFlag, fsUser, VariantType.JSON);
        }
        catch (Exception e) {
            return new FSVariation<>(fsFlag.getDefaultKey(), fsFlag.getDefaultValue());
        }
    }

    @Override
    public void recordCodeError(String flagId, String variationKey) {
        if (StringUtils.isNotBlank(flagId) && StringUtils.isNotBlank(variationKey))
            this.eventService.addCodeBugsCount(flagId, variationKey);
    }

    private FSVariation<?> evaluate(FSFlag<?> fsFlag, FSUser fsUser, VariantType expectedVariantType) {
        UserVariantDTO userVariantDTO = UserVariantDTO.builder()
                .flagId(fsFlag.getFlagId())
                .userId(fsUser.getUserId())
                .attributes(fsUser.getAttributes())
                .defaultKey(fsFlag.getDefaultKey())
                .defaultValue(fsFlag.getDefaultValue())
                .expectedVariantType(expectedVariantType)
                .build();

        this.evaluate(userVariantDTO);
        return new FSVariation<>(userVariantDTO.getKey(), userVariantDTO.getValue());
    }

    private void evaluate(UserVariantDTO userVariantDTO) {
        try {
            if (this.data.getLastUpdatedOn() == 0)
                throw new FlagsenseException("Loading data");
            this.userVariantService.getUserVariant(userVariantDTO);
            this.eventService.addEvaluationCount(userVariantDTO.getFlagId(), userVariantDTO.getKey());
        }
        catch (Exception e) {
//            System.out.println(e.toString());
            userVariantDTO.setKey(userVariantDTO.getDefaultKey());
            userVariantDTO.setValue(userVariantDTO.getDefaultValue());
            this.eventService.addEvaluationCount(userVariantDTO.getFlagId(), userVariantDTO.getDefaultKey() != null ? userVariantDTO.getDefaultKey() : "default");
            this.eventService.addErrorsCount(userVariantDTO.getFlagId());
        }
    }
}
