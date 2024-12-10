package com.mhc.springbootclouddisk.web.filter;

import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Resource
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response,@Nonnull FilterChain filterChain) throws ServletException, IOException {
        //获取Token
        String authorization = request.getHeader("Authorization");
        if (authorization == null|| !authorization.startsWith("Bearer-")) {
            doFilter(request, response, filterChain);
            return;
        }
        String token = Objects.requireNonNull(authorization).substring("Bearer-".length());
        Claims claims = jwtUtils.parseToken(token);
        String userId = claims.get("userId", String.class);
        String avatar = claims.get("avatar", String.class);
        String nickName = claims.get("nickName", String.class);
        Long useSpace = claims.get("useSpace", Long.class);
        Long totalSpace = claims.get("totalSpace", Long.class);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setQqAvatar(avatar);
        userInfo.setNickName(nickName);
        userInfo.setUseSpace(useSpace);
        userInfo.setTotalSpace(totalSpace);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userInfo, null, userInfo.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }
}
