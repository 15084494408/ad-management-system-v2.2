# 财务模块重写 + 菜单调整 完成概要

## 一、菜单调整
- **新增**: 订单管理 → 订单报价（从财务结算移入）
- **移除**: 财务结算 → 订单报价
- 路由未变（`/finance/quote` 依然指向 `FinanceQuoteView.vue`）

## 二、创建 FinanceRecordService（财务记录服务层）
**路径**: `backend/.../finance/service/FinanceRecordService.java`

统一所有 `fin_record` 写入的集中入口：
| 方法 | 用途 |
|------|------|
| `createIncome()` | 创建收入记录（自动生成流水号） |
| `createExpense()` | 创建支出记录 |
| `createRefund()` | 创建退款记录（expense + 退款类别） |
| `generateRecordNo()` | 统一生成流水号（前缀+时间戳） |
| `calcUnpaid()` | 计算订单待收金额 = total - paid - rounding (静态方法) |
| `calcPaymentStatus()` | 计算支付状态 (静态方法) |

**业务前缀**: ORD(收款), FIN(记账), REF(退款), MAT(采购), FAC(工厂)

## 三、订单财务逻辑重构
| 操作 | 旧逻辑 | 新逻辑 |
|------|--------|--------|
| 订单收款 | 手动创建 FinanceRecord | 走 `FinanceRecordService.createIncome()` |
| 取消订单退款 | 只退会员余额，无财务流水 | 退余额 + 创建退款财务流水 |
| 抹零 | 手动更新 order | 不走财务流水（无实际资金流）|

## 四、快速记账统一
`FinanceController.createRecord()` 改为通过 `FinanceRecordService` 创建记录

## 五、财务设计原则
1. **只有实际资金流动才记 fin_record**（付款、收款、退款、采购、提成）
2. **抹零不记 fin_record**（抹零是豁免金额，无实际资金进出）
3. **所有流水号前缀统一**（ORD/FIN/REF/MAT/FAC）
4. **服务层可扩展**：方便后续加审计日志、对账、通知

## 待办（后续可迭代）
- FactoryController / MaterialController / SquareController / DesignerCommissionController 迁移到 FinanceRecordService
