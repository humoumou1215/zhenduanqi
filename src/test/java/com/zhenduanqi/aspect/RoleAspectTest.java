package com.zhenduanqi.aspect;

import com.zhenduanqi.annotation.RequireRole;
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

    private void setUpWithRole(String roleCode) {
        roleAspect = new RoleAspect(userRepository);

        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        SysUser user = new SysUser();
        user.setUsername("testuser");
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setAttribute("username", "testuser");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @Test
    void adminUser_allowedAccess_passes() throws Throwable {
        setUpWithRole("ADMIN");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminEndpoint")
        );
        when(joinPoint.proceed()).thenReturn("success");

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
    }

    @Test
    void operatorUser_deniedAdminAccess_returnsNull() throws Throwable {
        setUpWithRole("OPERATOR");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminEndpoint")
        );

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void operatorUser_allowedOperatorAccess_passes() throws Throwable {
        setUpWithRole("OPERATOR");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("operatorOrAdminEndpoint")
        );
        when(joinPoint.proceed()).thenReturn("success");

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
    }

    @Test
    void unauthenticatedUser_returns401() throws Throwable {
        roleAspect = new RoleAspect(userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(
                RoleAspectTest.class.getDeclaredMethod("adminEndpoint")
        );

        Object result = roleAspect.checkRole(joinPoint);

        assertThat(result).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        verify(joinPoint, never()).proceed();
    }

    @RequireRole("ADMIN")
    public void adminEndpoint() {}

    @RequireRole({"OPERATOR", "ADMIN"})
    public void operatorOrAdminEndpoint() {}
}
