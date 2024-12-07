package com.mhc.springbootclouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.GetShareLoginInfoDto;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import com.mhc.springbootclouddisk.service.FileInfoService;
import com.mhc.springbootclouddisk.service.ShowShareService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("showShare")
@Slf4j
public class ShowShareController {

    @Resource
    private ShowShareService showShareService;

    @Resource
    private FileInfoService fileInfoService;

    @PostMapping("getShareLoginInfo")
    public CloudDiskResult getShareLoginInfo(@RequestParam("shareId") String shareId, @CookieValue(name = "Authorization", required = false) String token, @CookieValue(name = "sharingCode", required = false) String sharingCode) {
        GetShareLoginInfoDto getShareLoginInfoDto = showShareService.getShareLoginInfo(shareId, token, sharingCode);
        log.info("正在访问共享文件的用户信息：{}", getShareLoginInfoDto);
        return CloudDiskResult.success(getShareLoginInfoDto);
    }

    @PostMapping("getShareInfo")
    public CloudDiskResult getShareInfo(@RequestParam("shareId") String shareId) {
        GetShareLoginInfoDto getShareInfo = showShareService.getShareInfo(shareId);
        log.info("获取到共享信息：{}", getShareInfo);
        return CloudDiskResult.success(getShareInfo);
    }

    @PostMapping("checkShareCode")
    public CloudDiskResult checkShareCode(@RequestParam("shareId") String shareId, @RequestParam("code") @Length(min = 5, max = 5, message = "验证码长度应为5位数") String code, HttpServletResponse response) {
        log.info("开始验证分享码：{}，是否正确", code);
        showShareService.checkShareCode(shareId, code, response);
        log.info("分享码验证通过，正在获取文件列表");
        return CloudDiskResult.success();
    }

    @PostMapping("loadFileList")
    public CloudDiskResult loadFileList(@RequestParam(value = "pageNo", defaultValue = "1") Long pageNo, @RequestParam(value = "pageSize", defaultValue = "15") Long pageSize,
                                        @RequestParam("shareId") String shareId,
                                        @RequestParam("filePid") String filePid) {
        Page<FileInfo> loadFileListPage = showShareService.loadFileListPage(pageNo, pageSize);
        LoadDataListVo loadFileList = showShareService.loadFileList(loadFileListPage, shareId, filePid);
        return CloudDiskResult.success(loadFileList);
    }

    @PostMapping("getFolderInfo")
    public CloudDiskResult getFolderInfo(@RequestParam("shareId") String shareId, @RequestParam("path") String path) {
        List<GetFolderInfoVo> getFolderInfoVo = showShareService.getFolderInfo(shareId, path);
        log.info("获取目录信息成功");
        return CloudDiskResult.success(getFolderInfoVo);
    }

    @PostMapping("getFile/{shareId}/{fileId}")
    public void getFile(@PathVariable("shareId") String shareId, @PathVariable("fileId") String fileId, HttpServletResponse response) {
        fileInfoService.tsGetVideoInfo(fileId, response, null, shareId);
        log.info("[分享]getFile获取文件信息成功");
    }

    @GetMapping("ts/getVideoInfo/{shareId}/{fileId}")
    public void getVideoInfo(@PathVariable("shareId") String shareId, @PathVariable("fileId") String fileId, HttpServletResponse response) {
        fileInfoService.tsGetVideoInfo(fileId, response, null, shareId);
        log.info("[分享]getVideoInfo获取文件信息成功");
    }

    @PostMapping("createDownloadUrl/{shareId}/{fileId}")
    public CloudDiskResult createDownloadUrl(@PathVariable("shareId") String shareId, @PathVariable("fileId") String fileId) {
        String downloadUrl = fileInfoService.createDownloadUrl(fileId, null, shareId);
        log.info("生成下载链接：{}", downloadUrl);
        return CloudDiskResult.success(downloadUrl);
    }

    @GetMapping("download/{code}")
    public void download(@PathVariable("code") String code, HttpServletResponse response) {
        fileInfoService.download(code, response);
        log.info("获取下载链接成功，开始下载");
    }

    @PostMapping("saveShare")
    public CloudDiskResult saveShare(@RequestParam("shareId")String shareId,@RequestParam("shareFileIds")String shareFileIds,@RequestParam("myFolderId")String myFolderId,@CookieValue(name = "Authorization",required = false)String token) {
        showShareService.saveShare(shareId,shareFileIds,myFolderId,token);
        log.info("[分享]保存到我的网盘成功");
        return CloudDiskResult.success();
    }
}
