package com.enterprise.ad.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * 权限数据补丁 - 启动时自动检查并补全缺失的权限码
 * 
 * 首次启动补全后，可安全删除此配置类（或保留，幂等执行无副作用）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PermissionPatchConfig {

    private final DataSource dataSource;

    /**
     * 需要确保存在的权限码及对应的菜单名称和父级
     */
    private static final List<PermissionDef> PATCHES = Arrays.asList(
        new PermissionDef(7, "角色权限", "button", "system:role", 2),
        new PermissionDef(7, "操作日志", "button", "system:log", 3),
        new PermissionDef(7, "数据字典", "button", "system:dict", 4),
        new PermissionDef(7, "数据备份", "button", "system:backup", 5),
        new PermissionDef(7, "公告管理", "button", "system:notice", 6),
        new PermissionDef(7, "按钮管理", "button", "system:menu", 7),
        new PermissionDef(0, "物料管理", "menu", "material:view", 8),
        new PermissionDef(0, "设计文件", "menu", "design:file", 9),
        new PermissionDef(0, "统计报表", "menu", "statistics:view", 10),
        new PermissionDef(0, "广场管理", "menu", "square:manage", 11)
    );

    @Bean
    @Order(1)
    public CommandLineRunner patchPermissions() {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                int addedCount = 0;

                for (PermissionDef p : PATCHES) {
                    // 检查是否已存在
                    var rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM sys_permission WHERE permission_code = '" + p.code + "' AND deleted = 0"
                    );
                    rs.next();
                    if (rs.getInt(1) == 0) {
                        // 不存在则插入
                        String sql = String.format(
                            "INSERT INTO sys_permission (parent_id, name, type, permission_code, sort, visible, status, deleted) " +
                            "VALUES (%d, '%s', '%s', '%s', %d, 1, 1, 0)",
                            p.parentId, p.name, p.type, p.code, p.sort
                        );
                        stmt.executeUpdate(sql);
                        addedCount++;
                        log.info("补丁：新增权限 {} ({})", p.code, p.name);
                    }
                }

                if (addedCount > 0) {
                    // 将新权限授权给 SUPER_ADMIN (role_id=1)
                    stmt.executeUpdate(
                        "INSERT IGNORE INTO sys_role_permission (role_id, permission_id) " +
                        "SELECT 1, id FROM sys_permission " +
                        "WHERE permission_code IN ('system:role','system:log','system:dict','system:backup','system:notice','system:menu','material:view','design:file','statistics:view','square:manage') " +
                        "AND deleted = 0"
                    );
                    log.info("补丁：已将 {} 个新权限授权给 SUPER_ADMIN", addedCount);
                } else {
                    log.info("权限补丁检查完成，无新增（所有权限码已存在）");
                }

                // 打印最终权限总数
                var rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM sys_permission WHERE deleted = 0 AND status = 1 AND permission_code IS NOT NULL AND permission_code != ''"
                );
                rs.next();
                log.info("当前有效权限码总数: {}", rs.getInt(1));

            } catch (Exception e) {
                log.warn("权限补丁执行失败（不影响启动）: {}", e.getMessage());
            }
        };
    }

    private record PermissionDef(int parentId, String name, String type, String code, int sort) {}
}
