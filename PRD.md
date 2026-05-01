# PRD: Arthas 远程诊断工具 (zhenduanqi)

## Problem Statement

SRE / 运维团队在日常 Java 应用排障过程中，需要登录到目标服务器手动启动 Arthas 并执行诊断命令。这一流程存在以下痛点：

- **操作门槛高**：每次都需要 SSH 登录到服务器，找到 Java 进程，启动 Arthas，操作链路长
- **知识分散**：常用的诊断命令（死锁检查、内存分析、GC 诊断等）缺乏沉淀和模板化，新人学习成本高
- **安全风险**：Arthas 功能强大，高危命令（如 `ognl` 执行等）缺乏管控，误操作可能导致线上事故
- **审计缺失**：谁在什么时候对哪台服务器执行了什么诊断命令，没有任何记录
- **权限混乱**：任何人只要有服务器访问权限就可以执行任意 Arthas 命令，缺乏角色分级
- **会话泄漏**：Arthas 字节码增强命令（watch/trace/monitor）如果未正常结束，会持续影响目标 JVM 性能，缺乏会话安全管控

## Solution

构建一个集中式的 Arthas 远程诊断 Web 平台，提供以下核心能力：

1. **服务器集中管理**：在平台中统一配置目标服务器及 Arthas HTTP API 连接信息（含 Token 管理）
2. **场景化诊断引导**：按问题类型分类组织诊断场景，每个场景包含逐步操作的步骤列表，支持结构化结果渲染和半自动变量填充
3. **全命令模式**：支持用户自由输入任何 Arthas 命令，同时提供高危命令正则匹配黑白名单拦截
4. **完整权限管理**：RBAC 角色权限模型，区分管理员/普通操作员/只读用户
5. **操作审计日志**：完整记录每一次诊断操作（含命令原文和执行结果），提供查询
6. **Arthas 会话安全管控**：四层安全机制（命令超时、会话超时、自动 reset、孤儿清理），防止字节码增强泄漏影响生产
7. **应用运行时日志**：统一的日志配置、输出、脱敏和请求链路追踪，使平台自身具备可观测性

## User Stories

1. 作为 SRE 工程师，我希望能在一个 Web 页面上看到所有配置了 Arthas 的服务器列表，以便快速选择要诊断的目标
2. 作为 SRE 工程师，我希望能选择服务器后输入任意 Arthas 命令并查看执行结果，以便灵活排查问题
3. 作为 SRE 工程师，我想要一个按问题类型分类的场景列表（如"线程问题"→"线程死锁检测"），以便快速定位合适的诊断场景
4. 作为 SRE 工程师，我希望能通过业务场景关键词（如"应用卡死无响应"）检索诊断场景，以便根据现象快速找到排查路径
5. 作为 SRE 工程师，我想要场景以步骤列表形式展示，每步有引导文案，逐条手动执行，结果展现在同一页面，以便一屏完成所有操作
6. 作为 SRE 工程师，我希望诊断命令的执行结果能根据类型自动选择合适的展示方式（表格、进度条等），以便直观理解结果
7. 作为 SRE 工程师，我希望场景步骤的占位符能从前序步骤结果中自动提取并预填，我确认或修改后再执行，以便减少手动复制粘贴
8. 作为 SRE 工程师，我希望连续输出命令（如 watch、trace）能实时展示结果，并能随时停止，以便灵活排查
9. 作为 SRE 工程师，我希望系统自动拦截高危 Arthas 命令，以避免误操作导致线上故障
10. 作为 SRE 工程师，我希望能查看自己执行过的诊断历史，以便回顾和分享排查思路
11. 作为 SRE 工程师，我希望刷新页面后能恢复正在执行的步骤状态，以便应对意外刷新
12. 作为系统管理员，我希望能添加/编辑/删除 Arthas 目标服务器，以便管理诊断覆盖范围
13. 作为系统管理员，我希望能创建和管理用户账号，并分配不同的角色权限
14. 作为系统管理员，我希望能查看所有用户的操作审计日志，以便满足合规和追溯需求
15. 作为系统管理员，我希望能通过控制台直接检查目标服务器的 Arthas 连接状态，以便快速诊断连通性问题
16. 作为只读用户，我只能查看服务器的基本信息和执行结果，不能执行新命令或修改配置
17. 作为普通操作员，我可以执行诊断命令并使用场景模板，但不能管理服务器配置或其他用户
18. 作为所有用户，我希望能使用用户名密码登录系统，登录成功后会话由 HttpOnly Cookie + JWT 维持
19. 作为所有用户，我希望能看到命令执行的历史记录和结果，以便复盘
20. 作为系统管理员，我希望能配置高危命令黑白名单规则（正则表达式），并根据需要启用或禁用
21. 作为系统管理员，我希望能创建/编辑/删除诊断场景和其中的每一个步骤，以便持续积累和优化诊断知识库
22. 作为 SRE 工程师，我希望场景步骤中使用 `{placeHolder}` 占位符标记需要手动填写的参数，并有清晰的提示说明
23. 作为系统管理员，我希望能查看所有活跃的 Arthas 会话并手动关闭，以便管控对生产环境的性能影响
24. 作为系统管理员，我希望系统自动清理超时的 Arthas 会话和字节码增强，以防止泄漏影响生产
25. 作为 SRE 工程师，我希望平台的所有认证事件（登录成功/失败、Token 过期、账户锁定）都有运行时日志记录，以便排查登录问题
26. 作为 SRE 工程师，我希望平台的权限拦截事件（角色校验失败、未认证访问）都有 WARN 级别日志，以便发现异常访问行为
27. 作为 SRE 工程师，我希望 Arthas 命令执行的完整链路（接收请求 → CommandGuard 校验 → 发送到 Arthas → 收到响应）都有日志记录，以便追踪命令执行问题
28. 作为 SRE 工程师，我希望高危命令拦截事件有独立的 WARN 日志，以便监控安全风险
29. 作为系统管理员，我希望审计日志写入失败时有 ERROR 日志，以便及时发现审计数据丢失
30. 作为 SRE 工程师，我希望每条日志都包含请求唯一标识（requestId），以便在并发请求中关联同一请求的所有日志
31. 作为 SRE 工程师，我希望日志中自动脱敏 password 和 token 字段，以便日志可以安全地分享和存储
32. 作为系统管理员，我希望生产环境的日志持久化到文件，以便在服务重启后仍可回溯问题
33. 作为系统管理员，我希望日志文件有滚动策略（按大小/日期），以便控制磁盘占用
34. 作为 SRE 工程师，我希望开发环境使用 DEBUG 级别、生产环境使用 INFO 级别，以便在不同环境下获取合适的日志粒度
35. 作为 SRE 工程师，我希望登录请求也包含 requestId 链路追踪，以便排查登录相关的安全问题
36. 作为 SRE 工程师，我希望请求链路追踪在任何异常情况下都能正确清理（不会因认证失败或请求异常导致 MDC 泄漏），以便系统长期稳定运行
37. 作为 SRE 工程师，我希望日志中的客户端 IP 在反向代理场景下仍能获取真实 IP（通过 X-Forwarded-For），以便准确识别请求来源

