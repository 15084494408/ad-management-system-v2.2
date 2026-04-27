package com.enterprise.ad.module.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ad.module.system.role.entity.Role;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色ID查询关联的权限码
     */
    @Select("SELECT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.deleted = 0 AND p.status = 1 " +
            "AND p.permission_code IS NOT NULL AND p.permission_code != '' " +
            "GROUP BY p.permission_code, p.sort " +
            "ORDER BY p.sort")
    List<String> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色所有权限关联
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deletePermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限关联
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_permission (role_id, permission_id) VALUES " +
            "<foreach collection='permissionIds' item='pid' separator=','>" +
            "(#{roleId}, #{pid})" +
            "</foreach>" +
            "</script>")
    int batchInsertPermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
