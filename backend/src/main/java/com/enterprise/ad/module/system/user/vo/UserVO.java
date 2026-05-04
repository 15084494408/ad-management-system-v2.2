package com.enterprise.ad.module.system.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ★ 修复 P3-20: 用户信息 VO（View Object）
 * 用于 API 响应，不暴露 password、createBy 等敏感字段
 *
 * 替代直接返回 SysUser Entity，建议所有返回用户信息的接口使用此 VO
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private Long companyId;
    private String department;
    private List<String> roles;
    private List<String> permissions;
    private LocalDateTime createTime;

    /**
     * 从 SysUser Entity 转换为 VO（不包含 password）
     */
    public static UserVO fromEntity(com.enterprise.ad.module.system.user.entity.SysUser user,
                                     List<String> roles, List<String> permissions) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCompanyId(user.getCompanyId());
        vo.setDepartment(user.getDepartment());
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
