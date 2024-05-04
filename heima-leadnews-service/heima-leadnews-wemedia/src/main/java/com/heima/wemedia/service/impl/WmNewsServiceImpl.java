package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vos.WmNewsVo;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import com.heima.wemedia.service.WmUserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    private  final WmNewsMaterialMapper wmNewsMaterialMapper;

    private  final WmMaterialMapper wmMaterialMapper;

    private final WmNewsAutoScanService wmNewsAutoScanService;

    private final WmNewsTaskService wmNewsTaskService;

    private final WmNewsMapper wmNewsMapper;

    private final WmUserService wmUserService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    public WmNewsServiceImpl(WmNewsMaterialMapper wmNewsMaterialMapper, WmMaterialMapper wmMaterialMapper, WmNewsAutoScanService wmNewsAutoScanService, WmNewsTaskService wmNewsTaskService, WmNewsMapper wmNewsMapper, WmUserService wmUserService) {
        this.wmNewsMaterialMapper = wmNewsMaterialMapper;
        this.wmMaterialMapper = wmMaterialMapper;
        this.wmNewsAutoScanService = wmNewsAutoScanService;
        this.wmNewsTaskService = wmNewsTaskService;
        this.wmNewsMapper = wmNewsMapper;
        this.wmUserService = wmUserService;
    }

    /**
     * 条件分页查询文章
     * @param dto
     * @return
     */
    public ResponseResult findList(WmNewsPageReqDto dto) {
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        WmUser user = WmThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        IPage<WmNews> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId())
                    .eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus())
                    .eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId())
                    .like(StringUtils.isNotBlank(dto.getKeyword()), WmNews::getTitle, dto.getKeyword())
                    .between(dto.getBeginPubDate() != null && dto.getEndPubDate() != null, WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate())
                    .orderByDesc(WmNews::getCreatedTime);
        page = page(page, queryWrapper);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    /**
     * 发布修改文章或保存为草稿
     * @param dto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto) {
        if(dto == null || dto.getContent() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.新增或修改文章
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);
        //封面图片 list<String>  -->  String  [1.jpg,2.jpg] --> 1.jpg,2.jpg
        if(dto.getImages() != null && !dto.getImages().isEmpty()){
            //拿到每个jpg, jpg之间用,连接
            String imgs = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imgs);
        }
        //如果当前封面类型为自动 -1
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);
        //2.判断是否为草稿
        if(dto.getStatus().equals(WmNews.Status.NORMAL.getCode())){
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        //3.不是草稿 保存文章内容图片与素材的关系
        //获取文章内容中的图片信息
        List<String> materials = extractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials, wmNews.getId());
        //4.不是草稿 保存文章封面图片与素材的关系 如果为自动 封面要去内容图片中找
        saveRelativeInfoForCover(dto, wmNews, materials);
        //审核
        //wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        wmNewsTaskService.addNewsToTask(wmNews.getId(), wmNews.getPublishTime());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 根据id删除文章
     * @param id
     * @return
     */
    public ResponseResult delNews(Integer id) {
        wmNewsMapper.deleteById(id);
        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, id));
        return ResponseResult.okResult(null);
    }


    public ResponseResult down_or_up(WmNewsDto dto) {
        if(dto.getId() == null || dto.getEnable() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //根据文章id查询文章是否存在 是否是已发布状态
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "当前文章未发布，不能上下架");
        }
        if(dto.getEnable() > -1 && dto.getEnable() < 2){
            wmNews.setEnable(dto.getEnable());
            updateById(wmNews);
            if(wmNews.getArticleId() != null){
                //发送消息 通知article修改文章配置
                Map<String, Object> map = new HashMap<>();
                map.put("articleId", wmNews.getArticleId());
                map.put("enable", dto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString(map));
            }

        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 如果封面类型为自动 内容图片>=1 <3 type1 单图 >=3 多图 type3 无图 type0
     * 处理文章封面图片与素材的关系
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {

        List<String> images = dto.getImages();
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            //多图
            if(materials.size() >= 3){
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            }else if(materials.size() >= 1){
                //单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            }else {
                //无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
            //修改
            if(images != null && !images.isEmpty()){
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }
        if(images != null && !images.isEmpty()){
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }
    }

    /**
     * 处理文章内容图片与素材的关系
     * @param materials
     * @param newsId
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials, newsId, WemediaConstants.WM_CONTENT_REFERENCE);
    }

    /**
     * 保存文章图片(内容、封面)与素材的关系到数据库
     * @param materials
     * @param newsId
     * @param type
     */
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        if(materials != null && !materials.isEmpty()){
            //批量保存
            //通过图片url查询其id
            List<WmMaterial> wmMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));
            //判断素材是否有效
            if(wmMaterials == null || wmMaterials.isEmpty()){
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }
            //部分失效
            if(materials.size() != wmMaterials.size()){
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }
            List<Integer> materialIdList = wmMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
            wmNewsMaterialMapper.saveRelations(materialIdList, newsId, type);
        }
    }


    /**
     * 提取文章内容中的图片信息
     * @param content
     * @return
     */
    private List<String> extractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if(map.get("type").equals("image")){
                String imgUrl = (String)map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }

    /**
     * 保存或修改文章
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short)1);
        if(wmNews.getId() == null){
            //新增
            //没有id证明是新增 因为新增操作执行完才会返回id
            save(wmNews);
        }else {
            //修改
            //删除文章图片与素材的关系  通过先删再新增实现修改
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }


    public ResponseResult listVo(NewsAuthDto dto) {
        dto.checkParam();
        IPage<WmNews> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(dto.getTitle() != null, WmNews::getTitle, dto.getTitle())
                .eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus());
        page = page(page, queryWrapper);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int)page.getTotal());
        List<WmNews> records = page.getRecords();
        List<WmNewsVo> wmNewsVoList = records.stream().map((item -> {
            WmNewsVo wmNewsVo = new WmNewsVo();
            BeanUtils.copyProperties(item, wmNewsVo);
            //根据WmNews中的userId去WmUser中找其对应的name为AuthorName
            WmUser wmUser = wmUserService.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getId, item.getUserId()));
            wmNewsVo.setAuthorName(wmUser.getName());
            return wmNewsVo;
        })).collect(Collectors.toList());
        responseResult.setData(wmNewsVoList);
        return responseResult;
    }

    @Override
    public ResponseResult oneVo(Integer id) {
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(id);
        WmNewsVo wmNewsVo = new WmNewsVo();
        BeanUtils.copyProperties(wmNews, wmNewsVo);
        WmUser wmUser = wmUserService.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getId, wmNews.getUserId()));
        wmNewsVo.setAuthorName(wmUser.getName());
        return ResponseResult.okResult(wmNewsVo);
    }

    @Override
    public ResponseResult updateStatus(NewsAuthDto dto, Short status) {
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        wmNews.setStatus(status);
        if(StringUtils.isNotBlank(dto.getMsg())){
            wmNews.setReason(dto.getMsg());
        }
        updateById(wmNews);
        //审核成功 则创建app端文章数据 并修改自媒体文章状态为已发布
        if(status.equals(WemediaConstants.WM_NEWS_AUTH_PASS)){
            //返回值为ResponseResult.okResult(apArticle.getId()) 文章id
            ResponseResult responseResult = wmNewsAutoScanService.saveAppArticle(wmNews);
            if(responseResult.getCode().equals(200)){
                wmNews.setArticleId((Long) responseResult.getData());
                wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
                updateById(wmNews);
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
