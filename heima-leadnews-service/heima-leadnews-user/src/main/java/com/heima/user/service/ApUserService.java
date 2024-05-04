package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDTO;
import com.heima.model.user.pojos.ApUser;

/**
 * @author cyj
 */
public interface ApUserService extends IService<ApUser> {
    ResponseResult login(LoginDTO loginDTO);
}
