package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;

public interface WmChannelService extends IService<WmChannel> {
    //查询所有频道
    ResponseResult findAll();

    /**
     * 模糊分页查询
     * @param channelDto
     * @return
     */

    ResponseResult list(ChannelDto channelDto);

    ResponseResult delById(Integer id);

    ResponseResult insert(WmChannel wmChannel);

    ResponseResult update(WmChannel wmChannel);
}
