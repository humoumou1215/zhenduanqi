package com.zhenduanqi.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveDataConverterTest {

    private SensitiveDataConverter converter;

    @BeforeEach
    void setUp() {
        converter = new SensitiveDataConverter();
        Context context = mock(Context.class);
        when(context.getProperty("CONSOLE_LOG_PATTERN")).thenReturn(null);
        converter.setContext(context);
    }

    @Test
    void masksPasswordFieldValue() {
        String result = converter.convert(eventWithMessage("Login attempt: password=secret123"));
        assertThat(result).isEqualTo("Login attempt: password=******");
    }

    @Test
    void masksTokenFieldValue() {
        String result = converter.convert(eventWithMessage("Server config: token=abc-def-ghi"));
        assertThat(result).isEqualTo("Server config: token=******");
    }

    @Test
    void masksAuthorizationBearer() {
        String result = converter.convert(eventWithMessage("Request: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9"));
        assertThat(result).isEqualTo("Request: Authorization: Bearer ******");
    }

    @Test
    void doesNotMaskNonSensitiveFields() {
        String result = converter.convert(eventWithMessage("User login: username=admin, action=LOGIN"));
        assertThat(result).isEqualTo("User login: username=admin, action=LOGIN");
    }

    @Test
    void masksMultipleSensitiveFieldsInSameMessage() {
        String result = converter.convert(eventWithMessage("password=secret, token=abc123, username=admin"));
        assertThat(result).isEqualTo("password=******, token=******, username=admin");
    }

    @Test
    void masksEmptyPasswordValue() {
        String result = converter.convert(eventWithMessage("password="));
        assertThat(result).isEqualTo("password=******");
    }

    @Test
    void masksPasswordInJsonLikeFormat() {
        String result = converter.convert(eventWithMessage("{\"password\":\"mySecret\",\"username\":\"admin\"}"));
        assertThat(result).isEqualTo("{\"password\":\"******\",\"username\":\"admin\"}");
    }

    @Test
    void masksTokenInJsonLikeFormat() {
        String result = converter.convert(eventWithMessage("{\"token\":\"abc123\",\"id\":1}"));
        assertThat(result).isEqualTo("{\"token\":\"******\",\"id\":1}");
    }

    @Test
    void leavesMessageWithoutSensitiveDataUnchanged() {
        String original = "Command executed successfully on server-1";
        String result = converter.convert(eventWithMessage(original));
        assertThat(result).isEqualTo(original);
    }

    @Test
    void masksPasswordInNestedJson() {
        String result = converter.convert(eventWithMessage("{\"user\":{\"password\":\"secret\",\"name\":\"admin\"}}"));
        assertThat(result).isEqualTo("{\"user\":{\"password\":\"******\",\"name\":\"admin\"}}");
    }

    @Test
    void masksTokenInNestedJson() {
        String result = converter.convert(eventWithMessage("{\"data\":{\"auth\":{\"token\":\"abc123\"}}}"));
        assertThat(result).isEqualTo("{\"data\":{\"auth\":{\"token\":\"******\"}}}");
    }

    @Test
    void masksPasswordInUrl() {
        String result = converter.convert(eventWithMessage("Login URL: /api/login?password=secret123&username=admin"));
        assertThat(result).isEqualTo("Login URL: /api/login?password=******&username=admin");
    }

    @Test
    void masksTokenInUrl() {
        String result = converter.convert(eventWithMessage("API call: /api/data?token=abc-def-ghi&action=query"));
        assertThat(result).isEqualTo("API call: /api/data?token=******&action=query");
    }

    @Test
    void handlesMalformedJsonGracefully() {
        String result = converter.convert(eventWithMessage("{invalid json with password=secret"));
        assertThat(result).contains("password=******");
    }

    @Test
    void masksNullOrEmptyMessage() {
        assertThat(converter.convert(eventWithMessage(null))).isNull();
        assertThat(converter.convert(eventWithMessage(""))).isEqualTo("");
    }

    private ILoggingEvent eventWithMessage(String message) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(message);
        return event;
    }
}
