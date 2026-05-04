package com.zhenduanqi.controller;

import com.zhenduanqi.entity.DiagnoseScene;
import com.zhenduanqi.entity.SceneStep;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.SceneService;
import com.zhenduanqi.service.AuthService;
import com.zhenduanqi.aspect.RoleAspect;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(SceneController.class)
@Import(RoleAspect.class)
@EnableAspectJAutoProxy
class SceneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SceneService sceneService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SysUserRepository userRepository;

    private final Cookie adminCookie = new Cookie("zhenduanqi_token", "admin-jwt");

    @BeforeEach
    void setUp() {
        com.zhenduanqi.entity.SysRole adminRole = new com.zhenduanqi.entity.SysRole();
        adminRole.setRoleCode("ADMIN");
        com.zhenduanqi.entity.SysUser adminUser = new com.zhenduanqi.entity.SysUser();
        adminUser.setUsername("admin");
        adminUser.setRoles(Set.of(adminRole));

        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    }

    @Test
    void getScenes_withValidData_returnsScenes() throws Exception {
        DiagnoseScene scene = new DiagnoseScene();
        scene.setId(1L);
        scene.setName("接口响应慢排查");
        scene.setDescription("排查接口响应时间长、超时等问题");
        scene.setCategory("SLOW_RESPONSE");

        SceneStep step = new SceneStep();
        step.setId(1L);
        step.setTitle("查看 JVM 概览");
        step.setCommand("dashboard -n 1");
        step.setStepOrder(1);

        scene.setSteps(Arrays.asList(step));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("接口响应慢排查"))
                .andExpect(jsonPath("$[0].category").value("SLOW_RESPONSE"))
                .andExpect(jsonPath("$[0].steps[0].title").value("查看 JVM 概览"))
                .andExpect(jsonPath("$[0].steps[0].scene").doesNotExist());
    }

    @Test
    void getScene_withValidId_returnsSceneWithSteps() throws Exception {
        DiagnoseScene scene = new DiagnoseScene();
        scene.setId(2L);
        scene.setName("CPU 飙高排查");
        scene.setCategory("CPU_HIGH");

        SceneStep step = new SceneStep();
        step.setId(1L);
        step.setTitle("查看 JVM 概览");
        step.setCommand("dashboard -n 1");
        step.setStepOrder(1);
        step.setScene(scene);

        scene.setSteps(Arrays.asList(step));

        when(sceneService.getSceneById(2L)).thenReturn(Optional.of(scene));

        mockMvc.perform(get("/api/scenes/2").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("CPU 飙高排查"))
                .andExpect(jsonPath("$.category").value("CPU_HIGH"))
                .andExpect(jsonPath("$.steps[0].title").value("查看 JVM 概览"))
                .andExpect(jsonPath("$.steps[0].scene").doesNotExist());
    }
}
