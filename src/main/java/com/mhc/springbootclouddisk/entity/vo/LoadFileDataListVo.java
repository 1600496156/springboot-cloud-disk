package com.mhc.springbootclouddisk.entity.vo;

import com.mhc.springbootclouddisk.entity.dto.LoadFileDataListDto;
import lombok.Data;

import java.util.List;

@Data
public class LoadFileDataListVo {
    private Long pageNo;
    private Long pageSize;
    private Long totalCount;
    private Long pageTotal;
    private List<LoadFileDataListDto> list;
}
