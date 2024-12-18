package com.mhc.springbootclouddisk.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.FileShare;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.GetShareLoginInfoDto;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import com.mhc.springbootclouddisk.mapper.FileInfoMapper;
import com.mhc.springbootclouddisk.mapper.FileShareMapper;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.service.FileInfoService;
import com.mhc.springbootclouddisk.service.ShowShareService;
import com.mhc.springbootclouddisk.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static com.mhc.springbootclouddisk.utils.FileUtils.createCover;
import static com.mhc.springbootclouddisk.utils.FileUtils.videoCut;

@Service
@Slf4j
public class ShowShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements ShowShareService {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public GetShareLoginInfoDto getShareLoginInfo(String shareId, String token, String sharingCode) {
        GetShareLoginInfoDto getShareLoginInfoDto = new GetShareLoginInfoDto();
        FileShare fileShare = lambdaQuery().eq(FileShare::getShareId, shareId).one();
        if (fileShare == null) {
            log.info("获取不到用户分享的文件信息");
            throw new ServerException("获取不到用户分享的文件信息");
        }
        getShareLoginInfoDto.setShareTime(fileShare.getShareTime());
        getShareLoginInfoDto.setExpireTime(fileShare.getExpireTime());
        FileInfo fileInfo = fileInfoMapper.selectById(fileShare.getFileId());
        getShareLoginInfoDto.setFileName(fileInfo.getFileName());
        getShareLoginInfoDto.setFileId(fileInfo.getFileId());
        UserInfo userInfo = userInfoMapper.selectById(fileShare.getUserId());
        getShareLoginInfoDto.setNickName(userInfo.getNickName());
        getShareLoginInfoDto.setUserId(userInfo.getUserId());
        Claims claims;
        try {
            claims = jwtUtils.getClaims(token);
        } catch (RuntimeException e) {
            try {
                log.info("获取不到用户token，开始验证分享码({})", e.getMessage());
                claims = jwtUtils.parseToken(sharingCode);
            } catch (Exception ex) {
                log.info("获取不到用户分享码，开始跳转输入分享码页面({})", e.getMessage());
                return null;
            }
        }
        try {
            Claims sharingCodeClaims = jwtUtils.parseToken(sharingCode);
            String ShareCode = sharingCodeClaims.get("sharingCode", String.class);
            if (!ShareCode.equals(fileShare.getCode())) {
                log.info("用户输入的分享码错误");
                return null;
            }
        } catch (RuntimeException e) {
            log.info("你没有输入分享码,{}", e.getMessage());
            return null;
        }
        if (!fileShare.getUserId().equals(claims.get("userId", String.class))) {
            log.info("访客访问文件共享Id：【{}】的文件", shareId);
            getShareLoginInfoDto.setCurrentUser(false);
            return getShareLoginInfoDto;
        } else {
            String avatar = claims.get("avatar", String.class);
            getShareLoginInfoDto.setAvatar(avatar);
            getShareLoginInfoDto.setCurrentUser(true);
        }
        return getShareLoginInfoDto;
    }

    @Override
    public GetShareLoginInfoDto getShareInfo(String shareId) {
        GetShareLoginInfoDto getShareLoginInfoDto = new GetShareLoginInfoDto();
        FileShare fileShare = lambdaQuery().eq(FileShare::getShareId, shareId).one();
        if (fileShare == null) {
            log.info("无法获取分享的文件信息");
            throw new ServerException("无法获取分享的文件信息");
        }
        getShareLoginInfoDto.setShareTime(fileShare.getShareTime());
        getShareLoginInfoDto.setExpireTime(fileShare.getExpireTime());
        FileInfo fileInfo = fileInfoMapper.selectById(fileShare.getFileId());
        getShareLoginInfoDto.setFileName(fileInfo.getFileName());
        getShareLoginInfoDto.setCurrentUser(null);
        getShareLoginInfoDto.setFileId(fileInfo.getFileId());
        UserInfo userInfo = userInfoMapper.selectById(fileShare.getUserId());
        getShareLoginInfoDto.setAvatar(userInfo.getAvatar());
        getShareLoginInfoDto.setNickName(userInfo.getNickName());
        getShareLoginInfoDto.setUserId(userInfo.getUserId());
        return getShareLoginInfoDto;
    }

