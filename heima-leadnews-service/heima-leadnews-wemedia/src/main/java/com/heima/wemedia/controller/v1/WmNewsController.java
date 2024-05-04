package com.heima.wemedia.controller.v1;


import com.heima.common.constants.WemediaConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.wemedia.service.WmNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {

    @Autowired
    private WmNewsService wmNewsService;

    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmNewsPageReqDto dto){
        return wmNewsService.findList(dto);
    }

    @PostMapping("/submit")
    public ResponseResult submitNews(@RequestBody WmNewsDto dto){
        return wmNewsService.submitNews(dto);
    }

    @GetMapping("/del_news/{id}")
    public ResponseResult delNews(@PathVariable("id") Integer id){
        return wmNewsService.delNews(id);
    }
    @PostMapping("/down_or_up")
    public ResponseResult down_or_up(@RequestBody WmNewsDto dto){
        return wmNewsService.down_or_up(dto);
    }

    @PostMapping("/list_vo")
    public ResponseResult listVo(@RequestBody NewsAuthDto dto){
        return wmNewsService.listVo(dto);
    }

    @GetMapping("/one_vo/{id}")
    public ResponseResult oneVo(@PathVariable("id") Integer id){
        return wmNewsService.oneVo(id);
    }

    @PostMapping("/auth_pass")
    public ResponseResult authPass(@RequestBody NewsAuthDto dto){
        return wmNewsService.updateStatus(dto, WemediaConstants.WM_NEWS_AUTH_PASS);
    }
    @PostMapping("/auth_fail")
    public ResponseResult authFail(@RequestBody NewsAuthDto dto){
        return wmNewsService.updateStatus(dto, WemediaConstants.WM_NEWS_AUTH_FAIL);
    }
}
