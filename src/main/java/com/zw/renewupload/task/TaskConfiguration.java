package com.zw.renewupload.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author LXR
 * @className TaskConfiguration
 * @date 2019/12/17 11:40
 * @description
 */


@EnableAsync
@Configuration
public class TaskConfiguration implements AsyncConfigurer {

    private static Logger logger = LogManager.getLogger(TaskConfiguration.class);

    @Value("${thread.pool.corePoolSize:10}")
    private int corePoolSize;

    @Value("${thread.pool.maxPoolSize:20}")
    private int maxPoolSize;

    @Value("${thread.pool.keepAliveSeconds:4}")
    private int keepAliveSeconds;

    @Value("${thread.pool.queueCapacity:512}")
    private int queueCapacity;

    @Value("${thread.pool.waitForTasksToCompleteOnShutdown:true}")
    private boolean waitForTasksToCompleteOnShutdown;

    @Value("${thread.pool.awaitTerminationSeconds:60}")
    private int awaitTerminationSeconds;


    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数
        executor.setCorePoolSize(corePoolSize);
        //线程池最大的线程数，只有在缓冲队列满了之后，才会申请超过核心线程数的线程
        executor.setMaxPoolSize(maxPoolSize);
        //允许线程的空闲时间,当超过了核心线程之外的线程,在空闲时间到达之后会被销毁
        executor.setKeepAliveSeconds(keepAliveSeconds);
        ////用来缓冲执行任务的队列
        executor.setQueueCapacity(queueCapacity);
        //线程池名的前缀,可以用于定位处理任务所在的线程池
        executor.setThreadNamePrefix("taskExecutor-");
        //线程池对拒绝任务的处理策略
        executor.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor exe) -> {
            logger.warn("当前任务线程池队列已满.");
        });
        //该方法用来设置线程池关闭的时候等待所有任务都完成后,再继续销毁其他的Bean，这样这些异步任务的销毁就会先于数据库连接池对象的销毁。
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        //该方法用来设置线程池中,任务的等待时间,如果超过这个时间还没有销毁就强制销毁,以确保应用最后能够被关闭,而不是阻塞住。
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> logger.error("线程池执行任务发生未知异常.", ex);
    }
}