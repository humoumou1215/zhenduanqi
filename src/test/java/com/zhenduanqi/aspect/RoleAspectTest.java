package com.zhenduanqi.aspect;

import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
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

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleAspectTest {

    @Mock
    private SysUserRepository userRepository;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature signature;

    private RoleAspect roleAspect;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private void setUpRequest(String username) {
        roleAspect = new RoleAspect(userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        if (username != null) {
            request.setAttribute("username", username);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @Test
    void adminUser_onAdminEndpoint_methodProceeds() throws Throwable {
        SysRole adminRole = new SysRole();
        adminRole.setRoleCode("ADMIN");
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setRoles(Set.of(adminRole));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        setUpRequest("admin");
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
        SysRole operatorRole = new SysRole();
        operatorRole.setRoleCode("OPERATOR");
        SysUser user = new SysUser();
        user.setUsername("operator");
        user.setRoles(Set.of(operatorRole));
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(user));

        setUpRequest("operator");
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
        setUpRequest(null);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminOnlyEndpoint"));

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        verify(joinPoint, never()).proceed();
    }

    @com.zhenduanqi.annotation.RequireRole("ADMIN")
    public void adminOnlyEndpoint() {}
}
