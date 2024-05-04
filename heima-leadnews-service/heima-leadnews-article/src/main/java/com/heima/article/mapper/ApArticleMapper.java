package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.dtos.ArticleHomeDTO;
import com.heima.model.article.pojos.ApArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.util.Date;
import java.util.List;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {

    /**
     * 加载文章列表
     * @param articleHomeDTO
     * @param type  1加载更多 2加载最新
     * @return
     */
    List<ApArticle> loadArticleList(ArticleHomeDTO articleHomeDTO, Short type);

    List<ApArticle> findArticleListByLast5days(@Param("dayParam") Date dayParam);
}