    @Override
    public void checkShareCode(String shareId, String code, HttpServletResponse response) {
        FileShare fileShare = lambdaQuery().eq(FileShare::getShareId, shareId).eq(FileShare::getCode, code).one();
        if (fileShare == null) {
            log.info("用户输入的分享码：{}不正确，验证失败", code);
            throw new ServerException("你输入的分享码：" + code + " 不正确，验证失败");
        }
        log.info("验证通过，分享码通过，获取到文件分享信息：{}", fileShare);
        lambdaUpdate().eq(FileShare::getShareId, shareId).eq(FileShare::getCode, code).set(FileShare::getShowCount, fileShare.getShowCount() + 1).update();
        HashMap<String, Object> sharingCode = new HashMap<>();
        sharingCode.put("sharingCode", code);
        sharingCode.put("visitor", "visitor");
        sharingCode.put("avatar", "https://springboot-cloud-disk.oss-cn-shenzhen.aliyuncs.com/Avatar/default.jpg");
        String token = jwtUtils.createToken(sharingCode);
        Cookie cookie = new Cookie("sharingCode", token);
        cookie.setPath("/api");
        response.addCookie(cookie);
        log.info("设置Cookie-sharingCode成功，访客已更新sharingCode-Token");
    }

    @Override
    public Page<FileInfo> loadFileListPage(Long pageNo, Long pageSize) {
        Page<FileInfo> page = Page.of(pageNo, pageSize);
        page.addOrder(OrderItem.desc("update_time"));
        return fileInfoService.page(page);
    }

    @Override
    public LoadDataListVo loadFileList(Page<FileInfo> loadFileListPage, String shareId, String filePid) {
        FileShare fileShare = lambdaQuery().eq(FileShare::getShareId, shareId).one();
        FileInfo fileInfo = fileInfoMapper.selectById(fileShare.getFileId());
        if (filePid.equals("0")) {
            List<FileInfo> list = new ArrayList<>();
            list.add(fileInfo);
            LoadDataListVo loadDataListVo = new LoadDataListVo();
            loadDataListVo.setPageNo(loadFileListPage.getCurrent());
            loadDataListVo.setPageSize(loadFileListPage.getSize());
            loadDataListVo.setTotalCount(loadFileListPage.getCurrent());
            loadDataListVo.setPageTotal(1L);
            loadDataListVo.setList(list);
            return loadDataListVo;
        } else {
            List<FileInfo> list = fileInfoService.lambdaQuery().eq(FileInfo::getFilePid, filePid).eq(FileInfo::getUserId, fileShare.getUserId()).list(loadFileListPage);
            return FileInfoServiceImpl.getLoadDataListVo(loadFileListPage, list);
        }
    }

