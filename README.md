# 企业广告管理系统 V3.0

> 基于 Spring Boot + Vue3 + MySQL 的前后端分离广告公司业务管理平台

---

## 🏗️ 项目结构

```
enterprise-ad-system/
├── frontend/                 # 前端 Vue3 + Vite + TypeScript
│   ├── src/
│   │   ├── api/              # Axios 请求层（按模块拆分）
│   │   │   ├── modules/      # auth / order / customer / member / factory / finance
│   │   │   └── request.ts    # 拦截器、token注入、错误处理
│   │   ├── components/
│   │   ├── layouts/
│   │   │   └── MainLayout.vue
│   │   ├── router/           # Vue Router + 权限守卫 + NProgress
│   │   ├── stores/           # Pinia（auth / order / ...）
│   │   ├── styles/           # SCSS变量 / 全局样式
│   │   ├── types/            # TypeScript 类型定义
│   │   └── views/            # 业务页面（37个Vue页面）
│   └── vite.config.ts        # Vite 配置（代理 /api → 后端8080）
│
├── backend/                  # 后端 Spring Boot 3.2.5 + Java 17
│   ├── src/main/java/com/enterprise/ad/
│   │   ├── config/           # SecurityConfig / CorsConfig / MybatisPlusConfig
│   │   ├── security/         # JWT过滤器 / JWT工具 / UserDetailsService
│   │   ├── common/           # Result<T> / PageResult<T> / 异常处理
│   │   └── module/
│   │       ├── auth/         # 认证模块（含Service层）
│   │       ├── order/        # 订单管理
│   │       ├── customer/     # 客户管理
│   │       ├── member/       # 会员管理（充值/消费）
│   │       ├── factory/      # 工厂管理 + 账单
│   │       ├── finance/      # 财务管理（流水/概览/快速记账）
│   │       ├── material/     # 物料管理（分类/库存/出入库/预警）
│   │       ├── design/       # 设计文件（上传/版本管理）
│   │       ├── square/       # 设计广场（需求/接单/收入）
│   │       ├── notice/       # 消息通知
│   │       ├── dashboard/    # 仪表盘
│   │       ├── statistics/   # 统计分析
│   │       └── system/       # 系统管理（用户/角色/字典/日志）
│   ├── src/main/resources/
│   │   └── application.yml   # Spring Boot 配置
│   └── database/
│       └── init.sql          # 完整建表SQL + 初始数据
│
└── README.md
```

---

## 🚀 本地开发启动

### 前置要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis（可选，用于Token黑名单）
- Node.js >= 18

### 1. 数据库初始化

```bash
# 使用 MySQL 客户端执行 SQL
mysql -u root -p < backend/database/init.sql
```

### 2. 启动后端

```bash
cd backend
# 修改 src/main/resources/application.yml 中的数据库/Redis配置
mvn spring-boot:run
# 服务运行在 http://localhost:8080
# API文档 http://localhost:8080/doc.html
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
# 前端请求 /api/* 会自动代理到后端 8080（已配置 rewrite 去掉 /api 前缀）
```

**默认账号：admin / 123456**

---

## 📦 技术栈

| 层级 | 技术 | 说明 |
|---|---|---|
| 前端框架 | Vue 3 + TypeScript | Composition API |
| 构建工具 | Vite 5 | 极速HMR、按需打包 |
| UI组件库 | Element Plus | 自动按需引入 |
| 状态管理 | Pinia | 持久化插件 |
| 路由 | Vue Router 4 | History模式 + 权限守卫 |
| 网络请求 | Axios | 拦截器、统一错误处理 |
| CSS | SCSS + CSS变量 | 主题化支持 |
| 后端框架 | Spring Boot 3.2.5 | Java 17 |
| ORM | MyBatis-Plus 3.5.6 | 自动分页、逻辑删除 |
| 安全 | Spring Security + JWT | 无状态认证、权限控制 |
| 缓存 | Redis | Token黑名单 |
| 数据库 | MySQL 8.0 | 连接池 |
| API文档 | Knife4j | 在线调试 |

---

## 🔑 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 超级管理员 |
| designer1 | 123456 | 设计师 |
| finance1 | 123456 | 财务 |

---

## 📋 核心API路由

| 模块 | 路径前缀 | 说明 |
|---|---|---|
| 认证 | `/auth` | 登录/登出/当前用户 |
| 订单 | `/orders` | 订单CRUD + 物料明细 |
| 客户 | `/customers` | 客户CRUD |
| 会员 | `/members` | 会员CRUD + 充值/消费/流水 |
| 工厂 | `/factory` | 工厂CRUD + 账单管理 |
| 财务 | `/finance` | 流水/概览/快速记账 |
| 物料 | `/material` | 分类/库存/出入库/预警 |
| 设计文件 | `/design/file` | 文件上传/版本管理 |
| 广场 | `/square` | 需求/接单/收入 |
| 通知 | `/notice` | 通知列表/设置 |
| 仪表盘 | `/dashboard` | 核心指标/趋势图 |
| 统计 | `/statistics` | 订单/营收/客户/物料统计 |
| 系统 | `/system` | 用户/角色/字典/日志 |
