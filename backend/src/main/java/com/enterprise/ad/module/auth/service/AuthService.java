package com.enterprise.ad.module.auth.service;

import com.enterprise.ad.module.auth.dto.LoginRequest;
import com.enterprise.ad.module.auth.dto.LoginResponse;

public interface AuthService {
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 微信小程序登录
     */
    LoginResponse wxLogin(String code);

    /**
     * 绑定微信openid（已登录用户）
     */
    void bindWxOpenid(Long userId, String code);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * 获取当前登录用户信息
     */
    LoginResponse.UserInfo getCurrentUser();
}
