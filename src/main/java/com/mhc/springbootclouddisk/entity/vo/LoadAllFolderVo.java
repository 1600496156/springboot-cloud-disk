package com.mhc.springbootclouddisk.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoadAllFolderVo {
    private String fileId;
    private String filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;
    private String filePath;
    private LocalDateTime updateTime;
    private Short folderType;
    private Short fileCategory;
    private Short fileType;
    private Short status;
}
