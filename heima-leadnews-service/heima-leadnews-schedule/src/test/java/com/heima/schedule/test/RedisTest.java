package com.heima.schedule.test;

import com.alibaba.fastjson.JSON;
import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.ScheduleApplication;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.junit.Test;
import com.heima.common.redis.CacheService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;
import java.util.Set;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class RedisTest {

    @Autowired
    private CacheService cacheService;
    @Test
    public void testList(){
        //list左边添加元素
        cacheService.lLeftPush("list_001", "hello,redis");

        //list右边删除元素
        cacheService.lRightPop("list_001");

        cacheService.lLeftPush("list_002", "SuckMyDick");
    }

    @Test
    public void testZset(){
        cacheService.zAdd("zSet_001", "hello,zSet 001", 1000);
        cacheService.zAdd("zSet_001", "hello,zSet 002", 2000);
        cacheService.zAdd("zSet_001", "hello,zSet 003", 3000);

    }

    @Test
    public void testKeys(){
        Set<String> keys = cacheService.keys("future_*");
        System.out.println(keys);

        Set<String> scan = cacheService.scan("future_*");
        System.out.println(scan);
    }

    //耗时4817ms  redis请求响应
    @Test
    public  void testPiple1(){
        long start =System.currentTimeMillis();
        for (int i = 0; i <10000 ; i++) {
            Task task = new Task();
            task.setTaskType(1001);
            task.setPriority(1);
            task.setExecuteTime(new Date().getTime());
            cacheService.lLeftPush("1001_1", JSON.toJSONString(task));
        }
        System.out.println("耗时"+(System.currentTimeMillis()- start));
    }

    //耗时654ms  redis管道
    @Test
    public void testPiple2(){
        long start  = System.currentTimeMillis();
        //使用管道技术
        List<Object> objectList = cacheService.getstringRedisTemplate().executePipelined(new RedisCallback<Object>() {
            @Nullable
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                for (int i = 0; i <10000 ; i++) {
                    Task task = new Task();
                    task.setTaskType(1001);
                    task.setPriority(1);
                    task.setExecuteTime(new Date().getTime());
                    redisConnection.lPush("1001_1".getBytes(), JSON.toJSONString(task).getBytes());
                }
                return null;
            }
        });
        System.out.println("使用管道技术执行10000次自增操作共耗时:"+(System.currentTimeMillis()-start)+"毫秒");
    }
}
