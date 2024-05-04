package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.common.constants.UserConstants;
import com.heima.common.constants.WemediaConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {

    public ResponseResult list(AuthDto authDto) {
        //1.检查参数
        if(authDto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        authDto.checkParam();
        IPage page = new Page(authDto.getPage(), authDto.getSize());
        LambdaQueryWrapper<ApUserRealname> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(authDto.getStatus() != null, ApUserRealname::getStatus, authDto.getStatus());
        page = page(page, queryWrapper);
        ResponseResult responseResult = new PageResponseResult(authDto.getPage(), authDto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }


    public ResponseResult updateStatus(AuthDto authDto, Short status) {
        //1.检查参数
        if(authDto == null || authDto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApUserRealname apUserRealname = new ApUserRealname();
        apUserRealname.setId(authDto.getId());
        apUserRealname.setStatus(status);
        if(StringUtils.isNotBlank(authDto.getMsg())){
            apUserRealname.setReason(authDto.getMsg());
        }
        updateById(apUserRealname);
        //如果通过审核 需要开通自媒体账户
        if(status.equals(UserConstants.PASS_AUTH)){
            //开通自媒体账户 需要wemedia服务 在user微服务中调用wemedia服务 需要通过feign远程调用
            ResponseResult responseResult = createWmUserAndAuthor(authDto);
            if(responseResult != null){
                return responseResult;
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private IWemediaClient wemediaClient;
    @Autowired
    private ApUserMapper apUserMapper;

    /**
     * 创建自媒体账户
     * @param authDto
     * @return
     */
    private ResponseResult createWmUserAndAuthor(AuthDto authDto) {
        //查询用户认证信息
        ApUserRealname apUserRealname = getById(authDto.getId());
        if(apUserRealname == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //查询qpp端用户信息
        Integer userId = apUserRealname.getUserId();
        ApUser apUser = apUserMapper.selectById(userId);
        if(apUser == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //创建自媒体账户
        WmUser wmUser = wemediaClient.findWmUserByName(apUser.getName());
        if(wmUser == null){
            //新建
            wmUser = new WmUser();
            wmUser.setApUserId(apUser.getId());
            wmUser.setCreatedTime(new Date());
            wmUser.setName(apUser.getName());
            wmUser.setPassword(apUser.getPassword());
            wmUser.setSalt(apUser.getSalt());
            wmUser.setPhone(apUser.getPhone());
            wmUser.setStatus(WemediaConstants.NORMALLY_AVAILABLE_USER);
            wemediaClient.saveWmUser(wmUser);
        }
        //apUser标识为自媒体人
        apUser.setFlag(UserConstants.WEMEDIA_USER_FLAG);
        apUserMapper.updateById(apUser);
        return null;
    }
}
