package com.mhc.springbootclouddisk.service.impl;


import cn.hutool.core.util.RandomUtil;
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
import com.mhc.springbootclouddisk.utils.SendEmailUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode> implements EmailCodeService {


    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private SendEmailUtils sendEmailUtils;

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
        String randomEmailCode = RandomUtil.randomNumbers(Constants.LENGTH_4);
        sendEmailUtils.startSendEmailCode(email, randomEmailCode);
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
}
