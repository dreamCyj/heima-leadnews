package com.heima.article.controller.v1;

import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDTO;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/article")
@Api(value = "app端首页")
public class ArticleHomeController {

    @Autowired
    private ApArticleService apArticleService;
    @ApiOperation("加载首页")
    @PostMapping("/load")
    public ResponseResult load(@RequestBody ArticleHomeDTO articleHomeDTO){
        //return apArticleService.load(articleHomeDTO, ArticleConstants.LOADTYPE_LOAD_MORE);
        return apArticleService.load2(articleHomeDTO, ArticleConstants.LOADTYPE_LOAD_MORE, true);
    }

    @ApiOperation("加载更多")
    @PostMapping("/loadmore")
    public ResponseResult loadmore(@RequestBody ArticleHomeDTO articleHomeDTO){
        return apArticleService.load(articleHomeDTO, ArticleConstants.LOADTYPE_LOAD_MORE);
    }

    @ApiOperation("加载最新")
    @PostMapping("/loadnew")
    public ResponseResult loadnew(@RequestBody ArticleHomeDTO articleHomeDTO){
        return apArticleService.load(articleHomeDTO, ArticleConstants.LOADTYPE_LOAD_NEW);
    }
}
