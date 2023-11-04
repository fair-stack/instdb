package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.Approve;
import cn.cnic.instdb.model.resources.FtpUser;
import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.EsDataService;
import cn.cnic.instdb.service.ExternalInterService;
import cn.cnic.instdb.utils.DateUtils;
import cn.cnic.instdb.utils.FileUtils;
import cn.cnic.instdb.utils.InstdbUrl;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Auther wdd
 * @Date 2021/4/22 18:25
 * @Desc Constant Dictionary Table Maintenance
 */
@Service
@Slf4j
public class EsDataServiceImpl implements EsDataService {

    @Resource
    private TransportClient client;

//    @Resource
//    private MongoDatabase mongoDatabase;

    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    @Lazy
    private AsyncDeal asyncDeal;

    @Resource
    private ExternalInterService externalInterService;

    @Override
    public Result resetES() {

        IndicesExistsResponse response = client.admin().indices().exists(new IndicesExistsRequest().indices(new String[]{Constant.RESOURCE_COLLECTION_NAME})).actionGet();
        if (response.isExists()) {
//            elasticsearchTemplate.deleteIndex(Constant.RESOURCE_COLLECTION_NAME);
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
            DeleteQuery deleteQuery = new DeleteQuery();
            deleteQuery.setQuery(queryBuilder);
            deleteQuery.setIndex(Constant.RESOURCE_COLLECTION_NAME);//ESTabularIndexTabular
            deleteQuery.setType(Constant.RESOURCE_COLLECTION_NAME);//ESTabularTypeTabular
            elasticsearchTemplate.delete(deleteQuery);//Delete statement
            log.info("delete index success!");
        }
        return ResultUtils.success("Index deleted successfully");
    }


