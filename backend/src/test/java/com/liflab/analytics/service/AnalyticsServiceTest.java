package com.liflab.analytics.service;

import com.liflab.analytics.model.AnalyticsEvent;
import com.liflab.analytics.model.DashboardMetrics;
import com.liflab.analytics.model.PageMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private StringRedisTemplate redisTemplate;
    private ZSetOperations<String, String> zSetOperations;
    private HashOperations<String, String, String> hashOperations;
    private AnalyticsService analyticsService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        zSetOperations = mock(ZSetOperations.class);
        hashOperations = mock(HashOperations.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        analyticsService = new AnalyticsService(redisTemplate);
        ReflectionTestUtils.setField(analyticsService, "activeWindowSeconds", 300L);
        ReflectionTestUtils.setField(analyticsService, "pageWindowSeconds", 900L);
    }

    @Test
    void recordEventShouldWriteAndPrune() {
        AnalyticsEvent event = validEvent();

        when(zSetOperations.rangeByScore(eq("analytics:page_event_timeline"), anyDouble(), anyDouble()))
                .thenReturn(Set.of());

        analyticsService.recordEvent(event);

        verify(zSetOperations).add(eq("analytics:active_users"), eq("usr_789"), anyLong());
        verify(zSetOperations).add(eq("analytics:active_sessions"), eq("sess_456|usr_789"), anyLong());
        verify(zSetOperations).add(eq("analytics:page_event_timeline"), anyString(), anyDouble());
        verify(hashOperations).put(eq("analytics:page_event_data"), anyString(), eq("/products/electronics"));

        verify(zSetOperations).removeRangeByScore(eq("analytics:active_users"), eq(0.0), anyDouble());
        verify(zSetOperations).removeRangeByScore(eq("analytics:active_sessions"), eq(0.0), anyDouble());
        verify(zSetOperations).removeRangeByScore(eq("analytics:page_event_timeline"), eq(0.0), anyDouble());
    }

    @Test
    void getMetricsShouldBuildExpectedDashboard() {
        when(zSetOperations.zCard("analytics:active_users")).thenReturn(12L);
        when(zSetOperations.zCard("analytics:active_sessions")).thenReturn(20L);

        when(zSetOperations.rangeByScore(eq("analytics:active_sessions"), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                .thenReturn(Set.of("sessA|userA", "sessB|userA", "sessC|userB", "invalid"));

        when(zSetOperations.rangeByScore(eq("analytics:page_event_timeline"), eq(0.0), anyDouble()))
                .thenReturn(Set.of());

        Set<String> eventIds = new LinkedHashSet<>(Arrays.asList("id1", "id2", "id3", "id4", "id5", "id6"));
        when(zSetOperations.rangeByScore(eq("analytics:page_event_timeline"), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                .thenReturn(eventIds);

        when(hashOperations.multiGet(eq("analytics:page_event_data"), any(List.class)))
                .thenReturn(Arrays.asList("/a", "/a", "/b", "/c", "/c", "/c"));

        DashboardMetrics metrics = analyticsService.getMetrics();

        assertEquals(12L, metrics.getActiveUsers());
        assertEquals(20L, metrics.getActiveSessions());
        assertEquals(2L, metrics.getActiveSessionsForSameUser());
        assertEquals("userA", metrics.getReferenceUserId());

        List<PageMetric> topPages = metrics.getTopPages();
        assertNotNull(topPages);
        assertEquals(3, topPages.size());
        assertEquals("/c", topPages.get(0).getPage());
        assertEquals(3L, topPages.get(0).getCount());

        verify(zSetOperations, times(2)).rangeByScore(eq("analytics:page_event_timeline"), anyDouble(), anyDouble());
    }

    @Test
    void getMetricsShouldReturnEmptyDefaultsWhenRedisIsEmpty() {
        when(zSetOperations.zCard("analytics:active_users")).thenReturn(null);
        when(zSetOperations.zCard("analytics:active_sessions")).thenReturn(null);
        when(zSetOperations.rangeByScore(eq("analytics:active_sessions"), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                .thenReturn(Set.of());
        when(zSetOperations.rangeByScore(eq("analytics:page_event_timeline"), eq(0.0), anyDouble()))
                .thenReturn(Set.of());
        when(zSetOperations.rangeByScore(eq("analytics:page_event_timeline"), anyDouble(), eq(Double.POSITIVE_INFINITY)))
                .thenReturn(Set.of());

        DashboardMetrics metrics = analyticsService.getMetrics();

        assertEquals(0L, metrics.getActiveUsers());
        assertEquals(0L, metrics.getActiveSessions());
        assertEquals(0L, metrics.getActiveSessionsForSameUser());
        assertEquals(null, metrics.getReferenceUserId());
        assertTrue(metrics.getTopPages().isEmpty());
    }

    private AnalyticsEvent validEvent() {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setTimestamp(Instant.parse("2024-03-15T14:30:00Z"));
        event.setUserId("usr_789");
        event.setEventType("page_view");
        event.setPageUrl("/products/electronics");
        event.setSessionId("sess_456");
        return event;
    }
}
