# 全项目代码审查报告（已修复）

> 扫描时间: 2026-05-03 15:25 | **修复时间: 2026-05-03 15:29**
> 前端文件: 61个vue + 21个ts/js | 后端文件: 150个java

---

## ✅ 已修复清单

### 🔴 P0-1 ✅ `generateOrderNo()` 重复 → **已提取到共享工具类**

| 操作 | 详情 |
|------|------|
| 新建 | `common/util/OrderNoGenerator.java` — Spring Component，统一使用Redis INCR生成+DB降级 |
| 删除 | `OrderService.java` + `OrderServiceImpl.java` — 无模块引用，完全重复 |
| 更新 | `OrderController.generateOrderNo()` 改为委托 `orderNoGenerator.generate()` |

> 以创建订单为主，Controller 的 `create()` 是订单创建唯一入口 ✅

### 🔴 P0-2 ✅ 异常静默吞掉 → **已加日志**

| 位置 | 修改 |
|------|------|
| `AuthServiceImpl.java:150` | `catch (Exception ignored) {}` → `catch (Exception e) { log.warn(...) }` + 添加 `@Slf4j` |
| `OrderController.java:669` | 该方法已随重复代码一起删除（已提取到 OrderNoGenerator，那里已有 log.warn） |

### 🟡 P1-3 ✅ MetaObjectHandler **已创建**

新建 `config/AutoMetaObjectHandler.java` — 自动填充 `createTime` / `updateTime`。  
（需要逐步给实体类加 `@TableField(fill = ...)` 注解才能全面生效，基础设施已就绪）

### 🟡 P1-5 ✅ OrderController 与 OrderServiceImpl 职责重叠 → **已删除Service**

`OrderService` + `OrderServiceImpl` 完全无模块引用，且与 Controller 业务重复，直接删除。

### 🟢 P2-6 ✅ HelloWorld.vue → **已删除**

Vite 默认模板死代码。

---

## ⏳ 未修复（不紧急，建议后续迭代）

| 问题 | 原因 |
|------|------|
| P1-4 Controller直接注Mapper | 涉及全部26个Controller，改造成本大，现有运行正常 |
| P2-7 部分方法过长 | 纯代码风格问题，不影响功能 |
| P2-8 两套统计逻辑 | DashboardController已有SQL聚合优化 `/dashboard/board` |
| P1-3 实体类 @TableField 注解 | 需逐个实体加注，现有手动 setCreateTime 不影响运行 |

---

## 总结

| 项目 | 变化 |
|------|------|
| 修复问题 | **5/8** 项已修复（P0全修🟢、P1 2/3、P2 1/3） |
| 删除文件 | `OrderService.java`、`OrderServiceImpl.java`、`HelloWorld.vue` |
| 新建文件 | `OrderNoGenerator.java`、`AutoMetaObjectHandler.java` |
| 后端编译 | ✅ 通过 |
| 前端编译 | ✅ 通过 |
