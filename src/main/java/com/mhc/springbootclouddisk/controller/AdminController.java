package com.mhc.springbootclouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.SendEmailCodeDto;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.LoadFileDataListVo;
import com.mhc.springbootclouddisk.entity.vo.LoadUserDataListVo;
import com.mhc.springbootclouddisk.service.AdminService;
import com.mhc.springbootclouddisk.service.FileInfoService;
import com.mhc.springbootclouddisk.service.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin")
@Slf4j
public class AdminController {
    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private AdminService adminService;

    @Resource
    private UserInfoService userInfoService;

    @PostMapping("loadFileList")
    public CloudDiskResult loadFileList(@RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
                                        @RequestParam(value = "pageSize", defaultValue = "15", required = false) Long pageSize,
                                        String fileNameFuzzy, String filePid) {
        Page<FileInfo> fileInfoPage = fileInfoService.loadDataListPage(pageNo, pageSize);
        LoadFileDataListVo loadFileList = adminService.loadFileList(fileInfoPage, fileNameFuzzy, filePid);
        log.info("管理员正在查询所有用户文件");
        return CloudDiskResult.success(loadFileList);
    }

    @PostMapping("getFolderInfo")
    public CloudDiskResult getFolderInfo(@RequestParam("path") String path) {
        List<GetFolderInfoVo> getFolderInfoVo = adminService.getFolderInfo(path);
        log.info("管理员获取目录信息成功");
        return CloudDiskResult.success(getFolderInfoVo);
    }

    @PostMapping("getSysSettings")
    public CloudDiskResult getSysSettings() {
        SendEmailCodeDto sendEmailCodeDto = adminService.getSysSettings();
        log.info("管理员获取系统设置信息成功");
        return CloudDiskResult.success(sendEmailCodeDto);
    }

    @PostMapping("saveSysSettings")
    public CloudDiskResult saveSysSettings(@RequestParam("registerEmailTitle") String registerEmailTitle,
                                           @RequestParam("registerEmailContent") String registerEmailContent,
                                           @RequestParam("userInitUseSpace") Long userInitUseSpace) {
        SendEmailCodeDto sendEmailCodeDto = new SendEmailCodeDto();
        sendEmailCodeDto.setRegisterEmailTitle(registerEmailTitle);
        sendEmailCodeDto.setRegisterEmailContent(registerEmailContent);
        sendEmailCodeDto.setUserInitUseSpace(userInitUseSpace);
        adminService.saveSysSettings(sendEmailCodeDto);
        log.info("管理员修改系统设置信息成功：{}", sendEmailCodeDto);
        return CloudDiskResult.success();
    }

    @PostMapping("loadUserList")
    public CloudDiskResult loadUserList(@RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
                                        @RequestParam(value = "pageSize", defaultValue = "15", required = false) Long pageSize,
                                        String nickNameFuzzy, Short status) {
        Page<UserInfo> loadUserListPage = userInfoService.loadUserListPage(pageNo, pageSize);
        LoadUserDataListVo loadUserDataListVo = adminService.loadUserList(loadUserListPage, nickNameFuzzy, status);
        log.info("管理员正在查询所有用户信息");
        return CloudDiskResult.success(loadUserDataListVo);
    }

    @PostMapping("updateUserStatus")
    public CloudDiskResult updateUserStatus(@RequestParam("userId") String userId, @RequestParam("status") Short status) {
        adminService.updateUserStatus(userId, status);
        log.info("管理员修改用户账号：{}的状态为：{}", userId, status == 0 ? "启用" : "禁用");
        return CloudDiskResult.success();
    }

    @PostMapping("updateUserSpace")
    public CloudDiskResult updateUserSpace(@RequestParam("userId") String userId, @RequestParam("changeSpace") Long changeSpace) {
        adminService.updateUserSpace(userId, changeSpace);
        log.info("管理员修改用户账号：{}的云盘存储空间为：{}GB", userId, changeSpace / 1024L);
        return CloudDiskResult.success();
    }

    @PostMapping("createDownloadUrl/{userId}/{fileId}")
    public CloudDiskResult createDownloadUrl(@PathVariable("userId") String userId, @PathVariable("fileId") String fileId, @CookieValue(name = "Authorization", required = false) String token) {
        String code = fileInfoService.createDownloadUrl(fileId, token, null, userId);
        log.info("管理员生成下载链接：{}", code);
        return CloudDiskResult.success(code);
    }

    @GetMapping("download/{code}")
    public void download(@PathVariable("code") String code, HttpServletResponse response) {
        fileInfoService.download(code, response);
        log.info("管理员获取下载链接成功，开始下载");
    }

    @GetMapping("ts/getVideoInfo/{userId}/{fileId}")
    public void tsGetVideoInfo(@PathVariable(value = "userId") String userId, @PathVariable(value = "fileId") String fileId, HttpServletResponse response) {
        fileInfoService.tsGetVideoInfo(userId, fileId, response, null, null);
    }

    @PostMapping("getFile/{userId}/{fileId}")
    public void getFile(@PathVariable(value = "userId") String userId, @PathVariable(value = "fileId") String fileId, HttpServletResponse response) {
        fileInfoService.tsGetVideoInfo(userId, fileId, response, null, null);
    }

    @PostMapping("delFile")
    public CloudDiskResult delFile(@RequestParam("fileIdAndUserIds") String fileIdAndUserIds) {
        adminService.delFile(fileIdAndUserIds);
        log.info("管理员执行删除文件接成功：{}", fileIdAndUserIds);
        return CloudDiskResult.success();
    }
}