## Implementation Decisions

### 认证模块

| 决策项 | 方案 |
|--------|------|
| Token 策略 | **单 Token 模式**，无 Refresh Token |
| Token 传输 | **HttpOnly Cookie** 传输，`SameSite=Strict`，前端 JS 无法读取，防御 XSS 和 CSRF |
| Token 时效 | 2 小时 |
| 密码加密 | bcrypt |
| 登录限流 | 同一 IP 5 分钟内最多尝试 5 次 |
| 账户锁定 | 连续 5 次登录失败后，账户锁定 15 分钟 |
| Token 吊销 | 登出时将 Token 加入服务端黑名单（内存缓存），剩余有效期作废 |
| 密码强度 | 至少 8 位，必须包含字母 + 数字 |

**Cookie 设置细节：**
```
Set-Cookie: zhenduanqi_token=xxxx; HttpOnly; SameSite=Strict; Path=/api; Max-Age=7200
```
- 前端 Axios 无需手动处理 Token 携带和续期
- 登出时清除 Cookie 并将当前 Token 加入服务端黑名单

### 权限模块 (RBAC)

采用**拦截器粗粒度 + 注解细粒度**双层校验：

**拦截器层（粗粒度）：**
- `POST /api/auth/login` → 公开，不校验
- `/api/**` → 必须已认证（存在有效的 JWT Token）
- 拦截器负责：Token 解析、校验、设置 `SecurityContext`

**注解层（细粒度）：**
```java
@RequireRole(ADMIN)                    // 仅管理员
@RequireRole({OPERATOR, ADMIN})        // 操作员和管理员
// 不加注解 → 任何已认证用户可访问
```

**角色定义：**
| 角色 | 编码 | 权限范围 |
|------|------|----------|
| 管理员 | ADMIN | 全部功能：服务器 CRUD、用户管理、角色分配、审计日志查看、场景管理、命令守卫配置、执行命令、会话管理 |
| 操作员 | OPERATOR | 执行诊断命令、使用场景模板、查看服务器列表和状态、查看自己的执行历史 |
| 只读用户 | READONLY | 查看服务器列表和状态、查看执行结果，不可执行命令或修改任何配置 |

**前端权限控制：**
- 路由守卫：未登录（无 Cookie）重定向到登录页
- 按钮级别：`v-if="role === 'ADMIN'"` 控制服务器管理、用户管理、场景编辑等按钮可见性
- 前端通过 `GET /api/auth/me` 获取当前用户信息和角色

### 审计日志模块

**采集方式：** AOP 切面 + `@AuditLog` 自定义注解，同步写入 `sys_audit_log` 表

```java
@AuditLog(action = "EXECUTE_COMMAND")
@PostMapping("/execute")
public ResponseEntity<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
    // ...
}
```

**记录内容（高可审计要求）：**
| 字段 | 内容 | 脱敏规则 |
|------|------|----------|
| 操作人 | 登录用户名 | 不脱敏 |
| 操作时间 | 请求时间 | 不脱敏 |
| 来源 IP | 请求 IP | 不脱敏 |
| 操作类型 | LOGIN / EXECUTE_COMMAND / MANAGE_SERVER / MANAGE_USER / MANAGE_SCENE / MANAGE_SESSION / etc. | 不脱敏 |
| 目标 | 服务器 ID、用户 ID 等操作对象标识 | 不脱敏 |
| 指令内容 | Arthas 命令**原文完整记录** | 不脱敏（诊断命令非敏感信息） |
| 请求参数 | 请求体的完整 JSON（不含已脱敏字段） | Token、password 字段自动替换为 `******` |
| 执行结果 | SUCCESS / FAILED | 不脱敏（根据 ExecuteResponse.state 判断：succeeded → SUCCESS，其余 → FAILED） |
| 结果详情 | 执行结果文本**完整记录**（ExecuteResponse 各字段序列化值） | 不脱敏 |
| 耗时 | 请求处理耗时（ms） | 不脱敏 |

