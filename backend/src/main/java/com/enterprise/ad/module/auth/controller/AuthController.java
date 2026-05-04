package com.enterprise.ad.module.auth.controller;

import com.enterprise.ad.common.Result;
import com.enterprise.ad.module.auth.dto.LoginRequest;
import com.enterprise.ad.module.auth.dto.LoginResponse;
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
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
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
