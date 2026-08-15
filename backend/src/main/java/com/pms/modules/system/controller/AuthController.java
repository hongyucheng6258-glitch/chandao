package com.pms.modules.system.controller;

import com.pms.common.result.Result;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.system.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginBody body) {
        return Result.ok(authService.login(body.getUsername(), body.getPassword()));
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.ok(authService.info(SecurityUtil.getUserId()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(SecurityUtil.getUserId());
        return Result.ok();
    }

    @Data
    public static class LoginBody {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
