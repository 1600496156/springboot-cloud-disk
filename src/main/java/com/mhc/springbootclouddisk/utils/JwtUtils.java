package com.mhc.springbootclouddisk.utils;

import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class JwtUtils {

    private final SecretKey key=Jwts.SIG.HS256.key().build();

    public String createToken(Map<String, Object> claims) {
        String token = Jwts.builder().claims(claims).signWith(key).issuedAt(new Date()).expiration(new Date(new Date().getTime() + 1000 * 60 * 60)).compact();
        log.info("创建Token成功：{}", token);
        return token;
    }

    public Claims parseToken(String token) {
        if (key==null){
            log.info("Token失效，请重新登录");
            throw new ServerException("Token失效，请重新登录");
        }
        Claims payload = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        log.info("解析Token：{} 成功!\n解析出信息：{}", token, payload);
        return payload;
    }

    public Claims getClaims(String jwt) {
        if (jwt == null || !jwt.startsWith("Bearer-")) {
            throw new ServerException("登录过期，请重新登录");
        }
        String token = Objects.requireNonNull(jwt).substring("Bearer-".length());
        return parseToken(token);
    }

    public void updateToken(HttpServletResponse response, UserInfo user) {
        HashMap<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("userId", user.getUserId());
        userInfoMap.put("nickName", user.getNickName());
        userInfoMap.put("avatar", user.getQqAvatar());
        userInfoMap.put("useSpace", user.getUseSpace());
        userInfoMap.put("totalSpace", user.getTotalSpace());
        String token = createToken(userInfoMap);
        Cookie cookie = new Cookie("Authorization", "Bearer-" + token);
        cookie.setPath("/api");
        response.addCookie(cookie);
        log.info("设置Cookie-Authorization成功，已更新Token");
    }

}