    @Override
    public List<GetFolderInfoVo> getFolderInfo(String shareId, String path) {
        FileShare fileShare = lambdaQuery().eq(FileShare::getShareId, shareId).one();
        String userId = fileShare.getUserId();
        List<String> pathList = Arrays.asList(path.split("/"));
        LambdaQueryWrapper<FileInfo> pathWrapper = new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, pathList);
        List<GetFolderInfoVo> GetFolderInfoList = fileInfoMapper.getFolderInfo(pathWrapper, pathList).stream().map(item -> {
            GetFolderInfoVo vo = new GetFolderInfoVo();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList();
        log.info("[分享]获取目录信息根据用户点击顺序，进行排序：{}", GetFolderInfoList);
        return GetFolderInfoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShare(String shareId, String shareFileIds, String myFolderId, String token) {
        FileShare fileShare = lambdaQuery().eq(FileShare::getShareId, shareId).one();
        List<String> shareFileList = Arrays.asList(shareFileIds.split(","));
        Claims claims = jwtUtils.getClaims(token);
        String newUserId = claims.get("userId", String.class);
        if (newUserId == null) {
            log.info("访客无法保存到我的云盘,请先进行登录后保存");
            throw new ServerException("访客无法保存到我的云盘,请先进行登录后保存");
        }
        // 创建用户文件夹
        createUserDirectories(newUserId);
        Map<String, String> fileIdMap = new HashMap<>();
        updateAllFileUser(shareFileList, fileShare.getUserId(), newUserId, myFolderId, fileIdMap);
        UserInfo userInfo = userInfoMapper.selectById(newUserId);
        long useSpace = userInfo.getUseSpace() + Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue().get(Constants.REDIS_CHUNK_SAVE_SHARE_SIZES + newUserId)).toString());
        if (useSpace > userInfo.getTotalSpace()) {
            log.info("用户保存文件到我的云盘失败，云盘存储空间不足");
            throw new ServerException("保存文件到我的云盘失败，你的云盘存储空间不足");
        }
        userInfo.setUseSpace(useSpace);
        userInfoMapper.updateById(userInfo);
        redisTemplate.delete(Constants.REDIS_CHUNK_SAVE_SHARE_SIZES + newUserId);
        log.info("执行saveShare成功");
    }

    private void createUserDirectories(String userId) {
        File userTargetFileDir = new File("file_target" + File.separator + userId);
        if (!userTargetFileDir.exists()) {
            boolean targetDir = userTargetFileDir.mkdirs();
            log.info("创建用户转换文件夹状态：{}", targetDir);
        }
        File userFileCoverDir = new File("file_target_cover" + File.separator + userId);
        if (!userFileCoverDir.exists()) {
            boolean userFileCoverDirs = userFileCoverDir.mkdirs();
            log.info("用户封面文件夹创建状态：{}", userFileCoverDirs);
        }
        File userFileCutDir = new File("file_target_cut" + File.separator + userId);
        if (!userFileCutDir.exists()) {
            boolean userFileCutDirs = userFileCutDir.mkdirs();
            log.info("用户视频切片文件夹创建状态：{}", userFileCutDirs);
        }
    }

