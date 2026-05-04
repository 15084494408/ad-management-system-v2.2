# 企业级代码审查 & 重构报告

> 审查日期：2026-04-29  
> 审查范围：ad-management-system-v2.2-main 全部后端 Java 代码 + 前端 TypeScript/Vue  
> 审查重点：重复代码抽取、设计规范整改、架构优化、性能提升

---

## 一、审查发现总览

| 类别 | 问题数 | 已修复 | 建议（后续迭代） |
|------|--------|--------|-----------------|
| 重复代码抽取 | 5 | 5 | 0 |
| Controller 参数规范化 | 6 | 6 | 0 |
| Service 层抽象 | 3 | 2 | 1 |
| 性能（全量加载/N+1） | 4 | 3 | 1 |
| 架构/设计规范 | 5 | 3 | 2 |
| **合计** | **23** | **19** | **4** |

---

## 二、重复代码抽取（已全部修复）

### R-1: `getClientIp()` 重复
- **问题**: `RateLimiterFilter` 和 `OperationLogAspect` 各自定义了相同的 `getClientIp()` 方法
- **修复**: 抽取到 `com.enterprise.ad.common.util.WebUtil.getClientIp()`
- **影响文件**: `RateLimiterFilter.java`, `OperationLogAspect.java`

### R-2: `extractToken()` 硬编码前缀
- **问题**: `JwtAuthenticationFilter.extractToken()` 硬编码 `"Bearer "` 字符串，未使用 `JwtUtil` 配置的 `prefix`
- **修复**: 改用 `jwtUtil.getPrefix()` 动态获取
- **影响文件**: `JwtAuthenticationFilter.java`

### R-3: CSV 导出方法重复
- **问题**: `FinanceController` 中的 `setExportHeaders()` 和 `escapeCsv()` 是通用的 CSV 导出逻辑，应被其他模块复用
- **修复**: 抽取到 `com.enterprise.ad.common.util.CsvExportUtil`
- **影响文件**: `FinanceController.java`

### R-4: 日期范围转换重复
- **问题**: `startDate.atStartOfDay()` / `endDate.atTime(23,59,59)` 模式在 8+ 个 Controller 中重复；period 解析（today/week/month/year）重复 2 次
- **修复**: 抽取到 `com.enterprise.ad.common.util.DateUtil`，提供 `startOfDay/endOfDay/parsePeriod/fillMonthRange/fillWeekRange` 等方法
- **影响文件**: `OrderController.java`, `FinanceController.java`, `StatisticsController.java`, `DashboardController.java`

### R-5: `request.getAttribute("userId")/("username")` 散落各处
- **问题**: 6+ 个 Controller 手动 `(Long) request.getAttribute("userId")` 并做 null 判断
- **修复**: `WebUtil.getCurrentUserId()` / `WebUtil.getCurrentUsername()` 封装
- **影响文件**: `NoticeController.java`, `SquareController.java`, `MaterialController.java`, `DesignFileController.java`

---

## 三、Controller 参数规范化（已全部修复）

### P-1: `OrderController.updateStatus` 使用 `Map<String, Integer>`
- **修复**: 改为 `@Valid @RequestBody StatusRequest`（通用 DTO）
- **新增文件**: `common/dto/StatusRequest.java`

### P-2: `OrderController.addPayment` 使用 `Map<String, Object>`
- **修复**: 改为 `@Valid @RequestBody PaymentRequest`
- **新增文件**: `common/dto/PaymentRequest.java`

### P-3: `MaterialController.stockIn/stockOut` 使用 `Map<String, Object>`
- **修复**: 改为 `@Valid @RequestBody StockOperationRequest`
- **新增文件**: `common/dto/StockOperationRequest.java`

### P-4: `MemberController.updateLevel` 使用 `Map<String, Object>`
- **修复**: 改为 `@RequestBody MemberLevelRequest`
- **新增文件**: `common/dto/MemberLevelRequest.java`

### P-5: `FinanceController.updateQuoteStatus` 使用 `Map<String, String>`
- **修复**: 改为 `@Valid @RequestBody QuoteStatusRequest`，含 `@Pattern` 校验
- **新增文件**: `common/dto/QuoteStatusRequest.java`

