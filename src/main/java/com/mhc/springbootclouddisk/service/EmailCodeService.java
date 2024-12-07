package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.EmailCode;


public interface EmailCodeService extends IService<EmailCode> {

    void sendEmailCode(String email, Integer type);

    void checkEmailCode(String email, String emailCode);
}
