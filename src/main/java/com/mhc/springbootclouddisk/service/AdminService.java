package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.vo.LoadUserDataListVo;

public interface AdminService extends IService<FileInfo> {
    LoadUserDataListVo loadUserList(Page<FileInfo> fileInfoPage, String nickNameFuzzy, String filePid);
}