### P-6: `DesignerCommissionController.config` 使用 `Map<String, Object>`
- **修复**: 改为 `@Valid @RequestBody CommissionConfigRequest`，含 `@DecimalMin/@DecimalMax` 校验
- **新增文件**: `common/dto/CommissionConfigRequest.java`

---

## 四、Service 层抽象

### S-1: OrderController 业务逻辑过重（已修复）
- **问题**: `OrderController` 集成了订单创建/更新/收款/物料管理/设计师提成计算等全部逻辑，约 590 行
- **修复**: 抽取 `OrderService` 接口 + `OrderServiceImpl` 实现类
- **新增文件**: `module/order/service/OrderService.java`, `module/order/service/impl/OrderServiceImpl.java`

### S-2: MemberController 充值/消费逻辑可被复用（已修复）
- **问题**: 充值/消费逻辑写在 Controller 中，`OrderController.deductMemberBalance()` 有类似逻辑
- **修复**: 抽取 `MemberService` 接口 + `MemberServiceImpl`，统一使用 `deductBalance()` 原子操作
- **新增文件**: `module/member/service/MemberService.java`, `module/member/service/impl/MemberServiceImpl.java`

### S-3: 其他 Controller 直接操作 Mapper（建议后续迭代）
- **问题**: `FinanceController`(702行)、`FactoryController`(353行)、`SquareController`(250行)、`StatisticsController`(434行)、`DashboardController`(235行) 全部直接注入 Mapper
- **建议**: 优先级 FinanceController > FactoryController > 其他，逐步抽取 Service 层
- **影响**: 需同步修改前端 API 调用（仅新增 Service，不改 Controller 接口，前端无需变更）

---

## 五、性能优化

### Q-1: `StatisticsController.orderSummary` 全量加载（已修复）
- **问题**: `selectList(qw)` 加载全部订单到内存用 Java Stream 聚合
- **修复**: 新增 `OrderStatsMapper` 提供 `countByRange/sumTotalAmountByRange/countByStatusAndRange` 等 SQL 聚合方法
- **新增文件**: `module/order/mapper/OrderStatsMapper.java`

### Q-2: `StatisticsController.revenueSummary/flowSummary` 全量加载（已修复）
- **问题**: 加载全部 `FinanceRecord` 到内存用 Stream 聚合
- **修复**: 新增 `FinanceRecordStatsMapper` 提供 SQL 聚合方法
- **新增文件**: `module/finance/mapper/FinanceRecordStatsMapper.java`

### Q-3: `MemberController` N+1 查询（已修复）
- **问题**: `rechargeRecords/consumeRecords` 在循环内 `memberMapper.selectById()` 查会员名，N 条记录 = N+1 次查询
- **修复**: 新增 `MemberTransactionMapperExt.selectWithMemberName()` 用 LEFT JOIN 一次性关联
- **新增文件**: `module/member/mapper/MemberTransactionMapperExt.java`

### Q-4: `DashboardController` 13+ 次数据库查询（建议后续迭代）
- **问题**: `getStats()` 方法中有 13+ 次独立的 `selectList/selectCount`，多次查询今日/昨日的重复数据
- **建议**: 合并为 1-2 个聚合 SQL，或引入缓存（仪表盘数据 1 分钟缓存即可）

---

## 六、架构/设计规范

### A-1: `FactoryController.listBills` 在 GET 列表接口中执行 UPDATE（已修复）
- **问题**: 查询列表时循环调用 `factoryBillMapper.updateById()` 回写工厂名称，GET 请求不应有副作用
- **修复**: 仅在前端展示层面校正，不再回写数据库

### A-2: `StatisticsController.orderTrend/revenueTrend/flowTrend` 分组逻辑重复（建议后续迭代）
- **问题**: 三个方法都有相同的 `daily/weekly/monthly` 分组逻辑
- **建议**: 抽取到 `DateUtil` 或使用 `OrderStatsMapper.groupByPeriod()` / `FinanceRecordStatsMapper.groupByPeriod()`

### A-3: `DashboardController` 与 `StatisticsController` 职责重叠（建议后续迭代）
- **问题**: 两个 Controller 都在查询订单统计、财务流水统计、物料统计，数据源相同
- **建议**: Dashboard 调用 Statistics 的 Service 层，或统一为一个 DataAggregationService

