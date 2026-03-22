package com.liflab.analytics.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventIngestionRateLimitFilterTest {

    @Test
    void shouldReturn429WhenLimitExceeded() throws Exception {
        EventIngestionRateLimitFilter filter = new EventIngestionRateLimitFilter();
        ReflectionTestUtils.setField(filter, "maxEventsPerSecond", 2);

        MockHttpServletResponse response1 = execute(filter, "/api/events", "POST");
        MockHttpServletResponse response2 = execute(filter, "/api/events", "POST");
        MockHttpServletResponse response3 = execute(filter, "/api/events", "POST");

        assertEquals(200, response1.getStatus());
        assertEquals(200, response2.getStatus());
        assertEquals(429, response3.getStatus());
    }

    @Test
    void shouldIgnoreOtherRoutes() throws Exception {
        EventIngestionRateLimitFilter filter = new EventIngestionRateLimitFilter();
        ReflectionTestUtils.setField(filter, "maxEventsPerSecond", 1);

        MockHttpServletResponse response = execute(filter, "/api/metrics", "GET");

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletResponse execute(
            EventIngestionRateLimitFilter filter,
            String path,
            String method
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        request.setMethod(method);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }
}
