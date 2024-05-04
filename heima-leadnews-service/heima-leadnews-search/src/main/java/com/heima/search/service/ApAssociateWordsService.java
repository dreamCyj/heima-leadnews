package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;

public interface ApAssociateWordsService {
    /**
     * 搜索联想
     * @param dto
     * @return
     */

    ResponseResult search(UserSearchDto dto);
}
