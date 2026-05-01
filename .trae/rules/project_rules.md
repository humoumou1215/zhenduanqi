# 项目规则

## 0 — 目的

这些规则确保代码可维护性、安全性和开发效率。**MUST** 规则由 CI 强制执行；**SHOULD** 规则强烈推荐。

---

## 0.5 — 执行前必读（Agent 必须遵循）

**任何开发任务开始前，必须执行以下步骤：**

### 步骤 1：检查当前分支状态

```bash
git branch          # 检查当前分支
git status          # 检查是否有未提交的改动
```

### 步骤 2：处理未提交的改动

**如果当前在 main 分支上：**

| 情况 | 处理方式 |
|------|----------|
| 无未提交改动 | 直接创建分支：`git checkout -b feature/issue-{编号}-{描述}` |
| 有未提交改动 | 先暂存：`git stash`，创建分支后恢复：`git stash pop` |

**如果已在 feature/bugfix 分支上：**
- 继续在当前分支工作

### 步骤 3：确认关联的 issue

- 每个 PR 必须关联一个 issue（GIT-15）
- 分支命名必须包含 issue 编号（GIT-2, GIT-3）

### 步骤 4：选择开发模式

| 开发模式 | 使用场景 | 触发方式 |
|---------|---------|---------|
| **TDD 模式** | 新功能开发、Bug 修复、核心业务逻辑 | 用户明确要求或任务提示词中指定 |
| **普通模式** | 文档更新、配置修改、简单改动 | 不涉及业务逻辑的简单任务 |

**TDD 模式执行流程：**
```
1. RED：先写失败的测试
2. GREEN：写最少的代码让测试通过
3. REFACTOR：重构代码，保持测试通过
```

---

## 1 — PRD 同步规则

### 核心原则

PRD.md 是代码的一部分，不是代码的附属文档。

### 规则

| 编号 | 级别 | 规则 |
|------|------|------|
| PRD-1 | MUST | PRD 更新必须与相关代码改动在**同一个 commit** 中提交 |
| PRD-2 | MUST | 每个 commit 只更新与该 commit 相关的 PRD 片段，采用增量更新策略 |
| PRD-3 | MUST | 以下场景必须同步更新 PRD：新功能、行为变更、API 变更、Schema 变更、安全机制变更、删除功能 |
| PRD-4 | MUST | QA 发现问题后，先通过 to-issues 流程创建 issue，修复代码时同步更新 PRD |
| PRD-5 | SHOULD NOT | 纯代码重构（行为不变）、性能优化（接口不变）、测试代码变更不需要更新 PRD |

### 触发场景速查表

| 场景 | 更新范围 | 提交时机 |
|------|----------|----------|
| 新功能开发 | User Stories, API 清单, Schema | 随代码 commit |
| Bug 修复（行为变更） | API 清单, 行为描述 | 随代码 commit |
| QA 发现问题 | 相关章节 | 先创建 issue，修复时随代码 commit |
| 重构（API/Schema 变更） | API 清单, Schema | 随代码 commit |
| 安全机制变更 | Implementation Decisions | 随代码 commit |
| 删除功能 | 移除相关章节 | 随代码 commit |
| to-prd 流程 | 全部 PRD | 单独提交（PRD 是产出物） |
| 纯代码重构 | 无需更新 | - |

### PRD 更新章节检查清单

更新 PRD.md 时检查以下章节：

- [ ] **User Stories**：新增或变更的用户故事
- [ ] **Implementation Decisions**：新增模块、技术方案变更、Schema 变更
- [ ] **完整 API 清单**：新增或变更的 API 端点
- [ ] **数据库全量 Schema**：新增或变更的表结构
- [ ] **项目结构**：新增或变更的文件
- [ ] **Implementation Phasing**：阶段调整
- [ ] **Testing Decisions**：新增测试策略
- [ ] **Out of Scope**：明确排除的范围变更

---

## 2 — Git 工作流规则

### 分支策略

| 编号 | 级别 | 规则 |
|------|------|------|
| GIT-1 | MUST | main 分支始终保持可部署状态 |
| GIT-2 | MUST | 功能分支从 main 检出，命名格式 `feature/issue-{编号}-{简短描述}` |
| GIT-3 | MUST | 修复分支从 main 检出，命名格式 `bugfix/issue-{编号}-{简短描述}` |

