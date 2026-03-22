package com.liflab.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageMetric {
    private String page;
    private long count;
}
