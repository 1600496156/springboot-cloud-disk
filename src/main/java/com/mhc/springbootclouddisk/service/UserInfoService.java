package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.UserLoginDto;
import com.mhc.springbootclouddisk.entity.dto.UserSpaceDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

public interface UserInfoService extends IService<UserInfo> {
    void register(String email, String emailCode, String nickName, String password);

    UserLoginDto login(String email, String password, HttpServletResponse response);

    void resetPwd(String email, String password, String emailCode);

    String avatar(String userId);

    void updateUserAvatar(MultipartFile avatar,HttpServletResponse response,String jwt);

    UserSpaceDto getUseSpace(String jwt,HttpServletResponse response, HttpSession session);

    Page<UserInfo> loadUserListPage(Long pageNo, Long pageSize);

    void updatePassword(@Length(min = 8, max = 18) String password, String jwt);

    void logout(HttpServletResponse response, HttpSession session);
}
