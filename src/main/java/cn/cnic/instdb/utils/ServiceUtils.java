package cn.cnic.instdb.utils;


import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.config.BasicConfigurationVo;
import cn.cnic.instdb.model.findata.DatasetFromInstdb;
import cn.cnic.instdb.model.findata.FileInfoFromInstdb;
import cn.cnic.instdb.model.findata.PushFinDatas;
import cn.cnic.instdb.model.findata.PushFinDatasParamVo;
import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.service.SystemConfigService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommonUtils
 *
 * @author wangCc
 * @date 2018/11/2
 */

@Slf4j
@Component
public final class ServiceUtils {

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private SettingService settingService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private TransportClient client;


    public static Query queryDataByFindata(String type, PushFinDatasParamVo pushFinDatasParam) {

        //executeOnce Execute once day Execute once  week Execute once  monthExecute once closeExecute once
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        switch (type) {
            case "day":
            case "week":
            case "month":
            case "executeOnce":
                List<String> resourceType = pushFinDatasParam.getResourceType();
                if (null != resourceType && resourceType.size() > 0) {
                    query.addCriteria(Criteria.where("resourceType").in(resourceType));
                }
                List<String> license = pushFinDatasParam.getLicense();
                if (null != license && license.size() > 0) {
                    query.addCriteria(Criteria.where("license").in(license));
                }
                List<String> subject = pushFinDatasParam.getSubject();
                if (null != subject && subject.size() > 0) {
                    query.addCriteria(Criteria.where("subject").in(subject));
                }
                List<String> keywords = pushFinDatasParam.getKeywords();
                if (null != keywords && keywords.size() > 0) {
                    query.addCriteria(Criteria.where("keywords").in(keywords));
                }
                List<String> privacyPolicy = pushFinDatasParam.getPrivacyPolicy();
                if (null != privacyPolicy && privacyPolicy.size() > 0) {
                    query.addCriteria(Criteria.where("privacyPolicy.type").in(privacyPolicy));
                }
                List<String> year = pushFinDatasParam.getYear();
                if (null != year && year.size() > 0) {
                    for (String y : year) {
                        Criteria criteria = new Criteria();
                        criteria.andOperator(Criteria.where("approveTime").gte(DateUtils.getLocalDateTimeByString2(y + "-01-01")),
                                Criteria.where("approveTime").lte(DateUtils.getLocalDateTimeByString2(y + "-12-31")));
                        query.addCriteria(criteria);
                        break;
                    }
                }
                break;
            case "close":
                break;
            default:
                return query;
        }
        return query;

    }


