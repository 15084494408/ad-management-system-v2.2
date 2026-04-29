package com.enterprise.ad.common.dto;

import lombok.Data;

/**
 * 会员等级变更请求 DTO
 * ★ 替代 MemberController.updateLevel() 的 Map<String, Object> body
 */
@Data
public class MemberLevelRequest {
    private String level;
}
