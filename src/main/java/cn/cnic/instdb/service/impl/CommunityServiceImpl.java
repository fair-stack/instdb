package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.config.ScheduledTask;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.findata.PushFinDatas;
import cn.cnic.instdb.model.findata.PushFinDatasParam;
import cn.cnic.instdb.model.findata.PushFinDatasParamVo;
import cn.cnic.instdb.model.resources.Community;
import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.special.SpecialResources;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.CommunityService;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@EnableScheduling
public class CommunityServiceImpl implements CommunityService {

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private AsyncDeal asyncDeal;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private ServiceUtils serviceUtils;

    @Resource
    private SettingService settingService;

    @Resource
    private MongoUtil mongoUtil;


    @Resource
    private ScheduledTask scheduledTask;


    @Override
    public Result getScidbCommunity(String token, String apiKey, String name) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doGetScidb(Constant.ScidbUrl.METRICS, apiKey);
            Map resultMap = JSONObject.parseObject(result, Map.class);
            if (resultMap.containsKey("status")) {
                return ResultUtils.errorOld("Failed to correctly match community information，Failed to correctly match community informationapiKeyFailed to correctly match community information");
            }
            int code = (int) resultMap.get("code");
            if (code == 20000) {
                JSONObject data = (JSONObject) resultMap.get("data");
                Community community = new Community();
                community.setId(apiKey);
                community.setName(name);
                community.setCreateTime(LocalDateTime.now());
                community.setState(Constant.Approval.YES);
                community.setStatus(Constant.Approval.NO);
                community.setNextTime(DateUtils.getCronNextValidTimeAfter("0 0 0 * * ?"));
                community.setByte_size(data.getIntValue("byte_size"));
                community.setVisit(data.getIntValue("visit"));
                community.setFiles_count(data.getIntValue("files_count"));
                community.setDatasets(data.getIntValue("datasets"));
                community.setDownload(data.getIntValue("download"));
                mongoTemplate.save(community);
                log.info("Community data acquisition and normal saving completed");
                return ResultUtils.success();
            } else {
                return ResultUtils.errorOld("scidbInterface response error，Interface response error：" + code + ",Interface response error：" + resultMap.get("message"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            return ResultUtils.error("scidbInterface call failed,Interface call failed");
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public void getScidbCommunityData() {
        log.info("The data extraction task has started");
        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("state").is(Constant.Approval.YES), Criteria.where("state").is(null)));
        List<Community> communities = mongoTemplate.find(query, Community.class);
        if (null != communities && communities.size() > 0) {
            for (Community community : communities) {
                Query queryCommunity = new Query();
                queryCommunity.addCriteria(Criteria.where("_id").is(community.getId()));

                Update update = new Update();
                update.set("nextTime", DateUtils.getCronNextValidTimeAfter("0 0 0 * * ?"));
                update.set("completeTime", LocalDateTime.now());
                update.set("status", Constant.Approval.YES);
                mongoTemplate.updateFirst(queryCommunity, update, Community.class);
                try {
                    int pageNo = 1;
                    int pageSize = 10;
                    JSONArray data;
                    while (null != (data = getData(pageNo, pageSize, community.getId())) && data.size() > 0) {//Readers can achieve pagination queries on their own
                        pageNo++;//Calculate the start position of the next query
                        log.info("Processing data in a loop，Processing data in a loop" + data.size() + "Processing data in a loop");
                        asyncDeal.setScidbCommunityData(data, community.getId());
                    }
                    //Task End Status Record
                    update.set("status", Constant.Approval.NO);
                    mongoTemplate.updateFirst(queryCommunity, update, Community.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("scidbInterface call failed,Interface call failed");
                    //Task End Status Record
                    update.set("status", Constant.Approval.NO);
                    mongoTemplate.updateFirst(queryCommunity, update, Community.class);
                    break;
                }
            }
        }
        log.info("The data extraction task has ended");
    }

    @Override
    public Result updateScidbCommunity(String token, String apiKey, String name) {

        if (StringUtils.isBlank(apiKey) || StringUtils.isBlank(name)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(apiKey));
        Community community = mongoTemplate.findOne(query, Community.class);
        if (null == community) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

        Update update = new Update();
        update.set("name", name);
        update.set("updateTime", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Community.class);
        return ResultUtils.success("UPDATE_SUCCESS");
    }



    @Override
    public Result deleteScidbCommunity(String token, String apiKey) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(apiKey));
        Community community = mongoTemplate.findOne(query, Community.class);
        if (null == community) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

        Query queryF = new Query();
        queryF.addCriteria(Criteria.where("scidb_communityId").is(apiKey));
        long count = mongoTemplate.count(queryF, Constant.RESOURCE_COLLECTION_NAME);
        if (count > 0) {
            return ResultUtils.error("COMMUNITY_ALREADY_EXISTS");
        }

        mongoTemplate.remove(query, Community.class);
        return ResultUtils.success("DELETE_SUCCESS");
    }

