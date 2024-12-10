package com.mhc.springbootclouddisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.LoadFileDataListDto;
import com.mhc.springbootclouddisk.entity.dto.SendEmailCodeDto;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.LoadFileDataListVo;
import com.mhc.springbootclouddisk.entity.vo.LoadUserDataListVo;
import com.mhc.springbootclouddisk.mapper.FileInfoMapper;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.service.AdminService;
import com.mhc.springbootclouddisk.service.RecycleInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements AdminService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private RecycleInfoService recycleInfoService;

    @Override
    public LoadFileDataListVo loadFileList(Page<FileInfo> fileInfoPage, String fileNameFuzzy, String filePid) {
        // 校验filePid非空
        if (filePid == null || filePid.isEmpty()) {
            throw new IllegalArgumentException("filePid不能为空");
        }
        // 构建查询条件
        LambdaQueryChainWrapper<FileInfo> query = lambdaQuery();
        query.eq(FileInfo::getFilePid, filePid);
        if (fileNameFuzzy != null && !fileNameFuzzy.isEmpty()) {
            query.like(FileInfo::getFileName, fileNameFuzzy);
        }
        // 执行分页查询
        List<FileInfo> fileList = query.list(fileInfoPage);
        // 根据userId批量查询用户信息
        List<UserInfo> userList = userInfoMapper.selectByIds(fileList.stream().map(FileInfo::getUserId).collect(Collectors.toList()));
        // 组装返回对象
        return getLoadUserDataListVo(fileInfoPage, userList, fileList);
    }

    @Override
    public List<GetFolderInfoVo> getFolderInfo(String path) {
        List<String> pathList = Arrays.asList(path.split("/"));
        LambdaQueryWrapper<FileInfo> lambdaQueryWrapper = new LambdaQueryWrapper<FileInfo>().in(FileInfo::getFileId, pathList);
        List<FileInfo> folderInfo = fileInfoMapper.getFolderInfo(lambdaQueryWrapper, pathList);
        log.info("管理员点击列表：{}", folderInfo);
        List<GetFolderInfoVo> GetFolderInfoList = folderInfo.stream().map(item -> {
            GetFolderInfoVo vo = new GetFolderInfoVo();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList();
        log.info("根据管理员点击顺序，进行排序：{}", GetFolderInfoList);
        return GetFolderInfoList;
    }

    @Override
    public SendEmailCodeDto getSysSettings() {
        SendEmailCodeDto sendEmailDto = (SendEmailCodeDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_SEND_EMAIL_CODE_DTO);
        if (sendEmailDto == null) {
            SendEmailCodeDto sendEmailCodeDto = new SendEmailCodeDto();
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_SEND_EMAIL_CODE_DTO, sendEmailCodeDto);
            return sendEmailCodeDto;
        }
        return sendEmailDto;
    }

    @Override
    public void saveSysSettings(SendEmailCodeDto sendEmailCodeDto) {
        redisTemplate.opsForValue().set(Constants.REDIS_KEY_SEND_EMAIL_CODE_DTO, sendEmailCodeDto);
    }

    @Override
    public LoadUserDataListVo loadUserList(Page<UserInfo> loadUserListPage, String nickNameFuzzy, Short status) {
        List<UserInfo> userInfo = userInfoMapper.selectAllUser(nickNameFuzzy, status);
        LoadUserDataListVo loadUserDataListVo = new LoadUserDataListVo();
        loadUserDataListVo.setPageNo(loadUserListPage.getCurrent());
        loadUserDataListVo.setPageSize(loadUserListPage.getSize());
        loadUserDataListVo.setTotalCount(loadUserListPage.getTotal());
        loadUserDataListVo.setPageTotal(loadUserListPage.getPages());
        loadUserDataListVo.setList(userInfo);
        return loadUserDataListVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserStatus(String userId, Short status) {
        userInfoMapper.updateUserStatus(userId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserSpace(String userId, Long changeSpace) {
        long totalSpace = changeSpace * 1024L * 1024L;
        LambdaUpdateWrapper<UserInfo> lambdaUpdateWrapper = new LambdaUpdateWrapper<UserInfo>().eq(UserInfo::getUserId, userId).set(UserInfo::getTotalSpace, totalSpace);
        userInfoMapper.update(lambdaUpdateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delFile(String fileIdAndUserIds) {
        // 将参数解析为文件和用户ID的列表
        String[] fileUserPairs = fileIdAndUserIds.split(",");
        HashSet<String> userList = new HashSet<>();
        for (String pair : fileUserPairs) {
            String[] ids = pair.split("_");
            if (ids.length != 2) {
                log.warn("无效的参数格式：{}", pair);
                continue;
            }
            String userId = ids[0];
            String fileId = ids[1];
            userList.add(userId);
            // 获取文件信息
            FileInfo fileInfo = lambdaQuery()
                    .eq(FileInfo::getUserId, userId)
                    .eq(FileInfo::getFileId, fileId)
                    .one();
            if (fileInfo == null) {
                log.warn("文件不存在或已被删除：userId={}, fileId={}", userId, fileId);
                continue;
            }
            // 删除文件并更新用户空间
            deleteFileRecursively(fileInfo, userId);
        }
        for (String user : userList) {
            UserInfo userInfo = userInfoMapper.selectById(user);
            Long useSpace = userInfo.getUseSpace();
            userInfo.setUseSpace(useSpace - Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue().get(Constants.REDIS_KEY_ADMIN_DELETE_USER_FILE_SPACE + user)).toString()));
            userInfoMapper.updateById(userInfo);
            log.info("数据库更新用户：{}数据成功", userInfo);
            redisTemplate.delete(Constants.REDIS_KEY_ADMIN_DELETE_USER_FILE_SPACE + user);
            log.info("删除用户：{}的缓存信息", userInfo);

        }
    }

    private void deleteFileRecursively(FileInfo fileInfo, String userId) {
        // 更新使用空间
        Long fileSize = fileInfo.getFileSize();
        Object useSpaceTotalObject = redisTemplate.opsForValue().get(Constants.REDIS_KEY_ADMIN_DELETE_USER_FILE_SPACE + userId);
        if (useSpaceTotalObject == null) {
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_ADMIN_DELETE_USER_FILE_SPACE + userId, fileSize);
        } else {
            long redisReduceSpace = Long.parseLong(useSpaceTotalObject.toString());
            long redisReduceSpaceTotal = redisReduceSpace + fileSize;
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_ADMIN_DELETE_USER_FILE_SPACE + userId, redisReduceSpaceTotal);
            log.info("用户使用空间redis更新成功：userId={}, newSpace={}", userId, redisReduceSpaceTotal);
        }
        // 标记文件删除
        lambdaUpdate()
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFileId, fileInfo.getFileId())
                .set(FileInfo::getRecoveryTime, LocalDateTime.now())
                .set(FileInfo::getDelFlat, (short) 1)
                .update();
        log.info("文件标记删除：fileId={}, userId={}", fileInfo.getFileId(), userId);
        // 如果是文件夹，递归删除其内容
        if (fileInfo.getFolderType() == (short) 1) {
            List<FileInfo> subFiles = lambdaQuery()
                    .eq(FileInfo::getUserId, userId)
                    .eq(FileInfo::getFilePid, fileInfo.getFileId())
                    .list();
            for (FileInfo subFile : subFiles) {
                deleteFileRecursively(subFile, userId);
            }
        }
    }

    private LoadFileDataListVo getLoadUserDataListVo(Page<FileInfo> loadDataListPage, List<UserInfo> userList, List<FileInfo> fileList) {
        LoadFileDataListVo loadFileDataListVo = new LoadFileDataListVo();
        loadFileDataListVo.setPageNo(loadDataListPage.getCurrent());
        loadFileDataListVo.setPageSize(loadDataListPage.getSize());
        loadFileDataListVo.setTotalCount(loadDataListPage.getTotal());
        loadFileDataListVo.setPageTotal(loadDataListPage.getPages());
        // 创建一个Map来根据userId快速查找UserInfo
        Map<String, UserInfo> userInfoMap = userList.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, user -> user));
        // 遍历fileList，为每个文件信息匹配对应的用户信息，并创建LoadUserDataListDto对象
        List<LoadFileDataListDto> loadFileDataListDtoList = fileList.stream().map(fileInfo -> {
            LoadFileDataListDto loadFileDataListDto = new LoadFileDataListDto();
            // 使用BeanUtils将FileInfo的属性复制到LoadUserDataListDto中
            BeanUtils.copyProperties(fileInfo, loadFileDataListDto);
            // 从userInfoMap中根据userId获取UserInfo，并将属性复制到LoadUserDataListDto中
            UserInfo userInfo = userInfoMap.get(fileInfo.getUserId());
            if (userInfo != null) {
                BeanUtils.copyProperties(userInfo, loadFileDataListDto, "status", "password", "userId"); // 假设我们不想复制密码字段
            }
            return loadFileDataListDto;
        }).collect(Collectors.toList());
        // 将组装好的LoadUserDataListDto列表设置到LoadUserDataListVo中
        loadFileDataListVo.setList(loadFileDataListDtoList);
        return loadFileDataListVo;
    }
}
