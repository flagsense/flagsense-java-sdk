package com.flagsense.model;

import com.flagsense.enums.VariantType;

import java.util.List;
import java.util.Map;

public class FlagDTO {
    private String id;
    private Integer seed;
    private VariantType type;
    private Map<String, Variant> variants;
    private List<String> variantsOrder;
    private EnvData envData;

    public String getId() {
        return id;
    }

    public Integer getSeed() {
        return seed;
    }

    public VariantType getType() {
        return type;
    }

    public Map<String, Variant> getVariants() {
        return variants;
    }

    public List<String> getVariantsOrder() {
        return variantsOrder;
    }

    public EnvData getEnvData() {
        return envData;
    }
}
