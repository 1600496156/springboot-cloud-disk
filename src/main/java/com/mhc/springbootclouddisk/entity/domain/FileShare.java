package com.mhc.springbootclouddisk.entity.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_share")
public class FileShare {
    @TableId(type = IdType.ASSIGN_UUID)
    private String shareId;
    private String fileId;
    private String userId;
    private Short validType;
    private LocalDateTime expireTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime shareTime;
    private String code;
    private Integer showCount;
}
