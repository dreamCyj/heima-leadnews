package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WmMaterialMapper wmMaterialMapper;
    /**
     * 图片上传
     * @param multipartFile
     * @return
     */
    public ResponseResult uploadPicture(MultipartFile multipartFile){
        //1.检查参数
        if(multipartFile == null || multipartFile.isEmpty()){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.上传图片到minIO
        String fileName = UUID.randomUUID().toString().replace("-", "");
        String originalFilename = multipartFile.getOriginalFilename();
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName + postfix, multipartFile.getInputStream());
            log.info("上传文件到minIO中,fileId:{}", fileId);
        } catch (IOException e) {
            log.error("WmMaterialServiceImpl-上传文件失败");
            throw new RuntimeException(e);
        }
        //3.保存到数据库
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
        wmMaterial.setType((short)0);
        wmMaterial.setUrl(fileId);
        wmMaterial.setIsCollection((short)0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);
        //4.返回结果
        return ResponseResult.okResult(wmMaterial);
    }

    /**
     * 素材分页查询
     * @param wmMaterialDto
     * @return
     */
    public ResponseResult findList(WmMaterialDto wmMaterialDto) {
        //1.检查参数
        wmMaterialDto.checkParam();
        //2.分页查询
        IPage<WmMaterial> page = new Page<>(wmMaterialDto.getPage(), wmMaterialDto.getSize());
        LambdaQueryWrapper<WmMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(wmMaterialDto.getIsCollection() != null && wmMaterialDto.getIsCollection() == 1, WmMaterial::getIsCollection, wmMaterialDto.getIsCollection())
                    .eq(WmMaterial::getUserId, WmThreadLocalUtil.getUser().getId())
                    .orderByDesc(WmMaterial::getCreatedTime);
        page= page(page, queryWrapper);
        //3.返回结果
        ResponseResult responseResult = new PageResponseResult(wmMaterialDto.getPage(), wmMaterialDto.getSize(), (int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    /**
     * 收藏素材
     * @param id
     * @return
     */
    public ResponseResult collect(Integer id) {
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setId(id);
        wmMaterial.setIsCollection(WemediaConstants.COLLECT_MATERIAL);
        wmMaterialMapper.updateById(wmMaterial);
        return ResponseResult.okResult(null);
    }

    /**
     * 取消收藏
     * @param id
     * @return
     */
    public ResponseResult notCollect(Integer id) {
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setId(id);
        wmMaterial.setIsCollection(WemediaConstants.CANCEL_COLLECT_MATERIAL);
        wmMaterialMapper.updateById(wmMaterial);
        return ResponseResult.okResult(null);
    }
}
