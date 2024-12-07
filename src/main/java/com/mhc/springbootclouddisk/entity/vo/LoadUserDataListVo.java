package com.mhc.springbootclouddisk.entity.vo;

import com.mhc.springbootclouddisk.entity.dto.LoadUserDataListDto;
import lombok.Data;

import java.util.List;

@Data
public class LoadUserDataListVo {
    private Long pageNo;
    private Long pageSize;
    private Long totalCount;
    private Long pageTotal;
    private List<LoadUserDataListDto> list;
}
