package com.liflab.analytics.controller;

import com.liflab.analytics.model.AnalyticsEvent;
import com.liflab.analytics.model.DashboardMetrics;
import com.liflab.analytics.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingestEvent(@Valid @RequestBody AnalyticsEvent event) {
        analyticsService.recordEvent(event);
    }

    @GetMapping("/metrics")
    public DashboardMetrics metrics() {
        return analyticsService.getMetrics();
    }
}
