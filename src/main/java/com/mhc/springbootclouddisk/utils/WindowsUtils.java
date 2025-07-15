package com.mhc.springbootclouddisk.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
@Slf4j
public class WindowsUtils {
    public static void CmdCommand(String cmd) {
        try {
            // 根据操作系统类型构建命令
            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows系统使用cmd.exe
                processBuilder = new ProcessBuilder("cmd.exe", "/c", cmd);
            } else {
                // Linux/Mac系统使用bash（回退到sh如果bash不可用）
                String shell = "/bin/bash";
                if (!new File(shell).exists()) {
                    shell = "/bin/sh";
                }
                processBuilder = new ProcessBuilder(shell, "-c", cmd);
            }

            // 合并错误流和输出流
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 根据操作系统选择字符编码
            String encoding = os.contains("win") ? "GBK" : "UTF-8";
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding));

            // 打印输出
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("命令执行结束，退出码: " + exitCode);

        } catch (IOException | InterruptedException e) {
            log.error("执行命令出错: {}", cmd, e);
            // 恢复中断状态
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
