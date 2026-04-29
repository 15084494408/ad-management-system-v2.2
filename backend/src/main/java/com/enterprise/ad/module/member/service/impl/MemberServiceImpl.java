package com.enterprise.ad.module.member.service.impl;

import com.enterprise.ad.common.exception.BusinessException;
import com.enterprise.ad.module.member.entity.Member;
import com.enterprise.ad.module.member.entity.MemberTransaction;
import com.enterprise.ad.module.member.mapper.MemberMapper;
import com.enterprise.ad.module.member.mapper.MemberTransactionMapper;
import com.enterprise.ad.module.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员服务层实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final MemberTransactionMapper transactionMapper;

    @Override
    @Transactional
    public void recharge(Long memberId, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "充值金额必须大于0");
        }

        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        BigDecimal before = member.getBalance();
        BigDecimal after = before.add(amount);

        member.setBalance(after);
        member.setTotalRecharge(member.getTotalRecharge().add(amount));
        member.setUpdateTime(LocalDateTime.now());
        memberMapper.updateById(member);

        MemberTransaction tx = new MemberTransaction();
        tx.setMemberId(memberId);
        tx.setType("recharge");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRemark(remark != null ? remark : "充值");
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);
    }

    @Override
    @Transactional
    public void consume(Long memberId, BigDecimal amount, Long orderId, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "消费金额必须大于0");
        }

        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (member.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(400, "余额不足");
        }

        // ★ 使用原子扣减
        int rows = memberMapper.deductBalance(memberId, amount);
        if (rows == 0) {
            throw new BusinessException(400, "余额不足或已被扣减");
        }

        Member updated = memberMapper.selectById(memberId);
        BigDecimal after = updated != null && updated.getBalance() != null ? updated.getBalance() : BigDecimal.ZERO;

        MemberTransaction tx = new MemberTransaction();
        tx.setMemberId(memberId);
        tx.setType("consume");
        tx.setAmount(amount);
        tx.setBalanceBefore(member.getBalance());
        tx.setBalanceAfter(after);
        tx.setOrderId(orderId);
        tx.setRemark(remark != null ? remark : "消费");
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);
    }

    @Override
    public String getMemberName(Long memberId) {
        if (memberId == null) return null;
        Member m = memberMapper.selectById(memberId);
        return m != null ? m.getMemberName() : null;
    }
}
