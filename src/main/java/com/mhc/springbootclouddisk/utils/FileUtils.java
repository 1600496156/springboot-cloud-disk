package com.mhc.springbootclouddisk.utils;

import cn.hutool.core.io.FileUtil;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.dto.UploadFileDto;
import com.mhc.springbootclouddisk.mapper.FileInfoMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
@Slf4j
public class FileUtils {

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Async
    public void transferFile(UploadFileDto uploadFileDto, FileInfo fileInfo) {
        String userId = fileInfo.getUserId();
        File userTargetFileDir = new File("file_target" + File.separator + userId);
        if (!userTargetFileDir.exists()) {
            boolean targetDir = userTargetFileDir.mkdirs();
            log.info("创建用户转换文件夹状态：{}", targetDir);
        }
        File userTempFileDirs = new File("file" + File.separator + userId);
        File userTempFile = new File(userTempFileDirs.getPath() + File.separator + uploadFileDto.getFileName() + "_");
        File userFile = new File(userTargetFileDir.getPath() + File.separator + uploadFileDto.getFileName());
        File userFileCoverDir = new File("file_target_cover" + File.separator + userId);
        File userFileCover = new File(userFileCoverDir.getPath() + File.separator + "cover_" + FileUtil.mainName(uploadFileDto.getFileName()) + Constants.FILE_TYPE_PICTURE_JPG);
        File userFileCutDir = new File("file_target_cut" + File.separator + userId);
        File userFileCut = new File(userFileCutDir.getPath() + File.separator + uploadFileDto.getFileName().substring(0, uploadFileDto.getFileName().lastIndexOf(".")));

        byte[] bytes = new byte[1024 * 10];
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(userFile, "rw");
            for (int i = 0; i < uploadFileDto.getChunks(); i++) {
                try (
                        RandomAccessFile readFile = new RandomAccessFile(userTempFile.getPath() + i, "r")
                ) {
                    int len;
                    while ((len = readFile.read(bytes)) != -1) {
                        writeFile.write(bytes, 0, len);
                    }
                }
            }
            fileInfo.setFilePath(userFile.getAbsolutePath());
            if (fileInfo.getFileType() == 1 || fileInfo.getFileType() == 3) {
                if (!userFileCoverDir.exists()) {
                    boolean userFileCoverDirs = userFileCoverDir.mkdirs();
                    log.info("用户封面文件夹创建状态：{}", userFileCoverDirs);
                }
                createCover(fileInfo.getFilePath(), userFileCover.getAbsolutePath());
                log.info("封面创建成功，路径：{}", userFileCover.getAbsolutePath());
                String fileName = userFileCover.getName();
                fileInfo.setFileCover(fileInfo.getFileType() + File.separator + fileName);
                if (fileInfo.getFileType() == 1) {
                    if (!userFileCutDir.exists()) {
                        boolean userFileCutDirs = userFileCutDir.mkdirs();
                        log.info("用户视频切片文件夹创建状态：{}", userFileCutDirs);
                    }
                    videoCut(userFile.getAbsolutePath(), userFileCutDir, fileInfo.getFileName(), userFileCut.getAbsolutePath());
                    log.info("视频切片创建成功，路径：{}", userFileCutDir.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            fileInfo.setStatus(Constants.FILE_STATUS_TRANSCODING_FAILED);
            log.info("Id为【{}】的文件合并失败", uploadFileDto.getFileId());
            throw new ServerException("文件合并失败：" + e.getMessage());
        } finally {
            try {
                if (writeFile != null) {
                    writeFile.close();
                }
                deleteTempFile(uploadFileDto, userTempFileDirs);
                fileInfo.setStatus(Constants.FILE_STATUS_TRANSCODING_SUCCESS);
                fileInfoMapper.insert(fileInfo);
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
    }

    public void deleteTempFile(UploadFileDto uploadFileDto, File userTempFileDirs) {
        File[] listFiles = userTempFileDirs.listFiles();
        if (listFiles != null) {
            for (File listFile : listFiles) {
                log.info("需要删除的文件：{}", listFile.getName());
                log.info("判断前缀是否包含：{}", uploadFileDto.getFileName().substring(0, uploadFileDto.getFileName().lastIndexOf(".")));
                if (listFile.getName().contains(uploadFileDto.getFileName().substring(0, uploadFileDto.getFileName().lastIndexOf(".")))) {
                    boolean delete = listFile.delete();
                    log.info("对比通过！执行删除临时文件：{}的状态为：{}", listFile.getName(), delete);
                }
            }
        }
    }

    public static void createCover(String inputFile, String targetFile) {
        String cmd = "ffmpeg -i \"%s\" -y -vframes 1 -vf scale=1080:-1 \"%s\"";
        WindowsUtils.CmdCommand(String.format(cmd, inputFile, targetFile));
        log.info("执行Windows-Cmd命令成功");
    }

    // 输出文件
    public static void readFile(HttpServletResponse response, File file) {
        try (
                OutputStream out = response.getOutputStream();
                FileInputStream in = new FileInputStream(file.getAbsoluteFile())
        ) {
            byte[] byteData = new byte[1024];
            int len;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            log.info("文件：{}，输出cover封面成功", file.getName());
        } catch (Exception e) {
            log.error("读取文件异常", e);
        }
    }

    //创建切片
    public static void videoCut(String inputFile, File userFileCutDir, String fileName, String targetFile) {
        String cmd = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s_%%4d.ts";
        String filePrefixName = fileName.substring(0, fileName.lastIndexOf("."));
        WindowsUtils.CmdCommand(String.format(cmd, inputFile, userFileCutDir.getAbsoluteFile() + File.separator + filePrefixName + Constants.FILE_TYPE_M3U8, targetFile));
        log.info("执行视频切片Cmd命令成功");
    }
}
