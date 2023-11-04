//package cn.cnic.instdb.config;
//
//import cn.cnic.instdb.constant.Constant;
//import cn.cnic.instdb.model.findata.PushFinDatasParam;
//import cn.cnic.instdb.model.findata.PushFinDatasParamVo;
//import cn.cnic.instdb.model.resources.ResourcesManage;
//import cn.cnic.instdb.service.CommunityService;
//import cn.cnic.instdb.service.impl.CommunityServiceImpl;
//import cn.cnic.instdb.utils.ServiceUtils;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.eclipse.rdf4j.query.algebra.Str;
//import org.springframework.beans.BeanUtils;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.SchedulingConfigurer;
//import org.springframework.scheduling.config.CronTask;
//import org.springframework.scheduling.config.ScheduledTaskRegistrar;
//import org.springframework.scheduling.support.CronTrigger;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//
//@EnableScheduling
//@Component
//@Slf4j
//public class FinDataTask implements SchedulingConfigurer {
//
//
//    private ServiceUtils serviceUtils;
//    private MongoTemplate mongoTemplate;
//    private static volatile ConcurrentHashMap<Integer, CronTask> cronTasks = new ConcurrentHashMap<Integer, CronTask>();
//
//
//
//    public FinDataTask(MongoTemplate mongoTemplate, ServiceUtils serviceUtils){
//        this.mongoTemplate = mongoTemplate;
//        this.serviceUtils = serviceUtils;
//        ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();
//        this.configureTasks(scheduledTaskRegistrar);
//    }
//
//
//
//
//    Runnable runnable = new Runnable() {
//        @SneakyThrows
//        @Override
//        public void run() {
//            log.info("Data recommendationFindataData recommendation");
//            List<PushFinDatasParam> pushFinDatasParams = mongoTemplate.find(new Query(), PushFinDatasParam.class);
//            if (null != pushFinDatasParams && pushFinDatasParams.size() > 0) {
//                PushFinDatasParam pushFinDatasParam = pushFinDatasParams.get(0);
//                PushFinDatasParamVo pushFinData = new PushFinDatasParamVo();
//                BeanUtils.copyProperties(pushFinDatasParam, pushFinData);
//                Query query = ServiceUtils.queryDataByFindata(pushFinDatasParam.getType(), pushFinData);
//                List<ResourcesManage> resourcesManages = mongoTemplate.find(query, ResourcesManage.class);
//                if (null != resourcesManages && resourcesManages.size() > 0) {
//                    serviceUtils.dataPushFinData(Constant.BATCH, resourcesManages);
//                }
//            }
//        }
//    };
//
//    @Override
//    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
//        scheduledTaskRegistrar.addTriggerTask(
//                //1.Add task content(Runnable)
//                () -> runnable.run(),//Task content
//                //2.Set execution cycle(Trigger)Set execution cycle
//                triggerContext -> {
//
//                    String cron = "";
//                    try {
//                        CronTrigger trigger;
//                        List<PushFinDatasParam> pushFinDatasParams = mongoTemplate.find(new Query(), PushFinDatasParam.class);
//                        if (null != pushFinDatasParams && pushFinDatasParams.size() > 0) {
//                            PushFinDatasParam pushFinDatasParam = pushFinDatasParams.get(0);
//                            if (StringUtils.isNotBlank(pushFinDatasParam.getCron())) {
//                                cron = pushFinDatasParam.getCron();
//                            }
//                        }
//                        // Once set，Once set
//                        trigger = new CronTrigger(cron);
//                        log.info("Data recommendationFindataData recommendation");
//                        return trigger.nextExecutionTime(triggerContext);
//                    } catch (Exception e) {
//                        CronTrigger trigger;
//                        // If there are any formatting issues, follow the default time（If there are any formatting issues, follow the default time23If there are any formatting issues, follow the default time55If there are any formatting issues, follow the default time）
//                        trigger = new CronTrigger("* 59 22 31 10 ? ");
//                        log.error(cron + "Expression error，Expression error * 59 22 31 10 ? ");
//                        return trigger.nextExecutionTime(triggerContext);
//                    }
//                }
//
//        );
//    }
//}
