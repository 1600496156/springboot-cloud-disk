package com.mhc.springbootclouddisk.entity.dto;

import lombok.Data;

@Data
public class CreateDownloadUrlDto {
    private String code;
    private String filePath;
    private String fileName;
}