    @Override
    public Result save(String id) {


        if (StringUtils.isBlank(id)) {
            return ResultUtils.error("idIs empty");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        Map dataMap = mongoTemplate.findOne(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);

        if (null != dataMap) {
            IndicesExistsResponse response = client.admin().indices().exists(new IndicesExistsRequest().indices(new String[]{Constant.RESOURCE_COLLECTION_NAME})).actionGet();
            if (response.isExists()) {
                //Query based on criteria
                MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("rootId", id);
                SearchRequestBuilder index = client.prepareSearch(Constant.RESOURCE_COLLECTION_NAME).setQuery(matchQueryBuilder).setSize(20);
                SearchHits hits = index.get().getHits();
                if (1 == hits.totalHits) {
                    return ResultUtils.error("Data already exists");
                }
            }
            dataMap.put("rootId", dataMap.get("_id").toString());
            if (null != dataMap.get("doi") && dataMap.containsKey("doi") && !Constant.APPLY.equals(dataMap.get("doi").toString())) {
                dataMap.put("doi", dataMap.get("doi").toString());
            }
            if (null != dataMap.get("cstr") && dataMap.containsKey("cstr") && !Constant.APPLY.equals(dataMap.get("cstr").toString())) {
                dataMap.put("cstr", dataMap.get("cstr").toString());
            }
            dataMap.remove("_id");
            dataMap.remove("callbackUrl");
            dataMap.remove("sendFileList");
            dataMap.remove("downloadFileFlag");
            dataMap.remove("json_id_content");
            dataMap.remove("images");
            dataMap.remove("@type");
            dataMap.remove("@context");
            dataMap.remove("organization");
            dataMap.remove("publish");
            if (dataMap.containsKey("createTime")) {
                Date createTime = (Date) dataMap.get("createTime");
                dataMap.put("createTime", DateUtils.getDateString(createTime));
            }
            if (dataMap.containsKey("approveTime") && null != dataMap.get("approveTime")) {
                dataMap.put("approveTime", DateUtils.getDateString((Date) dataMap.get("approveTime")));
            }
            LinkedHashMap privacyPolicyNap = new LinkedHashMap();
            try {
                ResourcesManage.PrivacyPolicy privacyPolicy = (ResourcesManage.PrivacyPolicy) dataMap.get("privacyPolicy");
                if (null != privacyPolicy) {
                    privacyPolicyNap.put("type", privacyPolicy.getType());
                    if (StringUtils.isNotBlank(privacyPolicy.getOpenDate())) {
                        privacyPolicyNap.put("openDate", privacyPolicy.getOpenDate());
                    }
                    if (StringUtils.isNotBlank(privacyPolicy.getCondition())) {
                        privacyPolicyNap.put("condition", privacyPolicy.getCondition());
                    }
                }
            } catch (Exception e) {
                privacyPolicyNap = (LinkedHashMap) dataMap.get("privacyPolicy");
            }
            dataMap.put("privacyPolicy", privacyPolicyNap);


            //After approval After approvalesAfter approval
            client.prepareIndex(Constant.RESOURCE_COLLECTION_NAME, Constant.RESOURCE_COLLECTION_NAME).setSource(dataMap).get();

        } else {
            return ResultUtils.error("The query data is empty");
        }
        return ResultUtils.success("Successfully added");
    }

    @Override
    public Result saveEsAll() {
        Query query = new Query();
        Object[] o = new Object[]{Constant.Approval.ADOPT, Constant.Approval.OFFLINE};
        query.addCriteria(Criteria.where("status").in(o));
        List<Map> list = mongoTemplate.find(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null != list && list.size() > 0) {
            log.info("Total amount of data to be imported：" + list.size());
            int num = 0;
            for (Map dataMap : list) {
                dataMap.put("rootId", dataMap.get("_id").toString());
                if (null != dataMap.get("doi") && dataMap.containsKey("doi") && !Constant.APPLY.equals(dataMap.get("doi").toString())) {
                    dataMap.put("doi", dataMap.get("doi").toString());
                }
                if (null != dataMap.get("cstr") && dataMap.containsKey("cstr") && !Constant.APPLY.equals(dataMap.get("cstr").toString())) {
                    dataMap.put("cstr", dataMap.get("cstr").toString());
                }
                dataMap.remove("_id");
                dataMap.remove("callbackUrl");
                dataMap.remove("sendFileList");
                dataMap.remove("downloadFileFlag");
                dataMap.remove("json_id_content");
                dataMap.remove("images");
                dataMap.remove("@type");
                dataMap.remove("@context");
                dataMap.remove("organization");
                dataMap.remove("publish");
                dataMap.put("createTime", DateUtils.getDateString((Date) dataMap.get("createTime")));
                dataMap.put("approveTime", DateUtils.getDateString((Date) dataMap.get("approveTime")));

                LinkedHashMap privacyPolicyNap = new LinkedHashMap();
                try {
                    ResourcesManage.PrivacyPolicy privacyPolicy = (ResourcesManage.PrivacyPolicy) dataMap.get("privacyPolicy");
                    if (null != privacyPolicy) {
                        privacyPolicyNap.put("type", privacyPolicy.getType());
                        if (StringUtils.isNotBlank(privacyPolicy.getOpenDate())) {
                            privacyPolicyNap.put("openDate", privacyPolicy.getOpenDate());
                        }
                        if (StringUtils.isNotBlank(privacyPolicy.getCondition())) {
                            privacyPolicyNap.put("condition", privacyPolicy.getCondition());
                        }
                    }
                } catch (Exception e) {
                    privacyPolicyNap = (LinkedHashMap) dataMap.get("privacyPolicy");
                }
                dataMap.put("privacyPolicy", privacyPolicyNap);

                //After approval After approvalesAfter approval
                IndexResponse indexResponse = client.prepareIndex(Constant.RESOURCE_COLLECTION_NAME, Constant.RESOURCE_COLLECTION_NAME).setSource(dataMap).get();
                Update updateResources = new Update();
                updateResources.set("esSync", Constant.Approval.YES);
                updateResources.set("es_id", indexResponse.getId());
                Query query2 = new Query();
                query2.addCriteria(Criteria.where("_id").is(dataMap.get("rootId").toString()));
                mongoTemplate.upsert(query2, updateResources, ResourcesManage.class);
                log.info("Successfully imported section" + num++ + "Successfully imported section...");
            }

        } else {
            return ResultUtils.error("No data to import");
        }
        list.clear();
        return ResultUtils.success("It's done");
    }

    @Override
    public Result update(String id, String field, String value) {
        try {
            UpdateRequest requestEs = new UpdateRequest();
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field(field, value)//Modifying Fields and Content
                    .endObject();
            requestEs.index(Constant.RESOURCE_COLLECTION_NAME)
                    .type(Constant.RESOURCE_COLLECTION_NAME)
                    .id(id)//To modifyid
                    .doc(contentBuilder);
            client.update(requestEs).get();
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtils.error("Modification failed");
        }
        return ResultUtils.success("Successfully modified");
    }

    @Override
    public Result delete(String id) {
        // hereIDhereeshereID，hereIDhere
        DeleteRequest deleteRequest = new DeleteRequest(Constant.RESOURCE_COLLECTION_NAME, Constant.RESOURCE_COLLECTION_NAME, id);
        client.delete(deleteRequest).actionGet();
        return ResultUtils.success("Successfully deleted");
    }

    @Override
    public Result deleteAll(String id) {
        Query queryResources = new Query();
        queryResources.addCriteria(Criteria.where("_id").is(id));
        ResourcesManage resourcesManage = mongoTemplate.findOne(queryResources, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error("No data found");
        }

        //Delete Dataset
        mongoTemplate.remove(queryResources, ResourcesManage.class);
        //Delete the corresponding file
        //cn.cnic.instdb.utils.FileUtils.deleteDirectory(instdbUrl.getResourcesFilePath() + resourcesManage.getId());
        asyncDeal.deleteDirectory(instdbUrl.getResourcesFilePath() + resourcesManage.getId());
        FileUtils.deleteFile(instdbUrl.getResourcesPicturePath() + resourcesManage.getId() + Constant.PNG);
        //Delete structured content
        Query structuredQuery = new Query();
        structuredQuery.addCriteria(Criteria.where("resourceId").is(resourcesManage.getId()));
        List<Map> result = mongoTemplate.find(structuredQuery, Map.class, Constant.TABLE_NAME);
        if (null != result && result.size() > 0) {
            for (Map map : result) {
                mongoTemplate.dropCollection(map.get("tableName").toString());
            }
            mongoTemplate.remove(structuredQuery, Constant.TABLE_NAME);
        }

        //Delete Dataset File Content
        Query removeQuery = new Query();
        removeQuery.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
        mongoTemplate.remove(removeQuery, ResourceFileTree.class);

        //Delete Approval Record
        Query queryapprove = new Query();
        queryapprove.addCriteria(Criteria.where("resourcesId").is(id));
        mongoTemplate.remove(queryapprove, Approve.class);

        //Delete the generated account password  Delete the generated account passwordftpDelete the generated account password
        Query queryFtp = new Query();
        queryFtp.addCriteria(Criteria.where("resourcesId").is(id));
        mongoTemplate.remove(queryFtp, FtpUser.class);

        if (StringUtils.isNotBlank(resourcesManage.getEs_id())) {
            DeleteRequest deleteRequest = new DeleteRequest(Constant.RESOURCE_COLLECTION_NAME, Constant.RESOURCE_COLLECTION_NAME, resourcesManage.getEs_id());
            client.delete(deleteRequest).actionGet();
        } else {
            return ResultUtils.error("esDelete failed  Delete failedesid");
        }
        return ResultUtils.success("Successfully deleted");
    }

    @Async
    @Override
    public void updateProject(String resourcesId) {

        Map map = mongoTemplate.findOne(new Query(Criteria.where("_id").is(resourcesId)), Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null == map) {
            return;
        }
        try {
            int num = 0;
            ArrayList project = new ArrayList();
            String field = "";
            if (map.containsKey("project") && null != map.get("project")) {
                project = (ArrayList) map.get("project");
                field = "project";
            } else if (map.containsKey("fundingReferences") && null != map.get("fundingReferences")) {
                project = (ArrayList) map.get("fundingReferences");
                field = "fundingReferences";
            }

            if (null != project && project.size() > 0) {
                String identifier = "";
                for (int i = 0; i < project.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) project.get(i);
                    if (null == object.get("@id")) {
                        log.info("absence@id absence");
                        return;
                    }
                    //Both the number and project type are available Both the number and project type are available
                    if (null != object.get("identifier") && null != object.get("fundType")) {
                        identifier = object.get("identifier").toString();
                        log.info("identifierWe have it all We have it all");
                        num = 1;
                        break;
                    }

                    String projectId = object.get("@id").toString();
                    Result Project = externalInterService.accessDataInfo(projectId, "Project", null);
                    if (200 == Project.getCode()) {
                        JSONArray data = (JSONArray) Project.getData();
                        if (null != data && data.size() > 0) {
                            for (int j = 0; j < data.size(); j++) {
                                JSONObject o = data.getJSONObject(j);
                                object.put("@type", "Project");
                                object.put("@id", projectId);
                                object.put("name", o.getString("zh_Name"));
                                object.put("identifier", o.getString("identifier"));
                                object.put("fundType", o.getString("fundType"));
                                identifier = o.getString("identifier");
                            }
                        }
                    }
                }

                if(num == 0){
                    Update updateResources = new Update();
                    updateResources.set(field, project);
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(resourcesId));
                    mongoTemplate.updateFirst(query, updateResources, ResourcesManage.class);
                }

                Query queryApprove = new Query();
                queryApprove.addCriteria(Criteria.where("resourcesId").is(resourcesId));
                queryApprove.addCriteria(Criteria.where("identifier").is(null));
                List<Approve> approves = mongoTemplate.find(queryApprove, Approve.class);
                if (StringUtils.isNotBlank(identifier) && null != approves && approves.size() > 0) {
                    Update updateApprove = new Update();
                    updateApprove.set("identifier", identifier);
                    mongoTemplate.updateMulti(queryApprove, updateApprove, Approve.class);
                }

            }

        } catch (Exception e) {
            log.info(resourcesId + " Abnormal error updating project information！！！！！！");
            e.printStackTrace();
            return;
        }
        return;
    }

    @Override
    public Result updateProjectAll() {
        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("project").elemMatch(Criteria.where("identifier").is(null)), Criteria.where("fundingReferences").elemMatch(Criteria.where("identifier").is(null))));
        query.addCriteria(Criteria.where("dataSetSource").ne("scidb"));
        List<Map> maps = mongoTemplate.find(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null == maps || maps.size() == 0) {
            return ResultUtils.error("No data found");
        }
        log.info(maps.size() + "Preparing to update project information for a dataset");
        for (Map map : maps) {
            updateProject(map.get("_id").toString());
        }
        log.info(maps.size() + "Completed updating project information for dataset！");
        return Result.success(maps.size() + "Completed updating project information for dataset！");
    }

    @Override
    public String add(Map dataMap) {


        LinkedHashMap privacyPolicyNap = new LinkedHashMap();
        try {
            ResourcesManage.PrivacyPolicy privacyPolicy = (ResourcesManage.PrivacyPolicy) dataMap.get("privacyPolicy");
            if (null != privacyPolicy) {
                privacyPolicyNap.put("type", privacyPolicy.getType());
                if (StringUtils.isNotBlank(privacyPolicy.getOpenDate())) {
                    privacyPolicyNap.put("openDate", privacyPolicy.getOpenDate());
                }
                if (StringUtils.isNotBlank(privacyPolicy.getCondition())) {
                    privacyPolicyNap.put("condition", privacyPolicy.getCondition());
                }
            }
        } catch (Exception e) {
            privacyPolicyNap = (LinkedHashMap) dataMap.get("privacyPolicy");
        }
        dataMap.put("privacyPolicy", privacyPolicyNap);

        try {
            IndicesExistsResponse response = client.admin().indices().exists(new IndicesExistsRequest().indices(new String[]{Constant.RESOURCE_COLLECTION_NAME})).actionGet();
            //Judge firstes indexJudge first Judge first
            if (response.isExists()) {
                //Query based on criteria
                MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("resourcesId", dataMap.get("resourcesId").toString());
                SearchRequestBuilder index = client.prepareSearch(Constant.RESOURCE_COLLECTION_NAME).setQuery(matchQueryBuilder).setSize(20);
                SearchHits hits = index.get().getHits();
                if (null != hits && hits.getHits().length > 0) {
                    for (SearchHit hit : hits) {
                        //Recurrent updates Recurrent updates
                        UpdateRequest request = new UpdateRequest();
                        XContentBuilder contentBuilder = XContentFactory.jsonBuilder()
                                .startObject()
                                .field("versionFlag", "")//Modifying Fields and Content
                                .endObject();
                        request.index(Constant.RESOURCE_COLLECTION_NAME)
                                .type(Constant.RESOURCE_COLLECTION_NAME)
                                .id(hit.getId())//To modifyid
                                .doc(contentBuilder);
                        client.update(request).get();
                    }
                }
            }
            //After approval After approvalesAfter approval
            IndexResponse indexResponse = client.prepareIndex(Constant.RESOURCE_COLLECTION_NAME, Constant.RESOURCE_COLLECTION_NAME).setSource(dataMap).get();
            dataMap.clear();
            return indexResponse.getId();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("context", e);
            return "500";
        }
    }

    @Override
    public Result updateDate(String status, String state) {
        Query queryFtp = new Query();
        queryFtp.addCriteria(Criteria.where("datePublished").ne(null));
        Object[] o = new Object[]{Constant.Approval.ADOPT, Constant.Approval.OFFLINE}; //Include all,Include all
        queryFtp.addCriteria(Criteria.where("status").in(o));
        List<Map> resources_manage = mongoTemplate.find(queryFtp, Map.class, "resources_manage");
        if (null != resources_manage) {
            log.info("altogether" + resources_manage.size());
            for (Map map : resources_manage) {
                try {
                    if (null != map.get("datePublished") && null != map.get("approveTime") && null != map.get("es_id")) {
                        String datePublished = map.get("datePublished").toString();
                        Date approveTime = (Date) map.get("approveTime");
                        String dateString = DateUtils.getDateString(approveTime);

                        if (datePublished.equals(dateString)) {
                            log.info(map.get("_id").toString());
                            log.info(datePublished + " datePublished  + dateString  " + dateString);
                            Query queryApprove = new Query();
                            queryApprove.addCriteria(Criteria.where("resourcesId").is(map.get("_id")));
                            queryApprove.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.ADOPT));
                            Approve one = mongoTemplate.findOne(queryApprove, Approve.class);
                            if (null == one) {
                                return ResultUtils.error("Approval not found Approval not found");
                            }

                            if ("yes".equals(status)) {
                                Query query = new Query();
                                query.addCriteria(Criteria.where("_id").is(map.get("_id")));
                                Update update = new Update();
                                LocalDateTime approvalTime = one.getApprovalTime();
                                Date date = DateUtils.LocalDateTimeasDate(approvalTime);
                                update.set("approveTime", date);
                                mongoTemplate.updateFirst(query, update, ResourcesManage.class);
                                log.info("I have completed the update " + map.get("_id"));

                            }
                            log.info("The above needs to be modified");
                            if (StringUtils.isBlank(state) || !"okk".equals(state)) {
                                return ResultUtils.error("Execute once Execute once！");
                            }

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResultUtils.error("error！");
                }
            }
        }
        return ResultUtils.success();
    }



}
