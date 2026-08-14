package com.harddisk.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harddisk.module.admin.dto.UserCreateRequest;
import com.harddisk.module.admin.dto.UserUpdateRequest;
import com.harddisk.module.auth.entity.SysUser;
import com.harddisk.module.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<SysUser> listUsers(int page, int pageSize, String username, String role) {
        Page<SysUser> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(username != null, SysUser::getUsername, username)
                .eq(role != null, SysUser::getRole, role)
                .orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectPage(p, wrapper);
    }

    @Transactional
    public SysUser createUser(UserCreateRequest req) {
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (existing != null) throw new IllegalArgumentException("用户名已存在");

        validatePassword(req.getPassword());

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setStatus(1);
        user.setRole("USER");
        sysUserMapper.insert(user);
        return user;
    }

    @Transactional
    public SysUser updateUser(Long id, UserUpdateRequest req) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("用户不存在");
        if (req.getDisplayName() != null) existing.setDisplayName(req.getDisplayName());
        if (req.getEmail() != null) existing.setEmail(req.getEmail());
        if (req.getPhone() != null) existing.setPhone(req.getPhone());
        if (req.getRole() != null) existing.setRole(req.getRole());
        if (req.getStatus() != null) existing.setStatus(req.getStatus());
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            validatePassword(req.getPassword());
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        sysUserMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void deleteUser(Long id) {
        sysUserMapper.deleteById(id);
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