package com.mhc.springbootclouddisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.SendEmailCodeDto;
import com.mhc.springbootclouddisk.entity.dto.UserLoginDto;
import com.mhc.springbootclouddisk.entity.dto.UserSpaceDto;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.service.EmailCodeService;
import com.mhc.springbootclouddisk.service.UserInfoService;
import com.mhc.springbootclouddisk.utils.AliOSSUtils;
import com.mhc.springbootclouddisk.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private AliOSSUtils aliOSSUtils;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String emailCode, String nickName, String password) {
        QueryWrapper<UserInfo> selectByEmailWrapper = new QueryWrapper<UserInfo>()
                .eq("email", email);
        UserInfo userInfo = baseMapper.selectOne(selectByEmailWrapper);
        if (userInfo != null) {
            throw new ServerException("当前邮箱已经被注册，请更换其他邮箱");
        }
        UserInfo nickNameUser = lambdaQuery().eq(UserInfo::getNickName, nickName).one();
        if (nickNameUser != null) {
            throw new ServerException("当前昵称已经被使用，请更换其他昵称");
        }
        //校验邮箱验证码
        emailCodeService.checkEmailCode(email, emailCode);
        password = passwordEncoder.encode(password);
        UserInfo user = new UserInfo();
        user.setNickName(nickName);
        user.setEmail(email);
        user.setPassword(password);
        user.setUseSpace(0L);
        SendEmailCodeDto sendEmailCodeDto = (SendEmailCodeDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_SEND_EMAIL_CODE_DTO);
        if (sendEmailCodeDto == null) {
            sendEmailCodeDto = new SendEmailCodeDto();
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_SEND_EMAIL_CODE_DTO, sendEmailCodeDto);
        }
        user.setTotalSpace(sendEmailCodeDto.getUserInitUseSpace());
        this.save(user);
    }

    @Override
    public UserLoginDto login(String email, String password, HttpServletResponse response) {
        //传入用户名和密码
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        } catch (ArithmeticException e) {
            log.error(e.getMessage());
            throw new ServerException("登录失败，你的账号可能被封禁");
        }
        UserInfo user = (UserInfo) authentication.getPrincipal();
        if (user == null) {
            throw new ServerException("账号或者密码错误，请重新输入");
        }
        //更新最后登录时间
        lambdaUpdate().set(UserInfo::getLastLoginTime, LocalDateTime.now())
                .eq(UserInfo::getEmail, email).update();
        UserLoginDto userLoginDto = new UserLoginDto();
        userLoginDto.setAvatar(user.getQqAvatar());
        userLoginDto.setUserId(user.getUserId());
        userLoginDto.setNickName(user.getNickName());
        userLoginDto.setAdmin(user.getEmail().equals("1600496156@qq.com"));
        jwtUtils.updateToken(response, user);
        return userLoginDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(String email, String password, String emailCode) {
        UserInfo selectUserByEmail = lambdaQuery().eq(UserInfo::getEmail, email).one();
        if (selectUserByEmail == null) {
            throw new ServerException("你输入的邮箱地址错误，请重新输入");
        }
        emailCodeService.checkEmailCode(email, emailCode);
        password = passwordEncoder.encode(password);
        selectUserByEmail.setPassword(password);
        this.updateById(selectUserByEmail);
    }

    @Override
    public String avatar(String userId) {
        UserInfo user = lambdaQuery().eq(UserInfo::getUserId, userId).one();
        if (user == null) {
            log.info("avatar获取不到用户");
            return "https://springboot-cloud-disk.oss-cn-shenzhen.aliyuncs.com/Avatar/074fab204f789e5a13bd0828c63cd8b1.jpg";
        }
        if (user.getQqAvatar() == null) {
            String url = aliOSSUtils.defaultUpload(userId);
            user.setQqAvatar(url);
            lambdaUpdate().set(UserInfo::getQqAvatar, url).eq(UserInfo::getUserId, user.getUserId()).update();
        }
        return user.getQqAvatar();
    }

    @Override
    public void updateUserAvatar(MultipartFile avatar, HttpServletResponse response, String jwt) {
        Claims claims = jwtUtils.getClaims(jwt);
        String userId = claims.get("userId", String.class);
        String url = aliOSSUtils.upload(avatar, userId);
        lambdaUpdate().set(UserInfo::getQqAvatar, url).eq(UserInfo::getUserId, userId).update();
        log.info("数据库更新头像链接成功");
        //更新token
        UserInfo user = lambdaQuery().eq(UserInfo::getUserId, userId).one();
        jwtUtils.updateToken(response, user);
    }

    @Override
    public UserSpaceDto getUseSpace(String jwt) {
        Claims claims = jwtUtils.getClaims(jwt);
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(claims.get("useSpace", Long.class));
        userSpaceDto.setTotalSpace(claims.get("totalSpace", Long.class));
        return userSpaceDto;
    }

    @Override
    public Page<UserInfo> loadUserListPage(Long pageNo, Long pageSize) {
        //1.分页参数
        Page<UserInfo> page = Page.of(pageNo, pageSize);
        //2.排序参数
        page.addOrder(OrderItem.desc("last_login_time"));
        //3.开始分页查询
        log.info("开始分页查询");
        return this.page(page);
    }
}