**数据表：**
```sql
CREATE TABLE sys_audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)   NOT NULL,
    user_ip     VARCHAR(50),
    action      VARCHAR(100)  NOT NULL,
    target      VARCHAR(200),
    command     TEXT,                      -- Arthas 命令原文
    params      TEXT,                      -- 请求参数 JSON（已脱敏）
    result      VARCHAR(20)   NOT NULL,    -- SUCCESS / FAILED / BLOCKED
    result_detail TEXT,                    -- 执行结果文本
    duration_ms BIGINT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**前端审计日志页面：**
- 分页表格，按时间倒序
- 筛选：操作人、操作类型、目标服务器、时间范围
- 行详情展开：完整查看 instruction 指令、params、result_detail
- 仅 ADMIN 可访问

### 应用运行时日志策略

**与审计日志的边界：**
- **审计日志**（sys_audit_log 表）：记录用户的业务操作，面向合规和追溯，仅 ADMIN 可查询
- **运行时日志**（logback 文件/控制台）：记录系统运行状态，面向 SRE 排障，包含异常堆栈、性能指标、内部状态
- 两者互补，不重叠：审计日志记录"谁做了什么"，运行时日志记录"系统发生了什么"

**日志框架：** 采用 Spring Boot 默认的 Logback（已通过 spring-boot-starter-web 引入 classpath），不引入额外日志框架依赖

**Logback 配置（logback-spring.xml）：**

| 配置项 | 方案 |
|--------|------|
| 控制台 Appender | 所有环境启用，ANSI 色彩输出 |
| 滚动文件 Appender | `prod` profile 启用，输出到 `./logs/zhenduanqi.log` |
| 滚动策略 | 按日期 + 大小滚动，单文件最大 50MB，保留 30 天，总大小上限 2GB |
| 日志格式 | `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{requestId},%X{username}] - %msg%n` |
| 敏感字段脱敏 | 自定义 Logback CompositeConverter，password/token/Authorization 字段自动替换为 `******` |
| Profile 差异 | `default`/`dev`：root=INFO，`com.zhenduanqi`=DEBUG；`prod`：root=WARN，`com.zhenduanqi`=INFO，启用文件 Appender |

**MDC 请求链路追踪：**

采用 **Servlet Filter + try/finally** 模式管理 MDC（而非 Interceptor），确保在任何异常场景下都能可靠清理：

- 使用 `jakarta.servlet.Filter` 而非 `HandlerInterceptor`，在 `doFilter` 中用 try/finally 包裹，保证 MDC 100% 清理
- `Filter` 优先于 `DispatcherServlet` 执行，覆盖 Servlet 异常、视图渲染异常等 Interceptor 无法捕获的场景
- `preHandle` 返回 `false` 时 Interceptor 的 `afterCompletion` 不会被调用，而 Filter 的 finally 始终执行

**MDC 注入字段：**
- `requestId`：UUID，每个请求唯一
- `username`：从 JWT 解析的用户名（未认证请求为 `-`）
- `clientIp`：真实客户端 IP，优先从 `X-Forwarded-For` / `X-Real-IP` 头获取，回退到 `request.getRemoteAddr()`

**Filter 实现骨架：**
```java
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    HttpServletRequest req = (HttpServletRequest) request;
    try {
        MDC.put("requestId", UUID.randomUUID().toString());
        MDC.put("username", extractUsername(req));   // 从 JWT 或 "-"
        MDC.put("clientIp", extractClientIp(req));    // X-Forwarded-For 优先
        chain.doFilter(request, response);
    } finally {
        MDC.clear();                                  // 100% 清理，无泄漏
    }
}
```

**`AuthInterceptor` 职责调整：** 仅负责 JWT 校验和 `request.setAttribute("username")`，不再管理 MDC。

**各组件日志埋点规范：**

| 组件 | 级别 | 日志内容 | 状态 |
|------|------|----------|------|
| AuthService | INFO | 登录成功、登录失败（含原因：密码错误/账户锁定/IP限流）、登出 | ✅ 已实现 |
| AuthService | WARN | 账户锁定触发 | ✅ 已实现 |
| AuthInterceptor | DEBUG | 认证通过 | ✅ 已实现 |
| AuthInterceptor | WARN | Token 缺失/无效/已吊销 | ✅ 已实现 |
| RoleAspect | WARN | 权限不足（含用户角色和所需角色） | ✅ 已实现 |
| AuditLogAspect | DEBUG | 审计日志写入成功 | ✅ 已实现 |
| AuditLogAspect | ERROR | 审计日志写入失败 | ✅ 已实现 |
| CommandGuardService | INFO | 规则加载/重载（含规则数量） | ✅ 已实现 |
| CommandGuardService | WARN | 命令被拦截（含命令原文和匹配规则） | ✅ 已实现 |
| ArthasExecuteService | INFO | 命令执行开始（含 serverId、命令摘要） | ✅ 已实现 |
| ArthasExecuteService | INFO | 命令执行完成（含结果状态、耗时） | ✅ 已实现 |
| ArthasHttpClient | INFO | Arthas API 请求（含服务器名、命令） | ✅ 已实现 |
| ArthasHttpClient | WARN | 连接检测失败 | ✅ 已实现 |
| ArthasSessionService | INFO | 会话创建/关闭/超时清理 | TODO 待P2实现 |
| ArthasSessionService | WARN | 孤儿会话检测与清理 | TODO 待P2实现 |
| JwtUtil | WARN | Token 校验异常（不输出 Token 原文） | ✅ 已实现 |
| ArthasServerService | INFO | 服务器 CRUD 操作 | ✅ 已实现 |
| UserService | INFO | 用户 CRUD 操作、密码重置 | ✅ 已实现 |

**日志级别规范：**

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 影响核心功能的异常，需要立即关注 | 审计日志写入失败、Arthas API 请求异常 |
| WARN | 需要关注但不影响核心流程的事件 | 命令被拦截、认证失败、权限不足、连接检测失败 |
| INFO | 关键业务流程节点 | 登录成功/失败、命令执行、CRUD 操作、规则重载 |
| DEBUG | 调试信息，生产环境通常关闭 | 认证通过详情、Arthas 原始响应、审计日志写入成功 |

**application.yml 日志配置（开发环境默认值）：**
```yaml
logging:
  level:
    root: INFO
    com.zhenduanqi: DEBUG
  file:
    name: ./logs/zhenduanqi.log
```

注：logback-spring.xml 中的 Profile 配置优先级高于 application.yml，yml 中的配置作为开发环境的默认值。

### 高危命令拦截器 (CommandGuard)

**匹配策略：** 正则表达式匹配 + 黑白名单双列表（白名单优先级高于黑名单）

```
匹配流程：
  1. 提取 Arthas 命令原文
  2. 遍历白名单规则列表 → 任一正则匹配 → 放行（跳过黑名单校验）
  3. 遍历黑名单规则列表 → 任一正则匹配 → 拦截，返回 BLOCKED + 拦截原因
  4. 均未命中 → 放行
```

**系统命令豁免：** 后端自动执行的 `reset`、`version` 等系统命令跳过 CommandGuard 检查，不受用户命令规则限制。

**默认黑名单规则（正则）：**
```
^ognl\b          -- OGNL 表达式可执行任意代码，极高风险
^mc\b            -- 内存编译器，可编译恶意类
^redefine\b      -- 热替换类字节码，可能导致不可预期行为
^retransform\b   -- 类似 redefine，热替换字节码
^heapdump\b      -- dump 堆可能产生大文件，影响磁盘和性能
```

**默认白名单规则：** 空（需要管理员手动添加）

**数据表：**
```sql
CREATE TABLE command_guard_rule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_type   VARCHAR(20) NOT NULL,     -- BLACKLIST / WHITELIST
    pattern     VARCHAR(500) NOT NULL,    -- 正则表达式
    description VARCHAR(200),
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**API：**
| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| GET | `/api/command-guard/rules` | 规则列表 | ADMIN |
| POST | `/api/command-guard/rules` | 新增规则 | ADMIN |
| PUT | `/api/command-guard/rules/{id}` | 更新规则 | ADMIN |
| DELETE | `/api/command-guard/rules/{id}` | 删除规则 | ADMIN |

**与审计日志联动：** 被拦截的命令同样记录审计日志，`result=BLOCKED`，在 `result_detail` 中记录拦截原因。

### Arthas HTTP Client 改造

**改造目标：** 将手动字符串解析替换为 Jackson 正确解析 Arthas HTTP API 返回的结构化 JSON。

**改造要点：**
1. 使用 Jackson ObjectMapper 解析 Arthas 响应 JSON，提取 `body.results` 数组
2. 每个 result 保留完整结构化数据（含 `type` 字段），透传给前端
3. 后端不做结果过滤，前端根据 `type` 选择渲染方式
4. 同时支持 `exec` 同步模式和 `async_exec` + `pull_results` + `interrupt_job` 异步模式
5. 系统命令（`reset`、`version`）跳过 CommandGuard 检查