    public Result dataPushFinData(String type, List<ResourcesManage> resourcesManages) {
        log.info("I'm pushing data");
        CenterAccount centerConf = settingService.getCenterConf();
        if (StringUtils.isBlank(centerConf.getOrgName())) {
            return ResultUtils.error("CENTER_SET");
        }
        BasicConfigurationVo indexCopyrightLinks = systemConfigService.getBasicConfig();
        List<DatasetFromInstdb> instdbList = new ArrayList<>();
        List<PushFinDatas> pushFinDatasLists = new ArrayList<>();
        HttpClient httpClient = new HttpClient();
        int error = 0;
        for (ResourcesManage resourcesManage : resourcesManages) {
            if (null != resourcesManage) {

                //If it exists, don't continue pushing
                Query queryResourcesManage = new Query();
                queryResourcesManage.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
                List<PushFinDatas> pushFinDataLists = mongoTemplate.find(queryResourcesManage, PushFinDatas.class);
                if (null != pushFinDataLists && pushFinDataLists.size() > 0) {
                    error++;
                    continue;
                }

                //If already recommended, update the recommendation status
                Query queryFinData = new Query();
                queryFinData.addCriteria(Criteria.where("_id").is(resourcesManage.getId()));
                Update update = new Update();
                update.set("finData", true);
                mongoTemplate.updateFirst(queryFinData, update, ResourcesManage.class);
                try {
                    if (StringUtils.isNotBlank(resourcesManage.getEs_id())) {
                        UpdateRequest requestEs = new UpdateRequest();
                        XContentBuilder contentBuilder = XContentFactory.jsonBuilder()
                                .startObject()
                                .field("finData", true)//Modifying Fields and Content
                                .endObject();
                        requestEs.index(Constant.RESOURCE_COLLECTION_NAME)
                                .type(Constant.RESOURCE_COLLECTION_NAME)
                                .id(resourcesManage.getEs_id())//To modifyid
                                .doc(contentBuilder);
                        client.update(requestEs).get();
                        contentBuilder.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Save push records
                PushFinDatas pushFinDatas = new PushFinDatas();
                BeanUtils.copyProperties(resourcesManage, pushFinDatas);
                pushFinDatas.setId(CommonUtils.generateUUID());
                pushFinDatas.setResourcesId(resourcesManage.getId());
                pushFinDatas.setCreateTime(LocalDateTime.now());
                pushFinDatas.setType(type);
                pushFinDatas.setPrivacyPolicy(resourcesManage.getPrivacyPolicy());
                pushFinDatasLists.add(pushFinDatas);
                DatasetFromInstdb instdb = new DatasetFromInstdb();
                //Author processing
                net.sf.json.JSONArray authors = resourcesManage.getAuthor();
                if (null != authors && authors.size() > 0) {
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < authors.size(); i++) {
                        net.sf.json.JSONObject jsonObject = authors.getJSONObject(i);
                        list.add(jsonObject.getString("name"));
                    }
                    instdb.setAuthor(list);
                }

                instdb.setDataSetId(resourcesManage.getId());
                instdb.setStatus(resourcesManage.getStatus().equals(Constant.Approval.ADOPT) ? "online" : "offline");
                instdb.setTitle(resourcesManage.getName());
                instdb.setVersion(resourcesManage.getVersion());
                instdb.setDoi(resourcesManage.getDoi());
                instdb.setCstr(resourcesManage.getCstr());
                instdb.setIntroduction(resourcesManage.getDescription());
                instdb.setPublishDate(DateUtils.LocalDateTimeasDate(resourcesManage.getApproveTime()));
                instdb.setYear(DateUtils.getDateTimeString3(resourcesManage.getApproveTime()));
                instdb.setKeyword(CommonUtils.listToStr(resourcesManage.getKeywords(),","));
              //  instdb.setCode("instdb");
                instdb.setFrom(centerConf.getOrgName());
                instdb.setSource(instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId());

//                List<String> url11 = new ArrayList<>();
//                url11.add(instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId());
//                instdb.setTextUrl(url11);
                instdb.setInstitutions(StringUtils.isBlank(indexCopyrightLinks.getName()) ? "Institutional data repository" : indexCopyrightLinks.getName());
                instdb.setSimpleSource(centerConf.getHost());

//                Query query = new Query();
//                query.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
//                List<ResourceFileTree> lists = mongoTemplate.find(query, ResourceFileTree.class);
//                if (null != lists && lists.size() > 0) {
//                    List<FileInfoFromInstdb> fileInfoFromInstdbs = new ArrayList<>();
//                    for (ResourceFileTree file : lists) {
//                        FileInfoFromInstdb fromInstdb = new FileInfoFromInstdb();
//                        fromInstdb.setName(file.getFileName());
//                        fromInstdb.setId(file.getId());
//                        fromInstdb.setType(file.getFileName().substring((file.getFileName().lastIndexOf(".") + 1), file.getFileName().length()));
//                        fromInstdb.setSize(file.getSize());
//                        fromInstdb.setUrl(instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId());
//                        fromInstdb.setUrlEffective("1");
//                        fileInfoFromInstdbs.add(fromInstdb);
//                    }
//                    instdb.setFileInfo(fileInfoFromInstdbs);
//                }

                instdbList.add(instdb);
            }
        }

        if (pushFinDatasLists.size() > 0 && instdbList.size() > 0) {
            mongoTemplate.insertAll(pushFinDatasLists);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("datasetFromInstdbs", instdbList);

            String result = null;
            try {
                result = httpClient.doPostJsonWayTwo(JSON.toJSONString(instdbList), instdbUrl.getFindataAPIUrl() + "/collection/datasetFromInstdb");
                Map resultMap = JSONObject.parseObject(result, Map.class);
                int code = (int) resultMap.get("code");
                if (code != 200) {
                    return ResultUtils.error((String) resultMap.get("message"));
                }
                log.info(result);
                return ResultUtils.successOld("Successfully recommended" + instdbList.size() + "Successfully recommended，Successfully recommended" + error + "Successfully recommended");

            } catch (Exception e) {
                e.printStackTrace();
                return ResultUtils.error("SYSTEM_ERROR");
            }
        }
        return ResultUtils.successOld("Successfully recommended" + instdbList.size() + "Successfully recommended，Successfully recommended" + error + "Successfully recommended");
    }


}