    @Override
    public Result disableCommunityState(String token, String apiKey, String state) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(apiKey));
        Community community = mongoTemplate.findOne(query, Community.class);
        if (null == community) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

        Update update = new Update();
        update.set("state", state);
        update.set("updateTime", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Community.class);
        return ResultUtils.success(Constant.Approval.YES.equals(state) ? "TASK_START" : "TASK_STOP");
    }

    @Override
    public Result getCommunityList(String token, String name) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Query query = new Query();
        if (StringUtils.isNotBlank(name)) {
            query.addCriteria(Criteria.where("name").regex(name));
        }
        List<Community> communities = mongoTemplate.find(query, Community.class);
        if (null != communities && communities.size() > 0) {
            for (Community community : communities) {
                //Number of query data resources
                Query queryResourcesManage = new Query();
                queryResourcesManage.addCriteria(Criteria.where("scidb_communityId").is(community.getId()));
                List<ResourcesManage> resourcesManages = mongoTemplate.find(queryResourcesManage, ResourcesManage.class);
                Query queryCommunity = new Query();
                queryCommunity.addCriteria(Criteria.where("_id").is(community.getId()));
                Update update = new Update();
                update.set("sync_count", resourcesManages.size());
                //To initialize  To initialize  To initialize
                if (StringUtils.isBlank(community.getState())) {
                    update.set("state", Constant.Approval.YES);
                }
                mongoTemplate.updateFirst(queryCommunity, update, Community.class);
            }

        }
        return ResultUtils.success(communities);
    }

    @Override
    public void getFtpFileByDoi() {
        Query query = new Query();
        List<Community> communities = mongoTemplate.find(query, Community.class);
        if (null != communities && communities.size() > 0) {
            for (Community community : communities) {
                Query queryResources = new Query();
                queryResources.addCriteria(Criteria.where("scidb_communityId").is(community.getId()));
                queryResources.addCriteria(Criteria.where("downloadFileFlag").is("false"));
                List<ResourcesManage> resourcesManages = mongoTemplate.find(queryResources, ResourcesManage.class);

                if (null != resourcesManages && resourcesManages.size() > 0) {
                    log.info(community.getName() + "community--communitydownloadFileFlagcommunityfalsecommunity");
                    for (ResourcesManage resourcesManage : resourcesManages) {
                        if (!Constant.VERSION_FLAG.equals(resourcesManage.getDownloadFileFlag())) {
                            log.info("Processing" + resourcesManage.getName());
                            //Delete the corresponding file
                            asyncDeal.deleteDirectory(instdbUrl.getResourcesFilePath() + resourcesManage.getId());
                            //Delete Dataset File Content
                            Query removeQuery = new Query();
                            removeQuery.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
                            mongoTemplate.remove(removeQuery, ResourceFileTree.class);
                            asyncDeal.getDataFile(resourcesManage.getDoi(), resourcesManage.getId(), community.getId());
                        }
                    }
                } else {
                    log.error(community.getName() + "community--communitydownloadFileFlagcommunityfalsecommunity");
                }
            }
        } else {
            log.error("No community");
        }
    }

    @Override
    public Result manualPushFinData(String type, List<String> ids) {
        CenterAccount centerConf = settingService.getCenterConf();
        if (StringUtils.isBlank(centerConf.getOrgName())) {
            return ResultUtils.error("CENTER_SET");
        }
        if (null != ids && ids.size() > 0) {
            Query queryResources = new Query();
            queryResources.addCriteria(Criteria.where("_id").in(ids));
            List<ResourcesManage> resourcesManages = mongoTemplate.find(queryResources, ResourcesManage.class);
            if (resourcesManages.size() > 0) {
                if (StringUtils.isBlank(type) || !Constant.Approval.YES.equals(type)) {
                    return ResultUtils.success("Checked in total" + resourcesManages.size() + "Checked in total，Checked in total？");
                }
                return serviceUtils.dataPushFinData(Constant.MANUAL, resourcesManages);
            }
        }
        return ResultUtils.error("SYSTEM_ERROR");
    }


    @Override
    public PageHelper getPushFinDatas(String type, String resourceType, String version, String name, String startDate, String endDate, Integer pageOffset, Integer pageSize) {
        Query query = new Query();
        if (StringUtils.isNotBlank(name)) {
            query.addCriteria(Criteria.where("name").regex(name));
        }
        if (StringUtils.isNotBlank(type)) {
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (StringUtils.isNotBlank(resourceType)) {
            query.addCriteria(Criteria.where("resourceType").is(resourceType));
        }
        if (StringUtils.isNotBlank(version)) {
            query.addCriteria(Criteria.where("version").is(version));
        }
        //Sort records by creation time
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            Criteria criteria = new Criteria();
            query.addCriteria(criteria.andOperator(Criteria.where("createTime").gte(DateUtils.getLocalDateTimeByString2(startDate)),
                    Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(endDate))));
        } else if (StringUtils.isNotBlank(startDate)) {
            query.addCriteria(Criteria.where("createTime").gte(DateUtils.getLocalDateTimeByString2(startDate)));
        } else if (StringUtils.isNotBlank(endDate)) {
            query.addCriteria(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(endDate)));
        }
        long count = mongoTemplate.count(query, PushFinDatas.class);
        mongoUtil.start(pageOffset, pageSize, query);
        List<PushFinDatas> pushFinDatas = mongoTemplate.find(query, PushFinDatas.class);

        return mongoUtil.pageHelper(count, pushFinDatas);
    }

    @Override
    public Result getFindataStatistics() {
        CenterAccount centerConf = settingService.getCenterConf();
        if (StringUtils.isBlank(centerConf.getOrgName())) {
            return ResultUtils.error("CENTER_SET");
        }
        HashMap map = new HashMap();
        map.put("downloads", 0);
        map.put("views", 0);
        map.put("references", 0);
        map.put("recommend", 0);
        map.put("url", instdbUrl.getFindataUrl());

        List<PushFinDatas> pushFinDatas = mongoTemplate.find(new Query(), PushFinDatas.class);
        if (null != pushFinDatas && pushFinDatas.size() > 0) {
            List<PushFinDatas> firstMenu = pushFinDatas.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PushFinDatas::getResourcesId))), ArrayList::new));
            map.put("recommend", firstMenu.size());
        }


        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("from", centerConf.getOrgName()));
        HttpClient httpClient = new HttpClient();
        String result = null;
        try {
            result = httpClient.doPostJsonWayTwo(params, instdbUrl.getFindataAPIUrl() + "/collection/toInstdb");
            Map resultMap = JSONObject.parseObject(result, Map.class);
            int code = (int) resultMap.get("code");
            if (code == 200) {
                Map data = (Map) resultMap.get("data");
                if (data.size() > 1) {
                    map.put("downloads", null != data.get("downloads") ? data.get("downloads").toString() : 0);
                    map.put("views", null != data.get("views") ? data.get("views").toString() : 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }
        return ResultUtils.success(map);
    }

    @Override
    public Result batchPushDataToFindata(String token, PushFinDatasParamVo pushFinDatasParam) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        String usernameFromToken = jwtTokenUtils.getUsernameFromToken(token);
                //Record recommended configurations
        List<PushFinDatasParam> pushFinDatasParams = mongoTemplate.find(new Query(), PushFinDatasParam.class);

        //executeOnce Execute once day Execute once  week Execute once  monthExecute once closeExecute once
        String type = pushFinDatasParam.getType();

        Query query = ServiceUtils.queryDataByFindata(type, pushFinDatasParam);
        //Closed situation
        if ("close".equals(type)) {
            if (null != pushFinDatasParams & pushFinDatasParams.size() > 0) {
                Query query1 = new Query(Criteria.where("_id").is(pushFinDatasParams.get(0).getId()));
                Update update = new Update();
                update.set("type", type);
                mongoTemplate.updateFirst(query1, update, PushFinDatasParam.class);
                scheduledTask.refresh(pushFinDatasParams.get(0).getId(), "");
            }
            return ResultUtils.success();
        }
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

        if (null != resourcesManages) {
            if (StringUtils.isBlank(pushFinDatasParam.getRun()) || !Constant.Approval.YES.equals(pushFinDatasParam.getRun())) {
                return ResultUtils.success("According to the criteria, a total of" + resourcesManages.size() + "According to the criteria, a total of，According to the criteria, a total of？");
            }
        }

        PushFinDatasParam pushFinData = new PushFinDatasParam();
        BeanUtils.copyProperties(pushFinDatasParam, pushFinData);
        //Operator records
        pushFinData.setAuthor(usernameFromToken);
        pushFinData.setCreateTime(LocalDateTime.now());
        //If it already exists If it already exists
        if (null != pushFinDatasParams && pushFinDatasParams.size() > 0) {
            pushFinData.setId(pushFinDatasParams.get(0).getId());
            pushFinData.setStatus(pushFinDatasParams.get(0).getStatus());
        }
        if ("day".equals(type)) {
            //Every morning 00:01Every morning
            pushFinData.setCron("0 1 0 ? * * ");
            log.info("Every day");
        } else if ("week".equals(type)) {
            //Updated every Sunday night 23:59
            pushFinData.setCron("0 59 23 ? * 7 ");
            log.info("weekly");
        } else if ("month".equals(type)) {
            //Every month's1Every month's
            pushFinData.setCron("0 0 1 1 * ? ");
            log.info("monthly");
        } else if ("executeOnce".equals(type)) {
            pushFinData.setCron("");
            log.info("Manual operation");
        }

        if (!"executeOnce".equals(type)) {
            mongoTemplate.save(pushFinData);
            scheduledTask.refresh(pushFinData.getId(), pushFinData.getCron());
        }else if(0 == pushFinDatasParams.size()){
            pushFinData.setStatus(Constant.Approval.YES);
            mongoTemplate.save(pushFinData);
        }

        return serviceUtils.dataPushFinData(Constant.BATCH, resourcesManages);
    }

    @Override
    public Result getPushDataToFindataConfig(String token) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        List<PushFinDatasParam> pushFinDatasParams = mongoTemplate.find(new Query(), PushFinDatasParam.class);
        if (null != pushFinDatasParams && pushFinDatasParams.size() > 0) {
            return ResultUtils.success(pushFinDatasParams.get(0));
        }
        return ResultUtils.success();
    }

    @Override
    public Result setfindataStatus(String token, String status) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        if (StringUtils.isBlank(status)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        List<PushFinDatasParam> pushFinDatasParams = mongoTemplate.find(new Query(), PushFinDatasParam.class);
        if (null != pushFinDatasParams && pushFinDatasParams.size() > 0) {
            Update update = new Update();
            Query query = new Query();
            PushFinDatasParam pushFinDatasParam = pushFinDatasParams.get(0);
            query.addCriteria(Criteria.where("_id").is(pushFinDatasParam.getId()));
            update.set("status", status);

            if (!"close".equals(pushFinDatasParam.getType()) && !"executeOnce".equals(pushFinDatasParam.getType())) {
                if (StringUtils.isNotBlank(pushFinDatasParam.getCron()) && Constant.Approval.YES.equals(status)) {
                    scheduledTask.refresh(pushFinDatasParams.get(0).getId(), pushFinDatasParam.getCron());
                    log.info("recommendationfindatarecommendation--->" + pushFinDatasParam.getType() + ",cron:" + pushFinDatasParam.getCron());
                } else if (Constant.Approval.NO.equals(status)) {
                    scheduledTask.refresh(pushFinDatasParams.get(0).getId(), "");
                    log.info("recommendationfindatarecommendation--->");
                }
            }

            mongoTemplate.upsert(query, update, PushFinDatasParam.class);
            return ResultUtils.success();
        }
        return ResultUtils.success();
    }


