package com.mhc.springbootclouddisk.controller;

import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import com.mhc.springbootclouddisk.entity.dto.UserLoginDto;
import com.mhc.springbootclouddisk.entity.dto.UserSpaceDto;
import com.mhc.springbootclouddisk.service.EmailCodeService;
import com.mhc.springbootclouddisk.service.UserInfoService;
import com.wf.captcha.SpecCaptcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.wf.captcha.base.Captcha.TYPE_ONLY_NUMBER;

/**
 * 用户登录
 */
@Slf4j
@RestController
@Validated
public class LoginController {

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private UserInfoService userInfoService;

    /**
     * 生成登录页面验证码和发送邮箱验证码
     */
    @GetMapping("checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, @RequestParam(value = "type", required = false) Integer type) throws IOException {
        response.setContentType("image/png");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 38, 4);
        specCaptcha.setCharType(TYPE_ONLY_NUMBER);
        String code = specCaptcha.text();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE, code);
            log.info("登录注册页面生成验证码：{}", code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_EMAIL, code);
            log.info("发送邮箱验证码页面生成验证码：{}", code);
        }
        specCaptcha.out(response.getOutputStream());
    }

    @PostMapping("sendEmailCode")
    public CloudDiskResult sendEmailCode(
            @RequestParam("email") @Email @NotBlank(message = "邮箱不能为空") @Length(max = 50) String email,
            @RequestParam("checkCode") String checkCode,
            @RequestParam("type") Integer type, HttpSession session) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_EMAIL))) {
                throw new ServerException("图片验证码不正确");
            }
            emailCodeService.sendEmailCode(email, type);
            return CloudDiskResult.success();
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_EMAIL);
        }
    }

    @PostMapping("register")
    public CloudDiskResult register(
            @RequestParam("email") @Email @NotBlank(message = "邮箱不能为空") @Length(max = 50) String email,
            @RequestParam("emailCode") String emailCode,
            @RequestParam("nickName") String nickName,
            @RequestParam("password") String password,
            @RequestParam("checkCode") String checkCode,
            HttpSession session) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE))) {
                throw new ServerException("图片验证码不正确");
            }
            userInfoService.register(email, emailCode, nickName, password);
            return CloudDiskResult.success();
        } finally {
            session.removeAttribute(Constants.CHECK_CODE);
        }
    }

    @PostMapping("login")
    public CloudDiskResult login(
            @RequestParam("email") @Email @NotBlank(message = "邮箱不能为空") @Length(max = 50) String email,
            @RequestParam("password") String password,
            @RequestParam("checkCode") String checkCode,
            HttpSession session, HttpServletResponse response) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE))) {
                throw new ServerException("图片验证码不正确");
            }
            UserLoginDto userLoginDto = userInfoService.login(email, password, response);
            return CloudDiskResult.success(userLoginDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE);
        }
    }

    @PostMapping("resetPwd")
    public CloudDiskResult resetPwd(
            @RequestParam("email") @Email @NotBlank(message = "邮箱不能为空") @Length(max = 50) String email,
            @RequestParam("password")  String password,
            @RequestParam("checkCode") String checkCode,
            @RequestParam("emailCode") String emailCode,
            HttpSession session) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE))) {
                throw new ServerException("图片验证码不正确");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return CloudDiskResult.success();
        } finally {
            session.removeAttribute(Constants.CHECK_CODE);
        }
    }

    @GetMapping("avatar")
    public CloudDiskResult avatar(@RequestParam("userId") String userId, HttpServletResponse response) {
        response.setContentType("image/png");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        String url = userInfoService.avatar(userId);
        return CloudDiskResult.success(url);
    }

    @GetMapping("getAvatar/{userId}")
    public CloudDiskResult getAvatar(@PathVariable("userId") String userId, HttpServletResponse response) {
        response.setContentType("image/png");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        String url = userInfoService.avatar(userId);
        return CloudDiskResult.success(url);
    }

    @PostMapping("getUseSpace")
    public CloudDiskResult getUseSpace(@CookieValue(name = "Authorization", required = false) String jwt,
                                       HttpServletResponse response, HttpSession session) {
        UserSpaceDto userSpaceDto = userInfoService.getUseSpace(jwt,response,session);
        log.info("获取用户空间信息userSpaceDto对象成功");
        return CloudDiskResult.success(userSpaceDto);
    }

    @PostMapping("logout")
    public CloudDiskResult logout(HttpServletResponse response, HttpSession session) {
        userInfoService.logout(response,session);
        return CloudDiskResult.success();
    }

    @PostMapping("updateUserAvatar")
    public CloudDiskResult updateUserAvatar(@RequestParam("avatar") MultipartFile avatar, HttpServletResponse response, @CookieValue(name = "Authorization", required = false) String jwt) {
        userInfoService.updateUserAvatar(avatar, response, jwt);
        log.info("成功更新用户头像");
        return CloudDiskResult.success();
    }

    @PostMapping("updatePassword")
    public CloudDiskResult updatePassword(@RequestParam("password") @Length(min = 8, max = 18) String password,
                                          @CookieValue(name = "Authorization", required = false) String jwt,
                                          HttpServletResponse response, HttpSession session) {
        userInfoService.updatePassword(password, jwt);
        log.info("用户执行修改密码成功");
        userInfoService.logout(response,session);
        return CloudDiskResult.success();
    }
}
