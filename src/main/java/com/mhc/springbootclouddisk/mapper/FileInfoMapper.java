package com.mhc.springbootclouddisk.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    List<FileInfo> getFolderInfo(@Param(Constants.WRAPPER) LambdaQueryWrapper<FileInfo> pathWrapper, List<String> pathList);
}
