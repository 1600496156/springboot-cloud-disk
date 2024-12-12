package com.mhc.springbootclouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.dto.LoadDataListDto;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.LoadAllFolderVo;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import com.mhc.springbootclouddisk.entity.dto.UploadFileDto;
import com.mhc.springbootclouddisk.entity.vo.UploadFileVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface FileInfoService extends IService<FileInfo> {

    Page<FileInfo> loadDataListPage(Long pageNo, Long pageSize);

    LoadDataListVo loadDataList(LoadDataListDto loadDataListDto, Page<FileInfo> loadDataListPage, String token,HttpServletResponse response, HttpSession session);

    UploadFileVo uploadFile(String token, UploadFileDto uploadFileDto, HttpServletResponse response);

    void getImage(Integer imageFolder, String imageName, HttpServletResponse response);

    void tsGetVideoInfo(String userId, String fileId, HttpServletResponse response, String token, String shareId);

    void newFolder(String filePid, String fileName, String token);

    List<GetFolderInfoVo> getFolderInfo(String path, String token);

    void rename(String fileId, String fileName, String token);

    List<LoadAllFolderVo> loadAllFolder(String filePid, String currentFileIds, String token);

    void changeFileFolder(String fileIds, String filePid, String token);

    void delFile(String fileIds, String token);

    String createDownloadUrl(String fileId, String token, String shareId, String userId);

    void download(String code, HttpServletResponse response);
}
