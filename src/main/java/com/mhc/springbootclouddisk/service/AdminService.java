package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.SendEmailCodeDto;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.LoadFileDataListVo;
import com.mhc.springbootclouddisk.entity.vo.LoadUserDataListVo;

import java.util.List;

public interface AdminService extends IService<FileInfo> {
    LoadFileDataListVo loadFileList(Page<FileInfo> fileInfoPage, String fileNameFuzzy, String filePid);

    List<GetFolderInfoVo> getFolderInfo(String path);

    SendEmailCodeDto getSysSettings();

    void saveSysSettings(SendEmailCodeDto sendEmailCodeDto);

    LoadUserDataListVo loadUserList(Page<UserInfo> loadUserListPage, String nickNameFuzzy, Short status);

    void updateUserStatus(String userId, Short status);

    void updateUserSpace(String userId, Long changeSpace);

    void delFile(String fileIdAndUserIds);
}