**Arthas 响应数据模型：**
```java
public class ArthasApiResponse {
    private String state;              // SUCCEEDED / FAILED / SCHEDULED
    private String sessionId;
    private String consumerId;
    private List<ArthasResult> results; // 结构化结果列表
    private String rawResponse;         // 原始 JSON（调试用）
}

public class ArthasResult {
    private String type;               // thread / memory / watch / trace / status / enhancer / ...
    private Map<String, Object> data;  // 完整 result 数据，按 type 不同结构不同
}
```

### 场景模板引擎 (Scene)

**场景分类体系：** 按问题类型分类，同时支持业务场景检索

| 分类编码 | 分类名称 | 说明 |
|----------|----------|------|
| THREAD | 线程问题 | 死锁、CPU飙高等 |
| MEMORY | 内存问题 | OOM、内存泄漏、GC频繁等 |
| JVM | JVM 基础 | 配置确认、环境信息等 |
| METHOD | 方法调试 | 耗时追踪、调用监控等 |
| CLASSLOADER | 类加载问题 | 类冲突、ClassNotFoundException等 |

**交互模式：智能列表手动执行**

场景页面以列表形式展示所有步骤，用户逐条手动点击执行，每步的结果直接展示在列表行内，不弹窗、不开新标签页，一屏完成所有操作。

**结果渲染策略：** 后端透传 Arthas 结构化 JSON（含 `type` 字段），前端根据 type 选择渲染组件，未适配的 type 降级为 `<pre>` 文本。

第一版支持的渲染器：

| result type | 渲染方式 | 说明 |
|-------------|---------|------|
| `thread` | Element Plus 表格 | 列：线程名、状态、CPU%、Delta/阻塞锁 |
| `memory` | Element Plus 表格 + 进度条 | 列：区域名、已用、总量、使用率进度条 |
| `status` | 状态标签 | 成功=绿色✅，失败=红色❌ + message |
| `enhancer` | 增强结果标签 | 成功=绿色，失败=红色，显示增强类/方法数 |
| 其他所有 | `<pre>` 文本 | 降级展示，底部提示"后续版本将支持结构化展示" |

前端渲染器用策略模式注册，新增 type 只需添加一个渲染组件，无需改动现有代码。

**半自动变量填充：**
- 步骤定义中包含 `extract_rules`（JSON 格式），声明如何从前序步骤结果中提取变量
- 前端使用 JSONPath（jsonpath-plus 库）在完整 Arthas results 数据上执行提取
- 提取的变量预填到后续步骤的命令输入框，用户确认或修改后执行
- 多结果场景（如 `sc -d` 返回多个类）先支持用户手动复制

**extract_rules 格式：**
```json
[
  {
    "variable": "threadId",
    "jsonPath": "$.results[?(@.type=='thread')][0].threadId",
    "description": "从结果中提取第一个线程的ID"
  }
]
```

**步骤引导：** 通过 `expected_hint` 字段提供文案引导，不做条件步骤自动跳转。

**页面布局示意：**
```
场景：线程死锁检测
分类：线程问题 ｜ 业务场景：应用卡死无响应、请求超时
服务器: [下拉选择]                          [开始新诊断]
───────────────────────────────────────────────────────

┌─ 步骤1: 检查死锁线程 ─────────────────────────────┐
│  thread -b                             [▶ 执行]    │
│  说明: 检测JVM中是否存在死锁线程                      │
│  ──────────────────────────────────────             │
│  ✅ 成功 (230ms)                                     │
│  [status 渲染器] 🟢 命令执行成功                       │
│  [thread 渲染器]                                     │
│  ┌──────────────────────────────────────────────┐   │
│  │ 线程名      │ 状态    │ CPU% │ 阻塞锁        │   │
│  │ Thread-1    │ BLOCKED │ 0.0  │ lock-A (持有)  │   │
│  │ Thread-2    │ BLOCKED │ 0.0  │ lock-B (持有)  │   │
│  └──────────────────────────────────────────────┘   │
│  💡 发现死锁！继续步骤2查看CPU热点，或步骤3查看线程栈   │
└──────────────────────────────────────────────────────┘

┌─ 步骤2: TOP 5 CPU 活跃线程 ───────────────────────┐
│  thread -n 5                            [▶ 执行]    │
│  说明: 查看CPU占用最高的5个线程                       │
│  ──────────────────────────────────────             │
│  [待执行]                                           │
│  💡 提示: 记录CPU最高的线程ID，填入步骤3              │
└──────────────────────────────────────────────────────┘

┌─ 步骤3: 查看具体线程栈 ───────────────────────────┐
│  thread {threadId}                      [▶ 执行]    │
│       ↓ 自动预填为: thread 1                         │
│  说明: 查看指定线程的完整堆栈信息                     │
│  ──────────────────────────────────────             │
│  [待执行]                                           │
└──────────────────────────────────────────────────────┘
```

**步骤可编辑：** 管理员可对场景和步骤做完整的 CRUD 操作，便于持续优化诊断知识库。

**数据表：**
```sql
CREATE TABLE diagnose_scene (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    description        TEXT,
    category           VARCHAR(50),               -- THREAD / MEMORY / JVM / METHOD / CLASSLOADER
    business_scenario  VARCHAR(200),              -- 业务场景描述，用于检索
    icon               VARCHAR(50),               -- Element Plus icon name
    sort_order         INT DEFAULT 0,
    enabled            BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scene_step (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    scene_id        BIGINT NOT NULL,
    step_order      INT NOT NULL,
    title           VARCHAR(200),
    description     TEXT,                          -- 步骤说明文案
    command         TEXT NOT NULL,                 -- Arthas 命令模板，支持 {placeholder}
    expected_hint   TEXT,                          -- 期望输出说明 / 下一步引导提示
    continuous      BOOLEAN DEFAULT FALSE,         -- 是否为连续输出命令（watch/trace/monitor等）
    max_exec_time   INT DEFAULT 30000,             -- 命令级超时（ms），对应 L1 安全机制
    extract_rules   TEXT,                          -- JSON 格式的变量提取规则，前端用于半自动填充
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scene_id) REFERENCES diagnose_scene(id)
);
```

**预置场景（8 个）：**

1. **线程死锁检测** (THREAD, 应用卡死无响应、请求超时)
   - 步骤1: `thread -b` → 检查死锁线程 (continuous=false, maxExecTime=10000)
   - 步骤2: `thread -n 5` → TOP 5 CPU活跃线程 (continuous=false, maxExecTime=10000, extract_rules: 提取 threadId)
   - 步骤3: `thread {threadId}` → 查看具体线程栈 (continuous=false, maxExecTime=10000)

