package com.zhenduanqi.config;

import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.entity.DiagnoseScene;
import com.zhenduanqi.entity.SceneStep;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.repository.ArthasServerRepository;
import com.zhenduanqi.repository.CommandGuardRuleRepository;
import com.zhenduanqi.repository.DiagnoseSceneRepository;
import com.zhenduanqi.repository.SceneStepRepository;
import com.zhenduanqi.repository.SysRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("test")
@Order(1)
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    private final SysRoleRepository roleRepository;
    private final ArthasServerRepository serverRepository;
    private final CommandGuardRuleRepository ruleRepository;
    private final DiagnoseSceneRepository sceneRepository;
    private final SceneStepRepository stepRepository;

    public TestDataInitializer(SysRoleRepository roleRepository,
                                ArthasServerRepository serverRepository,
                                CommandGuardRuleRepository ruleRepository,
                                DiagnoseSceneRepository sceneRepository,
                                SceneStepRepository stepRepository) {
        this.roleRepository = roleRepository;
        this.serverRepository = serverRepository;
        this.ruleRepository = ruleRepository;
        this.sceneRepository = sceneRepository;
        this.stepRepository = stepRepository;
    }

    @Override
    public void run(String... args) {
        initRoles();
        initServers();
        initCommandGuardRules();
        initScenesAndSteps();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            log.info("Initializing test roles...");
            SysRole admin = new SysRole();
            admin.setRoleCode("ADMIN");
            admin.setRoleName("管理员");
            admin.setDescription("系统管理员，拥有所有权限");

            SysRole operator = new SysRole();
            operator.setRoleCode("OPERATOR");
            operator.setRoleName("操作员");
            operator.setDescription("可执行诊断命令和使用场景模板");

            SysRole readonly = new SysRole();
            readonly.setRoleCode("READONLY");
            readonly.setRoleName("只读用户");
            readonly.setDescription("仅可查看服务器列表和执行结果");

            roleRepository.saveAll(List.of(admin, operator, readonly));
            log.info("Test roles initialized successfully");
        }
    }

    private void initServers() {
        if (serverRepository.count() == 0) {
            log.info("Initializing test servers...");
            ArthasServerEntity server1 = new ArthasServerEntity();
            server1.setId("server-1");
            server1.setName("生产环境-应用A");
            server1.setHost("192.168.1.100");
            server1.setHttpPort(8563);
            server1.setUsername("arthas");
            server1.setPassword("pswd123");

            ArthasServerEntity server2 = new ArthasServerEntity();
            server2.setId("server-2");
            server2.setName("测试环境-应用B");
            server2.setHost("192.168.1.101");
            server2.setHttpPort(8563);
            server2.setUsername("arthas");
            server2.setPassword("pswd123");

            ArthasServerEntity serverTest = new ArthasServerEntity();
            serverTest.setId("server-test");
            serverTest.setName("测试服务器");
            serverTest.setHost("47.99.63.148");
            serverTest.setHttpPort(8563);
            serverTest.setUsername("arthas");
            serverTest.setPassword("pswd123");

            serverRepository.saveAll(List.of(server1, server2, serverTest));
            log.info("Test servers initialized successfully");
        }
    }

    private void initCommandGuardRules() {
        if (ruleRepository.count() == 0) {
            log.info("Initializing test command guard rules...");
            CommandGuardRule rule1 = new CommandGuardRule();
            rule1.setRuleType("BLACKLIST");
            rule1.setPattern("^ognl\\b");
            rule1.setDescription("OGNL 表达式可执行任意代码，极高风险");
            rule1.setEnabled(true);

            CommandGuardRule rule2 = new CommandGuardRule();
            rule2.setRuleType("BLACKLIST");
            rule2.setPattern("^mc\\b");
            rule2.setDescription("内存编译器，可编译恶意类");
            rule2.setEnabled(true);

            CommandGuardRule rule3 = new CommandGuardRule();
            rule3.setRuleType("BLACKLIST");
            rule3.setPattern("^redefine\\b");
            rule3.setDescription("热替换类字节码，可能导致不可预期行为");
            rule3.setEnabled(true);

            CommandGuardRule rule4 = new CommandGuardRule();
            rule4.setRuleType("BLACKLIST");
            rule4.setPattern("^retransform\\b");
            rule4.setDescription("类似 redefine，热替换字节码");
            rule4.setEnabled(true);

            CommandGuardRule rule5 = new CommandGuardRule();
            rule5.setRuleType("BLACKLIST");
            rule5.setPattern("^heapdump\\b");
            rule5.setDescription("dump 堆可能产生大文件，影响磁盘和性能");
            rule5.setEnabled(true);

            ruleRepository.saveAll(List.of(rule1, rule2, rule3, rule4, rule5));
            log.info("Test command guard rules initialized successfully");
        }
    }

    private void initScenesAndSteps() {
        if (sceneRepository.count() == 0) {
            log.info("Initializing test scenes and steps...");
            
            DiagnoseScene scene1 = new DiagnoseScene();
            scene1.setName("线程死锁检测");
            scene1.setDescription("检测 JVM 中是否存在死锁线程");
            scene1.setCategory("THREAD");
            scene1.setBusinessScenario("应用卡死无响应、请求超时");
            scene1.setIcon("Odometer");
            scene1.setSortOrder(1);
            scene1.setEnabled(true);
            scene1 = sceneRepository.save(scene1);

            SceneStep step1_1 = new SceneStep();
            step1_1.setScene(scene1);
            step1_1.setStepOrder(1);
            step1_1.setTitle("检查死锁线程");
            step1_1.setDescription("检测 JVM 中是否存在死锁线程");
            step1_1.setCommand("thread -b");
            step1_1.setContinuous(false);
            step1_1.setMaxExecTime(10000);
            stepRepository.save(step1_1);

            SceneStep step1_2 = new SceneStep();
            step1_2.setScene(scene1);
            step1_2.setStepOrder(2);
            step1_2.setTitle("查看所有线程状态");
            step1_2.setDescription("查看所有线程状态");
            step1_2.setCommand("thread");
            step1_2.setContinuous(false);
            step1_2.setMaxExecTime(10000);
            stepRepository.save(step1_2);

            log.info("Test scenes and steps initialized successfully");
        }
    }
}
