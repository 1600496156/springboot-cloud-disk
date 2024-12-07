package com.mhc.springbootclouddisk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoadShareListListVo {
    private String shareId;
    private String fileId;
    private String userId;
    private Short validType;
    private LocalDateTime expireTime;
    private LocalDateTime shareTime;
    private String code;
    private Integer showCount;
    private String fileName;
    private Short folderType;
    private Short fileCategory;
    private Short fileType;
    private String fileCover;
}
