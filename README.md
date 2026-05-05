# 诊断器 (Zhenduanqi)

## 项目简介
Arthas 远程诊断工具 - 支持场景化诊断向导、RBAC 权限管理、操作审计的 Web 管理平台

## 功能特性
- 场景化诊断向导
- RBAC 权限管理
- 操作审计日志
- Arthas 命令执行
- 诊断结果可视化
- 服务器连接管理
- 会话管理

## 技术栈
- 后端: Spring Boot 3.2
- 前端: Vue 3 + Element Plus
- 数据库: H2/MySQL
- 认证: JWT

## 项目结构
```
zhenduanqi/
├── src/                    # 后端源代码
│   ├── main/java/         # Java 主代码
│   ├── main/resources/    # 配置文件
│   └── test/java/         # 测试代码
├── frontend/              # 前端源代码
│   ├── src/               # Vue 源代码
│   └── package.json       # 前端依赖配置
├── pom.xml                # Maven 配置
└── README.md              # 项目文档
```

## 快速开始
### 环境要求
- Java 17+
- Node.js 20+
- Maven 3.8+

### 启动项目
1. 后端启动
```bash
./mvn-java17.sh spring-boot:run
```

2. 前端启动
```bash
cd frontend
npm install
npm run dev
```

### 默认账号
- 用户名: admin
- 密码: admin123

## 贡献指南
我们欢迎任何形式的贡献！

### 开发流程
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/issue-{编号}-{描述}`)
3. 提交更改 (`git commit -m 'feat(scope): description (#issue-number)'`)
4. 推送到分支 (`git push origin feature/issue-{编号}-{描述}`)
5. 创建 Pull Request

### 代码规范
- 后端遵循 Java 代码规范
- 前端使用 Prettier 格式化代码
- 每个提交对应一个 Issue
- PR 需要通过 CI 检查

### 提交类型
- `feat`: 新功能
- `fix`: Bug 修复
- `refactor`: 重构
- `docs`: 文档更新
- `test`: 测试相关
- `chore`: 构建/工具相关

