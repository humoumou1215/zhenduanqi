INSERT INTO arthas_server (id, name, host, http_port, token, created_at, updated_at) VALUES ('server-1', '生产环境-应用A', '192.168.1.100', 8563, 'your-arthas-token-here', NOW(), NOW());
INSERT INTO arthas_server (id, name, host, http_port, token, created_at, updated_at) VALUES ('server-2', '测试环境-应用B', '192.168.1.101', 8563, 'your-arthas-token-here', NOW(), NOW());

-- 默认角色
INSERT INTO sys_role (role_code, role_name, description) VALUES ('ADMIN', '管理员', '系统管理员，拥有所有权限');
INSERT INTO sys_role (role_code, role_name, description) VALUES ('OPERATOR', '操作员', '可执行诊断命令和使用场景模板');
INSERT INTO sys_role (role_code, role_name, description) VALUES ('READONLY', '只读用户', '仅可查看服务器列表和执行结果');

-- 默认管理员: admin / Abcd1234
-- 注意：密码需要预先用 bcrypt 编码，此 Hash 由应用初始化时生成
-- 应用启动后可通过 AuthService 创建用户

-- 默认高危命令黑名单规则
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^ognl\b', 'OGNL 表达式执行', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^reset\b', '重置 Arthas 增强类', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^mc\b', '内存编译器', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^redefine\b', '重新定义类', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^classloader\b', '类加载器操作', TRUE, NOW(), NOW());
INSERT INTO command_guard_rule (rule_type, pattern, description, enabled, created_at, updated_at) VALUES ('BLACKLIST', '^heapdump\b', 'Heap Dump', TRUE, NOW(), NOW());
