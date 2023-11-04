package cn.cnic.instdb.config;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.findata.PushFinDatasParam;
import cn.cnic.instdb.model.findata.PushFinDatasParamVo;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.special.SpecialResources;
import cn.cnic.instdb.utils.ServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;


@Component
@Slf4j
public class ScheduledTask implements SchedulingConfigurer {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ServiceUtils serviceUtils;

    private static volatile ScheduledTaskRegistrar registrar;
    private static volatile ConcurrentHashMap<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<String, ScheduledFuture<?>>();
    private static volatile ConcurrentHashMap<String, CronTask> cronTasks = new ConcurrentHashMap<String, CronTask>();

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        //set up20set up,set up
        registrar.setScheduler(Executors.newScheduledThreadPool(3));
        this.registrar = registrar;
    }

    public void refresh(String taskId, String expression) {

        //Cancel deleted policy tasks
        ScheduledFuture<?> scheduledFuture = scheduledFutures.get(taskId);
        if (null == scheduledFuture && StringUtils.isNotBlank(expression)) {
            addTask(expression, taskId);
        } else {
            //Skip if the planned task already exists and the expression has not changed
            if (scheduledFutures.containsKey(taskId) && !cronTasks.get(taskId).getExpression().equals(expression)) {
                //If the execution time of the policy changes，If the execution time of the policy changes
                if (scheduledFutures.containsKey(taskId)) {
                    scheduledFutures.get(taskId).cancel(false);
                    scheduledFutures.remove(taskId);
                    cronTasks.remove(taskId);
                    log.info("Closed");
                }
                if (StringUtils.isNotBlank(expression)) {
                    addTask(expression, taskId);
                }else {
                    log.info("Creation task not executed，Creation task not executedexpression："+expression);
                }
            }
        }

    }


    private void addTask(String expression, String taskId) {
        CronTask task = new CronTask(new Runnable() {
            @Override
            public void run() {
                //The specific business logic that each planned task actually needs to execute
                log.info("Data recommendationFindataData recommendation");
                List<PushFinDatasParam> pushFinDatasParams = mongoTemplate.find(new Query(), PushFinDatasParam.class);
                if (null == pushFinDatasParams || 0 == pushFinDatasParams.size()) {
                    log.info("No data matched-No data matchedFindataNo data matched");
                    return;
                }
                PushFinDatasParam pushFinDatasParam = pushFinDatasParams.get(0);
                    PushFinDatasParamVo pushFinData = new PushFinDatasParamVo();
                    BeanUtils.copyProperties(pushFinDatasParam, pushFinData);
                    Query query = ServiceUtils.queryDataByFindata(pushFinDatasParam.getType(), pushFinData);
                    List<ResourcesManage> resourcesManages = mongoTemplate.find(query, ResourcesManage.class);

                    //Situations with special topics
                    List<String> special = pushFinDatasParam.getSpecial();
                    if (null != special && special.size() > 0) {
                        Query querySpecial = new Query();
                        querySpecial.addCriteria(Criteria.where("specialId").in(special));
                        List<SpecialResources> specialResources = mongoTemplate.find(querySpecial, SpecialResources.class);
                        if (null != specialResources && !specialResources.isEmpty()) {
                            Set<String> set = new TreeSet<>();
                            for (SpecialResources data : specialResources) {
                                set.add(data.getResourcesId());
                            }
                            List<ResourcesManage> resourcesManagesList = new ArrayList<>();
                            Query queryResourcesManage = new Query();
                            queryResourcesManage.addCriteria(Criteria.where("_id").in(set));
                            List<ResourcesManage> resourcesManages1 = mongoTemplate.find(queryResourcesManage, ResourcesManage.class);
                            if(null!= resourcesManages && resourcesManages.size()>0){
                                if(null!= resourcesManages1 && resourcesManages1.size()>0){

                                    for (ResourcesManage resourcesManage:resourcesManages) {
                                        for (ResourcesManage resourcesManage1:resourcesManages1) {
                                            if(resourcesManage.getId().equals(resourcesManage1.getId())){
                                                resourcesManagesList.add(resourcesManage);
                                            }
                                        }
                                    }
                                    resourcesManages = resourcesManagesList;
                                }
                            }
                        }
                    }

                    if (null != resourcesManages && resourcesManages.size() > 0) {
                        serviceUtils.dataPushFinData(Constant.BATCH, resourcesManages);
                        log.info("Data recommendationFindataData recommendation");
                    }
            }
        }, expression);
        if (null != task && StringUtils.isNotBlank(task.getExpression()) && null != task.getTrigger() && null != registrar) {
            ScheduledFuture<?> future = registrar.getScheduler().schedule(task.getRunnable(), task.getTrigger());
            cronTasks.put(taskId, task);
            scheduledFutures.put(taskId, future);
        }
    }


}
