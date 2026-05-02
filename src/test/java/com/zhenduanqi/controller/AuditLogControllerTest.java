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

@WebMvcTest(AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SysUserRepository userRepository;

    @Test
    void query_withDefaultParams_returnsPage() throws Exception {
        SysAuditLog log = new SysAuditLog();
        log.setId(1L);
        log.setUsername("zhangsan");
        log.setAction("EXECUTE_COMMAND");
        log.setResult("SUCCESS");
        log.setDurationMs(100L);

        SysUser adminUser = new SysUser();
        adminUser.setUsername("admin");
        SysRole adminRole = new SysRole();
        adminRole.setRoleCode("ADMIN");
        adminUser.setRoles(Set.of(adminRole));

        when(authService.validateToken("valid-jwt")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(adminUser));
        when(auditLogService.query(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/audit-logs")
                        .cookie(new jakarta.servlet.http.Cookie("zhenduanqi_token", "valid-jwt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("zhangsan"))
                .andExpect(jsonPath("$.content[0].action").value("EXECUTE_COMMAND"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
