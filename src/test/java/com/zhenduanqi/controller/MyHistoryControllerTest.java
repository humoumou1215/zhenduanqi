package com.zhenduanqi.controller;

import com.zhenduanqi.entity.SysAuditLog;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.AuditLogService;
import com.zhenduanqi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MyHistoryController.class)
class MyHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SysUserRepository userRepository;

    @Test
    void getMyHistory_returnsOnlyCurrentUserLogs() throws Exception {
        SysAuditLog log = new SysAuditLog();
        log.setId(1L);
        log.setUsername("zhangsan");
        log.setAction("EXECUTE_COMMAND");
        log.setCommand("thread -n 5");
        log.setTarget("server-1");
        log.setResult("SUCCESS");
        log.setDurationMs(100L);

        SysUser normalUser = new SysUser();
        normalUser.setUsername("zhangsan");
        SysRole operatorRole = new SysRole();
        operatorRole.setRoleCode("OPERATOR");
        normalUser.setRoles(Set.of(operatorRole));

        when(authService.validateToken("valid-jwt")).thenReturn("zhangsan");
        when(userRepository.findByUsername("zhangsan")).thenReturn(java.util.Optional.of(normalUser));
        when(auditLogService.queryMyHistory(eq("zhangsan"), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/my-history")
                        .cookie(new jakarta.servlet.http.Cookie("zhenduanqi_token", "valid-jwt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("zhangsan"))
                .andExpect(jsonPath("$.content[0].command").value("thread -n 5"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMyHistory_withFilters_returnsFilteredLogs() throws Exception {
        SysAuditLog log = new SysAuditLog();
        log.setId(1L);
        log.setUsername("zhangsan");
        log.setAction("EXECUTE_COMMAND");
        log.setTarget("server-1");
        log.setResult("SUCCESS");

        SysUser normalUser = new SysUser();
        normalUser.setUsername("zhangsan");
        SysRole operatorRole = new SysRole();
        operatorRole.setRoleCode("OPERATOR");
        normalUser.setRoles(Set.of(operatorRole));

        when(authService.validateToken("valid-jwt")).thenReturn("zhangsan");
        when(userRepository.findByUsername("zhangsan")).thenReturn(java.util.Optional.of(normalUser));
        when(auditLogService.queryMyHistory(eq("zhangsan"), eq("EXECUTE_COMMAND"), eq("server-1"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/my-history")
                        .cookie(new jakarta.servlet.http.Cookie("zhenduanqi_token", "valid-jwt"))
                        .param("action", "EXECUTE_COMMAND")
                        .param("target", "server-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("EXECUTE_COMMAND"))
                .andExpect(jsonPath("$.content[0].target").value("server-1"));
    }

    @Test
    void getMyHistory_emptyResult_returnsEmptyPage() throws Exception {
        SysUser normalUser = new SysUser();
        normalUser.setUsername("zhangsan");
        SysRole operatorRole = new SysRole();
        operatorRole.setRoleCode("OPERATOR");
        normalUser.setRoles(Set.of(operatorRole));

        when(authService.validateToken("valid-jwt")).thenReturn("zhangsan");
        when(userRepository.findByUsername("zhangsan")).thenReturn(java.util.Optional.of(normalUser));
        when(auditLogService.queryMyHistory(eq("zhangsan"), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/my-history")
                        .cookie(new jakarta.servlet.http.Cookie("zhenduanqi_token", "valid-jwt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
