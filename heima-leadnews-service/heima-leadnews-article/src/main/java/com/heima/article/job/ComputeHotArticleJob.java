package com.heima.article.job;

import com.heima.article.service.HotArticleService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ComputeHotArticleJob {

    @Autowired
    private HotArticleService hotArticleService;
    @XxlJob("computeHotArticleJob") //里面是任务里的bean
    public void handle(){
        log.info("热文章计算调度任务开始执行");
        hotArticleService.computeHotArticle();
        log.info("热文章计算调度任务执行完成");
    }
}
