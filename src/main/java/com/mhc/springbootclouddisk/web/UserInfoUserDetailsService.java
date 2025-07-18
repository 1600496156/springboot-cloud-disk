package com.mhc.springbootclouddisk.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserInfoUserDetailsService implements UserDetailsService {
    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<UserInfo> user = new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, username);
        UserInfo userInfo = userInfoMapper.selectOne(user);
        log.info("UserInfoUserDetailsService根据用户名：{}，查询到用户:{}", username,userInfo);
        if (userInfo == null) {
            throw new ServerException("登录失败，账号或者密码错误，若忘记密码请点击左下角进行重置密码");
        }
        return userInfo;
    }
}
