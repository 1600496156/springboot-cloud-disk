package com.mhc.springbootclouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.dto.LoadDataListDto;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.LoadAllFolderVo;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import com.mhc.springbootclouddisk.entity.dto.UploadFileDto;
import com.mhc.springbootclouddisk.entity.vo.UploadFileVo;
import com.mhc.springbootclouddisk.service.FileInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("file")
@Slf4j
public class FileInfoController {
    @Resource
    private FileInfoService fileInfoService;

    @PostMapping("loadDataList")
    public CloudDiskResult loadDataList(LoadDataListDto loadDataListDto, @CookieValue(name = "Authorization", required = false) String token,
                                        @RequestParam(defaultValue = "1") Long pageNo, @RequestParam(defaultValue = "15") Long pageSize,
                                        HttpServletResponse response, HttpSession session) {
        Page<FileInfo> loadDataListPage = fileInfoService.loadDataListPage(pageNo, pageSize);
        LoadDataListVo loadDataListVo = fileInfoService.loadDataList(loadDataListDto, loadDataListPage, token,response,session);
        return CloudDiskResult.success(loadDataListVo);
    }

    @PostMapping("uploadFile")
    public CloudDiskResult uploadFile(UploadFileDto uploadFileDto, @CookieValue(name = "Authorization", required = false) String token, HttpServletResponse response) {
        UploadFileVo uploadFileVo = fileInfoService.uploadFile(token, uploadFileDto, response);
        return CloudDiskResult.success(uploadFileVo);
    }

    @GetMapping("getImage/{imageFolder}/{imageName}")
    public void getImage(@PathVariable Integer imageFolder, @PathVariable String imageName, HttpServletResponse response) {
        fileInfoService.getImage(imageFolder, imageName, response);
    }

    @GetMapping("ts/getVideoInfo/{fileId}")
    public void tsGetVideoInfo(@PathVariable String fileId, HttpServletResponse response, @CookieValue(name = "Authorization", required = false) String token) {
        fileInfoService.tsGetVideoInfo(null, fileId, response, token, null);
    }

    @PostMapping("getFile/{fileId}")
    public void getFile(@PathVariable String fileId, HttpServletResponse response, @CookieValue(name = "Authorization", required = false) String token) {
        fileInfoService.tsGetVideoInfo(null, fileId, response, token, null);
    }

    @PostMapping("newFolder")
    public CloudDiskResult newFolder(@RequestParam("filePid") String filePid, @RequestParam("fileName") String fileName, @CookieValue(name = "Authorization", required = false) String token) {
        fileInfoService.newFolder(filePid, fileName, token);
        return CloudDiskResult.success();
    }

    @PostMapping("getFolderInfo")
    public CloudDiskResult getFolderInfo(@RequestParam("path") String path, @CookieValue(name = "Authorization", required = false) String token) {
        List<GetFolderInfoVo> folderInfo = fileInfoService.getFolderInfo(path, token);
        return CloudDiskResult.success(folderInfo);
    }

    @PostMapping("rename")
    public CloudDiskResult rename(@RequestParam("fileId") String fileId, @RequestParam("fileName") String fileName, @CookieValue(name = "Authorization", required = false) String token) {
        fileInfoService.rename(fileId, fileName, token);
        return CloudDiskResult.success();
    }

    @PostMapping("loadAllFolder")
    public CloudDiskResult loadAllFolder(@RequestParam("filePid") String filePid, String currentFileIds, @CookieValue(name = "Authorization", required = false) String token) {
        List<LoadAllFolderVo> allFolderList = fileInfoService.loadAllFolder(filePid, currentFileIds, token);
        return CloudDiskResult.success(allFolderList);
    }

    @PostMapping("changeFileFolder")
    public CloudDiskResult changeFileFolder(@RequestParam("fileIds") String fileIds, @RequestParam("filePid") String filePid, @CookieValue(name = "Authorization", required = false) String token) {
        fileInfoService.changeFileFolder(fileIds, filePid, token);
        return CloudDiskResult.success();
    }

    @PostMapping("delFile")
    public CloudDiskResult delFile(@RequestParam("fileIds") String fileIds, @CookieValue(name = "Authorization", required = false) String token) {
        fileInfoService.delFile(fileIds, token);
        return CloudDiskResult.success();
    }

    @PostMapping("createDownloadUrl/{fileId}")
    public CloudDiskResult createDownloadUrl(@PathVariable("fileId") String fileId, @CookieValue(name = "Authorization", required = false) String token) {
        String code = fileInfoService.createDownloadUrl(fileId, token, null, null);
        log.info("生成下载链接：{}", code);
        return CloudDiskResult.success(code);
    }

    @GetMapping("download/{code}")
    public void download(@PathVariable("code") String code, HttpServletResponse response) {
        fileInfoService.download(code, response);
        log.info("获取下载链接成功，开始下载");
    }
}
