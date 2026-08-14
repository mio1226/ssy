package com.harddisk.module.admin.dto;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String password;
    private String displayName;
    private String email;
    private String phone;
}
