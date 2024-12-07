package com.mhc.springbootclouddisk.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.FileShare;
import com.mhc.springbootclouddisk.entity.vo.LoadShareListListVo;
import com.mhc.springbootclouddisk.entity.vo.LoadShareListVo;
import com.mhc.springbootclouddisk.mapper.FileInfoMapper;
import com.mhc.springbootclouddisk.mapper.FileShareMapper;
import com.mhc.springbootclouddisk.service.FileShareService;
import com.mhc.springbootclouddisk.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements FileShareService {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Override
    public Page<FileShare> loadShareListPage(Long pageNo, Long pageSize) {
        //1.分页参数
        Page<FileShare> page = Page.of(pageNo, pageSize);
        //2.排序参数
        page.addOrder(OrderItem.desc("share_time"));
        //3.开始分页查询
        log.info("开始分页查询");
        return this.page(page);
    }

    @Override
    public LoadShareListVo loadShareList(Page<FileShare> loadShareListPage, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        List<FileShare> list = lambdaQuery().eq(FileShare::getUserId, userId).list(loadShareListPage);
        List<LoadShareListListVo> loadShareListList = list.stream().map(item -> {
            FileInfo fileInfo = fileInfoMapper.selectById(item.getFileId());
            if (fileInfo == null) {
                lambdaUpdate().eq(FileShare::getFileId, item.getFileId()).eq(FileShare::getUserId, userId).remove();
                log.info("文件【{}】已删除，已取消分享", item.getFileId());
                return null;
            }
            LoadShareListListVo loadShareListListVo = new LoadShareListListVo();
            BeanUtils.copyProperties(item, loadShareListListVo);
            BeanUtils.copyProperties(fileInfo, loadShareListListVo);
            return loadShareListListVo;
        }).toList();
        LoadShareListVo loadShareListVo = new LoadShareListVo();
        loadShareListVo.setPageNo(loadShareListPage.getCurrent());
        loadShareListVo.setPageSize(loadShareListPage.getSize());
        loadShareListVo.setPageTotal(loadShareListPage.getPages());
        loadShareListVo.setTotalCount(loadShareListPage.getTotal());
        loadShareListVo.setList(loadShareListList);
        return loadShareListVo;
    }

    @Override
    public LoadShareListListVo shareFile(String fileId, Short validType, String code, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        FileShare file = lambdaQuery().eq(FileShare::getFileId, fileId).eq(FileShare::getUserId, userId).one();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = validType == 0 ? now.plusDays(1) : validType == 1 ? now.plusDays(7) : validType == 2 ? now.plusDays(30) : now.plusYears(1);
        if (file == null) {
            file = new FileShare();
            file.setFileId(fileId);
            file.setUserId(userId);
            file.setValidType(validType);
            file.setExpireTime(expireTime);
            file.setShareTime(LocalDateTime.now());
            file.setCode(code);
            log.info("成功将【{}】插入到数据库", file);
        } else {
            file.setCode(code);
            file.setShareTime(LocalDateTime.now());
            file.setExpireTime(expireTime);
            file.setValidType(validType);
        }
        this.saveOrUpdate(file);
        LoadShareListListVo loadShareListListVo = new LoadShareListListVo();
        BeanUtils.copyProperties(file, loadShareListListVo);
        return loadShareListListVo;
    }

    @Override
    public void cancelShare(String shareIds, String token) {
        Claims claims = jwtUtils.getClaims(token);
        String userId = claims.get("userId", String.class);
        String[] shareId = shareIds.split(",");
        List<String> shareIdList = Arrays.stream(shareId).toList();
        lambdaUpdate().eq(FileShare::getUserId, userId).in(FileShare::getShareId, shareIdList).remove();
        log.info("已取消文件分享id为【{}】的文件分享", shareIds);
    }
}
