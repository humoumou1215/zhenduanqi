package com.zhenduanqi.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zhenduanqi.dto.ExecuteRequest;
import com.zhenduanqi.dto.ExecuteResponse;
import com.zhenduanqi.dto.LoginRequest;
import com.zhenduanqi.entity.SysAuditLog;
import com.zhenduanqi.repository.AuditLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature signature;

    private AuditLogAspect aspect;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        aspect = new AuditLogAspect(auditLogRepository);
        request = new MockHttpServletRequest();
        request.setAttribute("username", "zhangsan");
        request.setRemoteAddr("10.0.1.100");
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse()));
    }

    @Test
    void successPath_recordsAuditLog() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("dummyDiagnoseMethod"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"cmd1", "server-1"});
        when(joinPoint.proceed()).thenReturn("success");

        aspect.logAround(joinPoint);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getUsername()).isEqualTo("zhangsan");
        assertThat(log.getUserIp()).isEqualTo("10.0.1.100");
        assertThat(log.getAction()).isEqualTo("EXECUTE_COMMAND");
        assertThat(log.getTarget()).isEqualTo("server-1");
        assertThat(log.getCommand()).isEqualTo("cmd1");
        assertThat(log.getResult()).isEqualTo("SUCCESS");
        assertThat(log.getDurationMs()).isNotNull();
    }

    @Test
    void failurePath_recordsFailedResult() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("dummyDiagnoseMethod"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"cmd1", "server-1"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("连接失败"));

        assertThatThrownBy(() -> aspect.logAround(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("连接失败");

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getResult()).isEqualTo("FAILED");
        assertThat(log.getResultDetail()).contains("连接失败");
    }

    @com.zhenduanqi.annotation.AuditLog(action = "EXECUTE_COMMAND")
    public String dummyDiagnoseMethod() { return ""; }

    @com.zhenduanqi.annotation.AuditLog(action = "执行诊断命令")
    public void executeMethodWithDTO(ExecuteRequest request) {}

    @com.zhenduanqi.annotation.AuditLog(action = "LOGIN")
    public void loginMethodWithDTO(LoginRequest request) {}

    @Test
    void executeRequest_extractsCommandAndServerId() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("executeMethodWithDTO", ExecuteRequest.class));
        ExecuteRequest req = new ExecuteRequest();
        req.setServerId("server-123");
        req.setCommand("thread -n 5");
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});
        when(joinPoint.proceed()).thenReturn("success");

        aspect.logAround(joinPoint);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getCommand()).isEqualTo("thread -n 5");
        assertThat(log.getTarget()).isEqualTo("server-123");
    }

    @Test
    void loginRequest_masksPasswordInParams() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("loginMethodWithDTO", LoginRequest.class));
        LoginRequest req = new LoginRequest();
        req.setUsername("zhangsan");
        req.setPassword("mysecret123");
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});
        when(joinPoint.proceed()).thenReturn("success");

        aspect.logAround(joinPoint);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getParams()).contains("password");
        assertThat(log.getParams()).contains("******");
        assertThat(log.getParams()).doesNotContain("mysecret123");
        assertThat(log.getParams()).contains("username");
        assertThat(log.getParams()).contains("zhangsan");
    }

    @Test
    void loginRequest_withoutUsernameInRequestAttribute_extractsFromLoginRequest() throws Throwable {
        request.setAttribute("username", null);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("loginMethodWithDTO", LoginRequest.class));
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("secret123");
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});
        when(joinPoint.proceed()).thenReturn("success");

        aspect.logAround(joinPoint);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getUsername()).isEqualTo("admin");
        assertThat(log.getAction()).isEqualTo("LOGIN");
    }

    @Test
    void executeResponse_succeeded_recordsSuccess() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("dummyDiagnoseMethod"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"cmd1", "server-1"});

        ExecuteResponse resp = new ExecuteResponse();
        resp.setState("succeeded");
        resp.setResults(List.of("result1"));
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(resp));

        aspect.logAround(joinPoint);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getResult()).isEqualTo("SUCCESS");
        assertThat(log.getResultDetail()).contains("state='succeeded'");
        assertThat(log.getResultDetail()).contains("result1");
    }

    @Test
    void executeResponse_failed_recordsFailed() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("dummyDiagnoseMethod"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"cmd1", "server-1"});

        ExecuteResponse resp = new ExecuteResponse();
        resp.setState("failed");
        resp.setError("请求超时: timed out");
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(resp));

        aspect.logAround(joinPoint);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getResult()).isEqualTo("FAILED");
        assertThat(log.getResultDetail()).contains("state='failed'");
        assertThat(log.getResultDetail()).contains("请求超时");
    }

    @Test
    void executeResponse_blocked_recordsBlocked() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                AuditLogAspectTest.class.getDeclaredMethod("dummyDiagnoseMethod"));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"cmd1", "server-1"});

        ExecuteResponse resp = new ExecuteResponse();
        resp.setState("blocked");
        resp.setError("命令被拦截");
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(resp));

        aspect.logAround(joinPoint);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        SysAuditLog log = captor.getValue();
        assertThat(log.getResult()).isEqualTo("BLOCKED");
        assertThat(log.getResultDetail()).contains("state='blocked'");
    }

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(AuditLogAspect.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(AuditLogAspect.class);
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    @Test
    void successPath_logsDebug() throws Throwable {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getMethod()).thenReturn(
                    AuditLogAspectTest.class.getDeclaredMethod("dummyDiagnoseMethod"));
            when(joinPoint.getArgs()).thenReturn(new Object[]{"cmd1", "server-1"});
            when(joinPoint.proceed()).thenReturn("success");

            aspect.logAround(joinPoint);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.DEBUG && e.getFormattedMessage().contains("审计日志写入成功"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }

    @Test
    void failurePath_logsError() throws Throwable {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getMethod()).thenReturn(
                    AuditLogAspectTest.class.getDeclaredMethod("dummyDiagnoseMethod"));
            when(joinPoint.getArgs()).thenReturn(new Object[]{"cmd1", "server-1"});
            when(joinPoint.proceed()).thenThrow(new RuntimeException("连接失败"));

            assertThatThrownBy(() -> aspect.logAround(joinPoint))
                    .isInstanceOf(RuntimeException.class);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("审计日志写入失败"))).isTrue();
        } finally {
            removeAppender(appender);
        }
    }
}
