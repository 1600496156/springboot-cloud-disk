package com.mhc.springbootclouddisk;

import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.utils.WindowsUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.*;
import java.time.Duration;
import java.util.Arrays;

@SpringBootTest
class SpringbootCloudDiskApplicationTests {

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set("test", "test", Duration.ofMinutes(1));
    }

    @Test
    void contextLoads2() {
        UserInfo user = new UserInfo();
        user.setNickName("121");
        user.setEmail("email");
        user.setPassword("password");
        user.setUseSpace(0L);
        user.setTotalSpace(100L*1024L*1024L);
        userInfoMapper.insert(user);
    }

    @Test
    void contextLoads3() {
        File file = new File("default.jpg");
        String extendName = file.getName().substring(file.getName().lastIndexOf("."));
        System.out.println(extendName);
    }

    @Test
    public void contextLoads4() {
        String cookie = "Bearer-eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IjE2MDA0OTYxNTYiLCJ1c2VTcGFjZSI6MCwiYXZhdGFyIjoiaHR0cHM6Ly9zcHJpbmdib290LWNsb3VkLWRpc2sub3NzLWNuLXNoZW56aGVuLmFsaXl1bmNzLmNvbS9BdmF0YXIvMjYwMTExZTJmMDk3M2VlOTk1ODhmYzNlN2YzNWE1NDkucG5nIiwidG90YWxTcGFjZSI6MTA0ODU3NjAwLCJ1c2VySWQiOiIyNjAxMTFlMmYwOTczZWU5OTU4OGZjM2U3ZjM1YTU0OSIsImlhdCI6MTcxNDc1NTM3NywiZXhwIjoxNzE0NzU4OTc3fQ.r536hJdi7flYrq6sp55DqnF0Nvzbi6vgWhrJgDjPwzM; userInfo=%7B%22nickName%22%3A%221600496156%22%2C%22userId%22%3A%22260111e2f0973ee99588fc3e7f35a549%22%2C%22avatar%22%3A%22https%3A%2F%2Fspringboot-cloud-disk.oss-cn-shenzhen.aliyuncs.com%2FAvatar%2F260111e2f0973ee99588fc3e7f35a549.png%22%2C%22admin%22%3Atrue%7D";
        System.out.println(cookie.substring(7));
    }

    @Test
    public void contextLoads5(){
        String substring = "sasdasd.mp4".substring("sasdasd.mp4".lastIndexOf("."));
        System.out.println(substring);
    }
    @Test
    public void contextLoads6(){
        WindowsUtils.CmdCommand("ipconfig");
    }
    @Test
    public void contextLoads7(){
        String path = "unRvr8LTZR/sadasdasdas";
        String[] split = path.split("/");
        System.out.println(Arrays.toString(split));
    }
}
