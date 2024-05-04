package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.ScheduleConstants;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import com.heima.common.redis.CacheService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskinfoMapper taskinfoMapper;
    private final TaskinfoLogsMapper taskinfoLogsMapper;

    private final  CacheService cacheService;

    public TaskServiceImpl(TaskinfoMapper taskinfoMapper, TaskinfoLogsMapper taskinfoLogsMapper, CacheService cacheService){

        this.taskinfoMapper = taskinfoMapper;
        this.taskinfoLogsMapper = taskinfoLogsMapper;
        this.cacheService = cacheService;
    }
    /**
     * 添加延迟任务
     * @param task   任务对象
     * @return taskId
     */
    public long addTask(Task task) {
        //1.添加任务到数据库
        boolean success = addTaskToDb(task);

        if(success){
            //2.添加任务到redis
            addTaskToCache(task);
        }
        return task.getTaskId();
    }


    public boolean cancelTask(long taskId) {
        boolean flag = false;
        //删除任务，更新任务日志
        Task task = updateDb(taskId, ScheduleConstants.CANCELLED);
        
        //删除redis中的数据
        if(task != null){
            removeTaskFromCache(task);
            flag = true;
        }
        return flag;
    }

    public Task poll(int type, int priority) {
        Task task = null;
        try {
            String key = type + "_" + priority;
            //从redis中拉取
            String task_json = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
            if (StringUtils.isNotBlank(task_json)) {
                task = JSON.parseObject(task_json, Task.class);
                //修改数据库信息
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        } catch (Exception e) {
            log.error("poll task exception");
            throw new RuntimeException(e);
        }
        return task;
    }
    private void removeTaskFromCache(Task task) {
        String key = task.getTaskType() + "_" +task.getPriority();
        if(task.getExecuteTime() <= System.currentTimeMillis()){
            cacheService.lRemove(ScheduleConstants.TOPIC + key, 0, JSON.toJSONString(task));
        }else{

            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));
        }
    }

    private Task updateDb(long taskId, int status) {
        Task task = null;
        try {
            //删除任务
            taskinfoMapper.deleteById(taskId);
            //更新任务日志
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);
            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task);
            //executeTime类型不一致 拷贝不了
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (Exception e) {
            log.error("task cancel exception taskId:{}", taskId);
        }
        return task;
    }

    /**
     * 添加任务到redis
     * @param task
     */
    private void addTaskToCache(Task task) {
        String key = task.getTaskType() + "_" +task.getPriority();
        //获取5分钟之后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        //转为ms
        long nextScheduleTime = calendar.getTimeInMillis();
        //2.1 如果任务执行时间<=当前时间 存入list
        if(task.getExecuteTime() <= System.currentTimeMillis()){
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        }else if(task.getExecuteTime() <= nextScheduleTime){
            //2.2 >当前时间 且 <=预设时间（未来5分钟） 存入zset
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }

    }

    /**
     * 添加任务到数据库
     * @param task
     * @return
     */
    private boolean addTaskToDb(Task task) {
        boolean flag = false;
        //保存任务表
        try {
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);
            //设置TaskId
            task.setTaskId(taskinfo.getTaskId());
            //保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogs.setVersion(1);
            taskinfoLogsMapper.insert(taskinfoLogs);
            flag = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return flag;
    }

    /**
     * 未来任务定时刷新 zSet-->list
     * corn：每分钟执行一次
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void refresh(){
        //分布式锁 多个服务启动时保证只有一台进行此工作 避免重复工作
        String token = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);
        if(StringUtils.isNotBlank(token)){
            log.info("未来任务定时刷新");
            //获取所有未来任务的集合
            Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
            for (String futureKey : futureKeys) { //future_100_50  -> topic_100_50
                //获取当前任务的key
                String topicKey = ScheduleConstants.TOPIC + futureKey.split(ScheduleConstants.FUTURE)[1];
                //按照key和分值查询符合条件的key
                Set<String> tasks = cacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());

                //同步数据
                if(!tasks.isEmpty()){
                    //从未来zSet中删除 添加到当前list中
                    cacheService.refreshWithPipeline(futureKey, topicKey, tasks);
                    log.info("成功将{}刷新到了{}", futureKey, topicKey);
                }
            }
        }
    }
    /**
     * 数据库任务定时同步到redis
     */
    @PostConstruct //微服务启动时执行
    @Scheduled(cron = "0 */5 * * * ?")
    public void  reloadData(){
        //清理缓存中的数据 list和zSet
        cleanCache();
        //查询符合体条件的任务 <=未来5分钟
        //获取5分钟之后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        List<Taskinfo> taskinfoList = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().le(Taskinfo::getExecuteTime, calendar.getTime()));
        //把任务添加到redis
        if(taskinfoList != null && !taskinfoList.isEmpty()){
            for (Taskinfo taskinfo : taskinfoList) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTaskToCache(task);
            }
        }
        log.info("数据库的任务同步到了redis");
    }

    /**
     * 清理缓存中的数据
     */
    public void cleanCache(){
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        cacheService.delete(topicKeys);
        cacheService.delete(futureKeys);
    }
}
