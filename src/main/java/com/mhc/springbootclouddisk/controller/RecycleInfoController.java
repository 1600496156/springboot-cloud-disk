package com.mhc.springbootclouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mhc.springbootclouddisk.common.response.CloudDiskResult;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import com.mhc.springbootclouddisk.service.FileInfoService;
import com.mhc.springbootclouddisk.service.RecycleInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("recycle")
@Slf4j
public class RecycleInfoController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RecycleInfoService recycleInfoService;

    @PostMapping("loadRecycleList")
    public CloudDiskResult loadRecycleList(@RequestParam(value = "pageNo",defaultValue = "1")Long pageNo, @RequestParam(value = "pageSize",defaultValue = "15")Long pageSize, @CookieValue(name = "Authorization",required = false)String token){
        Page<FileInfo> fileInfoPage = fileInfoService.loadDataListPage(pageNo, pageSize);
        LoadDataListVo loadDataListVo = recycleInfoService.loadRecycleList(fileInfoPage,token);
        return CloudDiskResult.success(loadDataListVo);
    }

    @PostMapping("recoverFile")
    public CloudDiskResult recoverFile(@RequestParam("fileIds")String fileIds,@CookieValue(name = "Authorization",required = false)String token){
        recycleInfoService.recoverFile(fileIds,token);
        return CloudDiskResult.success();
    }

    @PostMapping("delFile")
    public CloudDiskResult delFile(@RequestParam("fileIds")String fileIds, @CookieValue(name = "Authorization",required = false)String token, HttpServletResponse response){
        recycleInfoService.delFile(fileIds,token,response);
        return CloudDiskResult.success();
    }
}