2. **CPU 飙高排查** (THREAD, CPU使用率飙升、服务器负载高)
   - 步骤1: `thread -n 5` → 找出CPU最高的线程 (continuous=false, maxExecTime=10000, extract_rules: 提取 threadId)
   - 步骤2: `thread {threadId}` → 查看线程栈 (continuous=false, maxExecTime=10000)
   - 步骤3: `dashboard -n 1` → 查看JVM概览 (continuous=false, maxExecTime=15000)

3. **内存泄漏初步检查** (MEMORY, OOM告警、堆内存持续增长)
   - 步骤1: `memory` → 查看内存区使用情况 (continuous=false, maxExecTime=10000)
   - 步骤2: `heap -h 10` → 查看堆中大对象 (continuous=false, maxExecTime=15000)
   - 步骤3: `sc -d {className}` → 查看类加载信息 (continuous=false, maxExecTime=10000)

4. **GC 概况诊断** (MEMORY, GC频繁、应用卡顿停顿)
   - 步骤1: `memory` → 查看内存区 (continuous=false, maxExecTime=10000)
   - 步骤2: `vmtool --action getInstances --className java.lang.management.MemoryPoolMXBean` → 查看各内存池详情 (continuous=false, maxExecTime=15000)

5. **JVM 基础信息** (JVM, 应用行为异常、配置确认)
   - 步骤1: `dashboard -n 1` → JVM概览 (continuous=false, maxExecTime=15000)
   - 步骤2: `vmoption` → JVM参数 (continuous=false, maxExecTime=10000)
   - 步骤3: `sysenv` → 系统环境变量 (continuous=false, maxExecTime=10000)

6. **方法耗时追踪** (METHOD, 接口响应慢、方法耗时异常)
   - 步骤1: `sc -d {className}` → 确认类已加载 (continuous=false, maxExecTime=10000)
   - 步骤2: `trace {className} {methodName} -n 5` → 追踪方法调用路径和耗时 (continuous=true, maxExecTime=30000)
   - 步骤3: `watch {className} {methodName} '{params, returnObj, throwExp}' -n 5 -x 2` → 观察方法入参和返回值 (continuous=true, maxExecTime=30000)

7. **方法调用监控** (METHOD, 方法调用频次异常、失败率高)
   - 步骤1: `sc -d {className}` → 确认类已加载 (continuous=false, maxExecTime=10000)
   - 步骤2: `monitor {className} {methodName} -n 10` → 监控方法调用统计 (continuous=true, maxExecTime=60000)
   - 步骤3: `stack {className} {methodName} -n 5` → 查看方法调用路径 (continuous=true, maxExecTime=30000)

8. **类冲突排查** (CLASSLOADER, ClassNotFoundException、NoSuchMethodError)
   - 步骤1: `sc -d {className}` → 查看类加载信息 (continuous=false, maxExecTime=10000)
   - 步骤2: `classloader -t` → 查看ClassLoader继承树 (continuous=false, maxExecTime=10000)
   - 步骤3: `jad {className}` → 反编译查看源码 (continuous=false, maxExecTime=15000)

**场景管理 API：**

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| GET | `/api/scenes` | 场景列表（支持 category 筛选） | 已认证 |
| POST | `/api/scenes` | 创建场景 | ADMIN |
| PUT | `/api/scenes/{id}` | 更新场景 | ADMIN |
| DELETE | `/api/scenes/{id}` | 删除场景 | ADMIN |
| GET | `/api/scenes/{id}/steps` | 步骤列表 | 已认证 |
| POST | `/api/scenes/{id}/steps` | 添加步骤 | ADMIN |
| PUT | `/api/scenes/steps/{stepId}` | 更新步骤 | ADMIN |
| DELETE | `/api/scenes/steps/{stepId}` | 删除步骤 | ADMIN |
| PUT | `/api/scenes/{id}/steps/reorder` | 调整步骤顺序 | ADMIN |

### Arthas 会话安全管控

**架构：** 每个步骤执行时创建独立的 Arthas 会话，执行完毕后立即 `reset` + `close_session`，步骤间完全隔离。

**四层安全机制：**

| 层 | 机制 | 说明 |
|----|------|------|
| L1 命令级超时 | 步骤定义的 `max_exec_time` 字段，默认 30s | 到时后自动 `interrupt_job` |
| L2 会话级超时 | 每个 Arthas 会话最大存活时间，默认 10 分钟 | 到时后自动 `close_session` + `reset` |
| L3 增强类自动重置 | 每个步骤执行结束后立即执行 `reset` 命令 | 清除 watch/trace 的字节码增强，安全优先级最高 |
| L4 孤儿会话清理 | 后端定时任务（每 5 分钟）扫描超时会话，主动关闭 | 防止任何异常导致的会话泄漏 |

**会话生命周期：**
```
创建 Arthas 会话 (init_session)
  → 执行命令 (exec / async_exec)
  → [拉取结果 (pull_results)]（异步命令）
  → 中断命令 (interrupt_job)（如需要）
  → 执行 reset（清除字节码增强）
  → 关闭会话 (close_session)
```

**刷新恢复：** 用户刷新页面后，前端查询活跃会话 API，对执行中的会话 `join_session` 重新获取 consumerId，继续轮询结果。步骤执行状态由前端管理，刷新后通过活跃会话信息恢复。

**数据表：**
```sql
CREATE TABLE arthas_session (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id           VARCHAR(50)  NOT NULL,
    arthas_session_id   VARCHAR(100),              -- Arthas 会话ID
    arthas_consumer_id  VARCHAR(100),              -- Arthas 消费者ID
    current_job_id      INT,                       -- 当前运行的 Job ID
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / RESET / CLOSED
    username            VARCHAR(50) NOT NULL,      -- 操作人
    scene_id            BIGINT,                    -- 关联场景
    step_id             BIGINT,                    -- 关联步骤
    command             TEXT,                      -- 执行的命令
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 最后活跃时间，用于超时判断
    closed_at           TIMESTAMP
);
```

**会话管理 API：**

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| POST | `/api/arthas-sessions` | 创建 Arthas 会话并执行命令 | OPERATOR, ADMIN |
| GET | `/api/arthas-sessions?serverId=&sceneId=` | 查询活跃会话（用于刷新恢复和管理） | 已认证 |
| GET | `/api/arthas-sessions/{id}/results` | 拉取增量结果 | OPERATOR, ADMIN |
| POST | `/api/arthas-sessions/{id}/interrupt` | 中断当前命令 | OPERATOR, ADMIN |
| POST | `/api/arthas-sessions/{id}/close` | 关闭会话（reset + close_session） | OPERATOR, ADMIN |

