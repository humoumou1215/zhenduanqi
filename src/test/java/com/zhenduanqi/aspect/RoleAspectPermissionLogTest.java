package com.zhenduanqi.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAspectPermissionLogTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature signature;

    private ListAppender<ILoggingEvent> createListAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = ctx.getLogger(RoleAspect.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void removeAppender(ListAppender<ILoggingEvent> appender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(RoleAspect.class).detachAppender(appender);
    }

    @Test
    void operatorAccessAdminEndpoint_logsWarnWithRoleInfo() throws Throwable {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            RoleAspect roleAspect = new RoleAspect();
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setAttribute("username", "operator");
            request.setAttribute("userRoles", Set.of("OPERATOR"));
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getMethod()).thenReturn(
                    RoleAspectPermissionLogTest.class.getDeclaredMethod("adminOnlyEndpoint"));

            roleAspect.checkRole(joinPoint);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("权限不足"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("OPERATOR"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("ADMIN"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("operator"))).isTrue();
        } finally {
            removeAppender(appender);
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void readonlyAccessOperatorEndpoint_logsWarnWithRoleInfo() throws Throwable {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            RoleAspect roleAspect = new RoleAspect();
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setAttribute("username", "readonly_user");
            request.setAttribute("userRoles", Set.of("READONLY"));
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getMethod()).thenReturn(
                    RoleAspectPermissionLogTest.class.getDeclaredMethod("operatorOrAdminEndpoint"));

            roleAspect.checkRole(joinPoint);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().anyMatch(e ->
                    e.getLevel() == Level.WARN && e.getFormattedMessage().contains("权限不足"))).isTrue();
            assertThat(events.stream().anyMatch(e ->
                    e.getFormattedMessage().contains("READONLY"))).isTrue();
        } finally {
            removeAppender(appender);
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void adminAccessAdminEndpoint_noWarnLog() throws Throwable {
        ListAppender<ILoggingEvent> appender = createListAppender();
        try {
            RoleAspect roleAspect = new RoleAspect();
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setAttribute("username", "admin");
            request.setAttribute("userRoles", Set.of("ADMIN"));
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getMethod()).thenReturn(
                    RoleAspectPermissionLogTest.class.getDeclaredMethod("adminOnlyEndpoint"));
            when(joinPoint.proceed()).thenReturn("success");

            Object result = roleAspect.checkRole(joinPoint);

            List<ILoggingEvent> events = appender.list;
            assertThat(events.stream().noneMatch(e -> e.getLevel() == Level.WARN)).isTrue();
            assertThat(result).isEqualTo("success");
        } finally {
            removeAppender(appender);
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @com.zhenduanqi.annotation.RequireRole("ADMIN")
    public void adminOnlyEndpoint() {}

    @com.zhenduanqi.annotation.RequireRole({"OPERATOR", "ADMIN"})
    public void operatorOrAdminEndpoint() {}
}