    private void updateAllFileUser(List<String> fileIdList, String oldUserId, String newUserId, String newParentFolderId, Map<String, String> fileIdMap) {
        for (String fileId : fileIdList) {
            FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, fileId).eq(FileInfo::getUserId, oldUserId));
            if (fileInfo == null) {
                log.error("updateAllFileUser出现异常，查询不到fileInfo文件信息");
                throw new ServerException("updateAllFileUser出现异常，查询不到fileInfo文件信息");
            }
            // 检查同级目录中是否存在相同名字的文件或文件夹
            if (existsSameNameFile(newUserId, newParentFolderId, fileInfo.getFileName())) {
                log.error("同级目录已存在名为 {} 的文件或文件夹，保存失败", fileInfo.getFileName());
                throw new ServerException("同级目录已存在名为 " + fileInfo.getFileName() + " 的文件或文件夹，保存失败");
            }
            String newFileId = RandomUtil.randomString(Constants.LENGTH_10);
            fileIdMap.put(fileId, newFileId);
            fileInfo.setFileId(newFileId);
            fileInfo.setUserId(newUserId);
            fileInfo.setFilePid(fileIdMap.getOrDefault(fileInfo.getFilePid(), newParentFolderId)); // 更新父文件夹ID
            // 获取新文件路径
            String newFilePath = getUserFileTarget(newUserId, fileInfo.getFileName());
            // 将文件复制到新文件夹中
            if (fileInfo.getFolderType() == 0) { // 如果是文件才复制
                copyFileToUserDirectory(fileInfo.getFilePath(), newFilePath);
                fileInfo.setFilePath(newFilePath);
                if (fileInfo.getFileType() == 1 || fileInfo.getFileType() == 3) {
                    createCover(fileInfo.getFilePath(), getUserFileTargetCover(newUserId, fileInfo.getFileName()).getAbsolutePath());
                    log.info("封面创建成功，路径：{}", getUserFileTargetCover(newUserId, fileInfo.getFileName()).getAbsolutePath());
                    if (fileInfo.getFileType() == 1) {
                        videoCut(getUserFileTargetFile(newUserId, fileInfo.getFileName()).getAbsolutePath(), getUserFileTargetCut(newUserId), fileInfo.getFileName(), getUserFileTargetCut(newUserId, fileInfo.getFileName()).getAbsolutePath());
                        log.info("视频切片创建成功，路径：{}", getUserFileTargetCut(newUserId).getAbsolutePath());
                    }
                }
                Object chunkSizes = redisTemplate.opsForValue().get(Constants.REDIS_CHUNK_SAVE_SHARE_SIZES + newUserId);
                if (chunkSizes == null) {
                    redisTemplate.opsForValue().set(Constants.REDIS_CHUNK_SAVE_SHARE_SIZES + newUserId, fileInfo.getFileSize());
                } else {
                    redisTemplate.opsForValue().set(Constants.REDIS_CHUNK_SAVE_SHARE_SIZES + newUserId, Long.valueOf(Objects.requireNonNull(redisTemplate.opsForValue().get(Constants.REDIS_CHUNK_SAVE_SHARE_SIZES + newUserId)).toString() + fileInfo.getFileSize()));
                }
            }
            fileInfoMapper.insert(fileInfo);
            log.info("用户【{}】将文件【{}】保存到云盘成功！", newUserId, fileInfo.getFileId());
            // 如果是文件夹，递归处理其子文件
            if (fileInfo.getFolderType() == 1) {
                List<FileInfo> folderFiles = getFolderFile(oldUserId, fileId);
                updateAllFileUser(folderFiles.stream().map(FileInfo::getFileId).collect(Collectors.toList()), oldUserId, newUserId, newFileId, fileIdMap);
            }
        }
    }

    private boolean existsSameNameFile(String userId, String parentFolderId, String fileName) {
        Long count = fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getFilePid, parentFolderId)
                .eq(FileInfo::getFileName, fileName));
        return count > 0;
    }

    private List<FileInfo> getFolderFile(String userId, String parentFolderId) {
        return fileInfoMapper.selectList(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, parentFolderId));
    }

    private String getUserFileTarget(String userId, String fileName) {
        return "file_target" + File.separator + userId + File.separator + fileName;
    }

    private File getUserFileTargetFile(String userId, String fileName) {
        return new File("file_target" + File.separator + userId + File.separator + fileName);
    }

    private File getUserFileTargetCut(String userId) {
        return new File("file_target_cut" + File.separator + userId);
    }

    private File getUserFileTargetCut(String userId, String fileName) {
        return new File("file_target_cut" + File.separator + userId + File.separator + fileName.substring(0, fileName.lastIndexOf(".")));
    }

    private File getUserFileTargetCover(String userId, String fileName) {
        return new File("file_target_cover" + File.separator + userId + File.separator + "cover_" + fileName.substring(0, fileName.lastIndexOf(".")) + Constants.FILE_TYPE_PICTURE_JPG);
    }

    private void copyFileToUserDirectory(String oldFilePath, String newFilePath) {
        try {
            Files.copy(Paths.get(oldFilePath), Paths.get(newFilePath), StandardCopyOption.REPLACE_EXISTING);
            log.info("文件复制成功，从 {} 到 {}", oldFilePath, newFilePath);
        } catch (IOException e) {
            log.error("文件复制失败：{}", e.getMessage());
            throw new ServerException("文件复制失败：" + e.getMessage());
        }
    }
}
