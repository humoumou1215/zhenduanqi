# PRD: Arthas 远程诊断工具 (zhenduanqi)

## Problem Statement

SRE / 运维团队在日常 Java 应用排障过程中，需要登录到目标服务器手动启动 Arthas 并执行诊断命令。这一流程存在以下痛点：

- **操作门槛高**：每次都需要 SSH 登录到服务器，找到 Java 进程，启动 Arthas，操作链路长
- **知识分散**：常用的诊断命令（死锁检查、内存分析、GC 诊断等）缺乏沉淀和模板化，新人学习成本高
- **安全风险**：Arthas 功能强大，高危命令（如 `reset`、`ognl` 执行等）缺乏管控，误操作可能导致线上事故
- **审计缺失**：谁在什么时候对哪台服务器执行了什么诊断命令，没有任何记录
- **权限混乱**：任何人只要有服务器访问权限就可以执行任意 Arthas 命令，缺乏角色分级

## Solution

构建一个集中式的 Arthas 远程诊断 Web 平台，提供以下核心能力：

1. **服务器集中管理**：在平台中统一配置目标服务器及 Arthas HTTP API 连接信息（含 Token 管理）
2. **场景化诊断向导**：预置常见问题的诊断场景模板，引导用户按步骤完成问题排查，降低使用门槛
3. **全命令模式**：支持用户自由输入任何 Arthas 命令，同时提供高危命令黑白名单拦截
4. **完整权限管理**：RBAC 角色权限模型，区分管理员/普通操作员/只读用户
5. **操作审计日志**：完整记录每一次诊断操作，提供查询和导出

## User Stories

1. 作为 SRE 工程师，我希望能在一个 Web 页面上看到所有配置了 Arthas 的服务器列表，以便快速选择要诊断的目标
2. 作为 SRE 工程师，我希望能选择服务器后输入任意 Arthas 命令并查看执行结果，以便灵活排查问题
3. 作为 SRE 工程师，我想要一个场景化的诊断向导（如"线程死锁检测"），引导我完成逐步操作，以便降低工具使用门槛
4. 作为 SRE 工程师，我希望诊断命令的执行结果能以清晰的格式展示（支持文本和未来图表），以便快速定位问题
5. 作为 SRE 工程师，我希望系统能拦截高危 Arthas 命令（如 ognl 表达式执行），以避免误操作导致线上故障
6. 作为 SRE 工程师，我希望能查看自己执行过的诊断历史，以便回顾和分享排查思路
7. 作为系统管理员，我希望能添加/编辑/删除 Arthas 目标服务器，以便管理诊断覆盖范围
8. 作为系统管理员，我希望能创建和管理用户账号，并分配不同的角色权限
9. 作为系统管理员，我希望能查看所有用户的操作审计日志，以便满足合规和追溯需求
10. 作为系统管理员，我希望能通过控制台直接检查目标服务器的 Arthas 连接状态，以便快速诊断连通性问题
11. 作为只读用户，我只能查看服务器的基本信息和执行结果，不能执行新命令或修改配置
12. 作为普通操作员，我可以执行诊断命令并使用场景模板，但不能管理服务器配置或其他用户
13. 作为所有用户，我希望能使用用户名密码登录系统，通过 JWT Token 维持会话
14. 作为所有用户，我希望能看到命令执行的历史记录和结果，以便复盘
15. 作为系统管理员，我希望能配置高危命令黑白名单，并根据需要启用或禁用

## Implementation Decisions

### 模块划分

#### 1. 认证模块 (Auth)
- 用户注册/登录/登出
- JWT Token 签发与校验（access token + refresh token）
- 密码使用 bcrypt 加密存储
- 拦截器校验所有 `/api/*` 请求（白名单除外如 `/api/auth/login`）

#### 2. 权限模块 (RBAC)
- 数据表：`sys_user`、`sys_role`、`sys_user_role`
- 角色：ADMIN（管理员）、OPERATOR（操作员）、READONLY（只读用户）
- 权限注解 `@RequireRole` + Spring AOP 拦截器
- 前端路由守卫 + 按钮级别权限控制

#### 3. 审计日志模块 (Audit)
- AOP 切面拦截所有 Controller 请求
- 记录：操作人、时间、IP、请求路径、参数（脱敏）、结果状态、耗时
- 数据表：`sys_audit_log`
- 提供分页查询和按条件筛选 API

#### 4. 场景模板引擎 (Scene)
- 数据表：`diagnose_scene`（场景定义）、`scene_step`（步骤定义）
- 每个场景包含多个步骤步骤，每个步骤包含：命令模板、说明文本、预期输出说明
- 支持按顺序执行场景步骤，或在某个步骤中选择分支
- 预置场景（第一版）：
  - **线程死锁检测**：`thread -b` → 分析死锁线程栈
  - **CPU 热点分析**：`thread -n 5` → `thread <id>` 查看栈
  - **内存泄漏初步检查**：`memory` → `heap -h 10` 查看大对象 → `sc -d <class>` 查加载源
  - **GC 概况诊断**：`memory` → `vmtool --action getInstances --className java.lang.management.MemoryPoolMXBean`
  - **JVM 基础信息**：`dashboard -n 1` → `vmoption` / `sysenv`

#### 5. 高危命令拦截器 (CommandGuard)
- 可配置的黑名单列表（支持通配符匹配）
- 执行前校验，命中黑名单直接拒绝
- 默认黑名单：`ognl`、`reset`、`mc`、`redefine`、`classloader --*` 等
- 可配置白名单覆盖（针对特定用户或场景）

