package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface WmMaterialService extends IService<WmMaterial> {
    //图片上传
    ResponseResult uploadPicture(MultipartFile multipartFile) throws IOException;
    //素材列表查询
    ResponseResult findList(@RequestBody WmMaterialDto wmMaterialDto);

    ResponseResult collect(Integer id);

    ResponseResult notCollect(Integer id);
}
