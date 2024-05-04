package com.heima.apis.wemedia;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient("leadnews-wemedia")
public interface IWemediaClient {

    /**
     * 远程调用获取所有频道 app调用
     * @return
     */
    @GetMapping("/api/v1/channel/list")
    ResponseResult getChannels();

    /**
     * 远程调用创建WmUser  user微服务调用wemedia微服务
     * @param name
     * @return
     */
    @GetMapping("/api/v1/user/findByName/{name}")
    WmUser findWmUserByName(@PathVariable("name") String name);

    @PostMapping("/api/v1/wm_user/save")
    ResponseResult saveWmUser(@RequestBody WmUser wmUser);
}
