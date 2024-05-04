package com.heima.model.wemedia.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

@Data
public class ChannelDto extends PageRequestDto {
    /**
     * 频道名称
     */
    private String name;

    /**
     * 频道状态 这里status前端没传过来
     */
    private Boolean status;
}
