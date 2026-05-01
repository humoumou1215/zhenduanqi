package com.zhenduanqi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MdcFilterTest {

    private MdcFilter mdcFilter;

    @Mock
    private FilterChain filterChain;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-for-unit-test-min-32chars!!", 7200000);
        mdcFilter = new MdcFilter(jwtUtil);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_injectsRequestIdIntoMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        mdcFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_requestIdIsUuidFormat() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            String requestId = MDC.get("requestId");
            assertThat(requestId).isNotBlank();
            assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_clearsMdcAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        mdcFilter.doFilter(request, response, filterChain);

        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("username")).isNull();
        assertThat(MDC.get("clientIp")).isNull();
    }

    @Test
    void doFilter_usernameFromJwtToken() throws ServletException, IOException {
        String token = jwtUtil.generateToken("zhangsan");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("zhenduanqi_token", token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("username")).isEqualTo("zhangsan");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_usernameIsDashWhenNoToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("username")).isEqualTo("-");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_usernameIsDashWhenInvalidToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("zhenduanqi_token", "invalid-token-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("username")).isEqualTo("-");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_clientIpFromRemoteAddr() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("clientIp")).isEqualTo("10.0.1.100");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_clientIpFromXForwardedFor() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("clientIp")).isEqualTo("203.0.113.50");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_clientIpFromXRealIp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("clientIp")).isEqualTo("203.0.113.99");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_xForwardedForTakesPriorityOverXRealIp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50");
        request.addHeader("X-Real-IP", "203.0.113.99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get("clientIp")).isEqualTo("203.0.113.50");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }

    @Test
    void doFilter_clearsMdcEvenWhenChainThrowsException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new RuntimeException("Simulated error")).when(filterChain)
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        try {
            mdcFilter.doFilter(request, response, filterChain);
        } catch (RuntimeException ignored) {
        }

        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("username")).isNull();
        assertThat(MDC.get("clientIp")).isNull();
    }

    @Test
    void doFilter_loginRequestHasRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            String requestId = MDC.get("requestId");
            assertThat(requestId).isNotBlank();
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        mdcFilter.doFilter(request, response, filterChain);
    }
}
