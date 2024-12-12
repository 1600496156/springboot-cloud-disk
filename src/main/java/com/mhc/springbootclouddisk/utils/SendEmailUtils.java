package com.mhc.springbootclouddisk.utils;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.EmailCode;
import com.mhc.springbootclouddisk.entity.dto.SendEmailCodeDto;
import com.mhc.springbootclouddisk.mapper.EmailCodeMapper;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class SendEmailUtils {

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private EmailCodeMapper emailCodeMapper;

    @Async
    public void startSendEmailCode(String toUserEmail, String randomEmailCode) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            mimeMessageHelper.setFrom("1600496156@qq.com");
            mimeMessageHelper.setTo(toUserEmail);
            SendEmailCodeDto sendEmailCodeDto = (SendEmailCodeDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_SEND_EMAIL_CODE + toUserEmail);
            if (sendEmailCodeDto == null) {
                SendEmailCodeDto sendEmailDto = (SendEmailCodeDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_SEND_EMAIL_CODE_DTO);
                if (sendEmailDto == null) {
                    sendEmailDto = new SendEmailCodeDto();
                    redisTemplate.opsForValue().set(Constants.REDIS_KEY_SEND_EMAIL_CODE_DTO, sendEmailDto);
                }
                sendEmailCodeDto = sendEmailDto;
                String content = String.format(sendEmailCodeDto.getRegisterEmailContent(), randomEmailCode);
                sendEmailCodeDto.setRegisterEmailContent(content);
                //重置邮箱验证码状态为已使用
                LambdaUpdateWrapper<EmailCode> updateWrapper = new LambdaUpdateWrapper<EmailCode>()
                        .set(EmailCode::getStatus, 1)
                        .eq(EmailCode::getEmail, toUserEmail)
                        .eq(EmailCode::getStatus, 0);
                emailCodeMapper.update(updateWrapper);
                log.info("成功重置邮箱【{}】验证码为过期状态", toUserEmail);
                //存储验证码信息到数据库
                EmailCode emailCode = new EmailCode();
                emailCode.setEmail(toUserEmail);
                emailCode.setCode(randomEmailCode);
                emailCodeMapper.insert(emailCode);
                redisTemplate.opsForValue().set(Constants.REDIS_KEY_SEND_EMAIL_CODE + toUserEmail, sendEmailCodeDto, Duration.ofMinutes(30));
                log.info("发送邮箱验证码给【{}】成功！验证码为【{}】", toUserEmail, randomEmailCode);
            }
            mimeMessageHelper.setSubject(sendEmailCodeDto.getRegisterEmailTitle());
            mimeMessageHelper.setText(sendEmailCodeDto.getRegisterEmailContent());
            javaMailSender.send(mimeMessage);
            log.info("发送邮箱验证码给【{}】成功！用户未使用验证码【{}】", toUserEmail, sendEmailCodeDto.getRegisterEmailContent());
        } catch (MessagingException e) {
            log.info("邮件发送失败", e);
            throw new ServerException("邮件发送失败");
        }
    }
}
