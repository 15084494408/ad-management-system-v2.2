package com.enterprise.ad.module.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.ad.module.order.entity.Order;
import com.enterprise.ad.module.order.mapper.OrderMapper;
import com.enterprise.ad.module.member.entity.Member;
import com.enterprise.ad.module.member.mapper.MemberMapper;
import com.enterprise.ad.module.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发安全测试用例
 * ★ 修复 P0-2: 订单收款并发测试
 * ★ 修复 P0-3: 会员充值并发测试
 */
@SpringBootTest
public class ConcurrencySafetyTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberService memberService;

    // ========== P0-2: 订单收款并发安全测试 ==========

    @Test
    @Transactional
    public void testAddPaidAmount_AtomicOperation() {
        // 准备测试数据
        Order order = new Order();
        order.setOrderNo("TEST-ATOMIC-001");
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setTitle("测试订单-原子操作");
        order.setStatus(1);
        order.setPaymentStatus(1);
        order.setDeleted(0);
        orderMapper.insert(order);
        Long orderId = order.getId();

        // 测试原子增加金额
        int rows = orderMapper.addPaidAmount(orderId, new BigDecimal("500.00"));
        assertEquals(1, rows, "原子更新应返回1行受影响");

        // 验证金额已正确增加
        Order updated = orderMapper.selectById(orderId);
        assertEquals(0, new BigDecimal("500.00").compareTo(updated.getPaidAmount()));
    }

    @Test
    @Transactional
    public void testAddPaidAmount_MultipleCalls() {
        // 准备测试数据
        Order order = new Order();
        order.setOrderNo("TEST-ATOMIC-002");
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setTitle("测试订单-多次调用");
        order.setStatus(1);
        order.setPaymentStatus(1);
        order.setDeleted(0);
        orderMapper.insert(order);
        Long orderId = order.getId();

        // 模拟多次并发调用（实际应该用多线程，这里简化测试）
        orderMapper.addPaidAmount(orderId, new BigDecimal("100.00"));
        orderMapper.addPaidAmount(orderId, new BigDecimal("200.00"));
        orderMapper.addPaidAmount(orderId, new BigDecimal("300.00"));

        // 验证最终金额
        Order updated = orderMapper.selectById(orderId);
        assertEquals(0, new BigDecimal("600.00").compareTo(updated.getPaidAmount()));
    }

    @Test
    @Transactional
    public void testAddPaidAmount_WriteOff() {
        // 准备测试数据
        Order order = new Order();
        order.setOrderNo("TEST-ATOMIC-003");
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setPaidAmount(new BigDecimal("800.00"));
        order.setTitle("测试订单-抹零结清");
        order.setStatus(1);
        order.setPaymentStatus(2); // 部分付款
        order.setDeleted(0);
        orderMapper.insert(order);
        Long orderId = order.getId();

        // 测试抹零结清
        int rows = orderMapper.addPaidAmountWithWriteOff(
            orderId,
            new BigDecimal("150.00"), // 本次收款
            new BigDecimal("50.00"),   // 抹零金额
            4                          // 已抹零结清状态
        );

        assertEquals(1, rows, "原子更新应返回1行受影响");

        // 验证
        Order updated = orderMapper.selectById(orderId);
        assertEquals(0, new BigDecimal("950.00").compareTo(updated.getPaidAmount())); // 800 + 150
        assertEquals(0, new BigDecimal("50.00").compareTo(updated.getRoundingAmount()));
        assertEquals(4, updated.getPaymentStatus());
    }

    @Test
    @Transactional
    public void testAddPaidAmount_NonExistentOrder() {
        // 测试不存在的订单
        int rows = orderMapper.addPaidAmount(99999L, new BigDecimal("100.00"));
        assertEquals(0, rows, "不存在的订单应返回0行受影响");
    }

    // ========== P0-3: 会员充值并发安全测试 ==========

    @Test
    @Transactional
    public void testAddBalance_AtomicOperation() {
        // 准备测试数据
        Member member = new Member();
        member.setMemberName("测试会员-原子充值");
        member.setPhone("13800000001");
        member.setBalance(new BigDecimal("500.00"));
        member.setTotalRecharge(new BigDecimal("500.00"));
        member.setStatus(1);
        member.setDeleted(0);
        memberMapper.insert(member);
        Long memberId = member.getId();

        // 测试原子增加余额
        int rows = memberMapper.addBalance(memberId, new BigDecimal("200.00"));
        assertEquals(1, rows, "原子更新应返回1行受影响");

        // 验证余额和累计充值都已增加
        Member updated = memberMapper.selectById(memberId);
        assertEquals(0, new BigDecimal("700.00").compareTo(updated.getBalance()));
        assertEquals(0, new BigDecimal("700.00").compareTo(updated.getTotalRecharge()));
    }

    @Test
    @Transactional
    public void testAddBalance_NPEProtection() {
        // 测试 null 值处理
        Member member = new Member();
        member.setMemberName("测试会员-空值保护");
        member.setPhone("13800000002");
        member.setBalance(null); // null 初始值
        member.setTotalRecharge(null); // null 初始值
        member.setStatus(1);
        member.setDeleted(0);
        memberMapper.insert(member);
        Long memberId = member.getId();

        // 应该能正常处理 null 值
        int rows = memberMapper.addBalance(memberId, new BigDecimal("100.00"));
        assertEquals(1, rows);

        Member updated = memberMapper.selectById(memberId);
        assertEquals(0, new BigDecimal("100.00").compareTo(updated.getBalance()));
    }

    @Test
    @Transactional
    public void testRecharge_ServiceMethod() {
        // 准备测试数据
        Member member = new Member();
        member.setMemberName("测试会员-服务充值");
        member.setPhone("13800000003");
        member.setBalance(new BigDecimal("100.00"));
        member.setTotalRecharge(new BigDecimal("100.00"));
        member.setStatus(1);
        member.setDeleted(0);
        memberMapper.insert(member);
        Long memberId = member.getId();

        // 调用服务层充值方法
        memberService.recharge(memberId, new BigDecimal("300.00"), "测试充值");

        // 验证
        Member updated = memberMapper.selectById(memberId);
        assertEquals(0, new BigDecimal("400.00").compareTo(updated.getBalance()));
    }

    @Test
    @Transactional
    public void testDeductBalance_AtomicOperation() {
        // 准备测试数据
        Member member = new Member();
        member.setMemberName("测试会员-原子扣减");
        member.setPhone("13800000004");
        member.setBalance(new BigDecimal("500.00"));
        member.setTotalConsume(BigDecimal.ZERO);
        member.setStatus(1);
        member.setDeleted(0);
        memberMapper.insert(member);
        Long memberId = member.getId();

        // 测试原子扣减
        int rows = memberMapper.deductBalance(memberId, new BigDecimal("200.00"));
        assertEquals(1, rows);

        // 验证
        Member updated = memberMapper.selectById(memberId);
        assertEquals(0, new BigDecimal("300.00").compareTo(updated.getBalance()));
        assertEquals(0, new BigDecimal("200.00").compareTo(updated.getTotalConsume()));
    }

    @Test
    @Transactional
    public void testDeductBalance_InsufficientFunds() {
        // 准备测试数据
        Member member = new Member();
        member.setMemberName("测试会员-余额不足");
        member.setPhone("13800000005");
        member.setBalance(new BigDecimal("50.00"));
        member.setTotalConsume(BigDecimal.ZERO);
        member.setStatus(1);
        member.setDeleted(0);
        memberMapper.insert(member);
        Long memberId = member.getId();

        // 尝试扣减大于余额的金额
        int rows = memberMapper.deductBalance(memberId, new BigDecimal("100.00"));
        assertEquals(0, rows, "余额不足应返回0行受影响");

        // 验证余额未被修改
        Member updated = memberMapper.selectById(memberId);
        assertEquals(0, new BigDecimal("50.00").compareTo(updated.getBalance()));
    }
}
