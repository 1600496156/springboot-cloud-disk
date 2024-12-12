package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.FileShare;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.GetShareLoginInfoDto;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ShowShareService extends IService<FileShare> {
    GetShareLoginInfoDto getShareLoginInfo(String shareId, String token, String sharingCode);

    GetShareLoginInfoDto getShareInfo(String shareId);

    void checkShareCode(String shareId, String code, HttpServletResponse response);

    Page<FileInfo> loadFileListPage(Long pageNo, Long pageSize);

    LoadDataListVo loadFileList(Page<FileInfo> loadFileListPage, String shareId, String filePid);

    List<GetFolderInfoVo> getFolderInfo(String shareId, String path);

    void saveShare(String shareId, String shareFileIds, String myFolderId, String token);
}
