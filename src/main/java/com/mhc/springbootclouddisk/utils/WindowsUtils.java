package com.mhc.springbootclouddisk.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
@Slf4j
public class WindowsUtils {
    public static void CmdCommand(String cmd) {
        try {
            // 调用CMD命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", cmd); // /c参数表示执行后关闭CMD窗口
            processBuilder.redirectErrorStream(true); // 将错误输出流与标准输出流合并
            Process process = processBuilder.start();
            // 获取命令输出结果
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // 等待命令执行完成
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.info(e.getMessage());
        }
    }
}
