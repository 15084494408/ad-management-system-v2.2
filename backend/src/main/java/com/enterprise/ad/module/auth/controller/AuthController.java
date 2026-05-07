package com.enterprise.ad.module.auth.controller;

import com.enterprise.ad.common.Result;
import com.enterprise.ad.common.annotation.OperationLog;
import com.enterprise.ad.module.auth.dto.LoginRequest;
import com.enterprise.ad.module.auth.dto.LoginResponse;
import com.enterprise.ad.module.auth.dto.WxBindRequest;
import com.enterprise.ad.module.auth.dto.WxLoginRequest;
import com.enterprise.ad.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @OperationLog(value = "用户登录", module = "系统管理", recordParams = false)
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/wx-login")
    @Operation(summary = "微信小程序登录")
    public Result<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.ok(authService.wxLogin(request.getCode()));
    }

    @PostMapping("/wx-bind")
    @Operation(summary = "绑定微信（已登录用户）")
    public Result<Void> bindWx(@Valid @RequestBody WxBindRequest request, HttpServletRequest httpRequest) {
        Object userIdAttr = httpRequest.getAttribute("userId");
        if (userIdAttr == null) {
            return Result.fail("请先登录");
        }
        Long userId = (Long) userIdAttr;
        authService.bindWxOpenid(userId, request.getCode());
        return Result.ok();
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token)) {
            authService.logout(token);
        }
        return Result.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<LoginResponse.UserInfo> getCurrentUser() {
        return Result.ok(authService.getCurrentUser());
    }
}
