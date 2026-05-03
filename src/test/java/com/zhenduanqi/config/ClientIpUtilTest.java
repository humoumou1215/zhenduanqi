package com.zhenduanqi.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientIpUtilTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void extractClientIp_returnsXForwardedForFirstIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18, 10.0.0.1");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.50");
    }

    @Test
    void extractClientIp_returnsXRealIpWhenNoXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.99");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.99");
    }

    @Test
    void extractClientIp_returnsRemoteAddrWhenNoHeaders() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.1.100");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("10.0.1.100");
    }

    @Test
    void extractClientIp_xForwardedForTakesPriorityOverXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.50");
    }

    @Test
    void extractClientIp_handlesEmptyXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.99");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.99");
    }

    @Test
    void extractClientIp_handlesEmptyXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("10.0.1.100");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("10.0.1.100");
    }

    @Test
    void extractClientIp_trimsWhitespaceFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.50  , 70.41.3.18");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.50");
    }

    @Test
    void extractClientIp_singleIpInXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50");

        String clientIp = ClientIpUtil.extractClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.50");
    }
}
