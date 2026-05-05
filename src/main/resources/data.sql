INSERT INTO arthas_server (id, name, host, http_port, token, username, password, created_at, updated_at) VALUES ('server-1', '生产环境-应用A', '192.168.1.100', 8563, 'your-arthas-token-here', 'arthas', 'pswd123', NOW(), NOW());
INSERT INTO arthas_server (id, name, host, http_port, token, username, password, created_at, updated_at) VALUES ('server-2', '测试环境-应用B', '192.168.1.101', 8563, 'your-arthas-token-here', 'arthas', 'pswd123', NOW(), NOW());
INSERT INTO arthas_server (id, name, host, http_port, token, username, password, created_at, updated_at) VALUES ('server-test', '测试服务器', '47.99.63.148', 8563, null, 'arthas', 'pswd123', NOW(), NOW());

-- 默认角色
INSERT INTO sys_role (role_code, role_name, description) VALUES ('ADMIN', '管理员', '系统管理员，拥有所有权限');
INSERT INTO sys_role (role_code, role_name, description) VALUES ('OPERATOR', '操作员', '可执行诊断命令和使用场景模板');
INSERT INTO sys_role (role_code, role_name, description) VALUES ('READONLY', '只读用户', '仅可查看服务器列表和执行结果');

-- 默认高危命令黑名单规则 (与 PRD 一致)
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^ognl\b', 'OGNL 表达式可执行任意代码，极高风险', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^mc\b', '内存编译器，可编译恶意类', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^redefine\b', '热替换类字节码，可能导致不可预期行为', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^retransform\b', '类似 redefine，热替换字节码', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^heapdump\b', 'dump 堆可能产生大文件，影响磁盘和性能', TRUE, NOW(), NOW());

-- 预置场景 1: 线程死锁检测
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('线程死锁检测', '检测 JVM 中是否存在死锁线程', 'THREAD', '应用卡死无响应、请求超时', 'Odometer', 1, TRUE, NOW(), NOW());

-- 预置场景 2: CPU 飙高排查
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('CPU 飙高排查', '排查 CPU 使用率异常升高问题', 'THREAD', 'CPU 使用率飙升、服务器负载高', 'Odometer', 2, TRUE, NOW(), NOW());

-- 预置场景 3: 内存泄漏检测
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('内存泄漏检测', '排查堆内存泄漏和 OOM 问题', 'MEMORY', 'OOM 告警、堆内存持续增长', 'DataAnalysis', 3, TRUE, NOW(), NOW());

-- 预置场景 4: GC 分析
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('GC 分析', '分析 GC 频率和性能问题', 'MEMORY', 'GC 频繁、应用卡顿停顿', 'DataAnalysis', 4, TRUE, NOW(), NOW());

-- 预置场景 5: JVM 参数检查
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('JVM 参数检查', '检查 JVM 配置和环境信息', 'JVM', '配置确认、环境信息', 'Box', 5, TRUE, NOW(), NOW());

-- 预置场景 6: 方法追踪
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('方法追踪', '追踪方法调用路径和耗时', 'METHOD', '耗时追踪、调用监控', 'Monitor', 6, TRUE, NOW(), NOW());

-- 预置场景 7: 方法监控
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('方法监控', '监控方法执行情况和异常', 'METHOD', '方法调用监控、异常统计', 'Monitor', 7, TRUE, NOW(), NOW());

-- 预置场景 8: 类加载异常排查
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('类加载异常排查', '排查 ClassNotFoundException 和类冲突问题', 'CLASSLOADER', 'ClassNotFoundException、类冲突', 'Connection', 8, TRUE, NOW(), NOW());

-- 场景 1 步骤: 线程死锁检测
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 1, '检查死锁线程', '检测 JVM 中是否存在死锁线程', 'thread -b', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 2, '查看所有线程状态', '查看所有线程状态', 'thread', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 2 步骤: CPU 飙高排查
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 1, '找出 CPU 最高的线程', '查看 CPU 占用最高的 5 个线程', 'thread -n 5', '记录 CPU 最高的线程 ID，填入下一步', FALSE, 10000, '[{"variable":"threadId","jsonPath":"$[?(@.type==\"thread\")][0].data.id","description":"从 thread -n 5 结果中提取第一个线程的 ID"},{"variable":"threadId","jsonPath":"$[?(@.type==\"thread\")][0].data.threadId","description":"备用路径，从 thread -n 5 结果中提取第一个线程的 ID (使用 threadId 字段)"}]', NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 2, '查看线程栈', '查看指定线程的完整堆栈信息', 'thread {threadId}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 3, '查看所有线程', '查看所有线程状态', 'thread', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 3 步骤: 内存泄漏检测
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 1, '查看内存区使用情况', '查看各内存区使用情况', 'memory', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 2, '查看堆中大对象', '查看堆内存中占用最大的 10 个对象', 'heap -h 10', '', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 3, '查看 GC 历史', '查看 GC 历史统计信息', 'dashboard -n 1 -i 1', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 4 步骤: GC 分析
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 1, '查看内存区', '查看各内存区使用情况', 'memory', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 2, '查看 Dashboard', '查看 Dashboard 综合信息', 'dashboard -n 1 -i 1', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 3, '查看 JVM 参数', '查看 JVM 参数配置', 'vmoption', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 5 步骤: JVM 参数检查
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 1, '查看 JVM 参数', '查看 JVM 参数配置', 'vmoption', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 2, '查看系统环境', '查看系统环境变量', 'sysenv', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 3, '查看系统属性', '查看 JVM 系统属性', 'sysprop', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 6 步骤: 方法追踪
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 1, '确认类已加载', '确认目标类已加载', 'sc -d {className}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 2, '追踪方法调用路径', '追踪方法调用路径和耗时', 'trace {className} {methodName} -n 5', '', TRUE, 30000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 3, '观察方法入参和返回值', '观察方法入参和返回值', 'watch {className} {methodName} \'{params, returnObj, throwExp}\' -n 5 -x 2', '', TRUE, 30000, NULL, NOW(), NOW());

-- 场景 7 步骤: 方法监控
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (7, 1, '确认类已加载', '确认目标类已加载', 'sc -d {className}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (7, 2, '监控方法执行', '监控方法执行次数和耗时', 'monitor -c 5 {className} {methodName}', '', TRUE, 30000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (7, 3, '查看方法栈', '查看方法调用栈', 'stack {className} -n 5', '', TRUE, 30000, NULL, NOW(), NOW());

-- 场景 8 步骤: 类加载异常排查
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (8, 1, '查看类加载信息', '查看指定类的加载信息', 'sc -d {className}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (8, 2, '查看 ClassLoader 继承树', '查看 ClassLoader 继承树', 'classloader -t', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (8, 3, '反编译查看源码', '反编译指定类查看源码', 'jad {className}', '', FALSE, 15000, NULL, NOW(), NOW());

-- arthas_session 表
CREATE TABLE IF NOT EXISTS arthas_session (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id           VARCHAR(50)  NOT NULL,
    arthas_session_id   VARCHAR(100),
    arthas_consumer_id  VARCHAR(100),
    current_job_id      INT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    username            VARCHAR(50) NOT NULL,
    scene_id            BIGINT,
    step_id             BIGINT,
    command             TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at           TIMESTAMP
);
