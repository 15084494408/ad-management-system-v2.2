package com.enterprise.ad.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.member.entity.MemberTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MemberTransactionMapper extends BaseMapper<MemberTransaction> {

    /**
     * 查询充值/消费记录，自动关联会员名称
     * 替代 MemberController 中循环 selectById 的 N+1 查询
     */
    @Select("<script>" +
            "SELECT t.*, m.member_name " +
            "FROM mem_member_transaction t " +
            "LEFT JOIN mem_member m ON t.member_id = m.id " +
            "WHERE t.deleted = 0 AND t.type = #{type} " +
            "<if test='keyword != null'>" +
            "  AND (t.remark LIKE CONCAT('%', #{keyword}, '%') OR CAST(t.member_id AS CHAR) LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY t.create_time DESC" +
            "</script>")
    List<Map<String, Object>> selectWithMemberName(@Param("type") String type, @Param("keyword") String keyword);
}
