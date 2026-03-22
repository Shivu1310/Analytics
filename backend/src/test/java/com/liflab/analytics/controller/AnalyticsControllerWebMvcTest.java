package com.liflab.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liflab.analytics.model.AnalyticsEvent;
import com.liflab.analytics.model.DashboardMetrics;
import com.liflab.analytics.model.PageMetric;
import com.liflab.analytics.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@Import({ApiExceptionHandler.class})
class AnalyticsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void postEventsShouldReturnAccepted() throws Exception {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setTimestamp(Instant.parse("2024-03-15T14:30:00Z"));
        event.setUserId("usr_789");
        event.setEventType("page_view");
        event.setPageUrl("/products/electronics");
        event.setSessionId("sess_456");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        verify(analyticsService).recordEvent(event);
    }

    @Test
    void postEventsShouldReturnBadRequestForInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                  "timestamp": "2024-03-15T14:30:00Z",
                  "user_id": "bad user",
                  "event_type": "page_view",
                  "page_url": "products/electronics",
                  "session_id": "sess_456"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.user_id").exists())
                .andExpect(jsonPath("$.errors.page_url").exists());
    }

    @Test
    void postEventsShouldReturnBadRequestForMalformedJson() throws Exception {
        String malformedPayload = "{";

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void getMetricsShouldReturnPayload() throws Exception {
        DashboardMetrics metrics = new DashboardMetrics(
                42L,
                50L,
                3L,
                "usr_789",
                List.of(new PageMetric("/offers", 9L))
        );
        when(analyticsService.getMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUsers").value(42))
                .andExpect(jsonPath("$.activeSessions").value(50))
                .andExpect(jsonPath("$.activeSessionsForSameUser").value(3))
                .andExpect(jsonPath("$.referenceUserId").value("usr_789"))
                .andExpect(jsonPath("$.topPages[0].page").value("/offers"))
                .andExpect(jsonPath("$.topPages[0].count").value(9));
    }
}
