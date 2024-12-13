package com.mhc.springbootclouddisk.entity.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@TableName("user_info")
public class UserInfo implements Serializable,UserDetails {
    @TableId(type = IdType.ASSIGN_UUID)
    private String userId;
    private String nickName;
    private String email;
    private String avatar;
    private String password;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastLoginTime;
    @TableLogic
    private Short status;
    private Long useSpace;
    private Long totalSpace;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.getStatus() != 1;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.getStatus() != 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.getStatus() != 1;
    }

    @Override
    public boolean isEnabled() {
        return this.getStatus() != 1;
    }
}
