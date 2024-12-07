package com.mhc.springbootclouddisk.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.EmailCode;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.SendEmailCodeDto;
import com.mhc.springbootclouddisk.mapper.EmailCodeMapper;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.service.EmailCodeService;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZoneOffset;

@Slf4j
@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode> implements EmailCodeService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private JavaMailSender javaMailSender;

    //!!!redis自定义config配置文件内的RedisTemplate的方法名必须和注入的变量名一致
    //!!!配置文件内是RedisTemplate<Object, Object>，这里必须是RedisTemplate<Object, Object>，不能RedisTemplate<Object, SendEmailCodeDto>
    //!!!RedisConfig必须加@Component
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<UserInfo>()
                .eq("email", email);
        UserInfo userInfo = userInfoMapper.selectOne(userInfoQueryWrapper);
        if (userInfo != null && type == 0) {
            throw new ServerException("当前邮箱已经被注册，请更换其他邮箱");
        }

        //发送新的邮箱验证码
        String randomEmailCode = RandomStringUtils.random(Constants.LENGTH_4, false, true);
        startSendEmailCode(email, randomEmailCode);
    }

    @Override
    public void checkEmailCode(String email, String emailCode) {
        SendEmailCodeDto sendEmailCodeDto = (SendEmailCodeDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_SEND_EMAIL_CODE + email);
        EmailCode selectByEmailAndEmailCode = lambdaQuery()
                .eq(EmailCode::getEmail, email)
                .eq(EmailCode::getCode, emailCode)
                .eq(EmailCode::getStatus, 0)
                .one();

        if (selectByEmailAndEmailCode == null || sendEmailCodeDto == null) {
            throw new ServerException("邮箱验证码不正确或者已经过期，请重新获取邮箱验证码");
        }
        //重置邮箱验证码状态为已使用
        lambdaUpdate()
                .set(EmailCode::getStatus, 1)
                .eq(EmailCode::getEmail, email)
                .eq(EmailCode::getStatus, 0)
                .update();
        redisTemplate.delete(Constants.REDIS_KEY_SEND_EMAIL_CODE + email);
    }

    private void startSendEmailCode(String toUserEmail, String randomEmailCode) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            mimeMessageHelper.setFrom("1600496156@qq.com");
            mimeMessageHelper.setTo(toUserEmail);

            SendEmailCodeDto sendEmailCodeDto = (SendEmailCodeDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_SEND_EMAIL_CODE + toUserEmail);
            if (sendEmailCodeDto == null) {
                sendEmailCodeDto = new SendEmailCodeDto();
                String content = String.format(sendEmailCodeDto.getEmailContent(), randomEmailCode);
                sendEmailCodeDto.setEmailContent(content);
                //重置邮箱验证码状态为已使用
                lambdaUpdate()
                        .set(EmailCode::getStatus, 1)
                        .eq(EmailCode::getEmail, toUserEmail)
                        .eq(EmailCode::getStatus, 0)
                        .update();
                log.info("成功重置邮箱【{}】为过期状态", toUserEmail);
                //存储验证码信息到数据库
                EmailCode emailCode = new EmailCode();
                emailCode.setEmail(toUserEmail);
                emailCode.setCode(randomEmailCode);
                this.save(emailCode);
                redisTemplate.opsForValue().set(Constants.REDIS_KEY_SEND_EMAIL_CODE + toUserEmail, sendEmailCodeDto, Duration.ofMinutes(30));
                log.info("发送邮箱验证码给【{}】成功！验证码为【{}】", toUserEmail, randomEmailCode);
            }
            mimeMessageHelper.setSubject(sendEmailCodeDto.getMailTitle());
            mimeMessageHelper.setText(sendEmailCodeDto.getEmailContent());
            javaMailSender.send(mimeMessage);
            log.info("发送邮箱验证码给【{}】成功！用户未使用验证码【{}】", toUserEmail, sendEmailCodeDto.getEmailContent());
        } catch (MessagingException e) {
            log.info("邮件发送失败", e);
            throw new ServerException("邮件发送失败");
        }
    }

}
