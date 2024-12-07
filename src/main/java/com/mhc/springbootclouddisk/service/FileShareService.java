package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.FileShare;
import com.mhc.springbootclouddisk.entity.vo.LoadShareListListVo;
import com.mhc.springbootclouddisk.entity.vo.LoadShareListVo;

public interface FileShareService extends IService<FileShare> {

    Page<FileShare> loadShareListPage(Long pageNo, Long pageSize);

    LoadShareListVo loadShareList(Page<FileShare> loadShareListPage, String token);

    LoadShareListListVo shareFile(String fileId, Short validType,String code, String token);

    void cancelShare(String shareIds, String token);
}
