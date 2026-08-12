package com.harddisk.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public SysUser createUser(SysUser user) {
        SysUser existing = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (existing != null) throw new IllegalArgumentException("用户名已存在");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        if (user.getRole() == null) user.setRole("USER");
        sysUserMapper.insert(user);
        return user;
    }

    @Transactional
    public SysUser updateUser(Long id, SysUser user) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("用户不存在");
        if (user.getDisplayName() != null) existing.setDisplayName(user.getDisplayName());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getRole() != null) existing.setRole(user.getRole());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        sysUserMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void deleteUser(Long id) {
        sysUserMapper.deleteById(id);
    }
}
