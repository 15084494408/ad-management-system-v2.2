package com.enterprise.ad.common;

/**
 * ★ 修复 P2-8: 系统配置常量
 * 默认密码等敏感配置建议通过环境变量覆盖
 */
public final class SysConstants {

    private SysConstants() {}

    /**
     * 默认用户密码
     * 生产环境应通过环境变量 USER_DEFAULT_PASSWORD 设置
     */
    public static final String DEFAULT_PASSWORD = System.getenv("USER_DEFAULT_PASSWORD") != null
        ? System.getenv("USER_DEFAULT_PASSWORD")
        : "AdSystem@2024";
}
