# 开发规范

## Git 工作流

### 分支策略

| 分支 | 用途 | 命名格式 |
|------|------|----------|
| `main` | 主分支，始终保持可部署状态 | — |
| `feature/*` | 功能开发分支 | `feature/issue-{编号}-{简短描述}` |
| `bugfix/*` | 缺陷修复分支 | `bugfix/issue-{编号}-{简短描述}` |

**示例**：
- `feature/issue-24-jackson-refactor`
- `feature/issue-8-scene-crud`
- `bugfix/issue-11-command-guard-integration`

### 提交规范

```
<type>(<scope>): <subject> (#<issue-number>)
```

**type 类型**：
| type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `refactor` | 重构（不改变外部行为） |
| `docs` | 文档更新 |
| `test` | 测试相关 |
| `chore` | 构建/工具/配置相关 |

**scope 可选值**：
| scope | 说明 |
|-------|------|
| `auth` | 认证模块 |
| `scene` | 场景模板引擎 |
| `session` | Arthas 会话管理 |
| `guard` | 高危命令拦截 |
| `audit` | 审计日志 |
| `log` | 运行时日志 |
| `server` | 服务器管理 |
| `ui` | 前端页面 |
| `api` | API 层 |

**示例**：
```
feat(scene): add category and business_scenario fields (#8)
fix(guard): add system command exemption for reset (#25)
refactor(api): replace manual JSON parsing with Jackson (#24)
```

### Pull Request 规范

- **标题格式**：`[P{阶段}] 功能描述 (#issue-number)`
  - 示例：`[P0] ArthasHttpClient Jackson 改造 (#24)`
- **描述必须关联 issue**：使用 `Closes #xx` 关键词，合并后自动关闭 issue
- **每个 PR 对应一个 issue**：保持原子性，便于 review 和回滚
- **合并前必须通过**：
  - 后端：`mvn compile` 无编译错误
  - 前端：`npm run format:check` 格式检查通过
- **合并后删除分支**

### 工作流程

```
1. 认领 issue
2. git checkout main && git pull origin main
3. git checkout -b feature/issue-{编号}-{描述}
4. 开发 → 频繁提交（每个有意义的改动一次提交）
5. git push origin feature/issue-{编号}-{描述}
6. 创建 Pull Request（标题+描述关联 issue）
7. PR 审查通过后合并 → 自动关闭 issue → 删除分支
8. 更新 PRD.md
```

### 禁止事项

- ❌ 禁止直接向 `main` 推送代码
- ❌ 禁止 force push（`git push --force`）
- ❌ 禁止提交敏感信息（密码、Token、密钥）
- ❌ 禁止在未关联 issue 的情况下创建 PR

## 开发流程

### TDD 开发（推荐）

对于核心功能，建议使用 TDD（测试驱动开发）：

1. **RED**：先写失败的测试
2. **GREEN**：写最少的代码让测试通过
3. **REFACTOR**：重构代码，保持测试通过

### 测试要求

- ✅ 核心业务逻辑必须有单元测试
- ✅ 测试覆盖关键场景和边界条件
- ✅ 使用 Mockito mock 外部依赖
- ✅ 测试名称清晰描述测试场景
- ✅ 后端测试：JUnit 5 + Mockito + AssertJ
- ✅ 前端测试：Vitest

### 代码质量

- ✅ 遵循 Java 编码规范
- ✅ 使用有意义的变量和方法命名
- ✅ 保持方法简洁（单一职责）
- ✅ 不添加不必要的注释，代码应自解释

## Pre-commit Hooks

项目已配置 Git pre-commit hooks，在每次提交时自动执行代码检查。

### 检查内容

**Java 后端**：
- ✅ Maven 编译检查
- ✅ 确保代码无编译错误

**Vue 前端**：
- ✅ Prettier 格式化检查（如果已安装）
- ⚠️ 格式不符会警告但不阻止提交

### 手动格式化

**前端代码格式化**：
```bash
cd frontend
npm run format        # 格式化所有代码
npm run format:check  # 检查格式
```

**后端代码格式化**：
- 使用 IDE 的代码格式化功能
- 遵循 Java 编码规范

### 跳过 Pre-commit Hooks

如果需要临时跳过检查（不推荐）：
```bash
git commit --no-verify -m "your message"
```

### 安装前端依赖

首次克隆项目后，需要安装前端依赖：
```bash
cd frontend
npm install
```

## 环境要求

- **Java**: 17
- **Maven**: 3.9+
- **Spring Boot**: 3.2.5
- **Node.js**: 18+

### 便捷脚本

使用 `mvn-java17.sh` 确保 Java 17 环境：
```bash
./mvn-java17.sh test      # 运行测试
./mvn-java17.sh compile   # 编译项目
./mvn-java17.sh package   # 打包
```

## PRD 同步

PRD.md 是项目唯一的功能真相来源（Single Source of Truth），开发完成后必须同步更新：

- 每个 PR 合并后，检查 PRD.md 对应章节是否需要更新
- 更新范围：User Stories、Implementation Decisions、API 清单、Schema、项目结构、阶段调整
- 如果 PRD.md 与代码实现不一致，以代码实现为准，并更新 PRD.md

## 技能协作流程

推荐的需求到开发流程：

```
grill-me（需求探讨）→ to-prd（输出 PRD）→ to-issues（拆分 issue）→ 开发实现
```

- **grill-me**：新功能规划、技术选型、架构设计时使用
- **to-prd**：需求讨论完成，需要固化 PRD 时使用
- **to-issues**：PRD 完成，需要拆分为可执行 issue 时使用
