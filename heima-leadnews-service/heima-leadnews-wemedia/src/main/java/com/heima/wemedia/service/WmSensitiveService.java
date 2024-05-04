package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;

public interface WmSensitiveService extends IService<WmSensitive> {

    ResponseResult list(SensitiveDto dto);

    ResponseResult delById(Integer id);

    ResponseResult insert(WmSensitive wmSensitive);

    ResponseResult update(WmSensitive wmSensitive);
}
