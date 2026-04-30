package com.enterprise.ad.module.customer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.exception.BusinessException;
import com.enterprise.ad.module.customer.entity.Customer;
import com.enterprise.ad.module.customer.entity.CustomerLevel;
import com.enterprise.ad.module.customer.mapper.CustomerMapper;
import com.enterprise.ad.module.customer.mapper.CustomerLevelMapper;
import com.enterprise.ad.module.member.entity.MemberTransaction;
import com.enterprise.ad.module.member.mapper.MemberTransactionMapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "客户管理")
@Validated
public class CustomerController {

    private final CustomerMapper customerMapper;
    private final CustomerLevelMapper customerLevelMapper;
    private final MemberTransactionMapper transactionMapper;
    private final OrderMapper orderMapper;

    @GetMapping
    @Operation(summary = "客户列表（分页，支持按客户类型/会员状态筛选）")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<PageResult<Customer>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer customerType,
            @RequestParam(required = false) Integer isMember) {
        Page<Customer> page = new Page<>(current, size);
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<Customer>()
            .eq(Customer::getDeleted, 0)
            .eq(level != null, Customer::getLevel, level)
            .eq(customerType != null, Customer::getCustomerType, customerType)
            .eq(isMember != null, Customer::getIsMember, isMember)
            .and(keyword != null, w -> w
                .like(Customer::getCustomerName, keyword)
                .or()
                .like(Customer::getContactPerson, keyword)
                .or()
                .like(Customer::getPhone, keyword)
            )
            .orderByDesc(Customer::getCreateTime);
        Page<Customer> result = customerMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /**
     * 会员客户列表（便捷接口，等同于 ?isMember=1）
     * 用于原会员管理页面的展示
     */
    @GetMapping("/members")
    @Operation(summary = "会员客户列表")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<PageResult<Customer>> listMemberCustomers(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String memberLevel,
            @RequestParam(required = false) Integer status) {
        Page<Customer> page = new Page<>(current, size);
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<Customer>()
            .eq(Customer::getDeleted, 0)
            .eq(Customer::getIsMember, 1)
            .and(keyword != null, w -> w
                .like(Customer::getCustomerName, keyword)
                .or()
                .like(Customer::getPhone, keyword)
            )
            .eq(memberLevel != null, Customer::getMemberLevel, memberLevel)
            .eq(status != null, Customer::getStatus, status)
            .orderByDesc(Customer::getCreateTime);
        Page<Customer> result = customerMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /**
     * 工厂客户列表（便捷接口，等同于 ?customerType=2）
     */
    @GetMapping("/factories")
    @Operation(summary = "工厂客户列表（用于账单关联）")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<List<Customer>> listFactoryCustomers() {
        List<Customer> list = customerMapper.selectList(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .eq(Customer::getCustomerType, Customer.TYPE_FACTORY)
                .orderByAsc(Customer::getCustomerName));
        return Result.ok(list);
    }

    /**
     * 获取系统级零售客户（公共客户，用于零散订单）
     * 如果不存在则自动创建
     */
    @GetMapping("/retail")
    @Operation(summary = "获取系统零售客户（公共客户）")
    public Result<Customer> getRetailCustomer() {
        Customer retail = customerMapper.selectRetailCustomer();
        if (retail == null) {
            // 首次调用时自动创建
            customerMapper.insertRetailCustomer();
            retail = customerMapper.selectRetailCustomer();
        }
        return Result.ok(retail);
    }

    /**
     * 全部客户列表（不分页，用于下拉选择）
     */
    @GetMapping("/all")
    @Operation(summary = "全部客户列表（不分页，用于下拉选择）")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<List<Customer>> listAll() {
        List<Customer> list = customerMapper.selectList(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getDeleted, 0)
                .orderByDesc(Customer::getCreateTime));
        return Result.ok(list);
    }

    // ── 客户等级（精确路径必须在 /{id} 之前） ──

    @GetMapping("/levels")
    @Operation(summary = "客户等级列表")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<List<Object>> listLevels() {
        List<Object> list = customerLevelMapper.listWithCustomerCount().stream()
                .map(m -> {
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) m;
                    CustomerLevel cl = new CustomerLevel();
                    cl.setId(((Number) map.get("id")).longValue());
                    cl.setName((String) map.get("name"));
                    cl.setLevel(((Number) map.get("level")).intValue());
                    cl.setMinAmount(map.get("min_amount") != null ? new java.math.BigDecimal(map.get("min_amount").toString()) : java.math.BigDecimal.ZERO);
                    cl.setDiscount(((Number) map.get("discount")).intValue());
                    cl.setDescription((String) map.get("description"));
                    java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                    result.put("id", cl.getId());
                    result.put("name", cl.getName());
                    result.put("level", cl.getLevel());
                    result.put("minAmount", cl.getMinAmount());
                    result.put("discount", cl.getDiscount());
                    result.put("description", cl.getDescription());
                    result.put("customerCount", map.get("customerCount") != null ? ((Number) map.get("customerCount")).longValue() : 0L);
                    return (Object) result;
                }).toList();
        return Result.ok(list);
    }

    @PostMapping("/levels")
    @Operation(summary = "新增客户等级")
    @PreAuthorize("hasAuthority('customer:edit')")
    public Result<Long> createLevel(@RequestBody CustomerLevel level) {
        level.setCreateTime(LocalDateTime.now());
        level.setUpdateTime(LocalDateTime.now());
        if (level.getStatus() == null) level.setStatus(1);
        if (level.getSort() == null) level.setSort(0);
        customerLevelMapper.insert(level);
        return Result.ok(level.getId());
    }

    @PutMapping("/levels/{id}")
    @Operation(summary = "更新客户等级")
    @PreAuthorize("hasAuthority('customer:edit')")
    public Result<Void> updateLevel(@PathVariable Long id, @RequestBody CustomerLevel level) {
        level.setId(id);
        level.setUpdateTime(LocalDateTime.now());
        customerLevelMapper.updateById(level);
        return Result.ok();
    }

    // ── 客户跟进记录（stub） ──

    @GetMapping("/follow")
    @Operation(summary = "客户跟进记录列表")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<PageResult<Object>> listFollow(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(PageResult.of(0, current, size, java.util.Collections.emptyList()));
    }

    // ── 客户 CRUD ──

    @GetMapping("/{id}")
    @Operation(summary = "客户详情")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<Customer> getById(@PathVariable Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null || customer.getDeleted() == 1) {
            return Result.fail("客户不存在");
        }
        return Result.ok(customer);
    }

    @PostMapping
    @Operation(summary = "新建客户（支持普通/工厂类型，手机号选填）")
    @PreAuthorize("hasAuthority('customer:create')")
    public Result<Long> create(@RequestBody Customer customer) {
        String customerName = customer.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty()) {
            return Result.fail("客户名称不能为空");
        }
        customer.setCustomerName(customerName.trim());

        // 禁止手动创建零售客户（系统级公共客户，由系统自动管理）
        if (customer.getCustomerType() != null && customer.getCustomerType() == Customer.TYPE_RETAIL) {
            return Result.fail("零售客户为系统级公共客户，不允许手动创建");
        }

        if (customer.getCustomerType() == null) customer.setCustomerType(Customer.TYPE_NORMAL);

        if (customer.getCustomerType() == Customer.TYPE_FACTORY
            && (customer.getFactoryType() == null || customer.getFactoryType().trim().isEmpty())) {
            return Result.fail("工厂客户必须选择工厂类型（印刷/包装/广告制作）");
        }

        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        if (customer.getStatus() == null) customer.setStatus(1);
        if (customer.getTotalAmount() == null) customer.setTotalAmount(java.math.BigDecimal.ZERO);
        if (customer.getOrderCount() == null) customer.setOrderCount(0);
        if (customer.getIsMember() == null) customer.setIsMember(0);
        if (customer.getBalance() == null) customer.setBalance(java.math.BigDecimal.ZERO);
        if (customer.getTotalRecharge() == null) customer.setTotalRecharge(java.math.BigDecimal.ZERO);
        if (customer.getTotalConsume() == null) customer.setTotalConsume(java.math.BigDecimal.ZERO);
        customerMapper.insert(customer);
        return Result.ok(customer.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新客户")
    @PreAuthorize("hasAuthority('customer:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Customer customer) {
        Customer existing = customerMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            return Result.fail("客户不存在");
        }
        String customerName = customer.getCustomerName();
        if (customerName != null) {
            if (customerName.trim().isEmpty()) {
                return Result.fail("客户名称不能为空");
            }
            customer.setCustomerName(customerName.trim());
        }
        customer.setId(id);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户（有关联订单或有余额时禁止删除）")
    @PreAuthorize("hasAuthority('customer:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null || customer.getDeleted() == 1) {
            return Result.fail("客户不存在");
        }

        // 禁止删除零售客户（系统级公共客户）
        if (customer.getCustomerType() != null && customer.getCustomerType() == Customer.TYPE_RETAIL) {
            return Result.fail("零售客户为系统级公共客户，不允许删除");
        }

        // 检查是否有关联的未删除订单（不管完成还是未完成，一律禁止）
        Long orderCount = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getCustomerId, id)
                .eq(Order::getDeleted, 0)
        );
        if (orderCount != null && orderCount > 0) {
            return Result.fail("该客户有 " + orderCount + " 笔关联订单（含已完成），请先删除所有订单后再删除客户");
        }

        // 检查会员是否有余额（有余额禁止删除，避免资金丢失）
        if (customer.getIsMember() == 1
            && customer.getBalance() != null
            && customer.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            return Result.fail("该会员仍有预存余额 ¥" + customer.getBalance().toPlainString()
                + "，请先联系会员退还余额或消费完毕后再删除");
        }

        customerMapper.deleteById(id);
        return Result.ok();
    }

    // ============================
    //  会员能力接口（基于客户表）
    // ============================

    /**
     * 根据手机号匹配会员客户（订单创建时自动关联）
     */
    @GetMapping("/match")
    @Operation(summary = "根据手机号匹配会员客户")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<Customer> matchByPhone(@RequestParam String phone) {
        if (phone == null || phone.isBlank()) {
            return Result.ok(null);
        }
        Customer customer = customerMapper.selectOne(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone.trim())
                .eq(Customer::getIsMember, 1)
                .eq(Customer::getStatus, 1)
                .eq(Customer::getDeleted, 0)
                .last("LIMIT 1")
        );
        return Result.ok(customer);
    }

    /**
     * 升级客户为会员
     */
    @PostMapping("/{id}/upgrade-member")
    @Operation(summary = "升级客户为会员")
    @PreAuthorize("hasAuthority('member:create')")
    public Result<Void> upgradeToMember(
            @PathVariable Long id,
            @RequestBody(required = false) UpgradeMemberRequest request) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null || customer.getDeleted() == 1) {
            return Result.fail("客户不存在");
        }
        if (customer.getIsMember() == 1) {
            return Result.fail("该客户已经是会员");
        }

        String memberLevel = (request != null && request.getLevel() != null) ? request.getLevel() : "normal";
        BigDecimal initialBalance = (request != null && request.getBalance() != null) ? request.getBalance() : BigDecimal.ZERO;

        customer.setIsMember(1);
        customer.setMemberLevel(memberLevel);
        customer.setBalance(initialBalance);
        customer.setTotalRecharge(initialBalance);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);

        // 如果有初始余额，自动生成充值记录
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            MemberTransaction tx = new MemberTransaction();
            tx.setCustomerId(id);
            tx.setMemberId(id); // 兼容旧表 member_id NOT NULL
            tx.setType("recharge");
            tx.setAmount(initialBalance);
            tx.setBalanceBefore(BigDecimal.ZERO);
            tx.setBalanceAfter(initialBalance);
            tx.setRemark("升级会员初始充值");
            tx.setCreateTime(LocalDateTime.now());
            transactionMapper.insert(tx);
        }

        return Result.ok();
    }

    /**
     * 会员充值
     */
    @PostMapping("/{id}/recharge")
    @Operation(summary = "会员充值")
    @PreAuthorize("hasAuthority('member:recharge')")
    @Transactional
    public Result<Void> recharge(
            @PathVariable Long id,
            @RequestBody RechargeRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "充值金额必须大于0");
        }

        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            return Result.fail("客户不存在");
        }
        if (customer.getIsMember() != 1) {
            return Result.fail("该客户还不是会员，请先升级");
        }

        BigDecimal before = customer.getBalance();
        BigDecimal after = before.add(request.getAmount());

        customer.setBalance(after);
        customer.setTotalRecharge(customer.getTotalRecharge().add(request.getAmount()));
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);

        MemberTransaction tx = new MemberTransaction();
        tx.setCustomerId(id);
        tx.setMemberId(id); // 兼容旧表 member_id NOT NULL
        tx.setType("recharge");
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark(request.getRemark() != null ? request.getRemark() : "充值");
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        return Result.ok();
    }

    /**
     * 会员消费（余额扣减）
     */
    @PostMapping("/{id}/consume")
    @Operation(summary = "会员消费（使用余额）")
    @PreAuthorize("hasAuthority('member:edit')")
    @Transactional
    public Result<Void> consume(
            @PathVariable Long id,
            @RequestBody ConsumeRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "消费金额必须大于0");
        }

        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            return Result.fail("客户不存在");
        }
        if (customer.getIsMember() != 1) {
            return Result.fail("该客户不是会员");
        }
        if (customer.getBalance().compareTo(request.getAmount()) < 0) {
            return Result.fail(400, "余额不足");
        }

        // 原子扣减
        int rows = customerMapper.deductBalance(id, request.getAmount());
        if (rows == 0) {
            return Result.fail(400, "余额不足或已被扣减");
        }

        Customer updated = customerMapper.selectById(id);
        BigDecimal after = updated != null && updated.getBalance() != null ? updated.getBalance() : BigDecimal.ZERO;

        MemberTransaction tx = new MemberTransaction();
        tx.setCustomerId(id);
        tx.setMemberId(id); // 兼容旧表 member_id NOT NULL
        tx.setType("consume");
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(customer.getBalance());
        tx.setBalanceAfter(after);
        tx.setOrderId(request.getOrderId());
        tx.setRemark(request.getRemark() != null ? request.getRemark() : "消费");
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        return Result.ok();
    }

    /**
     * 更新会员等级
     */
    @PutMapping("/{id}/member-level")
    @Operation(summary = "更新会员等级")
    @PreAuthorize("hasAuthority('member:edit')")
    public Result<Void> updateMemberLevel(@PathVariable Long id, @RequestBody MemberLevelRequest body) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            return Result.fail("客户不存在");
        }
        Customer update = new Customer();
        update.setId(id);
        update.setMemberLevel(body.getLevel());
        update.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(update);
        return Result.ok();
    }

    /**
     * 会员流水记录
     */
    @GetMapping("/{id}/transactions")
    @Operation(summary = "客户会员流水记录")
    @PreAuthorize("hasAuthority('customer:list')")
    public Result<?> getTransactions(@PathVariable Long id) {
        List<MemberTransaction> list = transactionMapper.selectList(
            new LambdaQueryWrapper<MemberTransaction>()
                .and(w -> w.eq(MemberTransaction::getCustomerId, id)
                    .or().eq(MemberTransaction::getMemberId, id))
                .orderByDesc(MemberTransaction::getCreateTime));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        list.forEach(tx -> {
            tx.setTransactionNo("TX" + (tx.getCreateTime() != null ? tx.getCreateTime().format(fmt) : "") + tx.getId());
            // 填充客户名称
            if (tx.getCustomerId() != null) {
                Customer c = customerMapper.selectById(tx.getCustomerId());
                if (c != null) tx.setMemberName(c.getCustomerName());
            }
        });
        return Result.ok(list);
    }

    /**
     * 充值记录列表（分页）
     */
    @GetMapping("/transactions/recharge")
    @Operation(summary = "充值记录列表（分页）")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<PageResult<MemberTransaction>> rechargeRecords(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<MemberTransaction> page = new Page<>(current, size);
        LambdaQueryWrapper<MemberTransaction> qw = new LambdaQueryWrapper<MemberTransaction>()
                .eq(MemberTransaction::getType, "recharge")
                .isNotNull(MemberTransaction::getCustomerId)
                .and(keyword != null, w -> w
                        .like(MemberTransaction::getRemark, keyword)
                        .or().like(MemberTransaction::getCustomerId, keyword))
                .orderByDesc(MemberTransaction::getCreateTime);
        Page<MemberTransaction> result = transactionMapper.selectPage(page, qw);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        result.getRecords().forEach(tx -> {
            tx.setTransactionNo("TX" + (tx.getCreateTime() != null ? tx.getCreateTime().format(fmt) : "") + tx.getId());
            if (tx.getCustomerId() != null) {
                Customer c = customerMapper.selectById(tx.getCustomerId());
                if (c != null) tx.setMemberName(c.getCustomerName());
            }
        });

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /**
     * 消费记录列表（分页）
     */
    @GetMapping("/transactions/consume")
    @Operation(summary = "消费记录列表（分页）")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<PageResult<MemberTransaction>> consumeRecords(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<MemberTransaction> page = new Page<>(current, size);
        LambdaQueryWrapper<MemberTransaction> qw = new LambdaQueryWrapper<MemberTransaction>()
                .eq(MemberTransaction::getType, "consume")
                .isNotNull(MemberTransaction::getCustomerId)
                .and(keyword != null, w -> w
                        .like(MemberTransaction::getRemark, keyword)
                        .or().like(MemberTransaction::getCustomerId, keyword))
                .orderByDesc(MemberTransaction::getCreateTime);
        Page<MemberTransaction> result = transactionMapper.selectPage(page, qw);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        result.getRecords().forEach(tx -> {
            tx.setTransactionNo("TX" + (tx.getCreateTime() != null ? tx.getCreateTime().format(fmt) : "") + tx.getId());
            if (tx.getCustomerId() != null) {
                Customer c = customerMapper.selectById(tx.getCustomerId());
                if (c != null) tx.setMemberName(c.getCustomerName());
            }
        });

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    // ===== 内部 DTO =====

    @lombok.Data
    public static class UpgradeMemberRequest {
        private String level;        // 会员等级
        private BigDecimal balance;  // 初始余额
    }

    @lombok.Data
    public static class RechargeRequest {
        @NotNull(message = "充值金额不能为空")
        @DecimalMin(value = "0.01", message = "充值金额必须大于0")
        private BigDecimal amount;
        private String remark;
    }

    @lombok.Data
    public static class ConsumeRequest {
        @NotNull(message = "消费金额不能为空")
        @DecimalMin(value = "0.01", message = "消费金额必须大于0")
        private BigDecimal amount;
        private Long orderId;
        private String remark;
    }

    @lombok.Data
    public static class MemberLevelRequest {
        private String level;
    }
}
