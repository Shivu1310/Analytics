package com.liflab.analytics.model;

import java.util.List;

public class DashboardMetrics {
    private long activeUsers;
    private long activeSessions;
    private long activeSessionsForSameUser;
    private String referenceUserId;
    private List<PageMetric> topPages;

    public DashboardMetrics(
            long activeUsers,
            long activeSessions,
            long activeSessionsForSameUser,
            String referenceUserId,
            List<PageMetric> topPages
    ) {
        this.activeUsers = activeUsers;
        this.activeSessions = activeSessions;
        this.activeSessionsForSameUser = activeSessionsForSameUser;
        this.referenceUserId = referenceUserId;
        this.topPages = topPages;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(long activeSessions) {
        this.activeSessions = activeSessions;
    }

    public long getActiveSessionsForSameUser() {
        return activeSessionsForSameUser;
    }

    public void setActiveSessionsForSameUser(long activeSessionsForSameUser) {
        this.activeSessionsForSameUser = activeSessionsForSameUser;
    }

    public String getReferenceUserId() {
        return referenceUserId;
    }

    public void setReferenceUserId(String referenceUserId) {
        this.referenceUserId = referenceUserId;
    }

    public List<PageMetric> getTopPages() {
        return topPages;
    }

    public void setTopPages(List<PageMetric> topPages) {
        this.topPages = topPages;
    }
}
