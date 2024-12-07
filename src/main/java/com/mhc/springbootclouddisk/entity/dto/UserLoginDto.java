package com.mhc.springbootclouddisk.entity.dto;

import lombok.Data;

@Data
public class UserLoginDto {
    private String nickName;
    private String userId;
    private String avatar;
    private Boolean Admin;
}
