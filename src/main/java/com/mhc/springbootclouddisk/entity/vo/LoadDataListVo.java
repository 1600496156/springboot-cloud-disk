package com.mhc.springbootclouddisk.entity.vo;

import com.mhc.springbootclouddisk.entity.domain.FileInfo;
import lombok.Data;

import java.util.List;

@Data
public class LoadDataListVo {
    private Long pageNo;
    private Long pageSize;
    private Long totalCount;
    private Long pageTotal;
    private List<FileInfo> list;
}
