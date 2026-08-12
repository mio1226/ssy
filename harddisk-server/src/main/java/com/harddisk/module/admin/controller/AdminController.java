package com.harddisk.module.admin.controller;

import com.harddisk.common.Result;
import com.harddisk.common.PageResult;
import com.harddisk.module.admin.service.AdminService;
import com.harddisk.module.auth.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<PageResult<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        var p = adminService.listUsers(page, pageSize, username, role);
        return Result.success(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @PostMapping("/users")
    public Result<SysUser> createUser(@RequestBody SysUser user) {
        return Result.success(adminService.createUser(user));
    }

    @PutMapping("/users/{id}")
    public Result<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        return Result.success(adminService.updateUser(id, user));
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return Result.success();
    }
}
