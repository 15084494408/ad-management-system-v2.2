package com.enterprise.ad.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ad.common.PageResult;
import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.dto.MemberLevelRequest;
import com.enterprise.ad.common.exception.BusinessException;
import com.enterprise.ad.module.member.entity.Member;
import com.enterprise.ad.module.member.entity.MemberTransaction;
import com.enterprise.ad.module.member.mapper.MemberMapper;
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
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "会员管理")
@Validated
public class MemberController {

    private final MemberMapper memberMapper;
    private final MemberTransactionMapper transactionMapper;
    private final OrderMapper orderMapper;

    @GetMapping
    @Operation(summary = "会员列表（分页）")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<PageResult<Member>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer status) {
        Page<Member> page = new Page<>(current, size);
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<Member>()
            .and(keyword != null, w -> w
                .like(Member::getMemberName, keyword)
                .or()
                .like(Member::getPhone, keyword)
            )
            .eq(level != null, Member::getLevel, level)
            .eq(status != null, Member::getStatus, status)
            .orderByDesc(Member::getCreateTime);
        Page<Member> result = memberMapper.selectPage(page, qw);

        // 填充虚拟字段
        result.getRecords().forEach(m -> {
            m.setContact(m.getContactPerson());
            m.setOrderCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getMemberId, m.getId())
                    .ne(Order::getStatus, 4)
                    .eq(Order::getDeleted, 0)
            ));
        });

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    // ===== 固定路径端点（必须放在 /{id} 之前，避免路径冲突） =====

    @GetMapping("/match")
    @Operation(summary = "根据手机号匹配会员（订单创建时自动关联）")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<Member> matchByPhone(@RequestParam String phone) {
        if (phone == null || phone.isBlank()) {
            return Result.ok(null);
        }
        Member member = memberMapper.selectOne(
            new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, phone.trim())
                .eq(Member::getStatus, 1)
                .eq(Member::getDeleted, 0)
                .last("LIMIT 1")
        );
        return Result.ok(member);
    }

    @GetMapping("/{id:[\\d]+}/orders")
    @Operation(summary = "会员关联订单列表")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<PageResult<Order>> getMemberOrders(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
            .eq(Order::getMemberId, id)
            .ne(Order::getStatus, 4)
            .eq(Order::getDeleted, 0)
            .orderByDesc(Order::getCreateTime);
        Page<Order> result = orderMapper.selectPage(page, qw);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/levels")
    @Operation(summary = "会员等级配置")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<?> getLevels() {
        return Result.ok(List.of());
    }

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
                .and(keyword != null, w -> w
                        .like(MemberTransaction::getRemark, keyword)
                        .or().like(MemberTransaction::getMemberId, keyword))
                .orderByDesc(MemberTransaction::getCreateTime);
        Page<MemberTransaction> result = transactionMapper.selectPage(page, qw);

        // 填充虚拟字段
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        result.getRecords().forEach(tx -> {
            tx.setTransactionNo("TX" + (tx.getCreateTime() != null ? tx.getCreateTime().format(fmt) : "") + tx.getId());
            // 查询会员名称
            if (tx.getMemberId() != null) {
                Member m = memberMapper.selectById(tx.getMemberId());
                if (m != null) tx.setMemberName(m.getMemberName());
            }
        });

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

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
                .and(keyword != null, w -> w
                        .like(MemberTransaction::getRemark, keyword)
                        .or().like(MemberTransaction::getMemberId, keyword))
                .orderByDesc(MemberTransaction::getCreateTime);
        Page<MemberTransaction> result = transactionMapper.selectPage(page, qw);

        // 填充虚拟字段
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        result.getRecords().forEach(tx -> {
            tx.setTransactionNo("TX" + (tx.getCreateTime() != null ? tx.getCreateTime().format(fmt) : "") + tx.getId());
            if (tx.getMemberId() != null) {
                Member m = memberMapper.selectById(tx.getMemberId());
                if (m != null) tx.setMemberName(m.getMemberName());
            }
        });

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PutMapping("/{id}/level")
    @Operation(summary = "更新会员等级")
    @PreAuthorize("hasAuthority('member:edit')")
    public Result<Void> updateLevel(@PathVariable Long id, @RequestBody MemberLevelRequest body) {
        Member member = new Member();
        member.setId(id);
        member.setLevel(body.getLevel());
        member.setUpdateTime(LocalDateTime.now());
        memberMapper.updateById(member);
        return Result.ok();
    }

    // ===== 动态路径端点 =====

    @GetMapping("/{id:[\\d]+}")
    @Operation(summary = "会员详情")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<Member> getById(@PathVariable Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        member.setContact(member.getContactPerson());
        member.setOrderCount(orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getMemberId, id)
                .ne(Order::getStatus, 4)
                .eq(Order::getDeleted, 0)
        ));
        return Result.ok(member);
    }

    @PostMapping
    @Operation(summary = "新建会员")
    @PreAuthorize("hasAuthority('member:create')")
    @Transactional
    public Result<Long> create(@RequestBody Member member) {
        // 校验手机号唯一
        if (member.getPhone() != null) {
            long count = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>()
                    .eq(Member::getPhone, member.getPhone())
            );
            if (count > 0) {
                return Result.fail(400, "手机号已存在");
            }
        }
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        if (member.getBalance() == null) member.setBalance(BigDecimal.ZERO);
        if (member.getTotalRecharge() == null) member.setTotalRecharge(BigDecimal.ZERO);
        if (member.getTotalConsume() == null) member.setTotalConsume(BigDecimal.ZERO);
        if (member.getStatus() == null) member.setStatus(1);
        if (member.getLevel() == null) member.setLevel("normal");
        if (member.getDeleted() != null) member.setDeleted(null); // 不允许前端设置
        memberMapper.insert(member);

        // 如果初始余额 > 0，自动生成一条充值记录
        if (member.getBalance() != null && member.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            MemberTransaction tx = new MemberTransaction();
            tx.setCustomerId(member.getId());
            tx.setMemberId(member.getId());
            tx.setType("recharge");
            tx.setAmount(member.getBalance());
            tx.setBalanceBefore(BigDecimal.ZERO);
            tx.setBalanceAfter(member.getBalance());
            tx.setRemark("初始充值");
            tx.setCreateTime(LocalDateTime.now());
            transactionMapper.insert(tx);
        }

        return Result.ok(member.getId());
    }

    @PutMapping("/{id:[\\d]+}")
    @Operation(summary = "更新会员")
    @PreAuthorize("hasAuthority('member:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Member member) {
        member.setId(id);
        member.setUpdateTime(LocalDateTime.now());
        // 清除不允许前端修改的字段
        member.setDeleted(null);
        member.setBalance(null);
        member.setTotalRecharge(null);
        member.setTotalConsume(null);
        memberMapper.updateById(member);
        return Result.ok();
    }

    @DeleteMapping("/{id:[\\d]+}")
    @Operation(summary = "删除会员")
    @PreAuthorize("hasAuthority('member:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        // ★ 修复：deleteById 在 @TableLogic 下会自动转为逻辑删除
        memberMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/{id:[\\d]+}/recharge")
    @Operation(summary = "会员充值（预存金额）")
    @PreAuthorize("hasAuthority('member:recharge')")
    @Transactional
    public Result<Void> recharge(
            @PathVariable Long id,
            @RequestBody RechargeRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "充值金额必须大于0");
        }

        Member member = memberMapper.selectById(id);
        if (member == null) {
            return Result.fail("会员不存在");
        }

        BigDecimal before = member.getBalance();
        BigDecimal after = before.add(request.getAmount());

        member.setBalance(after);
        member.setTotalRecharge(member.getTotalRecharge().add(request.getAmount()));
        member.setUpdateTime(LocalDateTime.now());
        memberMapper.updateById(member);

        MemberTransaction tx = new MemberTransaction();
        tx.setCustomerId(id);
        tx.setMemberId(id);
        tx.setType("recharge");
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark(request.getRemark() != null ? request.getRemark() : "充值");
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        return Result.ok();
    }

    @PostMapping("/{id:[\\d]+}/consume")
    @Operation(summary = "会员消费（使用余额）")
    @PreAuthorize("hasAuthority('member:edit')")
    @Transactional
    public Result<Void> consume(
            @PathVariable Long id,
            @RequestBody ConsumeRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "消费金额必须大于0");
        }

        Member member = memberMapper.selectById(id);
        if (member == null) {
            return Result.fail("会员不存在");
        }
        if (member.getBalance().compareTo(request.getAmount()) < 0) {
            return Result.fail(400, "余额不足");
        }

        BigDecimal before = member.getBalance();
        BigDecimal after = before.subtract(request.getAmount());

        member.setBalance(after);
        member.setTotalConsume(member.getTotalConsume().add(request.getAmount()));
        member.setUpdateTime(LocalDateTime.now());
        memberMapper.updateById(member);

        MemberTransaction tx = new MemberTransaction();
        tx.setCustomerId(id);
        tx.setMemberId(id);
        tx.setType("consume");
        tx.setAmount(request.getAmount());
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setOrderId(request.getOrderId());
        tx.setRemark(request.getRemark() != null ? request.getRemark() : "消费");
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        return Result.ok();
    }

    @GetMapping("/{id:[\\d]+}/transactions")
    @Operation(summary = "会员流水记录")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<?> getTransactions(@PathVariable Long id) {
        List<MemberTransaction> list = transactionMapper.selectList(
            new LambdaQueryWrapper<MemberTransaction>()
                .eq(MemberTransaction::getMemberId, id)
                .orderByDesc(MemberTransaction::getCreateTime));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        list.forEach(tx -> {
            tx.setTransactionNo("TX" + (tx.getCreateTime() != null ? tx.getCreateTime().format(fmt) : "") + tx.getId());
        });
        return Result.ok(list);
    }

    // ===== 内部 DTO =====

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
}
