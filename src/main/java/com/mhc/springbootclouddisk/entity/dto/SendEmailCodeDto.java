package com.mhc.springbootclouddisk.entity.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SendEmailCodeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String registerEmailTitle = "springboot-cloud-disk | 邮箱验证码";
    private String registerEmailContent = "你的邮箱验证码为：%s，验证码30分钟内有效。如非本人操作，请勿转给他人，避免账号被盗用或个人信息泄漏，谨防诈骗。";
    private Long userInitUseSpace = 1024L * 1024L * 1024L * 2L;
}
