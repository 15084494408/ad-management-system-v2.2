package com.enterprise.ad.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.system.user.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 1")
    List<String> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.status = 1 " +
            "AND p.permission_code IS NOT NULL AND p.permission_code != ''")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteUserRole(@Param("userId") Long userId);

    @org.apache.ibatis.annotations.Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE r.role_code = 'DESIGNER' AND u.deleted = 0 AND u.status = 1 " +
            "ORDER BY u.id")
    List<SysUser> selectDesignerUsers();

    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON u.id = ur.user_id " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE r.role_code = #{roleCode} AND u.deleted = 0 AND u.status = 1 " +
            "ORDER BY u.id")
    List<SysUser> selectUsersByRoleCode(@Param("roleCode") String roleCode);
}