**与自由命令模式的关系：** 自由命令模式（`POST /api/execute`）继续使用一次性 `exec` 同步模式，不创建会话，不受会话管理影响。场景诊断走会话 API，两套模式并存。

### Arthas 执行引擎 (现有模块改造)

**改动点：**
1. ArthasHttpClient 改造：Jackson 解析 Arthas JSON 响应，透传结构化数据
2. 命令发出前经过 `CommandGuard` 拦截校验（系统命令豁免）
3. 执行完成后通过 `@AuditLog` 切面记录审计日志
4. 增加执行超时控制（60s 请求超时，与 Arthas HTTP API 对齐）
5. 支持 `exec` 同步模式（自由命令）和 `async_exec` + `pull_results` + `interrupt_job` 异步模式（场景连续命令）
6. 同时支持场景单步执行（会话 API）和自由命令执行（`POST /api/execute`）

### 服务器管理 (现有模块改造)

**改动点：**
1. 所有 CRUD 操作增加 `@RequireRole(ADMIN)` 权限注解
2. 服务器 Token 使用加密存储（非明文，不在数据库中以明文形式存放 token 列）
3. `ArthasServerDTO.fromEntity()` 继续保持不返回 Token 的策略

### 数据库全量 Schema

```sql
-- 用户表
CREATE TABLE sys_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(200) NOT NULL,          -- bcrypt
    real_name  VARCHAR(100),
    status     VARCHAR(20)  DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED / LOCKED
    fail_count INT          DEFAULT 0,          -- 连续失败次数
    lock_until TIMESTAMP,                       -- 锁定到何时
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(50)  NOT NULL UNIQUE,   -- ADMIN / OPERATOR / READONLY
    role_name   VARCHAR(100) NOT NULL,
    description VARCHAR(500)
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

-- Arthas 服务器表 (已有)
CREATE TABLE arthas_server (
    id          VARCHAR(50)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    host        VARCHAR(100) NOT NULL,
    http_port   INTEGER      NOT NULL DEFAULT 8563,
    token       VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 审计日志表
CREATE TABLE sys_audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)   NOT NULL,
    user_ip       VARCHAR(50),
    action        VARCHAR(100)  NOT NULL,
    target        VARCHAR(200),
    command       TEXT,
    params        TEXT,
    result        VARCHAR(20)   NOT NULL,     -- SUCCESS / FAILED / BLOCKED
    result_detail TEXT,
    duration_ms   BIGINT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 高危命令守卫规则表
CREATE TABLE command_guard_rule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_type   VARCHAR(20) NOT NULL,          -- BLACKLIST / WHITELIST
    pattern     VARCHAR(500) NOT NULL,         -- 正则表达式
    description VARCHAR(200),
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 诊断场景表
CREATE TABLE diagnose_scene (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    description        TEXT,
    category           VARCHAR(50),               -- THREAD / MEMORY / JVM / METHOD / CLASSLOADER
    business_scenario  VARCHAR(200),              -- 业务场景描述，用于检索
    icon               VARCHAR(50),               -- Element Plus icon name
    sort_order         INT DEFAULT 0,
    enabled            BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 场景步骤表
CREATE TABLE scene_step (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    scene_id        BIGINT NOT NULL,
    step_order      INT NOT NULL,
    title           VARCHAR(200),
    description     TEXT,                          -- 步骤说明文案
    command         TEXT NOT NULL,                 -- Arthas 命令模板，支持 {placeholder}
    expected_hint   TEXT,                          -- 期望输出说明 / 下一步引导提示
    continuous      BOOLEAN DEFAULT FALSE,         -- 是否为连续输出命令
    max_exec_time   INT DEFAULT 30000,             -- 命令级超时（ms）
    extract_rules   TEXT,                          -- JSON 格式的变量提取规则
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scene_id) REFERENCES diagnose_scene(id)
);

-- Arthas 会话表
CREATE TABLE arthas_session (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id           VARCHAR(50)  NOT NULL,
    arthas_session_id   VARCHAR(100),
    arthas_consumer_id  VARCHAR(100),
    current_job_id      INT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / RESET / CLOSED
    username            VARCHAR(50) NOT NULL,
    scene_id            BIGINT,
    step_id             BIGINT,
    command             TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at           TIMESTAMP
);
```

