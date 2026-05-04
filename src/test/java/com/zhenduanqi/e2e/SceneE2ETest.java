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
    void scene6_shouldHaveCorrectStepConfiguration() throws Exception {
        DiagnoseScene scene6 = new DiagnoseScene();
        scene6.setId(6L);
        scene6.setName("方法耗时追踪");
        scene6.setCategory("METHOD");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setTitle("确认类已加载");
        step1.setCommand("sc -d {className}");
        step1.setContinuous(false);
        step1.setMaxExecTime(10000);
        step1.setScene(scene6);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setTitle("追踪方法调用路径");
        step2.setCommand("trace {className} {methodName} -n 5");
        step2.setContinuous(true);
        step2.setMaxExecTime(30000);
        step2.setScene(scene6);

        SceneStep step3 = new SceneStep();
        step3.setId(3L);
        step3.setTitle("观察方法入参和返回值");
        step3.setCommand("watch {className} {methodName} '{params, returnObj, throwExp}' -n 5 -x 2");
        step3.setContinuous(true);
        step3.setMaxExecTime(30000);
        step3.setScene(scene6);

        scene6.setSteps(Arrays.asList(step1, step2, step3));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene6));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("方法耗时追踪"))
                .andExpect(jsonPath("$[0].category").value("METHOD"))
                .andExpect(jsonPath("$[0].steps[0].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[1].continuous").value(true))
                .andExpect(jsonPath("$[0].steps[2].continuous").value(true))
                .andExpect(jsonPath("$[0].steps[1].maxExecTime").value(30000))
                .andExpect(jsonPath("$[0].steps[2].maxExecTime").value(30000));
    }

    @Test
    void scene7_shouldHaveCorrectStepConfiguration() throws Exception {
        DiagnoseScene scene7 = new DiagnoseScene();
        scene7.setId(7L);
        scene7.setName("方法调用监控");
        scene7.setCategory("METHOD");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setContinuous(false);
        step1.setMaxExecTime(10000);
        step1.setScene(scene7);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setContinuous(true);
        step2.setMaxExecTime(60000);
        step2.setScene(scene7);

        SceneStep step3 = new SceneStep();
        step3.setId(3L);
        step3.setContinuous(true);
        step3.setMaxExecTime(30000);
        step3.setScene(scene7);

        scene7.setSteps(Arrays.asList(step1, step2, step3));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene7));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("方法调用监控"))
                .andExpect(jsonPath("$[0].steps[1].continuous").value(true))
                .andExpect(jsonPath("$[0].steps[1].maxExecTime").value(60000));
    }

    @Test
    void scene8_shouldHaveAllSyncSteps() throws Exception {
        DiagnoseScene scene8 = new DiagnoseScene();
        scene8.setId(8L);
        scene8.setName("类冲突排查");
        scene8.setCategory("CLASSLOADER");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setContinuous(false);
        step1.setScene(scene8);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setContinuous(false);
        step2.setScene(scene8);

        SceneStep step3 = new SceneStep();
        step3.setId(3L);
        step3.setContinuous(false);
        step3.setScene(scene8);

        scene8.setSteps(Arrays.asList(step1, step2, step3));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene8));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("类冲突排查"))
                .andExpect(jsonPath("$[0].steps[0].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[1].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[2].continuous").value(false));
    }

    @Test
    void scene4_shouldHaveCorrectConfiguration() throws Exception {
        DiagnoseScene scene4 = new DiagnoseScene();
        scene4.setId(4L);
        scene4.setName("GC 概况诊断");
        scene4.setCategory("MEMORY");

        SceneStep step1 = new SceneStep();
        step1.setId(1L);
        step1.setTitle("查看内存区");
        step1.setCommand("memory");
        step1.setContinuous(false);
        step1.setMaxExecTime(10000);
        step1.setScene(scene4);

        SceneStep step2 = new SceneStep();
        step2.setId(2L);
        step2.setTitle("查看各内存池详情");
        step2.setCommand("vmtool --action getInstances --className java.lang.management.MemoryPoolMXBean");
        step2.setContinuous(false);
        step2.setMaxExecTime(15000);
        step2.setScene(scene4);

        scene4.setSteps(Arrays.asList(step1, step2));

        when(sceneService.getAllScenes()).thenReturn(Arrays.asList(scene4));

        mockMvc.perform(get("/api/scenes").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("GC 概况诊断"))
                .andExpect(jsonPath("$[0].category").value("MEMORY"))
                .andExpect(jsonPath("$[0].steps[0].command").value("memory"))
                .andExpect(jsonPath("$[0].steps[0].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[0].maxExecTime").value(10000))
                .andExpect(jsonPath("$[0].steps[1].command").value("vmtool --action getInstances --className java.lang.management.MemoryPoolMXBean"))
                .andExpect(jsonPath("$[0].steps[1].continuous").value(false))
                .andExpect(jsonPath("$[0].steps[1].maxExecTime").value(15000));
    }
}
