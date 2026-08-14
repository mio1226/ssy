package com.harddisk.module.admin.controller;

import com.harddisk.common.Result;
import com.harddisk.common.PageResult;
import com.harddisk.module.admin.dto.UserCreateRequest;
import com.harddisk.module.admin.dto.UserUpdateRequest;
import com.harddisk.module.admin.service.AdminService;
import com.harddisk.module.auth.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
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
    public Result<SysUser> createUser(@RequestBody UserCreateRequest req) {
        return Result.success(adminService.createUser(req));
    }

    @PutMapping("/users/{id}")
    public Result<SysUser> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest req) {
        return Result.success(adminService.updateUser(id, req));
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return Result.success();
    }
}
