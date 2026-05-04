package com.zhenduanqi.e2e;

import com.zhenduanqi.aspect.RoleAspect;
import com.zhenduanqi.controller.SceneController;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SceneController.class)
@Import(RoleAspect.class)
@EnableAspectJAutoProxy
class SceneE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SceneService sceneService;

    @MockBean
    private AuthService authService;

    @MockBean
    private SysUserRepository userRepository;

    private final Cookie adminCookie = new Cookie("zhenduanqi_token", "admin-jwt");

    @BeforeEach
    void setUp() {
        SysRole adminRole = new SysRole();
        adminRole.setRoleCode("ADMIN");
        SysUser adminUser = new SysUser();
        adminUser.setUsername("admin");
        adminUser.setRoles(Set.of(adminRole));

        when(authService.validateToken("admin-jwt")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    }

    @Test
    void scene1_shouldHaveCorrectStepConfiguration() throws Exception {
        DiagnoseScene scene1 = new DiagnoseScene();
        scene1.setId(1L);
        scene1.setName("接口响应慢排查");
        scene1.setCategory("SLOW_RESPONSE");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setTitle("查看 JVM 概览");
        step1.setCommand("dashboard -n 1");
        step1.setContinuous(false);
        step1.setMaxExecTime(15000);
        step1.setScene(scene1);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setTitle("确认类已加载");
        step2.setCommand("sc -d {className}");
        step2.setContinuous(false);
        step2.setMaxExecTime(10000);
        step2.setScene(scene1);

        SceneStep step3 = new SceneStep();
        step3.setId(3L);
        step3.setTitle("追踪方法调用路径");
        step3.setCommand("trace {className} {methodName} -n 5");
        step3.setContinuous(true);
        step3.setMaxExecTime(30000);
        step3.setScene(scene1);

        SceneStep step4 = new SceneStep();
        step4.setId(4L);
        step4.setTitle("观察方法入参和返回值");
        step4.setCommand("watch {className} {methodName} '{params, returnObj, throwExp}' -n 5 -x 2");
        step4.setContinuous(true);
        step4.setMaxExecTime(30000);
        step4.setScene(scene1);

        scene1.setSteps(Arrays.asList(step1, step2, step3, step4));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene1));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("接口响应慢排查"))
                .andExpect(jsonPath("$[0].category").value("SLOW_RESPONSE"))
                .andExpect(jsonPath("$[0].steps[0].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[2].continuous").value(true))
                .andExpect(jsonPath("$[0].steps[3].continuous").value(true))
                .andExpect(jsonPath("$[0].steps[2].maxExecTime").value(30000))
                .andExpect(jsonPath("$[0].steps[3].maxExecTime").value(30000));
    }

    @Test
    void scene2_shouldHaveCorrectStepConfiguration() throws Exception {
        DiagnoseScene scene2 = new DiagnoseScene();
        scene2.setId(2L);
        scene2.setName("CPU 飙高排查");
        scene2.setCategory("CPU_HIGH");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setContinuous(false);
        step1.setMaxExecTime(15000);
        step1.setScene(scene2);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setContinuous(false);
        step2.setMaxExecTime(10000);
        step2.setScene(scene2);

        SceneStep step3 = new SceneStep();
        step3.setId(3L);
        step3.setContinuous(false);
        step3.setMaxExecTime(10000);
        step3.setScene(scene2);

        SceneStep step4 = new SceneStep();
        step4.setId(4L);
        step4.setContinuous(false);
        step4.setMaxExecTime(10000);
        step4.setScene(scene2);

        scene2.setSteps(Arrays.asList(step1, step2, step3, step4));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene2));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CPU 飙高排查"))
                .andExpect(jsonPath("$[0].category").value("CPU_HIGH"))
                .andExpect(jsonPath("$[0].steps[0].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[1].continuous").value(false));
    }

    @Test
    void scene6_shouldHaveAllSyncSteps() throws Exception {
        DiagnoseScene scene6 = new DiagnoseScene();
        scene6.setId(6L);
        scene6.setName("类加载异常排查");
        scene6.setCategory("CLASS_LOAD_ERROR");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setContinuous(false);
        step1.setScene(scene6);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setContinuous(false);
        step2.setScene(scene6);

        SceneStep step3 = new SceneStep();
        step3.setId(3L);
        step3.setContinuous(false);
        step3.setScene(scene6);

        scene6.setSteps(Arrays.asList(step1, step2, step3));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene6));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("类加载异常排查"))
                .andExpect(jsonPath("$[0].category").value("CLASS_LOAD_ERROR"))
                .andExpect(jsonPath("$[0].steps[0].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[1].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[2].continuous").value(false));
    }

    @Test
    void scene4_shouldHaveCorrectConfiguration() throws Exception {
        DiagnoseScene scene4 = new DiagnoseScene();
        scene4.setId(4L);
        scene4.setName("GC 频繁排查");
        scene4.setCategory("GC_FREQUENT");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setTitle("查看 JVM 概览");
        step1.setCommand("dashboard -n 1");
        step1.setContinuous(false);
        step1.setMaxExecTime(15000);
        step1.setScene(scene4);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setTitle("查看内存区");
        step2.setCommand("memory");
        step2.setContinuous(false);
        step2.setMaxExecTime(10000);
        step2.setScene(scene4);

        scene4.setSteps(Arrays.asList(step1, step2));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene4));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("GC 频繁排查"))
                .andExpect(jsonPath("$[0].category").value("GC_FREQUENT"))
                .andExpect(jsonPath("$[0].steps[0].command").value("dashboard -n 1"))
                .andExpect(jsonPath("$[0].steps[0].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[0].maxExecTime").value(15000))
                .andExpect(jsonPath("$[0].steps[1].command").value("memory"))
                .andExpect(jsonPath("$[0].steps[1].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[1].maxExecTime").value(10000));
    }
}
