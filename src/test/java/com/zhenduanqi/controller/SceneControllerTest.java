package com.zhenduanqi.controller;

import com.zhenduanqi.aspect.RoleAspect;
import com.zhenduanqi.entity.DiagnoseScene;
import com.zhenduanqi.entity.SceneStep;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysUserRepository;
import com.zhenduanqi.service.AuthService;
import com.zhenduanqi.service.SceneService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private SysUser adminUser;

    @BeforeEach
    void setUp() {
        SysRole adminRole = new SysRole();
        adminRole.setRoleCode("ADMIN");
        adminUser = new SysUser();
        adminUser.setUsername("admin");
        adminUser.setRoles(Set.of(adminRole));

        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    }

    private DiagnoseScene createMockScene() {
        DiagnoseScene scene = new DiagnoseScene();
        scene.setId(1L);
        scene.setName("测试场景");
        scene.setDescription("测试场景描述");
        scene.setCategory("内存");
        scene.setSortOrder(1);
        scene.setEnabled(true);

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setScene(scene);
        step1.setStepOrder(1);
        step1.setTitle("步骤1");
        step1.setDescription("步骤1描述");
        step1.setCommand("thread");

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setScene(scene);
        step2.setStepOrder(2);
        step2.setTitle("步骤2");
        step2.setDescription("步骤2描述");
        step2.setCommand("heapdump");

        scene.setSteps(List.of(step1, step2));
        return scene;
    }

    @Test
    void getScenes_withNoCategory_returnsScenesList() throws Exception {
        DiagnoseScene scene = createMockScene();
        when(sceneService.getAllScenes()).thenReturn(List.of(scene));

        mockMvc.perform(get("/api/scenes")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("测试场景"))
                .andExpect(jsonPath("$[0].steps").isArray())
                .andExpect(jsonPath("$[0].steps[0].title").value("步骤1"));
    }

    @Test
    void getSceneById_withValidId_returnsScene() throws Exception {
        DiagnoseScene scene = createMockScene();
        when(sceneService.getSceneById(1L)).thenReturn(java.util.Optional.of(scene));

        mockMvc.perform(get("/api/scenes/1")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("测试场景"))
                .andExpect(jsonPath("$.steps").isArray());
    }
}
