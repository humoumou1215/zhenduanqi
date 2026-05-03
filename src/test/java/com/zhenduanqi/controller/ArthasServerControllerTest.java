package com.zhenduanqi.controller;

import com.zhenduanqi.aspect.RoleAspect;
import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.dto.ServerStatusDTO;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.ArthasServerService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArthasServerController.class)
@Import(RoleAspect.class)
@EnableAspectJAutoProxy
class ArthasServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArthasServerService serverService;

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
        adminUser = new SysUser();
        adminUser.setUsername("admin");
        adminUser.setRoles(Set.of(adminRole));

        SysRole operatorRole = new SysRole();
        operatorRole.setRoleCode("OPERATOR");
        operatorUser = new SysUser();
        operatorUser.setUsername("operator");
        operatorUser.setRoles(Set.of(operatorRole));

        SysRole readonlyRole = new SysRole();
        readonlyRole.setRoleCode("READONLY");
        readonlyUser = new SysUser();
        readonlyUser.setUsername("readonly");
        readonlyUser.setRoles(Set.of(readonlyRole));

        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(authService.validateToken("operator-jwt")).thenReturn("operator");
        when(authService.validateToken("readonly-jwt")).thenReturn("readonly");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operatorUser));
        when(userRepository.findByUsername("readonly")).thenReturn(Optional.of(readonlyUser));
    }

    private SysUser adminUser;
    private SysUser operatorUser;
    private SysUser readonlyUser;

    @Test
    void listServers_withAdminAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/servers").cookie(adminCookie))
                .andExpect(status().isOk());
    }

    @Test
    void listServers_withOperatorAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/servers").cookie(operatorCookie))
                .andExpect(status().isOk());
    }

    @Test
    void listServers_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/servers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createServer_withAdminAuth_returns201() throws Exception {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-3");
        dto.setName("测试服务器");
        dto.setHost("192.168.1.102");
        dto.setHttpPort(8563);
        dto.setToken("test-token");

        when(serverService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/servers")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("server-3"));
    }

    @Test
    void createServer_withOperatorAuth_returns403() throws Exception {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-3");
        dto.setName("测试服务器");
        dto.setHost("192.168.1.102");
        dto.setHttpPort(8563);

        mockMvc.perform(post("/api/servers")
                        .cookie(operatorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createServer_withReadonlyAuth_returns403() throws Exception {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId("server-3");

        mockMvc.perform(post("/api/servers")
                        .cookie(readonlyCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateServer_withAdminAuth_returns200() throws Exception {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setName("更新名称");
        dto.setHost("192.168.1.100");
        dto.setHttpPort(8563);

        when(serverService.update(eq("server-1"), any())).thenReturn(dto);

        mockMvc.perform(put("/api/servers/server-1")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新名称"));
    }

    @Test
    void updateServer_withOperatorAuth_returns403() throws Exception {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setName("更新名称");

        mockMvc.perform(put("/api/servers/server-1")
                        .cookie(operatorCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteServer_withAdminAuth_returns204() throws Exception {
        mockMvc.perform(delete("/api/servers/server-1").cookie(adminCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteServer_withOperatorAuth_returns403() throws Exception {
        mockMvc.perform(delete("/api/servers/server-1").cookie(operatorCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteServer_withReadonlyAuth_returns403() throws Exception {
        mockMvc.perform(delete("/api/servers/server-1").cookie(readonlyCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkStatus_withAdminAuth_returns200() throws Exception {
        ServerStatusDTO status = ServerStatusDTO.success("连接成功");
        when(serverService.checkConnection("server-1")).thenReturn(status);

        mockMvc.perform(get("/api/servers/server-1/status").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.message").value("连接成功"));
    }

    @Test
    void checkStatus_withOperatorAuth_returns200() throws Exception {
        ServerStatusDTO status = ServerStatusDTO.success("连接成功");
        when(serverService.checkConnection("server-1")).thenReturn(status);

        mockMvc.perform(get("/api/servers/server-1/status").cookie(operatorCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    void checkStatus_withReadonlyAuth_returns200() throws Exception {
        ServerStatusDTO status = ServerStatusDTO.failure("连接失败");
        when(serverService.checkConnection("server-1")).thenReturn(status);

        mockMvc.perform(get("/api/servers/server-1/status").cookie(readonlyCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.error").value("连接失败"));
    }

    @Test
    void checkStatus_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/servers/server-1/status"))
                .andExpect(status().isUnauthorized());
    }
}
