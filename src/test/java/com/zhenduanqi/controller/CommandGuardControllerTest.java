package com.zhenduanqi.controller;

import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.AuthService;
import com.zhenduanqi.service.CommandGuardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommandGuardController.class)
class CommandGuardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommandGuardService guardService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SysUserRepository userRepository;

    private final Cookie adminCookie = new Cookie("zhenduanqi_token", "admin-jwt");

    private SysUser createMockUser(String username, String roleCode) {
        SysUser user = new SysUser();
        user.setUsername(username);
        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        user.setRoles(Set.of(role));
        return user;
    }

    @Test
    void getRules_returnsRuleList() throws Exception {
        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(createMockUser("admin", "ADMIN")));
        CommandGuardRule rule = new CommandGuardRule();
        rule.setId(1L);
        rule.setRuleType("BLACKLIST");
        rule.setPattern("^ognl\\b");
        rule.setEnabled(true);
        when(guardService.getAllRules()).thenReturn(List.of(rule));

        mockMvc.perform(get("/api/command-guard/rules").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleType").value("BLACKLIST"))
                .andExpect(jsonPath("$[0].pattern").value("^ognl\\b"));
    }

    @Test
    void addRule_returnsCreatedRule() throws Exception {
        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(createMockUser("admin", "ADMIN")));
        CommandGuardRule created = new CommandGuardRule();
        created.setId(1L);
        created.setRuleType("BLACKLIST");
        created.setPattern("^reset\\b");
        created.setEnabled(true);
        when(guardService.addRule(any())).thenReturn(created);

        String body = "{\"ruleType\":\"BLACKLIST\",\"pattern\":\"^reset\\\\b\",\"enabled\":true}";

        mockMvc.perform(post("/api/command-guard/rules")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pattern").value("^reset\\b"));
    }

    @Test
    void deleteRule_returnsNoContent() throws Exception {
        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(createMockUser("admin", "ADMIN")));

        mockMvc.perform(delete("/api/command-guard/rules/1").cookie(adminCookie))
                .andExpect(status().isNoContent());
    }
}
