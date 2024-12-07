package com.mhc.springbootclouddisk.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.UserSpaceDto;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import com.mhc.springbootclouddisk.mapper.FileInfoMapper;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.service.RecycleInfoService;
import com.mhc.springbootclouddisk.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mhc.springbootclouddisk.service.impl.FileInfoServiceImpl.getLoadDataListVo;

@Service
@Slf4j
public class RecycleInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements RecycleInfoService {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public LoadDataListVo loadRecycleList(Page<FileInfo> fileInfoPage, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        List<FileInfo> list = lambdaQuery().eq(FileInfo::getUserId, userId).isNotNull(FileInfo::getRecoveryTime).list(fileInfoPage);
        return getLoadDataListVo(fileInfoPage, list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFile(String fileIds, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        String[] splitFileId = fileIds.split(",");
        List<String> fileList = Arrays.asList(splitFileId);
        recoverAllFile(fileList, userId);
        log.info("成功清空文件id为【{}】的进入回收站时间，文件成功移出回收站", fileList);
    }

    private void recoverAllFile(List<String> fileList, String userId) {
        for (String fileId : fileList) {
            FileInfo fileInfo = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId).one();
            if (fileInfo != null) {
                if (!fileInfo.getFilePid().equals("0")) {
                    Long count = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileInfo.getFilePid()).isNull(FileInfo::getRecoveryTime).count();
                    if (count == 0) {
                        String fileName = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileInfo.getFilePid()).one().getFileName();
                        log.info("该文件所在的文件夹【{}】不存在或已被删除，请先恢复文件夹", fileName);
                        throw new ServerException("该文件所在的文件夹不存在或已被删除，请先恢复文件夹：" + fileName);
                    }
                }
                lambdaUpdate().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileInfo.getFileId()).set(FileInfo::getRecoveryTime, null).update();
                log.info("已将文件【{}】从回收站移出", fileInfo.getFileId());
                if (fileInfo.getFolderType() == (short) 1) {
                    List<FileInfo> list = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, fileInfo.getFileId()).list();
                    recoverAllFile(list.stream().map(FileInfo::getFileId).collect(Collectors.toList()), userId);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFile(String fileIds, String token, HttpServletResponse response) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        Long useSpace = claims.get("useSpace", Long.class);
        Long totalSpace = claims.get("totalSpace", Long.class);
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_DELETE_FILE_USE_SPACE);
        if (userSpaceDto == null) {
            userSpaceDto = new UserSpaceDto();
            userSpaceDto.setUseSpace(useSpace);
            userSpaceDto.setTotalSpace(totalSpace);
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_DELETE_FILE_USE_SPACE, userSpaceDto, Duration.ofMinutes(1));
        }
        String[] splitFileId = fileIds.split(",");
        List<String> fileList = Arrays.asList(splitFileId);
        delAllFile(fileList, userId);

        log.info("成功永久删除文件id为【{}】的文件", fileList);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                UserInfo user = userInfoMapper.selectById(userId);
                log.info("删除文件-从数据库中获取用户信息成功");
                UserSpaceDto userSpaceDto = (UserSpaceDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_DELETE_FILE_USE_SPACE);
                if (userSpaceDto != null && Objects.equals(userSpaceDto.getTotalSpace(), user.getTotalSpace()) && userSpaceDto.getUseSpace() >= 0) {
                    user.setUseSpace(userSpaceDto.getUseSpace());
                    log.info("删除文件-更新用户空间：{}/{}", userSpaceDto.getUseSpace(), user.getTotalSpace());
                } else {
                    log.info("用户总空间校验异常，cookie数据与数据库TotalSpace不一致，或者使用空间为负数");
                    throw new ServerException("用户总空间校验异常，cookie数据与数据库TotalSpace不一致，或者使用空间为负数");
                }
                userInfoMapper.updateById(user);
                log.info("删除文件-更新用户使用空间信息到数据库成功");
                jwtUtils.updateToken(response, user);
                log.info("删除文件-更新用户Token成功");
            }
        });

    }

    private void delAllFile(List<String> fileList, String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_DELETE_FILE_USE_SPACE);
        for (String fileId : fileList) {
            FileInfo fileInfo = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId).one();
            if (fileInfo != null && userSpaceDto != null) {
                userSpaceDto.setUseSpace(userSpaceDto.getUseSpace() - fileInfo.getFileSize());
                log.info("用户使用空间：{}-{}=={}", userSpaceDto.getUseSpace() + fileInfo.getFileSize(), fileInfo.getFileSize(), userSpaceDto.getUseSpace());
                redisTemplate.opsForValue().set(Constants.REDIS_KEY_DELETE_FILE_USE_SPACE, userSpaceDto, Duration.ofMinutes(1));
                lambdaUpdate().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileInfo.getFileId()).isNotNull(FileInfo::getRecoveryTime).set(FileInfo::getRecoveryTime, LocalDateTime.now()).set(FileInfo::getDelFlat, (short) 1).update();
                log.info("已将文件【{}】永久删除", fileInfo.getFileId());
                if (fileInfo.getFolderType() == (short) 1) {
                    List<FileInfo> list = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, fileInfo.getFileId()).isNotNull(FileInfo::getRecoveryTime).list();
                    delAllFile(list.stream().map(FileInfo::getFileId).collect(Collectors.toList()), userId);
                }
            }
        }
    }
}
