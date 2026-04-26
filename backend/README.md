# 企业广告管理系统 - 后端

## 📁 项目结构

```
backend/
├── src/main/java/com/enterprise/ad/
│   ├── AdSystemApplication.java          ← 启动类
│   ├── config/                           ← 配置层
│   │   ├── SecurityConfig.java           ← Spring Security + JWT
│   │   ├── CorsConfig.java               ← 跨域配置
│   │   ├── RedisConfig.java              ← Redis序列化
│   │   └── MybatisPlusConfig.java       ← 分页插件
│   ├── common/                           ← 通用类
│   │   ├── Result.java                   ← 统一响应体
│   │   ├── PageResult.java               ← 分页响应
│   │   └── exception/                    ← 全局异常处理
│   ├── security/                         ← 安全认证
│   │   ├── JwtUtil.java                  ← JWT工具（签发/解析/黑名单）
│   │   ├── JwtAuthenticationFilter.java  ← 请求过滤器
│   │   └── UserDetailsServiceImpl.java   ← 用户认证服务
│   └── module/                           ← 业务模块
│       ├── auth/                         ← 认证（登录/登出/当前用户）
│       ├── system/user/                  ← 系统管理（用户/角色/菜单）
│       ├── order/                        ← 订单管理
│       ├── customer/                     ← 客户管理
│       ├── member/                       ← 会员管理（预存金额/充值）
│       ├── factory/                      ← 工厂账单
│       ├── finance/                      ← 财务管理
│       └── dashboard/                    ← 仪表盘统计
├── database/
│   └── init.sql                          ← 数据库初始化脚本
└── pom.xml                              ← Maven依赖
```

---

## 🚀 快速启动

### 1. 环境要求

| 软件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | 已安装于 `C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot` |
| Maven | 3.9+ | 已安装于 `D:\dev\apache-maven-3.9.6` |
| MySQL | 8.0+ | 需自行安装（见下方"安装 MySQL"） |
| Redis | 7.x | 需自行安装（见下方"安装 Redis"） |

**环境变量**（用户级，已配置）：
- `JAVA_HOME` = `C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot`
- `MAVEN_HOME` = `D:\dev\apache-maven-3.9.6`
- `Path` 已追加 Maven 和 JDK bin 目录

---

### 2. 安装 MySQL（Windows）

**方式一：下载安装包（推荐）**
1. 访问 https://dev.mysql.com/downloads/mysql/ 下载 MySQL 8.0 Windows 版本
2. 选择 `Windows (x86, 64-bit), ZIP Archive` 下载
3. 解压到 `D:\dev\mysql-8.0\`（目录名不含中文和空格）
4. 在 `D:\dev\mysql-8.0\` 创建 `my.ini` 配置文件
5. 初始化并启动服务

**方式二：使用 phpStudy（最简单）**
1. 下载 phpStudy：https://www.xp.cn/download.html
2. 安装后启动，切换到 MySQL 8.x
3. 右键 → 管理数据库，即可使用

**方式三：使用宝塔面板（适合长期使用）**
1. 下载宝塔：https://www.bt.cn/new/download.html
2. 安装后一键部署 MySQL

**创建数据库：**
```sql
CREATE DATABASE enterprise_ad DEFAULT CHARSET utf8mb4;
```

---

### 3. 安装 Redis（Windows）

**下载：**
```bash
# 从 GitHub 下载 Windows 版本
https://github.com/tporadowski/redis/releases
```
选择 `Redis-x64-5.0.14.1.msi` 或更新版本下载安装。

**或使用 Memurai（Windows 原生 Redis 替代）：**
```
https://www.memurai.com/get
```

---

### 4. 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source D:/Users/Mr.wu/Desktop/enterprise-ad-system/backend/database/init.sql

# 或直接在 MySQL 客户端中复制粘贴 init.sql 内容
```

---

### 5. 修改数据库密码

编辑 `src/main/resources/application.yml`，修改密码：

