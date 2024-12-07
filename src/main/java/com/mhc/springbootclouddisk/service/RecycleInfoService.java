package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import jakarta.servlet.http.HttpServletResponse;

public interface RecycleInfoService extends IService<FileInfo> {
    LoadDataListVo loadRecycleList(Page<FileInfo> fileInfoPage, String token);

    void recoverFile(String fileIds, String token);

    void delFile(String fileIds, String token, HttpServletResponse response);
}
