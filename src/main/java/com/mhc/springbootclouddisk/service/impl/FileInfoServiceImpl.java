package com.mhc.springbootclouddisk.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.common.constants.Constants;
import com.mhc.springbootclouddisk.common.exception.ServerException;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.CreateDownloadUrlDto;
import com.mhc.springbootclouddisk.entity.dto.LoadDataListDto;
import com.mhc.springbootclouddisk.entity.dto.UserSpaceDto;
import com.mhc.springbootclouddisk.entity.vo.GetFolderInfoVo;
import com.mhc.springbootclouddisk.entity.vo.LoadAllFolderVo;
import com.mhc.springbootclouddisk.entity.vo.LoadDataListVo;
import com.mhc.springbootclouddisk.entity.dto.UploadFileDto;
import com.mhc.springbootclouddisk.entity.vo.UploadFileVo;
import com.mhc.springbootclouddisk.mapper.FileInfoMapper;
import com.mhc.springbootclouddisk.mapper.FileShareMapper;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.service.FileInfoService;
import com.mhc.springbootclouddisk.utils.FileUtils;
import com.mhc.springbootclouddisk.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private FileUtils fileUtils;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private FileShareMapper fileShareMapper;

    private final ReentrantLock lock = new ReentrantLock();

    /*
    使用分页和排序来加载文件数据列表，按更新时间排序。
     */
    @Override
    public Page<FileInfo> loadDataListPage(Long pageNo, Long pageSize) {
        //1.分页参数
        Page<FileInfo> page = Page.of(pageNo, pageSize);
        //2.排序参数
        page.addOrder(OrderItem.desc("update_time"));
        //3.开始分页查询
        log.info("开始分页查询");
        return this.page(page);
    }

    /*
    根据分类、文件名模糊匹配、父级文件夹等条件加载文件列表。
     */
    @Override
    public LoadDataListVo loadDataList(LoadDataListDto loadDataListDto, Page<FileInfo> loadDataListPage, String token) {
        String category = loadDataListDto.getCategory();
        short fileCategory = (short) (category.equals(Constants.CATEGORY_ALL) ? 0 : category.equals(Constants.CATEGORY_VIDEO) ? 1 : category.equals(Constants.CATEGORY_MUSIC) ? 2 : category.equals(Constants.CATEGORY_IMAGE) ? 3 : category.equals(Constants.CATEGORY_DOC) ? 4 : 5);
        Claims claims = jwtUtils.getClaims(token);
        String userId = (String) claims.get("userId");
        String fileNameFuzzy = loadDataListDto.getFileNameFuzzy();
        List<FileInfo> list;
        if (fileNameFuzzy == null || fileNameFuzzy.isEmpty()) {
            list = lambdaQuery().eq(!(fileCategory == 0), FileInfo::getFileCategory, fileCategory).eq(FileInfo::getUserId, userId).eq(fileCategory == 0, FileInfo::getFilePid, loadDataListDto.getFilePid()).isNull(FileInfo::getRecoveryTime).list(loadDataListPage);
        } else {
            list = lambdaQuery().eq(!(fileCategory == 0), FileInfo::getFileCategory, fileCategory).eq(FileInfo::getUserId, userId).eq(fileCategory == 0, FileInfo::getFilePid, loadDataListDto.getFilePid()).isNull(FileInfo::getRecoveryTime).like(FileInfo::getFileName, fileNameFuzzy).list(loadDataListPage);
        }
        return getLoadDataListVo(loadDataListPage, list);
    }

    public static LoadDataListVo getLoadDataListVo(Page<FileInfo> loadDataListPage, List<FileInfo> list) {
        LoadDataListVo loadDataListVo = new LoadDataListVo();
        loadDataListVo.setPageNo(loadDataListPage.getCurrent());
        loadDataListVo.setPageSize(loadDataListPage.getSize());
        loadDataListVo.setTotalCount(loadDataListPage.getTotal());
        loadDataListVo.setPageTotal(loadDataListPage.getPages());
        loadDataListVo.setList(list);
        return loadDataListVo;
    }

    /*
    处理文件的分块上传（支持秒传和普通上传）
    通过检查文件的MD5值来判断是否已经上传过该文件，上传过程中还会检查用户的剩余空间并更新用户的空间使用情况。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVo uploadFile(String token, UploadFileDto uploadFileDto, HttpServletResponse response) {
        String fileId = uploadFileDto.getFileId();
        if (fileId == null || fileId.isEmpty()) {
            fileId = RandomUtil.randomString(Constants.LENGTH_10);
        }
        Claims claims = jwtUtils.getClaims(token);

        String userId = (String) claims.get("userId");
        Long useSpace = claims.get("useSpace", Long.class);
        Long totalSpace = claims.get("totalSpace", Long.class);
        UploadFileVo uploadFileVo = new UploadFileVo();
        //秒传
        if (uploadFileDto.getChunkIndex() == 0) {
            List<FileInfo> list = lambdaQuery().eq(FileInfo::getFileMd5, uploadFileDto.getFileMd5()).eq(FileInfo::getStatus, Constants.FILE_STATUS_TRANSCODING_SUCCESS).list();
            if (!list.isEmpty()) {
                FileInfo file = list.getFirst();
                if (file.getFileSize() + useSpace > totalSpace) {
                    log.info("云盘剩余空间不足，无法上传文件");
                    throw new ServerException("云盘剩余空间不足，无法上传文件");
                }
                file.setFileId(fileId);
                file.setFilePid(uploadFileDto.getFilePid());
                file.setUserId(userId);
                file.setFileName(uploadFileDto.getFileName());
                String NewFileName = renameFile(file.getUserId(), file.getFilePid(), file.getFileName());
                file.setFileName(NewFileName);
                this.save(file);
                log.info("秒传-上传文件成功！文件实现秒传");
                UserInfo user = userInfoMapper.selectById(userId);
                log.info("秒传-从数据库中获取用户信息成功");
                user.setUseSpace(file.getFileSize() + useSpace);
                userInfoMapper.updateById(user);
                log.info("秒传-更新用户使用空间信息到数据库成功");
                jwtUtils.updateToken(response, user);
                log.info("秒传-更新用户Token成功");
                uploadFileVo.setFileId(file.getFileId());
                uploadFileVo.setStatus(Constants.FILE_UPLOAD_SECONDS);
                return uploadFileVo;
            } else {
                log.info("不满足秒传条件，正在使用普通传输");
            }
        }

        //普通传输
        long chunksSize;
        File userTempFileDirs = new File("file" + File.separator + userId);
        if (!userTempFileDirs.exists()) {
            boolean mkdirStatus = userTempFileDirs.mkdirs();
            log.info("创建用户文件夹状态：{}", mkdirStatus);
        }
        File userTempFile = new File(userTempFileDirs.getPath() + File.separator + uploadFileDto.getFileName() + "_" + uploadFileDto.getChunkIndex());
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(userTempFile)
        ) {
            lock.lock();
            UserSpaceDto userSpaceDto = (UserSpaceDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_UPLOAD_USE_SPACE);
            if (userSpaceDto == null) {
                userSpaceDto = new UserSpaceDto();
                userSpaceDto.setUseSpace(useSpace);
                userSpaceDto.setTotalSpace(totalSpace);
                redisTemplate.opsForValue().set(Constants.REDIS_KEY_UPLOAD_USE_SPACE, userSpaceDto, Duration.ofMinutes(1));
            }
            chunksSize = uploadFileDto.getFile().getInputStream().transferTo(fileOutputStream);
            if (redisTemplate.opsForValue().get(Constants.CHUNKS_SIZES + fileId) == null) {
                redisTemplate.opsForValue().set(Constants.CHUNKS_SIZES + fileId, chunksSize, Duration.ofMinutes(1));
            }
            Object redisChunkSizes = redisTemplate.opsForValue().get(Constants.REDIS_CHUNK_SIZES + fileId);
            if (redisChunkSizes == null) {
                redisTemplate.opsForValue().set(Constants.REDIS_CHUNK_SIZES + fileId, chunksSize, Duration.ofMinutes(1));
            } else {
                redisTemplate.opsForValue().set(Constants.REDIS_CHUNK_SIZES + fileId, Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue().get(Constants.REDIS_CHUNK_SIZES + fileId)).toString()) + chunksSize, Duration.ofMinutes(1));
            }
            log.info("文件chunksSize大小：{}", chunksSize);
            userSpaceDto.setUseSpace(userSpaceDto.getUseSpace() + chunksSize);
            log.info("{}+{}=={}", userSpaceDto.getUseSpace() - chunksSize, chunksSize, userSpaceDto.getUseSpace());
            redisTemplate.opsForValue().set(Constants.REDIS_KEY_UPLOAD_USE_SPACE, userSpaceDto, Duration.ofMinutes(1));
            if (userSpaceDto.getUseSpace() > totalSpace) {
                log.info("强制关闭文件输出流");
                fileOutputStream.close();
                log.info("可使用的空间不足，传输中断");
                redisTemplate.delete(Constants.REDIS_KEY_UPLOAD_USE_SPACE);
                log.info("可使用的空间不足，删除用户缓存");
                fileUtils.deleteTempFile(uploadFileDto, userTempFileDirs);
                throw new ServerException("可使用的空间不足，传输中断");
            }
            if (uploadFileDto.getChunkIndex() < uploadFileDto.getChunks() - 1) {
                uploadFileVo.setStatus(Constants.FILE_UPLOADING);
                uploadFileVo.setFileId(fileId);
            }
        } catch (IOException e) {
            log.info("文件转换失败：{}", e.getMessage());
            throw new ServerException("文件转换失败：" + e.getMessage());
        } finally {
            lock.unlock();
        }
        if (uploadFileDto.getChunkIndex() == uploadFileDto.getChunks() - 1) {
            uploadFileVo.setStatus(Constants.FILE_UPLOAD_FINISH);
            uploadFileVo.setFileId(fileId);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(userId);
            fileInfo.setFileMd5(uploadFileDto.getFileMd5());
            fileInfo.setFilePid(uploadFileDto.getFilePid());
            fileInfo.setFileSize(Long.valueOf(Objects.requireNonNull(redisTemplate.opsForValue().get(Constants.REDIS_CHUNK_SIZES + fileId)).toString()));
            String newFileName = renameFile(userId, uploadFileDto.getFilePid(), uploadFileDto.getFileName());
            fileInfo.setFileName(newFileName);
            fileInfo.setFolderType((short) 0);
            fileInfo.setFileType(getFileType(uploadFileDto));
            fileInfo.setFileCategory(getFileCategory(getFileType(uploadFileDto)));
            fileInfo.setStatus(Constants.FILE_STATUS_TRANSCODING);
            log.info("普通传输文件上传到数据库完成");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    TransactionSynchronization.super.afterCommit();
                    fileUtils.transferFile(uploadFileDto, fileInfo);
                    UserInfo user = userInfoMapper.selectById(userId);
                    log.info("普通上传-从数据库中获取用户信息成功");
                    UserSpaceDto userSpaceDto = (UserSpaceDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_UPLOAD_USE_SPACE);
                    if (userSpaceDto != null && Objects.equals(userSpaceDto.getTotalSpace(), user.getTotalSpace())) {
                        user.setUseSpace(userSpaceDto.getUseSpace());
                        log.info("更新用户空间：{}/{}", userSpaceDto.getUseSpace(), user.getTotalSpace());
                    } else {
                        log.info("用户总空间校验异常，cookie数据与数据库TotalSpace不一致");
                        throw new ServerException("用户总空间校验异常，cookie数据与数据库TotalSpace不一致");
                    }
                    userInfoMapper.updateById(user);
                    log.info("普通上传-更新用户使用空间信息到数据库成功");
                    jwtUtils.updateToken(response, user);
                    log.info("普通上传-更新用户Token成功");
                }
            });
        }
        return uploadFileVo;
    }

    /*
    根据指定的文件夹和文件名加载文件的封面图片并返回。
     */
    @Override
    public void getImage(Integer imageFolder, String imageName, HttpServletResponse response) {
        String imageCoverName = imageFolder + File.separator + imageName;
        List<FileInfo> list = lambdaQuery().eq(FileInfo::getFileType, imageFolder).eq(FileInfo::getFileCover, imageCoverName).list();
        String userId = list.getFirst().getUserId();
        File userFileCoverDir = new File("file_target_cover" + File.separator + userId + File.separator + imageName);
        response.setContentType("image/" + userFileCoverDir.getName());
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Content-Type", "image/jpeg");
        response.setDateHeader("Expires", 0);
        FileUtils.readFile(response, userFileCoverDir);
    }

    /*
    根据文件ID或分享ID加载视频文件。如果是视频文件，则提供.ts格式的文件下载。
     */
    @Override
    public void tsGetVideoInfo(String userId, String fileId, HttpServletResponse response, String token, String shareId) {
        if ((userId != null && token == null && shareId == null && fileId.endsWith(".ts"))) {
            log.info("[管理员]正在加载视频文件：{}", fileId);
            File userFileCutDir = new File("file_target_cut" + File.separator + userId + File.separator + fileId);
            FileUtils.readFile(response, userFileCutDir);
        }
        if ((token != null && shareId == null && fileId.endsWith(".ts"))) {
            log.info("[用户]正在加载视频文件：{}", fileId);
            Claims claims = jwtUtils.getClaims(token);
            userId = claims.get("userId", String.class);
            File userFileCutDir = new File("file_target_cut" + File.separator + userId + File.separator + fileId);
            FileUtils.readFile(response, userFileCutDir);
        }
        if (token == null && shareId != null && fileId.endsWith(".ts")) {
            log.info("[分享]正在加载视频文件：{}", fileId);
            userId = fileShareMapper.selectById(shareId).getUserId();
            File userFileCutDir = new File("file_target_cut" + File.separator + userId + File.separator + fileId);
            FileUtils.readFile(response, userFileCutDir);
        } else if (!fileId.endsWith(".ts")) {
            FileInfo fileInfo = baseMapper.selectById(fileId);
            if (fileInfo != null && fileInfo.getFileCategory() == 1) {
                userId = fileInfo.getUserId();
                String fileName = fileInfo.getFilePath().substring(fileInfo.getFilePath().lastIndexOf(File.separator));
                File userFileCutDir = new File("file_target_cut" + File.separator + userId);
                File m3u8File = new File(userFileCutDir.getAbsoluteFile() + File.separator + fileName.substring(0, fileName.lastIndexOf(".")) + Constants.FILE_TYPE_M3U8);
                FileUtils.readFile(response, m3u8File);
            } else {
                userId = Objects.requireNonNull(fileInfo).getUserId();
                String fileName = fileInfo.getFilePath().substring(fileInfo.getFilePath().lastIndexOf(File.separator));
                File userFileTargetDir = new File("file_target" + File.separator + userId);
                File othersFile = new File(userFileTargetDir.getPath() + File.separator + fileName);
                FileUtils.readFile(response, othersFile);
            }
        }
    }

    /*
    创建新的文件夹。如果文件夹名称已存在，则返回错误信息。
     */
    @Override
    public void newFolder(String filePid, String fileName, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        Long folderNumber = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, filePid).eq(FileInfo::getFileName, fileName).eq(FileInfo::getFolderType, (short) 1).count();
        if (folderNumber > 0) {
            log.info("创建文件夹失败，当前目录文件夹名字重复");
            throw new ServerException("创建文件夹失败，当前目录文件夹名字重复");
        }
        String randomFileId = RandomUtil.randomString(Constants.LENGTH_10);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setUserId(userId);
        fileInfo.setFileId(randomFileId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileSize(0L);
        fileInfo.setFileName(fileName);
        fileInfo.setFolderType((short) 1);
        fileInfo.setStatus((short) 2);
        this.save(fileInfo);
        log.info("创建文件夹成功");
    }

    /*
    根据路径获取文件夹内的文件信息。
     */
    @Override
    public List<GetFolderInfoVo> getFolderInfo(String path, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        String[] splitPath = path.split("/");
        List<String> pathList = Arrays.asList(splitPath);
        LambdaQueryWrapper<FileInfo> pathWrapper = new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, pathList);
        List<FileInfo> list = fileInfoMapper.getFolderInfo(pathWrapper, pathList);
        List<GetFolderInfoVo> GetFolderInfoList = list.stream().map(item -> {
            GetFolderInfoVo vo = new GetFolderInfoVo();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList();
        log.info("根据用户点击顺序，进行排序：{}", GetFolderInfoList);
        return GetFolderInfoList;
    }

    /*
    重命名指定的文件，确保目标文件夹中没有同名文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(String fileId, String fileName, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        FileInfo fileInfo = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId).one();
        String filePid = fileInfo.getFilePid();
        Long fileNumber;
        String newFileName;
        if (fileInfo.getFolderType() == (short) 0) {
            String fileSuffixName = fileInfo.getFileName().substring(fileInfo.getFileName().lastIndexOf("."));
            newFileName = fileName + fileSuffixName;
            fileNumber = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileName, newFileName).eq(FileInfo::getFilePid, filePid).eq(FileInfo::getFolderType, (short) 0).count();
        } else {
            fileNumber = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileName, fileName).eq(FileInfo::getFilePid, filePid).eq(FileInfo::getFolderType, (short) 1).count();
            newFileName = fileName;
        }
        if (fileNumber > 0) {
            log.info("文件重命名失败，当前目录文件名字重复");
            throw new ServerException("文件重命名失败，当前目录文件名字重复");
        }
        boolean update = lambdaUpdate().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId).set(FileInfo::getFileName, newFileName).update();
        log.info("数据库文件重命名状态为：{}", update);
    }

    /*
    加载指定父文件夹下的所有子文件夹。
     */
    @Override
    public List<LoadAllFolderVo> loadAllFolder(String filePid, String currentFileIds, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        List<FileInfo> list = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFolderType, (short) 1).eq(FileInfo::getFilePid, filePid).list();
        List<LoadAllFolderVo> LoadAllFolderList = list.stream().map(item -> {
            LoadAllFolderVo loadAllFolderVo = new LoadAllFolderVo();
            BeanUtils.copyProperties(item, loadAllFolderVo);
            return loadAllFolderVo;
        }).toList();
        log.info("查询到当前目录的所有文件夹：{}", LoadAllFolderList);
        return LoadAllFolderList;
    }

    /*
    将文件从一个文件夹移动到另一个文件夹，并确保目标文件夹没有同名文件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        String[] fileId = fileIds.split(",");
        List<String> list = List.of(fileId);
        List<FileInfo> targetPidFile = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, filePid).list();
        List<FileInfo> changeFile = lambdaQuery().eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, list).list();
        for (FileInfo fileInfo : targetPidFile) {
            for (FileInfo info : changeFile) {
                if (fileInfo.getFileName().equals(info.getFileName())) {
                    log.info("移动文件失败，正在移动的文件中存在与目标文件夹文件名字冲突");
                    throw new ServerException("移动文件失败，正在移动的文件中存在与目标文件夹文件名字冲突");
                }
            }
        }
        lambdaUpdate().eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, list).set(FileInfo::getFilePid, filePid).update();
        log.info("更新数据库【{}】个文件filePid为：【{}】成功", list.size(), filePid);
    }

    /*
    删除指定的文件，将其移入回收站。
     */
    @Override
    public void delFile(String fileIds, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        List<String> fileIdList = Arrays.asList(fileIds.split(","));
        delAllFile(fileIdList, userId);
        log.info("成功将【{}】个id为【{}】的文件移入到回收站", fileIdList.size(), fileIdList);
    }

    /*
    为指定文件生成一个有效的下载链接。返回一个唯一的随机代码，用户可通过该代码下载文件。
     */
    @Override
    public String createDownloadUrl(String fileId, String token, String shareId, String userId) {
        FileInfo fileInfo;
        try {
            if (token != null) {
                Claims claims = jwtUtils.getClaims(token);
                userId = claims.get("userId", String.class);
            }
            fileInfo = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId).eq(FileInfo::getFolderType, (short) 0).isNull(FileInfo::getRecoveryTime).one();
        } catch (RuntimeException e) {
            log.info("无法获取到用户token，可能为分享页面的createDownloadUrl，{}", e.getMessage());
            userId = fileShareMapper.selectById(shareId).getUserId();
            fileInfo = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId).eq(FileInfo::getFolderType, (short) 0).isNull(FileInfo::getRecoveryTime).one();
        }
        if (fileInfo == null) {
            log.info("查找不到文件，无法创建下载链接");
            throw new ServerException("查找不到文件，无法创建下载链接");
        }
        String randomCode = RandomUtil.randomString(Constants.LENGTH_30);
        CreateDownloadUrlDto createDownloadUrlDto = new CreateDownloadUrlDto();
        createDownloadUrlDto.setCode(randomCode);
        createDownloadUrlDto.setFilePath(fileInfo.getFilePath());
        createDownloadUrlDto.setFileName(fileInfo.getFileName());
        redisTemplate.opsForValue().set(Constants.REDIS_KEY_CREATE_DOWNLOAD_URL_DTO, createDownloadUrlDto, Duration.ofMinutes(30));
        return randomCode;
    }

    /*
    根据下载链接的随机代码来下载文件。
     */
    @Override
    public void download(String code, HttpServletResponse response) {
        CreateDownloadUrlDto createDownloadUrlDto = (CreateDownloadUrlDto) redisTemplate.opsForValue().get(Constants.REDIS_KEY_CREATE_DOWNLOAD_URL_DTO);
        if (createDownloadUrlDto == null) {
            log.info("无法获取下载链接，或者下载链接过期");
            throw new ServerException("无法获取下载链接，或者下载链接过期");
        }
        String redisCode = createDownloadUrlDto.getCode();
        if (!redisCode.equals(code)) {
            log.info("code错误，请重新下载");
            throw new ServerException("code错误，请重新下载");
        }
        File RedisFilePath = new File(createDownloadUrlDto.getFilePath());
        response.setContentType("application/download;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + createDownloadUrlDto.getFileName());
        FileUtils.readFile(response, RedisFilePath);
    }

    private void delAllFile(List<String> fileIdList, String userId) {
        for (String fileId : fileIdList) {
            FileInfo fileInfo = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId).one();
            if (fileInfo != null) {
                fileInfo.setRecoveryTime(LocalDateTime.now());
                try {
                    this.updateById(fileInfo);
                    log.info("已将文件【{}】移入回收站", fileInfo.getFileId());
                } catch (Exception e) {
                    log.error("更新文件【{}】的RecoveryTime时出错", fileInfo.getFileId(), e);
                }

                if (fileInfo.getFolderType() == (short) 1) {
                    List<FileInfo> folderFiles = getFolderFile(userId, fileInfo);
                    delAllFile(folderFiles.stream().map(FileInfo::getFileId).collect(Collectors.toList()), userId);
                }
            }
        }
    }

    private List<FileInfo> getFolderFile(String userId, FileInfo folderInfo) {
        return lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, folderInfo.getFileId()).list();
    }

    private static short getFileType(UploadFileDto uploadFileDto) {
        String category = uploadFileDto.getFileName().substring(uploadFileDto.getFileName().lastIndexOf("."));
        return (short) (category.equals(Constants.FILE_TYPE_VIDEO) ? 1 : category.equals(Constants.FILE_TYPE_MUSIC) ? 2 : category.equals(Constants.FILE_TYPE_PICTURE_JPG) ? 3 : category.equals(Constants.FILE_TYPE_PICTURE_PNG) ? 3 : category.equals(Constants.FILE_TYPE_PICTURE_JPEG) ? 3 : category.equals(Constants.FILE_TYPE_PDF) ? 4 : category.equals(Constants.FILE_TYPE_DOC) ? 5 : category.equals(Constants.FILE_TYPE_EXCEL) ? 6 : category.equals(Constants.FILE_TYPE_TXT) ? 7 : category.equals(Constants.FILE_TYPE_ZIP) ? 8 : 9);
    }

    private static short getFileCategory(Short fileType) {
        return (short) (fileType == 1 ? 1 : fileType == 2 ? 2 : fileType == 3 ? 3 : fileType == 4 ? 4 : fileType == 5 ? 4 : fileType == 6 ? 4 : fileType == 7 ? 4 : fileType == 8 ? 5 : 6);
    }

    private String renameFile(String userId, String filePid, String fileName) {
        Long count = lambdaQuery().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, filePid).eq(FileInfo::getFileName, fileName).count();
        if (count > 0) {
            String filePrefixName = fileName.substring(0, fileName.lastIndexOf("."));
            String filesSuffixName = fileName.substring(fileName.lastIndexOf("."));
            fileName = filePrefixName + "_" + RandomUtil.randomNumbers(Constants.LENGTH_5) + filesSuffixName;
            log.info("文件名冲突，已为文件更名成：{}", fileName);
            return fileName;
        }
        log.info("文件名没有冲突，使用原文件名");
        return fileName;
    }
}
