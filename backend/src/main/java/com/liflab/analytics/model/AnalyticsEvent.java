package com.liflab.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class AnalyticsEvent {

    @JsonProperty("user_id")
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "user_id must be alphanumeric with optional _ or -")
    private String userId;

    @JsonProperty("event_type")
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "event_type contains invalid characters")
    private String eventType;

    @JsonProperty("page_url")
    @NotBlank
    @Size(max = 2048)
    @Pattern(
            regexp = "^(https?://.+|/.*)$",
            message = "page_url must be an absolute URL or start with /"
    )
    private String pageUrl;

    @JsonProperty("session_id")
    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "session_id must be alphanumeric with optional _ or -")
    private String sessionId;

    @NotNull
    private Instant timestamp;
}
