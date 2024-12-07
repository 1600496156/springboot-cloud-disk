package com.mhc.springbootclouddisk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GetShareLoginInfoDto {
    private LocalDateTime shareTime;
    private LocalDateTime expireTime;
    private String nickName;
    private String fileName;
    private Boolean currentUser;
    private String fileId;
    private String avatar;
    private String userId;
}
