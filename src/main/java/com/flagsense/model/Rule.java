package com.flagsense.model;

import com.flagsense.enums.KeyType;
import com.flagsense.enums.Operator;

import java.util.List;

public class Rule<T> {
    private String key;
    private KeyType type;
    private Operator operator;
    private List<T> values;
    private Boolean match;

    public String getKey() {
        return key;
    }

    public KeyType getType() {
        return type;
    }

    public Operator getOperator() {
        return operator;
    }

    public List<T> getValues() {
        return values;
    }

    public Boolean getMatch() {
        return match;
    }
}
