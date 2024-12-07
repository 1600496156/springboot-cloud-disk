package com.mhc.springbootclouddisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mhc.springbootclouddisk.entity.domain.FileShare;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {
}
