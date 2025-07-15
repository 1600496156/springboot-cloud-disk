package com.mhc.springbootclouddisk.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyuncs.exceptions.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Objects;

@Component
@Slf4j
public class AliOSSUtils {
    private final String directory = "Avatar/";

    public String upload(MultipartFile file, String userId) {
        try (
                //获取文件上传输入流
                InputStream inputStream = file.getInputStream()
        ) {
            log.info("正在为用户：{} 配置用户上传的头像", userId);
            //获取文件全名（包括拓展名）
            String originalFilename = file.getOriginalFilename();
            //避免文件被覆盖
            String fileName = directory + userId + Objects.requireNonNull(originalFilename).substring(originalFilename.lastIndexOf("."));
            //上传文件到 OSS
            return putObjectToOOS(fileName, inputStream);
        } catch (IOException | ClientException e) {
            throw new RuntimeException("upload运行异常");
        }
    }

    public String defaultUpload(String userId) {
        final String filePath = "src/main/resources/static/default.jpg";
        try (
                //获取文件上传输入流
                InputStream inputStream = new FileInputStream(filePath)
        ) {
            log.info("正在为用户：{} 配置默认的头像", userId);
            File file = new File(filePath);
            //得到了扩展名.jpg
            String extendName = file.getName().substring(file.getName().lastIndexOf("."));
            //避免文件被覆盖
            String fileName = directory + userId + extendName;
            //上传文件到 OSS
            return putObjectToOOS(fileName, inputStream);
        } catch (ClientException | IOException e) {
            throw new RuntimeException("defaultUpload运行异常");
        }
    }

    private String putObjectToOOS(String fileName, InputStream inputStream) throws ClientException {
        String endpoint = "https://oss-cn-shenzhen.aliyuncs.com";
        String bucketName = "springboot-cloud-disk";
        //上传文件到 OSS
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        OSS ossClient = new OSSClientBuilder().build(endpoint, credentialsProvider);
        ossClient.putObject(bucketName, fileName, inputStream);
        //文件访问路径
        String url = endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + fileName;
        ossClient.shutdown();
        log.info("用户头像Url链接为：{}", url);
        return url;
    }
}
