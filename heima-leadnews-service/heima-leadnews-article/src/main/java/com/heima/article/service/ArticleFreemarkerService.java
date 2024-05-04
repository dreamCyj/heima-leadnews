package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {
    /**
     * 生成静态文件上传到minIO
     */
    public void buildArticleToMinIO(ApArticle apArticle, String content);
}
