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

-- 预置场景 1: 接口响应慢排查 (SLOW_RESPONSE)
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('接口响应慢排查', '排查接口响应时间长、超时等问题', 'SLOW_RESPONSE', '接口超时、响应时间长', 'Clock', 1, TRUE, NOW(), NOW());

-- 预置场景 2: CPU 飙高排查 (CPU_HIGH)
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('CPU 飙高排查', '排查 CPU 使用率异常升高问题', 'CPU_HIGH', 'CPU 使用率飙升、服务器负载高', 'Activity', 2, TRUE, NOW(), NOW());

-- 预置场景 3: 内存使用率高排查 (MEMORY_HIGH)
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('内存使用率高排查', '排查内存使用率高和 OOM 问题', 'MEMORY_HIGH', 'OOM 告警、堆内存持续增长', 'Database', 3, TRUE, NOW(), NOW());

-- 预置场景 4: GC 频繁排查 (GC_FREQUENT)
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('GC 频繁排查', '排查 GC 频繁导致的卡顿问题', 'GC_FREQUENT', 'GC 频繁、应用卡顿停顿', 'RefreshCw', 4, TRUE, NOW(), NOW());

-- 预置场景 5: 线程池使用率高排查 (THREAD_POOL_HIGH)
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('线程池使用率高排查', '排查线程池使用率高导致的请求积压', 'THREAD_POOL_HIGH', '请求积压、任务排队', 'Layers', 5, TRUE, NOW(), NOW());

-- 预置场景 6: 类加载异常排查 (CLASS_LOAD_ERROR)
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('类加载异常排查', '排查类加载和类冲突问题', 'CLASS_LOAD_ERROR', 'ClassNotFoundException、NoSuchMethodError', 'Search', 6, TRUE, NOW(), NOW());

-- 场景 1 步骤 (SLOW_RESPONSE)
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 1, '查看 JVM 概览', '查看 JVM 运行概览，判断问题类型', 'dashboard -n 1', '如果发现 CPU 或 Memory 异常，继续后续步骤', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 2, '确认类已加载', '确认目标类已加载', 'sc -d {className}', '确认类加载器信息', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 3, '追踪方法调用路径', '追踪方法调用路径和耗时', 'trace {className} {methodName} -n 5', '记录耗时最长的方法调用', TRUE, 30000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 4, '观察方法入参和返回值', '观察方法入参和返回值', 'watch {className} {methodName} \'{params, returnObj, throwExp}\' -n 5 -x 2', '检查入参和返回值是否异常', TRUE, 30000, NULL, NOW(), NOW());

-- 场景 2 步骤 (CPU_HIGH)
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 1, '查看 JVM 概览', '查看整体状态', 'dashboard -n 1', '确认 CPU 使用情况', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 2, '找出 CPU 最高的线程', '查看 CPU 占用最高的 5 个线程', 'thread -n 5', '记录 CPU 最高的线程 ID', FALSE, 10000, '[{"variable":"threadId","jsonPath":"$[?(@.type==\"thread\")][0].data.id","description":"从 thread -n 5 结果中提取第一个线程的 ID"},{"variable":"threadId","jsonPath":"$[?(@.type==\"thread\")][0].data.threadId","description":"备用路径，从 thread -n 5 结果中提取第一个线程的 ID"}]', NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 3, '查看线程栈', '查看指定线程的完整堆栈信息', 'thread {threadId}', '分析线程堆栈定位 CPU 热点', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 4, '检查死锁', '检查是否存在死锁', 'thread -b', '如果有死锁，需要先解决死锁问题', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 3 步骤 (MEMORY_HIGH)
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 1, '查看 JVM 概览', '查看整体状态', 'dashboard -n 1', '确认 Memory 使用情况', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 2, '查看内存区使用情况', '查看各内存区使用情况', 'memory', '确认哪个内存区使用率最高', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 3, '查看堆中大对象', '查看堆内存中占用最大的 10 个对象', 'heap -h 10', '分析大对象来源', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 4, '查看类加载信息', '查看指定类的加载信息', 'sc -d {className}', '分析类加载器是否有问题', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 4 步骤 (GC_FREQUENT)
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 1, '查看 JVM 概览', '查看整体状态', 'dashboard -n 1', '确认 GC 频率和耗时', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 2, '查看内存区', '查看各内存区使用情况', 'memory', '确认哪个内存区 GC 最频繁', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 3, '查看各内存池详情', '使用 vmtool 查看各内存池的详细信息', 'vmtool --action getInstances --className java.lang.management.MemoryPoolMXBean', '分析各内存池配置和使用情况', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 4, '查看 JVM 参数', '查看 JVM 参数配置', 'vmoption', '检查 -Xmx、-Xms 等内存相关参数', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 5 步骤 (THREAD_POOL_HIGH)
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 1, '查看 JVM 概览', '查看整体状态', 'dashboard -n 1', '确认线程数和 CPU 使用情况', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 2, '查看所有线程状态', '查看所有线程状态', 'thread', '找出 BLOCKED 或 WAITING 状态的线程', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 3, '检查死锁', '检查是否存在死锁', 'thread -b', '如果有死锁，需要先解决死锁问题', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 4, '查看 CPU 最高的线程', '查看 CPU 占用最高的 5 个线程', 'thread -n 5', '记录 CPU 最高的线程 ID', FALSE, 10000, '[{"variable":"threadId","jsonPath":"$[?(@.type==\"thread\")][0].data.id","description":"从 thread -n 5 结果中提取第一个线程的 ID"}]', NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 5, '查看具体线程栈', '查看指定线程的完整堆栈信息', 'thread {threadId}', '分析线程堆栈定位问题', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 6 步骤 (CLASS_LOAD_ERROR)
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 1, '查看类加载信息', '查看指定类的加载信息', 'sc -d {className}', '确认类是否已加载，加载器是什么', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 2, '查看 ClassLoader 继承树', '查看 ClassLoader 继承树', 'classloader -t', '确认 ClassLoader 层级关系', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 3, '反编译查看源码', '反编译指定类查看源码', 'jad {className}', '分析类源码是否有问题', FALSE, 15000, NULL, NOW(), NOW());

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

