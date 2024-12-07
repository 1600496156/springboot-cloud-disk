package com.mhc.springbootclouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.vo.LoadUserDataListVo;
import com.mhc.springbootclouddisk.service.AdminService;
import com.mhc.springbootclouddisk.service.FileInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin")
@Slf4j
public class AdminController {
    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private AdminService adminService;

    @PostMapping("loadFileList")
    public CloudDiskResult loadUserList(@RequestParam(value = "pageNo",defaultValue = "1") Long pageNo,
                                        @RequestParam(value = "pageSize",defaultValue = "15",required = false)Long pageSize,
                                        String nickNameFuzzy, String filePid){
        Page<FileInfo> fileInfoPage = fileInfoService.loadDataListPage(pageNo, pageSize);
        LoadUserDataListVo loadUserList = adminService.loadUserList(fileInfoPage,nickNameFuzzy,filePid);
        log.info("管理员正在查询所有用户文件");
        return CloudDiskResult.success(loadUserList);
    }
}
