package com.mhc.springbootclouddisk.entity.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("email_code")
public class EmailCode implements Serializable {
    @TableId
    private String email;
    private String code;
    @TableLogic
    private Short status;
}