//    public static void main(String[] args) {
//        int pageNo = 1;
//        int pageSize = 2;
//        JSONArray data ;
//        while ((data = getData(pageNo, pageSize)) != null && data.size() >0 ){//Readers can achieve pagination queries on their own
//            pageNo++;//Calculate the start position of the next query
//            for (int i = 0; i < data.size(); i++) {
//                JSONObject jsonObject = data.getJSONObject(i);
//                String dataSetId = jsonObject.getString("dataSetId");
//                System.out.println(dataSetId);
//            }
//        }
//
//    }

    private JSONArray getData(int pageNo, int pageSize, String appKey) {
        //Update synchronization time
        Query queryUpdate = new Query();
        queryUpdate.addCriteria(Criteria.where("_id").is(appKey));
        try {
            //Start calling interface
            HttpClient httpClient = new HttpClient();
            String result = httpClient.doGetScidb(Constant.ScidbUrl.HARVEST + "?pageNo=" + pageNo + "&pageSize=" + pageSize + "", appKey);
            Map resultMap = JSONObject.parseObject(result, Map.class);
            if (resultMap.containsKey("status")) {
                log.error(Constant.ScidbUrl.HARVEST + "Request data reporting error，Request data reporting errorapiKeyRequest data reporting error");
                Update update1 = new Update();
                update1.set("logs", Constant.ScidbUrl.HARVEST + "Request data reporting error，Request data reporting errorapiKeyRequest data reporting error");
                mongoTemplate.updateFirst(queryUpdate, update1, Community.class);
                return null;
            }
            int code = (int) resultMap.get("code");
            if (code == 20000) {
                JSONObject data = (JSONObject) resultMap.get("data");
                JSONArray journalZhList = data.getJSONArray("list");
                if (null != journalZhList && journalZhList.size() > 0) {
                    return journalZhList;
                }
            } else {
                Update updateLog = new Update();
                updateLog.set("logs", "scidbInterface response error，Interface response error：" + code + ",Interface response error：" + resultMap.get("message"));
                mongoTemplate.updateFirst(queryUpdate, updateLog, Community.class);
                log.error("scidbInterface response error，Interface response error：" + code + ",Interface response error：" + resultMap.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            log.error("scidbInterface call failed,Interface call failed");
            Update updateError = new Update();
            updateError.set("logs", "scidbAbnormal extraction of community data,Abnormal extraction of community data");
            mongoTemplate.updateFirst(queryUpdate, updateError, Community.class);
        }

        return null;
    }

}
