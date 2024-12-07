package com.mhc.springbootclouddisk.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import com.mhc.springbootclouddisk.entity.domain.UserInfo;
import com.mhc.springbootclouddisk.entity.dto.LoadUserDataListDto;
import com.mhc.springbootclouddisk.entity.vo.LoadUserDataListVo;
import com.mhc.springbootclouddisk.mapper.FileInfoMapper;
import com.mhc.springbootclouddisk.mapper.UserInfoMapper;
import com.mhc.springbootclouddisk.service.AdminService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements AdminService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public LoadUserDataListVo loadUserList(Page<FileInfo> fileInfoPage, String nickNameFuzzy, String filePid) {
        // 校验filePid非空
        if (filePid == null || filePid.isEmpty()) {
            throw new IllegalArgumentException("filePid不能为空");
        }
        // 构建查询条件
        LambdaQueryChainWrapper<FileInfo> query = lambdaQuery();
        query.eq(FileInfo::getFilePid, filePid);
        if (nickNameFuzzy != null && !nickNameFuzzy.isEmpty()) {
            query.like(FileInfo::getFileName, nickNameFuzzy);
        }
        // 执行分页查询
        List<FileInfo> fileList = query.list(fileInfoPage);
        // 根据userId批量查询用户信息
        List<UserInfo> userList = userInfoMapper.selectBatchIds(fileList.stream().map(FileInfo::getUserId).collect(Collectors.toList()));
        // 组装返回对象
        return getLoadUserDataListVo(fileInfoPage, userList, fileList);
    }

    private LoadUserDataListVo getLoadUserDataListVo(Page<FileInfo> loadDataListPage, List<UserInfo> userList, List<FileInfo> fileList) {
        LoadUserDataListVo loadUserDataListVo = new LoadUserDataListVo();
        loadUserDataListVo.setPageNo(loadDataListPage.getCurrent());
        loadUserDataListVo.setPageSize(loadDataListPage.getSize());
        loadUserDataListVo.setTotalCount(loadDataListPage.getTotal());
        loadUserDataListVo.setPageTotal(loadDataListPage.getPages());
        // 创建一个Map来根据userId快速查找UserInfo
        Map<String, UserInfo> userInfoMap = userList.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, user -> user));
        // 遍历fileList，为每个文件信息匹配对应的用户信息，并创建LoadUserDataListDto对象
        List<LoadUserDataListDto> loadUserDataListDtoList = fileList.stream().map(fileInfo -> {
            LoadUserDataListDto loadUserDataListDto = new LoadUserDataListDto();
            // 使用BeanUtils将FileInfo的属性复制到LoadUserDataListDto中
            BeanUtils.copyProperties(fileInfo, loadUserDataListDto);
            // 从userInfoMap中根据userId获取UserInfo，并将属性复制到LoadUserDataListDto中
            UserInfo userInfo = userInfoMap.get(fileInfo.getUserId());
            if (userInfo != null) {
                BeanUtils.copyProperties(userInfo, loadUserDataListDto, "status", "password", "userId"); // 假设我们不想复制密码字段
            }
            return loadUserDataListDto;
        }).collect(Collectors.toList());
        // 将组装好的LoadUserDataListDto列表设置到LoadUserDataListVo中
        loadUserDataListVo.setList(loadUserDataListDtoList);
        return loadUserDataListVo;
    }
}
