package com.mhc.springbootclouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import com.mhc.springbootclouddisk.entity.domain.FileShare;
import com.mhc.springbootclouddisk.entity.vo.LoadShareListListVo;
import com.mhc.springbootclouddisk.entity.vo.LoadShareListVo;
import com.mhc.springbootclouddisk.service.FileShareService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("share")
@Slf4j
public class FileShareController {
    
    @Resource
    private FileShareService fileShareService;

    @PostMapping("loadShareList")
    public CloudDiskResult loadShareList(@RequestParam(value = "pageNo",defaultValue = "1")Long pageNo, @RequestParam(value = "pageSize",defaultValue = "15")Long pageSize, @CookieValue(name = "Authorization",required = false)String token){
        Page<FileShare> loadShareListPage = fileShareService.loadShareListPage(pageNo,pageSize);
        LoadShareListVo loadShareListVo = fileShareService.loadShareList(loadShareListPage,token);
        return CloudDiskResult.success(loadShareListVo);
    }

    @PostMapping("shareFile")
    public CloudDiskResult shareFile(@RequestParam("fileId")String fileId,@RequestParam("validType")Short validType,@RequestParam(value = "code",required = false)String code,@CookieValue(name = "Authorization", required = false) String token){
        if (code==null){
            code = RandomStringUtils.random(Constants.LENGTH_5, true, true).toUpperCase();
        }
        LoadShareListListVo loadShareListListVo = fileShareService.shareFile(fileId,validType,code,token);
        return CloudDiskResult.success(loadShareListListVo);
    }

    @PostMapping("cancelShare")
    public CloudDiskResult cancelShare(@RequestParam("shareIds")String shareIds,@CookieValue(name = "Authorization", required = false) String token){
        fileShareService.cancelShare(shareIds,token);
        return CloudDiskResult.success();
    }
}
