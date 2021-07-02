package com.flagsense.model;

import java.util.List;

public class SegmentDTO {
    private String id;
    private List<List<Rule>> rules;

    public String getId() {
        return id;
    }

    public List<List<Rule>> getRules() {
        return rules;
    }
}
