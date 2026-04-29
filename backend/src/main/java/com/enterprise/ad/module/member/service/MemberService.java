package com.enterprise.ad.module.member.service;

import com.enterprise.ad.module.member.entity.MemberTransaction;

import java.math.BigDecimal;

/**
 * 会员服务层接口
 * ★ 从 MemberController 抽取充值/消费核心逻辑，便于 OrderService 复用
 */
public interface MemberService {

    /** 会员充值 */
    void recharge(Long memberId, BigDecimal amount, String remark);

    /** 会员消费（使用余额） */
    void consume(Long memberId, BigDecimal amount, Long orderId, String remark);

    /** 获取会员名称（供其他模块查询） */
    String getMemberName(Long memberId);
}
