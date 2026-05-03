package com.zhenduanqi.controller;

import com.zhenduanqi.aspect.RoleAspect;
import com.zhenduanqi.dto.ExecuteRequest;
import com.zhenduanqi.dto.ExecuteResponse;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.ArthasExecuteService;
import com.zhenduanqi.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArthasExecuteController.class)
@Import(RoleAspect.class)
@EnableAspectJAutoProxy
class ArthasExecuteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArthasExecuteService executeService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SysUserRepository userRepository;

    private final Cookie adminCookie = new Cookie("zhenduanqi_token", "admin-jwt");
    private final Cookie operatorCookie = new Cookie("zhenduanqi_token", "operator-jwt");
    private final Cookie readonlyCookie = new Cookie("zhenduanqi_token", "readonly-jwt");

    @BeforeEach
    void setUp() {
        SysRole adminRole = new SysRole();
        adminRole.setRoleCode("ADMIN");
        SysUser adminUser = new SysUser();
        adminUser.setUsername("admin");
        adminUser.setRoles(Set.of(adminRole));

        SysRole operatorRole = new SysRole();
        operatorRole.setRoleCode("OPERATOR");
        SysUser operatorUser = new SysUser();
        operatorUser.setUsername("operator");
        operatorUser.setRoles(Set.of(operatorRole));

        SysRole readonlyRole = new SysRole();
        readonlyRole.setRoleCode("READONLY");
        SysUser readonlyUser = new SysUser();
        readonlyUser.setUsername("readonly");
        readonlyUser.setRoles(Set.of(readonlyRole));

        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(authService.validateToken("operator-jwt")).thenReturn("operator");
        when(authService.validateToken("readonly-jwt")).thenReturn("readonly");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operatorUser));
        when(userRepository.findByUsername("readonly")).thenReturn(Optional.of(readonlyUser));
    }

    @Test
    void execute_withAdminAuth_returns200() throws Exception {
        ExecuteRequest req = new ExecuteRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        ExecuteResponse resp = new ExecuteResponse();
        resp.setState("succeeded");
        when(executeService.execute(anyString(), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/execute")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("succeeded"));
    }

    @Test
    void execute_withOperatorAuth_returns200() throws Exception {
        ExecuteRequest req = new ExecuteRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        ExecuteResponse resp = new ExecuteResponse();
        resp.setState("succeeded");
        when(executeService.execute(anyString(), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/execute")
                        .cookie(operatorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("succeeded"));
    }

    @Test
    void execute_withReadonlyAuth_returns403() throws Exception {
        ExecuteRequest req = new ExecuteRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        mockMvc.perform(post("/api/execute")
                        .cookie(readonlyCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void execute_withoutAuth_returns401() throws Exception {
        ExecuteRequest req = new ExecuteRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
