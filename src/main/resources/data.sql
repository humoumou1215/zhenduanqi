INSERT INTO arthas_server (id, name, host, http_port, token, username, password, created_at, updated_at) VALUES ('server-1', '生产环境-应用A', '192.168.1.100', 8563, 'your-arthas-token-here', 'arthas', 'pswd123', NOW(), NOW());
INSERT INTO arthas_server (id, name, host, http_port, token, username, password, created_at, updated_at) VALUES ('server-2', '测试环境-应用B', '192.168.1.101', 8563, 'your-arthas-token-here', 'arthas', 'pswd123', NOW(), NOW());

-- 默认角色
INSERT INTO sys_role (role_code, role_name, description) VALUES ('ADMIN', '管理员', '系统管理员，拥有所有权限');
INSERT INTO sys_role (role_code, role_name, description) VALUES ('OPERATOR', '操作员', '可执行诊断命令和使用场景模板');
INSERT INTO sys_role (role_code, role_name, description) VALUES ('READONLY', '只读用户', '仅可查看服务器列表和执行结果');

-- 默认高危命令黑名单规则 (与 PRD 一致)
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^ognl\\b', 'OGNL 表达式可执行任意代码，极高风险', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^mc\\b', '内存编译器，可编译恶意类', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^redefine\\b', '热替换类字节码，可能导致不可预期行为', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^retransform\\b', '类似 redefine，热替换字节码', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^heapdump\\b', 'dump 堆可能产生大文件，影响磁盘和性能', TRUE, NOW(), NOW());

-- 预置场景 1: 线程死锁检测
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('线程死锁检测', '检测 Java 线程死锁检测和分析', 'THREAD', '应用卡死无响应、请求超时', 'Lock', 1, TRUE, NOW(), NOW());

-- 预置场景 2: CPU 飙高排查
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('CPU 飙高排查', '排查 CPU 使用率异常升高问题', 'THREAD', 'CPU 使用率飙升、服务器负载高', 'Activity', 2, TRUE, NOW(), NOW());

-- 预置场景 3: 内存泄漏初步检查
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('内存泄漏初步检查', '初步检查内存使用情况和内存泄漏', 'MEMORY', 'OOM 告警、堆内存持续增长', 'Database', 3, TRUE, NOW(), NOW());

-- 预置场景 4: GC 概况诊断
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('GC 概况诊断', '诊断 GC 相关问题', 'MEMORY', 'GC 频繁、应用卡顿', 'RefreshCw', 4, TRUE, NOW(), NOW());

-- 预置场景 5: JVM 基础信息
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('JVM 基础信息', '查看 JVM 基础配置和环境信息', 'JVM', '应用行为异常、配置确认', 'Info', 5, TRUE, NOW(), NOW());

-- 预置场景 6: 方法耗时追踪
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('方法耗时追踪', '追踪方法调用路径和耗时', 'METHOD', '接口响应慢、方法耗时异常', 'Clock', 6, TRUE, NOW(), NOW());

-- 预置场景 7: 方法调用监控
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('方法调用监控', '监控方法调用统计', 'METHOD', '方法调用频次异常、失败率高', 'BarChart', 7, TRUE, NOW(), NOW());

-- 预置场景 8: 类冲突排查
INSERT INTO diagnose_scene (name, description, category, business_scenario, icon, sort_order, enabled, created_at, updated_at) VALUES ('类冲突排查', '排查类加载和类冲突问题', 'CLASSLOADER', 'ClassNotFoundException、NoSuchMethodError', 'Search', 8, TRUE, NOW(), NOW());

-- 场景 1 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 1, '检查死锁线程', '检测 JVM 中是否存在死锁线程', 'thread -b', '如果发现死锁，继续下一步查看 CPU 热点，或查看线程栈', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 2, 'TOP 5 CPU 活跃线程', '查看 CPU 占用最高的 5 个线程', 'thread -n 5', '记录 CPU 最高的线程 ID，填入下一步', FALSE, 10000, '[{"variable":"threadId","jsonPath":"$[0].id","description":"从 thread -n 5 结果中提取第一个线程的 ID"}]', NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (1, 3, '查看具体线程栈', '查看指定线程的完整堆栈信息', 'thread {threadId}', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 2 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 1, '找出 CPU 最高的线程', '查看 CPU 占用最高的 5 个线程', 'thread -n 5', '记录 CPU 最高的线程 ID，填入下一步', FALSE, 10000, '[{"variable":"threadId","jsonPath":"$[0].id","description":"从 thread -n 5 结果中提取第一个线程的 ID"}]', NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 2, '查看线程栈', '查看指定线程的完整堆栈信息', 'thread {threadId}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (2, 3, '查看 JVM 概览', '查看 JVM 运行概览', 'dashboard -n 1', '', FALSE, 15000, NULL, NOW(), NOW());

-- 场景 3 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 1, '查看内存区使用情况', '查看各内存区使用情况', 'memory', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 2, '查看堆中大对象', '查看堆内存中占用最大的 10 个对象', 'heapdump --live', '', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (3, 3, '查看类加载信息', '查看指定类的加载信息', 'sc -d {className}', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 4 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (4, 1, '查看内存区', '查看各内存区使用情况', 'memory', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 5 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 1, 'JVM 概览', '查看 JVM 运行概览', 'dashboard -n 1', '', FALSE, 15000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 2, 'JVM 参数', '查看 JVM 参数配置', 'vmoption', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (5, 3, '系统环境变量', '查看系统环境变量', 'sysenv', '', FALSE, 10000, NULL, NOW(), NOW());

-- 场景 6 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 1, '确认类已加载', '确认指定类已加载', 'sc -d {className}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 2, '追踪方法调用路径', '追踪方法调用路径和耗时', 'trace {className} {methodName} -n 5', '', TRUE, 30000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (6, 3, '观察方法入参和返回值', '观察方法入参和返回值', 'watch {className} {methodName} \'{params, returnObj, throwExp}\' -n 5 -x 2', '', TRUE, 30000, NULL, NOW(), NOW());

-- 场景 7 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (7, 1, '确认类已加载', '确认指定类已加载', 'sc -d {className}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (7, 2, '监控方法调用统计', '监控方法调用统计', 'monitor {className} {methodName} -n 10', '', TRUE, 60000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (7, 3, '查看方法调用路径', '查看方法调用路径', 'stack {className} {methodName} -n 5', '', TRUE, 30000, NULL, NOW(), NOW());

-- 场景 8 步骤
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (8, 1, '查看类加载信息', '查看指定类的加载信息', 'sc -d {className}', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (8, 2, '查看 ClassLoader 继承树', '查看 ClassLoader 继承树', 'classloader -t', '', FALSE, 10000, NULL, NOW(), NOW());
INSERT INTO scene_step (scene_id, step_order, title, description, command, expected_hint, continuous, max_exec_time, extract_rules, created_at, updated_at) VALUES (8, 3, '反编译查看源码', '反编译指定类查看源码', 'jad {className}', '', FALSE, 15000, NULL, NOW(), NOW());

