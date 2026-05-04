package com.zhenduanqi.controller;

import com.zhenduanqi.aspect.RoleAspect;
import com.zhenduanqi.dto.ArthasSessionDTO;
import com.zhenduanqi.dto.CreateSessionRequest;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.model.ArthasResult;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.ArthasSessionService;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArthasSessionController.class)
@Import(RoleAspect.class)
@EnableAspectJAutoProxy
class ArthasSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArthasSessionService sessionService;

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
    void createSession_withAdminAuth_returns200() throws Exception {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        ArthasSessionDTO resp = new ArthasSessionDTO();
        resp.setId(1L);
        resp.setStatus("ACTIVE");
        when(sessionService.createSession(any(CreateSessionRequest.class), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/arthas-sessions")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createSession_withOperatorAuth_returns200() throws Exception {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        ArthasSessionDTO resp = new ArthasSessionDTO();
        resp.setId(1L);
        resp.setStatus("ACTIVE");
        when(sessionService.createSession(any(CreateSessionRequest.class), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/arthas-sessions")
                        .cookie(operatorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createSession_withReadonlyAuth_returns403() throws Exception {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        mockMvc.perform(post("/api/arthas-sessions")
                        .cookie(readonlyCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSession_withoutAuth_returns401() throws Exception {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setServerId("server-1");
        req.setCommand("thread");

        mockMvc.perform(post("/api/arthas-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getActiveSessions_withAdminAuth_returns200() throws Exception {
        ArthasSessionDTO session1 = new ArthasSessionDTO();
        session1.setId(1L);
        session1.setStatus("ACTIVE");

        ArthasSessionDTO session2 = new ArthasSessionDTO();
        session2.setId(2L);
        session2.setStatus("ACTIVE");

        when(sessionService.getActiveSessions(eq("server-1"), eq(null))).thenReturn(List.of(session1, session2));

        mockMvc.perform(get("/api/arthas-sessions")
                        .cookie(adminCookie)
                        .param("serverId", "server-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getActiveSessions_withAdminAuthAndUsername_returns200() throws Exception {
        ArthasSessionDTO session1 = new ArthasSessionDTO();
        session1.setId(1L);
        session1.setStatus("ACTIVE");

        when(sessionService.getActiveSessions(eq(null), eq("user1"))).thenReturn(List.of(session1));

        mockMvc.perform(get("/api/arthas-sessions")
                        .cookie(adminCookie)
                        .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getActiveSessions_withOperatorAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/arthas-sessions")
                        .cookie(operatorCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveSessions_withReadonlyAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/arthas-sessions")
                        .cookie(readonlyCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveSessions_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/arthas-sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pullResults_withAdminAuth_returns200() throws Exception {
        ArthasResult result = new ArthasResult();
        result.setType("thread");

        when(sessionService.pullResults(1L)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/arthas-sessions/1/results")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("thread"));
    }

    @Test
    void pullResults_withOperatorAuth_returns200() throws Exception {
        ArthasResult result = new ArthasResult();
        result.setType("thread");

        when(sessionService.pullResults(1L)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/arthas-sessions/1/results")
                        .cookie(operatorCookie))
                .andExpect(status().isOk());
    }

    @Test
    void pullResults_withReadonlyAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/arthas-sessions/1/results")
                        .cookie(readonlyCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void interrupt_withAdminAuth_returns200() throws Exception {
        when(sessionService.interruptJob(1L)).thenReturn(true);

        mockMvc.perform(post("/api/arthas-sessions/1/interrupt")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void interrupt_withOperatorAuth_returns200() throws Exception {
        when(sessionService.interruptJob(1L)).thenReturn(true);

        mockMvc.perform(post("/api/arthas-sessions/1/interrupt")
                        .cookie(operatorCookie))
                .andExpect(status().isOk());
    }

    @Test
    void interrupt_withReadonlyAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/arthas-sessions/1/interrupt")
                        .cookie(readonlyCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void close_withAdminAuth_returns200() throws Exception {
        when(sessionService.closeSession(1L)).thenReturn(true);

        mockMvc.perform(post("/api/arthas-sessions/1/close")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void close_withOperatorAuth_returns200() throws Exception {
        when(sessionService.closeSession(1L)).thenReturn(true);

        mockMvc.perform(post("/api/arthas-sessions/1/close")
                        .cookie(operatorCookie))
                .andExpect(status().isOk());
    }

    @Test
    void close_withReadonlyAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/arthas-sessions/1/close")
                        .cookie(readonlyCookie))
                .andExpect(status().isForbidden());
    }
}
