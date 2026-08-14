package com.harddisk.module.admin.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String displayName;
    private String email;
    private String phone;
    private String role;
    private Integer status;
    private String password;
}
