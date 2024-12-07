package com.mhc.springbootclouddisk.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoadShareListVo {
    private Long pageNo;
    private Long pageSize;
    private Long totalCount;
    private Long pageTotal;
    private List<LoadShareListListVo> list;
}
