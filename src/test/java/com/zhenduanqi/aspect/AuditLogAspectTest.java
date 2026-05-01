package com.zhenduanqi.aspect;

import com.zhenduanqi.dto.ExecuteResponse;
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
    void executeResponse_blocked_recordsFailed() throws Throwable {
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
        assertThat(log.getResult()).isEqualTo("FAILED");
        assertThat(log.getResultDetail()).contains("state='blocked'");
    }
}
