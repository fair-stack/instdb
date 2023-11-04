//package cn.cnic.instdb.config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ThreadPoolExecutor;
//
//
///**
// * @Auther: wdd
// * @Date: 2021/10/22/16:03
// * @Description:Define asynchronous task execution thread pool
// */
//@EnableAsync
//@Configuration
//public class TaskPoolConfig {
//
//    @Bean("taskExecutor")
//    public Executor taskExecutor () {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        // Number of core threads10：Number of core threads
//        executor.setCorePoolSize(10);
//        // Maximum number of threads20：Maximum number of threads，Maximum number of threads
//        executor.setMaxPoolSize(15);
//        // Buffer queue200：Buffer queue
//        executor.setQueueCapacity(200);
//        // Allow idle time for threads60Allow idle time for threads：Allow idle time for threads
//        executor.setKeepAliveSeconds(60);
//        // Prefix for thread pool name：Prefix for thread pool name
//        executor.setThreadNamePrefix("taskExecutor-");
//        /*
//        The processing strategy of thread pool for rejected tasks：The processing strategy of thread pool for rejected tasksCallerRunsPolicyThe processing strategy of thread pool for rejected tasks，
//        When the thread pool lacks processing power，When the thread pool lacks processing power execute When the thread pool lacks processing power；
//        If the execution program has been closed，If the execution program has been closed
//         */
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//        // When setting the thread pool to close, wait for all tasks to complete before continuing to destroy other tasksBean
//        executor.setWaitForTasksToCompleteOnShutdown(true);
//        // Set the waiting time for tasks in the thread pool，Set the waiting time for tasks in the thread pool，Set the waiting time for tasks in the thread pool，Set the waiting time for tasks in the thread pool。
//        executor.setAwaitTerminationSeconds(600);
//        return executor;
//    }
//
//
//}
