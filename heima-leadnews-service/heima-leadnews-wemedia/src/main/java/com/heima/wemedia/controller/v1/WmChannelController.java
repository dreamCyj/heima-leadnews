package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.service.WmChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channel")
public class WmChannelController {

    @Autowired
    private WmChannelService wmChannelService;

    @GetMapping("/channels")
    public ResponseResult findAll(){
        return wmChannelService.findAll();
    }

    @PostMapping("/list")
    public ResponseResult list(@RequestBody ChannelDto channelDto){
        return wmChannelService.list(channelDto);
    }

    @GetMapping("/del/{id}")
    public ResponseResult delById(@PathVariable Integer id){
        return wmChannelService.delById(id);
    }

    @PostMapping("/save")
    public ResponseResult insert(@RequestBody WmChannel wmChannel){
        return wmChannelService.insert(wmChannel);
    }

    @PostMapping("/update")
    public ResponseResult update(@RequestBody WmChannel wmChannel){
        return wmChannelService.update(wmChannel);
    }

}