### 完整 API 清单

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/logout` | 用户登出 | 已认证 |
| GET | `/api/auth/me` | 获取当前用户信息及角色 | 已认证 |
| GET | `/api/users` | 用户列表 | ADMIN |
| POST | `/api/users` | 创建用户 | ADMIN |
| PUT | `/api/users/{id}` | 更新用户 | ADMIN |
| PUT | `/api/users/{id}/reset-password` | 重置密码 | ADMIN |
| GET | `/api/roles` | 角色列表 | ADMIN |
| GET | `/api/servers` | 服务器列表 | 已认证 |
| GET | `/api/servers/{id}` | 服务器详情 | 已认证 |
| POST | `/api/servers` | 新增服务器 | ADMIN |
| PUT | `/api/servers/{id}` | 更新服务器 | ADMIN |
| DELETE | `/api/servers/{id}` | 删除服务器 | ADMIN |
| GET | `/api/servers/{id}/status` | 服务器连接状态 | 已认证 |
| POST | `/api/execute` | 执行自由命令（一次性exec） | OPERATOR, ADMIN |
| GET | `/api/scenes` | 场景列表（支持category筛选） | 已认证 |
| POST | `/api/scenes` | 创建场景 | ADMIN |
| PUT | `/api/scenes/{id}` | 更新场景 | ADMIN |
| DELETE | `/api/scenes/{id}` | 删除场景 | ADMIN |
| GET | `/api/scenes/{id}/steps` | 场景步骤列表 | 已认证 |
| POST | `/api/scenes/{id}/steps` | 添加步骤 | ADMIN |
| PUT | `/api/scenes/steps/{stepId}` | 更新步骤 | ADMIN |
| DELETE | `/api/scenes/steps/{stepId}` | 删除步骤 | ADMIN |
| PUT | `/api/scenes/{id}/steps/reorder` | 调整步骤顺序 | ADMIN |
| POST | `/api/arthas-sessions` | 创建Arthas会话并执行命令 | OPERATOR, ADMIN |
| GET | `/api/arthas-sessions` | 查询活跃会话 | 已认证 |
| GET | `/api/arthas-sessions/{id}/results` | 拉取增量结果 | OPERATOR, ADMIN |
| POST | `/api/arthas-sessions/{id}/interrupt` | 中断当前命令 | OPERATOR, ADMIN |
| POST | `/api/arthas-sessions/{id}/close` | 关闭会话（reset+close） | OPERATOR, ADMIN |
| GET | `/api/audit-logs` | 审计日志（分页+筛选） | ADMIN |
| GET | `/api/command-guard/rules` | 高危命令规则列表 | ADMIN |
| POST | `/api/command-guard/rules` | 新增规则 | ADMIN |
| PUT | `/api/command-guard/rules/{id}` | 更新规则 | ADMIN |
| DELETE | `/api/command-guard/rules/{id}` | 删除规则 | ADMIN |

### 技术选型

- **后端**：Java 17 + Spring Boot 3.2.5 + Spring Data JPA + H2 Database + java.net.http.HttpClient + jjwt (JWT 库) + Jackson (JSON 解析)
- **前端**：Vue 3 + Vite 5 + Element Plus 2.7 + Pinia + Vue Router 4 + Axios + jsonpath-plus (变量提取)
- **认证**：JWT (jjwt) + bcrypt (Spring Security Crypto) + HttpOnly Cookie
- **构建**：Maven + frontend-maven-plugin（Vue 产物自动合并到 Spring Boot jar）
- **部署**：单 JAR 独立部署

### 前端页面结构

```
/                          → 重定向到 /scenes
/login                     → 登录页面
/scenes                    → 场景列表页（按分类展示所有场景卡片）
/scenes/:id/diagnose       → 场景诊断页（步骤列表 + 执行）
/scenes/manage             → 场景管理页（ADMIN，CRUD场景和步骤）
/servers                   → 服务器管理页
/diagnose                  → 自由命令诊断页
/audit-logs                → 审计日志页（ADMIN）
/users                     → 用户管理页（ADMIN）
/command-guard             → 高危命令规则页（ADMIN）
/sessions                  → 活跃会话管理页（ADMIN，查看和清理Arthas会话）
```

### 项目结构（最终态）

```
zhenduanqi/
├── pom.xml
├── PRD.md
├── frontend/                        # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── api/index.js             # Axios 请求封装
│       ├── router/index.js          # 路由 + 导航守卫
│       ├── components/
│       │   └── result-renderers/    # 结果渲染器组件
│       │       ├── index.js         # 渲染器注册（策略模式）
│       │       ├── ThreadRenderer.vue
│       │       ├── MemoryRenderer.vue
│       │       ├── StatusRenderer.vue
│       │       ├── EnhancerRenderer.vue
│       │       └── FallbackRenderer.vue  # <pre> 降级渲染
│       ├── stores/
│       │   ├── servers.js           # 服务器 Pinia store
│       │   ├── user.js              # 当前用户 store (登录/角色/权限)
│       │   ├── scene.js             # 场景 store
│       │   └── diagnose.js          # 诊断会话 store（步骤状态、变量缓存）
│       └── views/
│           ├── Login.vue            # 登录页面
│           ├── SceneList.vue        # 场景列表页（按分类卡片展示）
│           ├── SceneDiagnose.vue    # 场景诊断页（步骤列表 + 执行 + 结果渲染）
│           ├── SceneManage.vue      # 场景管理页 (ADMIN)
│           ├── ServerList.vue       # 服务器管理页 (ADMIN)
│           ├── Diagnose.vue         # 自由命令诊断页
│           ├── SessionManage.vue    # 活跃会话管理页 (ADMIN)
│           ├── AuditLog.vue         # 审计日志页 (ADMIN)
│           ├── UserManage.vue       # 用户管理页 (ADMIN)
│           └── CommandGuard.vue     # 高危命令规则配置页 (ADMIN)
└── src/main/
    ├── java/com/zhenduanqi/
    │   ├── Application.java
    │   ├── config/
    │   │   ├── MdcFilter.java                # MDC 请求链路追踪 Filter
    │   │   ├── AuthInterceptor.java         # JWT 拦截器
    │   │   ├── WebMvcConfig.java            # 注册拦截器
    │   │   └── SecurityConfig.java          # 密码编码器 Bean
    │   ├── annotation/
    │   │   ├── RequireRole.java             # 权限注解
    │   │   └── AuditLog.java                # 审计日志注解
    │   ├── aspect/
    │   │   ├── RoleAspect.java              # 权限校验切面
    │   │   └── AuditLogAspect.java          # 审计日志切面
    │   ├── controller/
    │   │   ├── AuthController.java          # 登录/登出/当前用户
    │   │   ├── UserController.java          # 用户管理 CRUD
    │   │   ├── ArthasServerController.java  # 服务器管理 CRUD
    │   │   ├── ArthasExecuteController.java # 自由命令执行
    │   │   ├── SceneController.java         # 场景 CRUD
    │   │   ├── ArthasSessionController.java # Arthas 会话管理
    │   │   ├── AuditLogController.java      # 审计日志查询
    │   │   └── CommandGuardController.java  # 高危命令规则管理
    │   ├── entity/
    │   │   ├── ArthasServerEntity.java
    │   │   ├── SysUser.java
    │   │   ├── SysRole.java
    │   │   ├── SysAuditLog.java
    │   │   ├── CommandGuardRule.java
    │   │   ├── DiagnoseScene.java
    │   │   ├── SceneStep.java
    │   │   └── ArthasSession.java
    │   ├── repository/
    │   │   ├── ArthasServerRepository.java
    │   │   ├── ArthasSessionRepository.java
    │   │   └── ...
    │   ├── service/
    │   │   ├── AuthService.java
    │   │   ├── UserService.java
    │   │   ├── ArthasServerService.java
    │   │   ├── ArthasExecuteService.java
    │   │   ├── ArthasSessionService.java     # Arthas 会话管理 + L1-L4 安全机制
    │   │   ├── SceneService.java
    │   │   ├── AuditLogService.java
    │   │   └── CommandGuardService.java
    │   ├── client/
    │   │   └── ArthasHttpClient.java         # Jackson 解析 + exec/async_exec/pull_results/interrupt
    │   ├── dto/
    │   │   ├── ArthasServerDTO.java
    │   │   ├── ExecuteRequest.java
    │   │   ├── ExecuteResponse.java
    │   │   ├── LoginRequest.java
    │   │   ├── LoginResponse.java
    │   │   └── ...
    │   └── model/
    │       ├── ServerInfo.java
    │       ├── ArthasApiResponse.java         # Arthas 结构化响应模型
    │       └── ArthasResult.java              # 单个 result 模型（type + data）
    └── resources/
        ├── application.yml
        ├── logback-spring.xml
        └── import.sql                         # 初始化数据（admin + 角色 + 默认规则 + 8个预置场景）
