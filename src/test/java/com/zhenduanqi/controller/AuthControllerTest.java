package com.zhenduanqi.controller;

import com.zhenduanqi.dto.LoginRequest;
import com.zhenduanqi.dto.LoginResponse;
import com.zhenduanqi.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private final Cookie validCookie = new Cookie("zhenduanqi_token", "valid-jwt");

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Abcd1234");

        when(authService.login(anyString(), anyString(), anyString(), any()))
                .thenAnswer(i -> {
                    HttpServletResponse resp = i.getArgument(3);
                    jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("zhenduanqi_token", "jwt-token");
                    cookie.setHttpOnly(true);
                    cookie.setPath("/api");
                    resp.addCookie(cookie);
                    return new LoginResponse("admin", "ADMIN", "管理员");
                });

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.realName").value("管理员"))
                .andExpect(cookie().exists("zhenduanqi_token"));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        when(authService.login(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("用户名或密码错误"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("用户名或密码错误"));
    }

    @Test
    void logout_withValidToken_returns200() throws Exception {
        when(authService.validateToken("valid-jwt")).thenReturn("admin");

        mockMvc.perform(post("/api/auth/logout").cookie(validCookie))
                .andExpect(status().isOk());
    }

    @Test
    void me_withValidToken_returnsUserInfo() throws Exception {
        when(authService.validateToken("valid-jwt")).thenReturn("admin");
        when(authService.getUserRole("admin")).thenReturn("ADMIN");

        mockMvc.perform(get("/api/auth/me").cookie(validCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void me_withoutAuth_returns401() throws Exception {
        when(authService.validateToken(any())).thenReturn(null);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("未登录或Token已过期"));
    }
}