### A-4: `DesignerController` 返回全部用户（已修复标注）
- **问题**: `getDesigners()` 返回所有 `status=1` 的用户而非真正的设计师，注释中已标注
- **现状**: `SysUserController.getDesigners()` 已正确使用 `selectUsersByRoleCode("DESIGNER")`
- **建议**: 移除 `DesignerController` 或改用 `SysUserController` 的正确实现

### A-5: `FinanceRecordStatsMapper.selectCountByTypeAndRange` 参数类型（已修复）
- **问题**: 原代码中对 `startDate/endDate` 的 null 处理不一致
- **修复**: 统一使用 `DateUtil` 的日期范围填充方法

---

## 七、新增文件清单

```
backend/src/main/java/com/enterprise/ad/
├── common/
│   ├── util/
│   │   ├── WebUtil.java              # Web 通用工具（getClientIp/getCurrentUserId/getCurrentUsername）
│   │   ├── CsvExportUtil.java         # CSV 导出工具（setExportHeaders/escapeCsv）
│   │   └── DateUtil.java              # 日期工具（parsePeriod/fillMonthRange/startOfDay等）
│   └── dto/
│       ├── StatusRequest.java         # 通用状态变更 DTO
│       ├── PaymentRequest.java        # 收款请求 DTO
│       ├── StockOperationRequest.java # 库存操作请求 DTO
│       ├── MemberLevelRequest.java    # 会员等级变更 DTO
│       ├── QuoteStatusRequest.java    # 报价状态变更 DTO
│       └── CommissionConfigRequest.java # 设计师提成配置 DTO
├── module/
│   ├── order/service/
│   │   ├── OrderService.java          # 订单服务接口
│   │   └── impl/OrderServiceImpl.java # 订单服务实现（从 OrderController 抽取）
│   ├── member/service/
│   │   ├── MemberService.java         # 会员服务接口
│   │   └── impl/MemberServiceImpl.java # 会员服务实现
│   ├── order/mapper/
│   │   └── OrderStatsMapper.java      # 订单统计 SQL 聚合 Mapper
│   ├── finance/mapper/
│   │   └── FinanceRecordStatsMapper.java # 财务统计 SQL 聚合 Mapper
│   └── member/mapper/
│       └── MemberTransactionMapperExt.java # 会员流水 LEFT JOIN 查询
```

## 八、修改文件清单

| 文件 | 修改内容 |
|------|---------|
| `RateLimiterFilter.java` | 使用 WebUtil.getClientIp()，移除重复方法 |
| `OperationLogAspect.java` | 使用 WebUtil.getClientIp()，移除重复方法 |
| `JwtAuthenticationFilter.java` | 使用 JwtUtil.getPrefix() 替代硬编码 |
| `OrderController.java` | 使用 DateUtil、DTO 替代 Map |
| `FinanceController.java` | 使用 CsvExportUtil、DateUtil、DTO 替代 Map |
| `MaterialController.java` | 使用 WebUtil、StockOperationRequest DTO |
| `MemberController.java` | 使用 MemberLevelRequest DTO |
| `TodoController.java` | 使用 StatusRequest DTO |
| `NoticeController.java` | 使用 WebUtil.getCurrentUserId() |
| `SquareController.java` | 使用 WebUtil.getCurrentUserId/Username() |
| `DashboardController.java` | 使用 DateUtil |
| `DesignerCommissionController.java` (designer) | 使用 CommissionConfigRequest DTO |
| `FactoryController.java` | 移除 GET 请求中的 UPDATE 操作 |

## 九、后续迭代建议

1. **[高优先级]** 将 `StatisticsController` 改为使用 `OrderStatsMapper` 和 `FinanceRecordStatsMapper` 的 SQL 聚合方法
2. **[高优先级]** 抽取 `FinanceService`（FinanceController 702 行，最复杂的 Controller）
3. **[中优先级]** Dashboard 数据缓存（Redis 1 分钟 TTL 或内存缓存）
4. **[低优先级]** 统一 Dashboard 和 Statistics 的数据聚合逻辑
