package com.enterprise.ad.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * ★ 修复 P1-7: 原子扣减会员余额（并发安全）
     * 返回影响行数：0 表示余额不足，1 表示扣减成功
     */
    @Update("UPDATE mem_member SET balance = balance - #{amount}, " +
            "total_consume = IFNULL(total_consume, 0) + #{amount}, " +
            "update_time = NOW() " +
            "WHERE id = #{memberId} AND balance >= #{amount} AND deleted = 0")
    int deductBalance(@Param("memberId") Long memberId, @Param("amount") java.math.BigDecimal amount);
}
