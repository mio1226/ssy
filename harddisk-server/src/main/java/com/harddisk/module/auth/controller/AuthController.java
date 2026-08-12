package com.harddisk.module.auth.controller;

import com.harddisk.common.Result;
import com.harddisk.module.auth.dto.LoginRequest;
import com.harddisk.module.auth.dto.LoginResponse;
import com.harddisk.module.auth.dto.RegisterRequest;
import com.harddisk.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }
}
