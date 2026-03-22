package com.liflab.analytics.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsEventValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidEvent() {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setTimestamp(Instant.parse("2024-03-15T14:30:00Z"));
        event.setUserId("usr_789");
        event.setEventType("page_view");
        event.setPageUrl("/products/electronics");
        event.setSessionId("sess_456");

        Set<ConstraintViolation<AnalyticsEvent>> violations = validator.validate(event);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectInvalidEvent() {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setTimestamp(null);
        event.setUserId("bad user");
        event.setEventType("");
        event.setPageUrl("products/electronics");
        event.setSessionId("sess 123");

        Set<ConstraintViolation<AnalyticsEvent>> violations = validator.validate(event);
        assertFalse(violations.isEmpty());
    }
}
