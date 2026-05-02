package com.zhenduanqi.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature signature;

    private RoleAspect roleAspect;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private void setUpRequest(String username, Set<String> userRoles) {
        roleAspect = new RoleAspect();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        if (username != null) {
            request.setAttribute("username", username);
        }
        if (userRoles != null) {
            request.setAttribute("userRoles", userRoles);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @Test
    void adminUser_onAdminEndpoint_methodProceeds() throws Throwable {
        setUpRequest("admin", Set.of("ADMIN"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminOnlyEndpoint"));
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    void operatorUser_onAdminEndpoint_returns403() throws Throwable {
        setUpRequest("operator", Set.of("OPERATOR"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminOnlyEndpoint"));

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void unauthenticatedUser_onSecuredEndpoint_returns401() throws Throwable {
        setUpRequest(null, null);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminOnlyEndpoint"));

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void userWithoutRoles_onSecuredEndpoint_returns403() throws Throwable {
        setUpRequest("user", null);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminOnlyEndpoint"));

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        verify(joinPoint, never()).proceed();
    }

    @com.zhenduanqi.annotation.RequireRole("ADMIN")
    public void adminOnlyEndpoint() {}
}
