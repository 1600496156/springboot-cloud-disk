package com.mhc.springbootclouddisk.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoadUserDataListDto {
    private String fileId;
    private String userId;
    private String fileMd5;
    private String filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;
    private String filePath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Short folderType;
    private Short fileCategory;
    private Short fileType;
    private Short status;
    private LocalDateTime recoveryTime;
    private Short delFlat;
    private String nickName;
    private String email;
    private Integer qqOpenId;
    private String qqAvatar;
    private String password;
    private LocalDateTime joinTime;
    private LocalDateTime lastLoginTime;
    private Long useSpace;
    private Long totalSpace;
}
