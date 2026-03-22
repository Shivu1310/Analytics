package com.liflab.analytics.service;

import com.liflab.analytics.model.AnalyticsEvent;
import com.liflab.analytics.model.DashboardMetrics;
import com.liflab.analytics.model.PageMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String ACTIVE_USERS_KEY = "analytics:active_users";
    private static final String ACTIVE_SESSIONS_KEY = "analytics:active_sessions";
    private static final String PAGE_EVENT_TIMELINE_KEY = "analytics:page_event_timeline";
    private static final String PAGE_EVENT_DATA_KEY = "analytics:page_event_data";

    private final StringRedisTemplate redisTemplate;

    @Value("${analytics.active-window-seconds:300}")
    private long activeWindowSeconds;

    @Value("${analytics.page-window-seconds:900}")
    private long pageWindowSeconds;

    public void recordEvent(AnalyticsEvent event) {
        long eventEpochSeconds = event.getTimestamp().getEpochSecond();

        pruneActiveState(eventEpochSeconds);
        prunePageEvents(eventEpochSeconds);

        redisTemplate.opsForZSet().add(ACTIVE_USERS_KEY, event.getUserId(), eventEpochSeconds);
        redisTemplate.opsForZSet().add(ACTIVE_SESSIONS_KEY, sessionMember(event.getSessionId(), event.getUserId()), eventEpochSeconds);

        String pageEventId = UUID.randomUUID().toString();
        redisTemplate.opsForZSet().add(PAGE_EVENT_TIMELINE_KEY, pageEventId, eventEpochSeconds);
        redisTemplate.opsForHash().put(PAGE_EVENT_DATA_KEY, pageEventId, event.getPageUrl());
    }

    public DashboardMetrics getMetrics() {
        long now = Instant.now().getEpochSecond();

        pruneActiveState(now);
        prunePageEvents(now);

        Long activeUsers = redisTemplate.opsForZSet().zCard(ACTIVE_USERS_KEY);
        Long activeSessions = redisTemplate.opsForZSet().zCard(ACTIVE_SESSIONS_KEY);

        UserSessionSummary userSessionSummary = buildUserSessionSummary(now);
        List<PageMetric> topPages = buildTopPages(now);

        return new DashboardMetrics(
                Objects.requireNonNullElse(activeUsers, 0L),
                Objects.requireNonNullElse(activeSessions, 0L),
                userSessionSummary.maxSessionsForSingleUser,
                userSessionSummary.userIdWithMaxSessions,
                topPages
        );
    }

    private UserSessionSummary buildUserSessionSummary(long nowEpochSeconds) {
        long cutoff = nowEpochSeconds - activeWindowSeconds;
        Set<String> activeSessionMembers = redisTemplate.opsForZSet().rangeByScore(ACTIVE_SESSIONS_KEY, cutoff, Double.POSITIVE_INFINITY);

        if (activeSessionMembers == null || activeSessionMembers.isEmpty()) {
            return new UserSessionSummary(null, 0L);
        }

        Map<String, Long> sessionsPerUser = new HashMap<>();
        for (String member : activeSessionMembers) {
            String[] parts = parseSessionMember(member);
            if (parts == null) {
                continue;
            }
            String userId = parts[1];
            sessionsPerUser.put(userId, sessionsPerUser.getOrDefault(userId, 0L) + 1);
        }

        String maxUserId = null;
        long maxSessions = 0;
        for (Map.Entry<String, Long> entry : sessionsPerUser.entrySet()) {
            if (entry.getValue() > maxSessions) {
                maxSessions = entry.getValue();
                maxUserId = entry.getKey();
            }
        }

        return new UserSessionSummary(maxUserId, maxSessions);
    }

    private List<PageMetric> buildTopPages(long nowEpochSeconds) {
        long cutoff = nowEpochSeconds - pageWindowSeconds;

        Set<String> eventIds = redisTemplate.opsForZSet().rangeByScore(PAGE_EVENT_TIMELINE_KEY, cutoff, Double.POSITIVE_INFINITY);
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Object> pageValues = redisTemplate.opsForHash().multiGet(PAGE_EVENT_DATA_KEY, new ArrayList<>(eventIds));
        if (pageValues == null || pageValues.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Long> pageCounts = new HashMap<>();
        for (Object pageObj : pageValues) {
            if (pageObj == null) {
                continue;
            }
            String page = pageObj.toString();
            pageCounts.put(page, pageCounts.getOrDefault(page, 0L) + 1);
        }

        return pageCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> new PageMetric(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private void pruneActiveState(long nowEpochSeconds) {
        long cutoff = nowEpochSeconds - activeWindowSeconds;
        redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_USERS_KEY, 0, cutoff);
        redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_SESSIONS_KEY, 0, cutoff);
    }

    private void prunePageEvents(long nowEpochSeconds) {
        long cutoff = nowEpochSeconds - pageWindowSeconds;
        Set<String> expiredEventIds = redisTemplate.opsForZSet().rangeByScore(PAGE_EVENT_TIMELINE_KEY, 0, cutoff);

        if (expiredEventIds == null || expiredEventIds.isEmpty()) {
            return;
        }

        redisTemplate.opsForZSet().removeRangeByScore(PAGE_EVENT_TIMELINE_KEY, 0, cutoff);
        redisTemplate.opsForHash().delete(PAGE_EVENT_DATA_KEY, expiredEventIds.toArray());
    }

    private String sessionMember(String sessionId, String userId) {
        return sessionId + "|" + userId;
    }

    private String[] parseSessionMember(String member) {
        int delimiter = member.indexOf('|');
        if (delimiter <= 0 || delimiter >= member.length() - 1) {
            return null;
        }
        String sessionId = member.substring(0, delimiter);
        String userId = member.substring(delimiter + 1);
        return new String[]{sessionId, userId};
    }

    private static class UserSessionSummary {
        private final String userIdWithMaxSessions;
        private final long maxSessionsForSingleUser;

        private UserSessionSummary(String userIdWithMaxSessions, long maxSessionsForSingleUser) {
            this.userIdWithMaxSessions = userIdWithMaxSessions;
            this.maxSessionsForSingleUser = maxSessionsForSingleUser;
        }
    }
}