### 提交规范

| 编号 | 级别 | 规则 |
|------|------|------|
| GIT-4 | MUST | 提交信息格式：`type(scope): description (#issue-number)` |
| GIT-5 | MUST | type 可选值：feat / fix / refactor / docs / test / chore |
| GIT-6 | SHOULD | scope 可选值：auth / scene / session / guard / audit / log / server / ui / api |

**提交信息示例**：
```
feat(auth): add Basic Auth support for Arthas (#50)
fix(audit): resolve null username in login audit log (#57)
```

### 合并规则

| 编号 | 级别 | 规则 |
|------|------|------|
| GIT-7 | MUST | 功能分支通过 Pull Request 合并到 main |
| GIT-8 | MUST | PR 标题格式：`[P{阶段}] 功能描述 (#issue-number)` |
| GIT-9 | MUST | PR 描述必须关联 issue 编号，使用 `Closes #xx` 自动关闭 |
| GIT-10 | MUST | 每个 PR 对应一个 issue，保持原子性 |
| GIT-11 | MUST | 合并前必须通过 lint 和 typecheck |

### 工作流程

```
1. 认领 issue → 从 main 检出 feature/bugfix 分支
2. 开发 → 频繁提交（每个有意义的改动一次提交，PRD 随代码同步更新）
3. 完成 → 推送分支 → 创建 PR
4. PR 合并 → 自动关闭 issue → 删除分支
```

### 禁止事项

| 编号 | 级别 | 规则 |
|------|------|------|
| GIT-12 | MUST NOT | 禁止直接向 main 推送代码 |
| GIT-13 | MUST NOT | 禁止 force push |
| GIT-14 | MUST NOT | 禁止提交敏感信息（密码、Token、密钥） |
| GIT-15 | MUST NOT | 禁止在未关联 issue 的情况下创建 PR |

---

## 3 — 技能使用规则

### grill-me：需求探讨与方案决策

**触发场景**：
- 新功能规划：用户提出新功能想法但方案未明确
- 技术方案选型：存在多种实现方案需要决策
- 架构设计讨论：涉及模块划分、接口设计、数据模型等架构决策
- PRD 细化：PRD 描述不够具体，需要逐项确认实现细节

**提醒话术**：`这个决策涉及多个分支，建议使用 grill-me 技能逐项确认。`

### to-prd：PRD 编写与发布

**触发场景**：
- 需求讨论完成：通过 grill-me 或其他方式完成了需求讨论，需要将结论固化为 PRD
- 功能范围变更：现有功能需要重大变更，PRD 需要修订
- 新模块规划：需要新增一个完整的功能模块

**提醒话术**：`需求讨论已充分，建议使用 to-prd 技能将结论输出为 PRD。`

### to-issues：任务拆分与发布

**触发场景**：
- PRD 完成后：PRD 编写完成，需要拆分为可执行的 issue
- 大任务拆分：用户提出复杂任务，需要分解为多个独立 issue
- 阶段规划：需要将一个阶段的工作拆分为具体的开发任务

**提醒话术**：`PRD 已完成，建议使用 to-issues 技能拆分为可执行的 issue。`

### 技能协作流程

```
grill-me（需求探讨）→ to-prd（输出 PRD）→ to-issues（拆分 issue）→ 开发实现
```

- grill-me 的结论是 to-prd 的输入
- to-prd 的输出是 to-issues 的输入
- to-issues 的输出是开发实现的任务清单
- 每个环节完成后提醒用户进入下一环节

---

## 4 — 快捷命令

### QPRD

当用户输入 `qprd` 时：

```
检查当前代码改动涉及的 PRD 更新：
1. 识别改动类型（新功能/Bug修复/重构/删除）
2. 确定需要更新的 PRD 章节
3. 在同一个 commit 中提交 PRD 更新
```

### QCHECK

当用户输入 `qcheck` 时：

```
对每个主要代码变更执行检查：
1. PRD 同步规则检查清单
2. Git 工作流规则检查
3. 确认 PRD 与代码实现一致
```

### QISSUE

当用户输入 `qissue` 时：

```
创建规范的 issue：
1. 标题清晰描述问题
2. 包含 What happened / What I expected / Steps to reproduce
3. 添加优先级标签（P0/P1/P2）
4. 关联相关 issue（如有阻塞关系）
```
