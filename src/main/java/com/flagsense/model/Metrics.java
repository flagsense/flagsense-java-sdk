package com.flagsense.model;

import java.math.BigDecimal;

public class Metrics {
    private Long count;
    private BigDecimal total;
    private BigDecimal minimum;
    private BigDecimal maximum;

    public static Metrics of(double value) {
        return new Metrics(value);
    }

    public static Metrics EMPTY() {
        return new Metrics();
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getMinimum() {
        return minimum;
    }

    public void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    public BigDecimal getMaximum() {
        return maximum;
    }

    public void setMaximum(BigDecimal maximum) {
        this.maximum = maximum;
    }

    private Metrics(double value) {
        this.count = 1L;
        this.total = BigDecimal.valueOf(value);
        this.minimum = BigDecimal.valueOf(value);
        this.maximum = BigDecimal.valueOf(value);
    }

    private Metrics() {
        this.count = 0L;
        this.total = null;
        this.minimum = null;
        this.maximum = null;
    }
}
