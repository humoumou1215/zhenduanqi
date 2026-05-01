# 开发规范

## Git 工作流

### 当前阶段：模型开发阶段

**简化工作流**：
- ✅ 直接在 `main` 分支上迭代开发
- ❌ 暂不使用 feature 分支和 Pull Request 流程
- ✅ 每个功能开发完成后直接提交到 `main` 分支

**提交规范**：
```
<type>(<scope>): <subject>

<body>

关闭 #<issue-number>
```

**type 类型**：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**：
```
feat(command-guard): 集成CommandGuard到ArthasExecuteService

- 添加CommandGuardService依赖注入
- 在execute方法中添加命令安全检查逻辑
- 创建测试验证集成正确性

关闭 #6
```

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

### 代码质量

- ✅ 遵循 Java 编码规范
- ✅ 使用有意义的变量和方法命名
- ✅ 添加必要的注释（复杂逻辑）
- ✅ 保持方法简洁（单一职责）

## 环境要求

- **Java**: 17
- **Maven**: 3.9+
- **Spring Boot**: 3.2.5

### 便捷脚本

使用 `mvn-java17.sh` 确保 Java 17 环境：
```bash
./mvn-java17.sh test      # 运行测试
./mvn-java17.sh compile   # 编译项目
./mvn-java17.sh package   # 打包
```

## 后续阶段

当项目进入正式开发阶段后，将切换到完整的 Git Flow 工作流：
- 使用 feature 分支开发新功能
- 通过 Pull Request 进行代码审查
- 使用 develop 和 release 分支管理版本