#### 6. Arthas 执行引擎 (已实现，需改造)
- 现有 `ArthasHttpClient` 保持不变
- 增加：高危命令拦截逻辑
- 增加：审计日志记录
- 增加：执行超时控制

#### 7. 服务器管理 (已实现，需改造)
- 现有 `ArthasServerController` + `ArthasServerService` 保持不变
- 增加：权限校验（仅 ADMIN 可 CRUD）
- 增加：Token 加密存储

### 数据库 Schema 变更

```sql
-- 用户表
CREATE TABLE sys_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(200) NOT NULL,  -- bcrypt
    real_name  VARCHAR(100),
    status     VARCHAR(20)  DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(50)  NOT NULL UNIQUE,  -- ADMIN / OPERATOR / READONLY
    role_name   VARCHAR(100) NOT NULL,
    description VARCHAR(500)
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 审计日志表
CREATE TABLE sys_audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)   NOT NULL,
    user_ip     VARCHAR(50),
    action      VARCHAR(100)  NOT NULL,  -- EXECUTE_COMMAND / MANAGE_SERVER / LOGIN / etc.
    target      VARCHAR(200),            -- 目标服务器id
    detail      TEXT,                     -- 操作详情（命令参数，脱敏）
    result      VARCHAR(20),             -- SUCCESS / FAILED / BLOCKED
    duration_ms BIGINT,                  -- 耗时
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 诊断场景表
CREATE TABLE diagnose_scene (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    icon        VARCHAR(50),
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 场景步骤表
CREATE TABLE scene_step (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    scene_id      BIGINT NOT NULL,
    step_order    INT NOT NULL,
    title         VARCHAR(200),
    description   TEXT,               -- 步骤说明文案
    command       TEXT NOT NULL,       -- Arthas 命令模板
    expected_hint TEXT,               -- 期望输出的说明
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API 新增接口

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录，返回 JWT | 公开 |
| POST | `/api/auth/logout` | 登出 | 已认证 |
| GET  | `/api/auth/me` | 获取当前用户信息 | 已认证 |
| GET  | `/api/users` | 用户列表 | ADMIN |
| POST | `/api/users` | 创建用户 | ADMIN |
| PUT  | `/api/users/{id}` | 更新用户 | ADMIN |
| GET  | `/api/roles` | 角色列表 | ADMIN |
| GET  | `/api/audit-logs` | 审计日志（分页+筛选） | ADMIN |
| GET  | `/api/scenes` | 场景列表 | 已认证 |
| POST | `/api/scenes` | 创建场景 | ADMIN |
| GET  | `/api/scenes/{id}/steps` | 场景步骤列表 | 已认证 |
| POST | `/api/scene-execute` | 按场景逐步执行 | 已认证 |
| GET  | `/api/command-guard` | 高危命令规则列表 | ADMIN |
| PUT  | `/api/command-guard` | 更新高危命令规则 | ADMIN |

### API 修改

| 变更 | 说明 |
|------|------|
| `/api/servers` CRUD | 增加 ADMIN 角色校验 |
| `/api/execute` | 增加高危命令拦截 + 审计日志记录 |
| `/api/servers/{id}/status` | 增加已认证用户权限 |

### 技术选型

- **后端**：Java 17 + Spring Boot 3.2.5 + Spring Data JPA + H2 Database + java.net.http.HttpClient
- **前端**：Vue 3 + Vite 5 + Element Plus 2.7 + Pinia + Vue Router 4 + Axios
- **认证**：JWT (jjwt 库) + bcrypt (Spring Security Crypto)
- **构建**：Maven + frontend-maven-plugin 合并前端产物
- **部署**：单 JAR 独立部署

## Testing Decisions

### 测试原则
- 只测试外部行为，不测试实现细节
- 后端优先测试 Service 层和 Client 层
- 前端的 API 层和 Store 层做集成测试

### 测试范围

| 模块 | 测试策略 | 优先级 |
|------|----------|--------|
| **ArthasHttpClient** | Mock HTTP 服务，验证请求构造、响应解析、超时处理 | P0 |
| **ArthasExecuteService** | Mock Repository + Client，验证命令执行链路 | P0 |
| **AuthService** | JWT 签发/校验、密码哈希认证 | P0 |
| **CommandGuard** | 黑白名单匹配逻辑、拦截逻辑 | P0 |
| **AuditAspect** | AOP 切面日志记录正确性 | P1 |
| **场景引擎** | 场景步骤编排、命令组装 | P1 |
| **前端 API 层** | 请求/响应拦截器、错误处理 | P1 |

### 测试技术
- JUnit 5 + Mockito + AssertJ
- 使用 `@WebMvcTest` 测试 Controller 层
- 前端使用 Vitest

## Out of Scope

- **实时 WebSocket 推送**：第一版采用同步请求-响应模式，后续可改为 SSE 或 WebSocket 推送实时诊断结果
- **仪表盘/Dashboard**：Arthas 自身 dashboard 命令的实时图表展示，放入后续迭代
- **OpenAPI/Swagger**：接口文档，未来按需添加
- **LDAP/OAuth 集成**：企业级 SSO 集成，放入后续迭代
- **多语言（i18n）**：当前仅中文，暂无国际化计划
- **Docker/K8s 部署**：当前仅提供 jar 独立部署，后续可按需提供容器化方案

## Further Notes

- **安全第一**：Token 在数据库中加密存储，前端不暴露；高危命令拦截是必选功能，非可选
- **渐进式增强**：第一阶段优先完成认证 + 权限 + 审计 + 高危拦截核心安全能力，场景模板和可视化诊断放入第二阶段
- **向后兼容**：现有 `/api/servers` 和 `/api/execute` 接口路径不变，仅增加认证校验，前端代码大部分可复用
