package com.harddisk.module.auth.service;

import com.harddisk.module.auth.dto.LoginRequest;
import com.harddisk.module.auth.dto.LoginResponse;
import com.harddisk.module.auth.dto.RegisterRequest;
import com.harddisk.module.auth.entity.SysUser;
import com.harddisk.module.auth.mapper.SysUserMapper;
import com.harddisk.module.auth.security.JwtUtil;
import com.harddisk.module.rule.service.RuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RuleService ruleService;

    public void register(RegisterRequest req) {
        validatePassword(req.getPassword());

        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername()));
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        String defaultRole = ruleService.getRuleValue("default_role");
        user.setRole(defaultRole != null ? defaultRole : "USER");
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    public LoginResponse login(LoginRequest req) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BadCredentialsException("账号已被禁用");
        }
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getUsername(), user.getDisplayName(), user.getRole());
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("密码必须包含字母和数字");
        }
    }
}