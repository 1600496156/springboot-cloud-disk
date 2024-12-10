package com.mhc.springbootclouddisk.entity.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo implements Serializable {
    @TableId("file_id")
    private String fileId;
    private String userId;
    private String fileMd5;
    private String filePid;
    private Long fileSize;
    private String fileName;
    private String fileCover;
    private String filePath;
    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;
    private Short folderType;
    private Short fileCategory;
    private Short fileType;
    private Short status;

    private LocalDateTime recoveryTime;
    @TableLogic
    private Short delFlat;
}
