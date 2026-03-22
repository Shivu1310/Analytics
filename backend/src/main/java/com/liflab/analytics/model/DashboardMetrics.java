package com.liflab.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DashboardMetrics {
    private long activeUsers;
    private long activeSessions;
    private long activeSessionsForSameUser;
    private String referenceUserId;
    private List<PageMetric> topPages;
}