```

## Implementation Phasing

### 第一阶段（P0）：安全基础设施 ✅ 已完成
目标：具备完整的认证、权限、审计安全能力
- ✅ 认证模块：登录/登出/JWT + HttpOnly Cookie
- ✅ RBAC 权限：拦截器 + @RequireRole 注解 + 用户/角色管理
- ✅ 审计日志：AOP @AuditLog 注解 + 审计日志查询页面
- ✅ 高危命令拦截：修订后的黑名单（ognl/mc/redefine/retransform/heapdump）+ 系统命令豁免
- ✅ 应用运行时日志：logback-spring.xml 配置 + MDC 请求链路追踪 + 敏感字段脱敏 + 各组件日志埋点
- ✅ ArthasHttpClient 改造：Jackson 解析 Arthas JSON 响应，透传结构化数据
- ✅ 现有服务器管理 + 命令执行接口增加权限校验
- ✅ 前端登录页 + 路由守卫 + 角色按钮控制

### 第二阶段（P1）：场景模板引擎 — 基础版
目标：实现一次性命令场景的诊断向导
- 场景/步骤 CRUD 管理（含新增字段：category、business_scenario、continuous、maxExecTime、extract_rules）
- 8 个预置场景（场景1-5 为一次性命令，场景6-8 为连续命令暂用 exec + -n 限制）
- 场景列表页（按分类卡片展示 + 业务场景检索）
- 场景诊断页（步骤列表 + 手动执行 + 结果渲染）
- 前端渲染器：thread、memory、status、enhancer + 降级文本
- 半自动变量填充（extract_rules + jsonpath-plus）
- 前端 diagnose store（步骤状态管理、变量缓存）

### 第三阶段（P2）：场景模板引擎 — 异步版
目标：实现连续输出命令场景的诊断 + 会话安全管控
- ArthasHttpClient 支持 async_exec + pull_results + interrupt_job
- Arthas 会话管理（创建/轮询/中断/关闭）
- arthas_session 表 + 持久化
- L1-L4 安全机制（命令超时、会话超时、自动 reset、孤儿清理定时任务）
- 活跃会话管理页（ADMIN）
- 刷新恢复（重连会话 + 恢复步骤状态）
- 场景6-8 完整异步模式支持

## Testing Decisions

### 测试原则
- 只测试外部行为，不测试实现细节
- 后端优先测试 Service 层和 Client 层
- 前端的 API 层和 Store 层做单元测试

### 测试范围

| 模块 | 测试策略 | 阶段 |
|------|----------|------|
| **AuthService** | JWT 签发/校验、密码哈希认证、登录限流、账户锁定逻辑 | P0 |
| **CommandGuardService** | 正则黑白名单匹配逻辑、优先级校验、系统命令豁免 | P0 |
| **ArthasHttpClient** | Mock HTTP 服务，验证 Jackson 解析、请求构造、响应解析、超时处理 | P0 |
| **ArthasExecuteService** | Mock Repository + Client + Guard，验证执行链路 | P0 |
| **AuditLogAspect** | AOP 切面日志记录正确性 | P0 |
| **RoleAspect** | @RequireRole 注解拦截逻辑 | P0 |
| **AuthInterceptor** | JWT 拦截器、白名单路径跳过 | P0 |
| **SensitiveDataConverter** | Logback 脱敏 Converter，验证 password/token/Authorization 字段替换正确性 | P0 |
| **MdcFilter** | MDC 注入和清理、认证失败场景、登录请求 | P0 |
| **AuthService 日志** | Logback ListAppender 捕获日志，验证级别和内容 | P0 | ✅ 已实现 |
| **各组件日志埋点测试** | AuthInterceptor/RoleAspect/CommandGuardService/JwtUtil/ArthasServerService/UserService 日志验证 | P0 | ✅ 已实现 |
| **SceneService** | 场景步骤编排、命令模板占位符处理 | P1 |
| **前端渲染器** | 各 type 渲染组件的输入输出 | P1 |
| **前端 extract_rules** | JSONPath 变量提取逻辑 | P1 |
| **ArthasSessionService** | 会话创建/关闭、L1-L4 安全机制、孤儿清理 | P2 |
| **ArthasHttpClient 异步** | async_exec/pull_results/interrupt_job | P2 |
| **前端 API 层** | Axios 拦截器、401 重定向 | P0 |

### 测试技术
- JUnit 5 + Mockito + AssertJ
- `@WebMvcTest` 测试 Controller 层
- JMockit 或 MockServer 模拟 Arthas HTTP API
- 前端使用 Vitest

## Out of Scope

- **WebSocket/SSE 实时推送**：第一版采用同步请求-响应 + 轮询模式，后续可改为 SSE 或 WebSocket
- **Dashboard 可视化图表**：Arthas dashboard 命令结果的图表展示，后续迭代
- **更多 result type 渲染器**：sc、watch、trace 等类型的结构化渲染，后续迭代
- **场景版本管理**：场景修改保留历史版本、回滚，后续迭代
- **场景导入导出**：JSON 格式导入导出场景，后续迭代
- **诊断报告导出**：HTML/Markdown 格式导出诊断报告，后续迭代
- **条件步骤自动跳转**：根据前序步骤结果自动跳过步骤，后续迭代
- **步骤结果自动解析抽取实体**：当前使用 extract_rules 半自动填充，后续可做更智能的自动解析
- **OpenAPI/Swagger**：接口文档，后续按需添加
- **LDAP/OAuth 集成**：企业级 SSO 集成，后续迭代
- **多语言（i18n）**：当前仅中文，暂无国际化计划
- **Docker/K8s 部署**：当前仅提供 jar 独立部署，后续可按需提供
- **诊断历史个人查询**：当前仅 ADMIN 可查全部审计日志，后续可增加个人查询入口

## Further Notes

- **安全第一**：Token 在 HttpOnly Cookie 中传输，前端无法读取；高危命令拦截是必选功能；Arthas 会话四层安全机制防止字节码增强泄漏
- **分阶段交付**：P0 安全基础设施 → P1 场景基础版（一次性命令）→ P2 场景异步版（连续命令 + 会话管控），三阶段递进
- **向后兼容**：现有 `/api/servers` 和 `/api/execute` 接口路径不变，仅增加认证校验
- **两套执行模式并存**：自由命令用一次性 exec，场景诊断用会话 API，互不影响
- **初始管理员账号**：`import.sql` 中创建默认管理员 `admin / Abcd1234`（系统导入后应提示修改密码）
- **渲染器可扩展**：前端渲染器用策略模式注册，新增 type 只需添加一个渲染组件
- **Arthas 会话持久化**：会话信息存数据库，诊断器重启后可扫描并清理孤儿会话