```yaml
spring:
  datasource:
    password: 你的MySQL密码
  data:
    redis:
      password: 你的Redis密码（如果没有则留空）
```

---

### 6. 启动后端

```bash
# 设置环境变量（当前会话）
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
$env:MAVEN_HOME = "D:\dev\apache-maven-3.9.6"
$env:Path = "D:\dev\apache-maven-3.9.6\bin;C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot\bin;$env:Path"

# 进入后端目录
cd "C:\Users\Mr.wu\Desktop\enterprise-ad-system\backend"

# 启动（开发模式）
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/ad-system-1.0.0.jar
```

启动成功后会看到：
```
Started AdSystemApplication in X.XXX seconds
```

后端运行在：**http://localhost:8080/api**

---

### 7. API 文档

启动后在浏览器打开：
- **Knife4j 文档**（推荐）：http://localhost:8080/api/doc.html
- **Swagger UI**：http://localhost:8080/api/swagger-ui/index.html

---

## 🔐 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 超级管理员 |
| finance | 123456 | 财务 |
| operator | 123456 | 操作员 |

> ⚠️ 首次登录后请立即修改密码！

---

## 📡 API 列表

### 认证
```
POST /api/auth/login        登录
POST /api/auth/logout       登出
GET  /api/auth/me           当前用户信息
```

### 订单
```
GET  /api/orders            分页列表
POST /api/orders            新建订单
GET  /api/orders/{id}       订单详情
PUT  /api/orders/{id}       更新订单
DELETE /api/orders/{id}     删除订单
GET  /api/orders/{id}/materials   物料明细
```

### 客户
```
GET  /api/customers         分页列表
POST /api/customers         新建客户
GET  /api/customers/{id}    客户详情
PUT  /api/customers/{id}   更新客户
```

### 会员
```
GET  /api/members           分页列表
POST /api/members           新建会员
POST /api/members/{id}/recharge   会员充值（预存金额）
GET  /api/members/{id}/transactions   流水记录
```

### 工厂账单
```
GET  /api/factory/list      工厂列表
GET  /api/factory/bills    账单列表（支持工厂/月份/状态筛选）
POST /api/factory/bills     新建账单
PUT  /api/factory/bills/{id}/status   更新账单状态
```

### 财务
```
GET  /api/finance/records   流水列表
POST /api/finance/records   快速记账
GET  /api/finance/summary  收支统计
```

### 仪表盘
```
GET  /api/dashboard/stats          核心指标
GET  /api/dashboard/charts/orderTrend   订单趋势
```

---

## 🔧 10000 用户并发保障

| 机制 | 实现 |
|---|---|
| 连接池 | HikariCP 最大50连接 |
| JWT 无状态 | Token 不存服务器，Redis 存黑名单 |
| Redis 缓存 | 热点数据缓存，加速查询 |
| 接口限流 | 可在 SecurityConfig 中加 `RateLimiterFilter` |
| 数据库索引 | 订单/客户/会员表关键字段建联合索引 |
| 横向扩展 | 无状态服务，可多实例 + Nginx 负载均衡 |

---

## 📦 前端对接

前端运行在 `http://localhost:5173`，已配置 Vite 代理：
- `/api/*` → `http://localhost:8080/api`

前端项目：`桌面/enterprise-ad-system/frontend/`

---

## 🐛 常见问题

**Q: Maven 编译报错 "找不到符号"**
A: 确保 JDK 17 已正确安装，并设置了 `JAVA_HOME` 环境变量

**Q: 启动报错 "连接数据库失败"**
A: 检查 MySQL 是否启动，密码是否正确，`application.yml` 中的数据库名 `enterprise_ad` 是否已创建

**Q: Redis 连接失败**
A: Redis 默认端口 6379，如未设置密码则留空。确认 Redis 服务已启动

**Q: 登录返回 401**
A: 检查密码是否正确。初始密码为 `123456`（BCrypt加密）
