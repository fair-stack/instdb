package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.elasticsearch.EsServiceParams;
import cn.cnic.instdb.elasticsearch.EsServiceParamsDb;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.*;
import cn.cnic.instdb.model.special.Special;
import cn.cnic.instdb.model.special.SpecialResources;
import cn.cnic.instdb.model.special.SpecialVo;
import cn.cnic.instdb.model.system.SearchConfig;
import cn.cnic.instdb.model.system.SubjectArea;
import cn.cnic.instdb.model.system.ValuesResult;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.EsDataPage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.IndexService;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.service.SpecialService;
import cn.cnic.instdb.service.SubjectAreaService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IndexServiceImpl implements IndexService {

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();
    private final Cache<String, Object> searchConfigCache = CaffeineUtil.getSearchConfig();
    public static final String SPECIAL_COLLECTION_NAME = "special";

    public static final String RESOURCE_COLLECTION_NAME = "resources_manage";

    @Resource
    private SubjectAreaService subjectAreaService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private SpecialService specialService;

    @Resource
    private MongoUtil mongoUtil;

    @Resource
    private TransportClient client;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private UserRepository userRepository;

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private SettingService settingService;

    @Override
    public PageHelper getIndexAllResource(ResourcesIndexQuery resourcesIndexQuery) {

        Query query = new Query();
        List<AggregationOperation> aggList = new ArrayList<>();
        List<AggregationOperation> aggList1 = new ArrayList<>();


        //Only display approved and published
        aggList.add(Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)));
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        //Display only the latest resource identification data
        aggList.add(Aggregation.match(Criteria.where("versionFlag").is(Constant.VERSION_FLAG)));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));

        String titleZh = resourcesIndexQuery.getTitleZh();
        if (StringUtils.isNotBlank(titleZh)) {
            aggList.add(Aggregation.match(Criteria.where("titleZh").regex(titleZh)));
            query.addCriteria(Criteria.where("titleZh").regex(titleZh));
        }

        //Subject search
        List<String> subject = resourcesIndexQuery.getSubject();
        if (null != subject && subject.size() > 0) {
            aggList.add(Aggregation.match(Criteria.where("subject").in(subject)));
            query.addCriteria(Criteria.where("subject").in(subject));
        }

        //Resource Type Query
        List<String> resourceType = resourcesIndexQuery.getResourceType();
        if (null != resourceType && resourceType.size() > 0) {
            aggList.add(Aggregation.match(Criteria.where("resourceType").in(resourceType)));
            query.addCriteria(Criteria.where("resourceType").in(resourceType));
        }
        //Query of privacy policy
        List<String> privacyPolicy = resourcesIndexQuery.getPrivacyPolicy();
        if (null != privacyPolicy && privacyPolicy.size() > 0) {
            aggList.add(Aggregation.match(Criteria.where("privacyPolicy.type").in(privacyPolicy)));
            query.addCriteria(Criteria.where("privacyPolicy.type").in(privacyPolicy));
        }

        //Keywords
        List<String> keywordZh = resourcesIndexQuery.getKeywordZh();
        if (null != keywordZh && keywordZh.size() > 0) {
            aggList.add(Aggregation.match(Criteria.where("keywordZh").in(keywordZh)));
            query.addCriteria(Criteria.where("keywordZh").in(keywordZh));
        }
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        aggList.add(Aggregation.sort(Sort.Direction.DESC, "createTime"));
        aggList1.addAll(aggList);

        long count = mongoTemplate.count(query, ResourcesListManage.class, RESOURCE_COLLECTION_NAME);
        mongoUtil.start(Integer.parseInt(resourcesIndexQuery.getPageOffset()), Integer.parseInt(resourcesIndexQuery.getPageSize()), query);
        List<ResourcesListManage> resourcesManage = mongoTemplate.find(query, ResourcesListManage.class, RESOURCE_COLLECTION_NAME);
        //Statistics on the number of privacy policies
        Map<String ,Object> map = new HashMap<>();
        Map<String, Integer> privacyPolicyTypeNum = getPrivacyPolicyTypeNum(aggList);
        map.put("privacyPolicyTypeNum",privacyPolicyTypeNum);

        //Statistics of keywords
        List<ValuesResult> valuesResult = getkeywordNum(aggList1);
        map.put("keywordNum",valuesResult);

        return mongoUtil.pageHelper(count, resourcesManage,map);

    }

    /**
     * Statistics on the number of privacy policies
     * @param aggList
     * @return
     */
    private Map<String ,Integer> getPrivacyPolicyTypeNum(List<AggregationOperation> aggList){
        Map<String, Integer> map = new HashMap<>();
        aggList.add(Aggregation.group("privacyPolicy.type").count().as("count"));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<Document> document = mongoTemplate.aggregate(aggregation, RESOURCE_COLLECTION_NAME, Document.class);
        List<Document> results = document.getMappedResults();
        if (null != results) {
            for (Document result : results) {
                String id = result.get("_id").toString();
                int count = (int) result.get("count");
                map.put(id, count);
            }
        }
        return map;
    }


    /**
     * Quantity statistics of keywords
     * @param aggList
     * @return
     */
    private List<ValuesResult> getkeywordNum(List<AggregationOperation> aggList){
        List<ValuesResult> xuekesss = new ArrayList<>();
        aggList.add(Aggregation.unwind("keywordZh"));
        aggList.add(Aggregation.group("keywordZh")
                .max("keywordZh").as("name")
                .count().as("value"));
        aggList.add(Aggregation.sort(Sort.Direction.DESC, "value"));
        aggList.add(Aggregation.skip(0));
        aggList.add(Aggregation.limit(6));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<ValuesResult> document = mongoTemplate.aggregate(aggregation, RESOURCE_COLLECTION_NAME, ValuesResult.class);
        if(null != document.getMappedResults() && document.getMappedResults().size()>0){
            xuekesss = document.getMappedResults();
        }
        return xuekesss;
    }

    @Override
    public List<String> getIndexHotSearch() {
        List<String> list = new ArrayList<>();
        Query query = new Query();
        query.with(PageRequest.of(0,5));
        query.with(Sort.by(Sort.Direction.DESC,"frequency"));
        List<HotSearch> hotSearch = mongoTemplate.find(query, HotSearch.class);
        if(null!= hotSearch && hotSearch.size()>0){
            for (HotSearch data:hotSearch) {
                list.add(data.getHotName());
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> getIndexBoutiqueSpecial() {
        Map<String,Object> map = new HashMap<>();
        Query query = new Query();
        long resourcesCount = mongoTemplate.count(query, SpecialResources.class);
        long specialCount = mongoTemplate.count(query, Special.class);
        query.with(Sort.by(Sort.Direction.DESC, "resourcesNum"));
//        query.with(Sort.by(Sort.Direction.DESC, "downloadNum"));
//        query.with(Sort.by(Sort.Direction.DESC, "visitNum"));
        query.skip(0);
        query.limit(6);
        List<SpecialVo> specialVos = mongoTemplate.find(query, SpecialVo.class, SPECIAL_COLLECTION_NAME);
        //Obtain and update information on three topics when querying the list
        if (null != specialVos && specialVos.size() > 0) {
            specialService.updateNumSpecial(specialVos);
            for (SpecialVo special : specialVos) {
                special.setAuthorizationList(new HashSet());
            }

        }
        map.put("resourcesCount",resourcesCount);
        map.put("specialCount",specialCount);
        map.put("list",specialVos);
        return map;
    }

    public void setQuery(Query query) {
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        query.skip(0);
        query.limit(5);
    }

    @Override
    public Map<String, Object> getIndexResourceRank() {
        Map<String, Object> map = new HashMap<>();
       // String lang = tokenCache.getIfPresent("lang");
        Query query = new Query();
        Query downloadQuery = new Query();
        Query followQuery = new Query();
        setQuery(query);
        query.with(Sort.by(Sort.Direction.DESC, "visitNum"));
        List<ResourcesManageIndex> visitResources = mongoTemplate.find(query, ResourcesManageIndex.class, RESOURCE_COLLECTION_NAME);
//        if (null != visitResources && visitResources.size() > 0) {
//            for (ResourcesManageIndex resourcesManageIndex : visitResources) {
//                if (Constant.Language.english.equals(lang) && StringUtils.isNotBlank(resourcesManageIndex.getName_en())) {
//                    resourcesManageIndex.setName(resourcesManageIndex.getName_en());
//                }
//            }
//        }

        setQuery(downloadQuery);
        downloadQuery.with(Sort.by(Sort.Direction.DESC, "downloadNum"));
        List<ResourcesManageIndex> downloadResources = mongoTemplate.find(downloadQuery, ResourcesManageIndex.class, RESOURCE_COLLECTION_NAME);
//        if (null != visitResources && visitResources.size() > 0) {
//            for (ResourcesManageIndex resourcesManageIndex : downloadResources) {
//                if (Constant.Language.english.equals(lang) && StringUtils.isNotBlank(resourcesManageIndex.getName_en())) {
//                    resourcesManageIndex.setName(resourcesManageIndex.getName_en());
//                }
//            }
//        }
        setQuery(followQuery);
        followQuery.with(Sort.by(Sort.Direction.DESC, "followNum"));
        List<ResourcesManageIndex> followResources = mongoTemplate.find(followQuery, ResourcesManageIndex.class, RESOURCE_COLLECTION_NAME);
//        if (null != visitResources && visitResources.size() > 0) {
//            for (ResourcesManageIndex resourcesManageIndex : followResources) {
//                if (Constant.Language.english.equals(lang) && StringUtils.isNotBlank(resourcesManageIndex.getName_en())) {
//                    resourcesManageIndex.setName(resourcesManageIndex.getName_en());
//                }
//            }
//        }
        map.put("visit", visitResources);
        map.put("download", downloadResources);
        map.put("follow", followResources);
        return map;
    }

    @Override
    public Map<String, Object> getIndexStatisticsNum() {
        Map<String, Object> map = new HashMap<>();
        Query queryResources = new Query();
        Criteria criteria = Criteria.where("status").is(Constant.Approval.ADOPT);
        criteria.and("versionFlag").is(Constant.VERSION_FLAG);
        queryResources.addCriteria(criteria);
        long ResourcesCount = mongoTemplate.count(queryResources, ResourcesManage.class);

        //Number of resource visits
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)),
                Aggregation.group()
                        .sum("visitNum").as("longValue"));
        AggregationResults<ValuesResult> results = mongoTemplate.aggregate(agg, RESOURCE_COLLECTION_NAME, ValuesResult.class);
        List<ValuesResult> mappedResults = results.getMappedResults();
        long visitNum = 0;
        if (null != mappedResults && mappedResults.size() > 0) {
            visitNum = mappedResults.get(0).getLongValue();
        }


        //Physical storage capacity
        Aggregation aggStorage = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)),
                Aggregation.group()
                        .sum("storageNum").as("longValue"));
        AggregationResults<ValuesResult> aggStorageResults = mongoTemplate.aggregate(aggStorage, RESOURCE_COLLECTION_NAME, ValuesResult.class);
        List<ValuesResult> storage = aggStorageResults.getMappedResults();
        long storageNum = 0;
        if (null != storage && storage.size() > 0) {
            storageNum = storage.get(0).getLongValue();
        }


        //Number of resource downloads
        Aggregation aggDownload = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)),
                Aggregation.group()
                        .sum("downloadNum").as("longValue"));
        AggregationResults<ValuesResult> downloadResults = mongoTemplate.aggregate(aggDownload, RESOURCE_COLLECTION_NAME, ValuesResult.class);
        List<ValuesResult> download = downloadResults.getMappedResults();

        long downloadNum = 0;
        if(null != download && download.size() > 0){
            downloadNum = download.get(0).getLongValue();
        }

        Query queryUser = new Query();
        queryUser.addCriteria(Criteria.where("state").is(1));
        long userCount = mongoTemplate.count(queryUser, ConsumerDO.class);

        Query query = new Query();
        long specialCount = mongoTemplate.count(query, Special.class);
        map.put("totalResources",  ResourcesCount);
        map.put("specialCount", specialCount);
        map.put("totalDownload", downloadNum);
        map.put("totalPage", visitNum);
        map.put("totalStorage", storageNum);
        map.put("totalUser", userCount);
        return map;
    }

    @Override
    public Map<String, String> getIndexResourceType() {
        Map<String,String> map = new HashMap<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        List<ResourcesListManage> resourcesManage = mongoTemplate.find(query, ResourcesListManage.class, RESOURCE_COLLECTION_NAME);
        if (null != resourcesManage && resourcesManage.size() > 0) {
            Map<String, List<ResourcesListManage>> collect = resourcesManage.stream().collect(Collectors.groupingBy(ResourcesListManage::getResourceType));
            if (null != collect && collect.size() > 0) {
                for (Map.Entry<String, List<ResourcesListManage>> entry : collect.entrySet()) {
                    map.put(entry.getKey(), "");
                }
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    for (String resourceType : Constant.RESOURCE_TYPES) {
                        String[] split = resourceType.split("&");
                        if (split[0].equals(entry.getKey())) {
                            String lang = tokenCache.getIfPresent("lang");
                            if(Constant.Language.chinese.equals(lang)){
                                entry.setValue(split[1]);
                            }else {
                                entry.setValue(split[2]);
                            }

                        }
                    }
                }
            }
        }
        return map;
    }

    @Override
    public List<ResourcesListManage> getResourceByType(String resourceType) {
        String lang = tokenCache.getIfPresent("lang");
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        query.addCriteria(Criteria.where("resourceType").is(resourceType));
        query.with(Sort.by(Sort.Direction.DESC, "approveTime"));
        query.skip(0);
        query.limit(6);
        List<ResourcesListManage> resourcesManage = mongoTemplate.find(query, ResourcesListManage.class, RESOURCE_COLLECTION_NAME);
        if (null != resourcesManage && resourcesManage.size() > 0) {
            for (ResourcesListManage resources : resourcesManage) {
                if (Constant.Language.english.equals(lang)) {
                    if (null != resources.getAuthor() && resources.getAuthor().size() > 0) {
                        JSONArray author = resources.getAuthor();
                        for (int i = 0; i < author.size(); i++) {
                            JSONObject o = (JSONObject) author.get(i);
                            if (null != o.get("en_name") && StringUtils.isNotBlank(o.getString("en_name"))) {
                                o.put("name", o.getString("en_name"));
                            }
                        }
                    }
                }
            }
        }
        return resourcesManage;
    }


    @Override
    public Map<String,Object> getIndexSubjectArea(Integer num) {
      //  String lang = tokenCache.getIfPresent("lang");
        Map<String,Object> map = new HashMap<>();
        num = null == num ? 6 : num;
        num = num > 12 ? 12 : num;
        Query query = new Query();
        query.skip(0);
        query.limit(num);
        query.addCriteria(Criteria.where("sort").ne(""));
        query.with(Sort.by(Sort.Direction.ASC, "sort"));
        List<SubjectArea> subjectArea = mongoTemplate.find(query, SubjectArea.class);
        long count = mongoTemplate.count(query, SubjectArea.class);

        if (null != subjectArea && subjectArea.size() > 0) {
            for (SubjectArea subject : subjectArea) {
                List<String> subs = subject.getSubject();
                Query queryResource = new Query();
                if (null != subs && subs.size() > 0) {
                    queryResource.addCriteria(Criteria.where("subject").in(subs));
                    queryResource.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
                    long resourcesNum = mongoTemplate.count(queryResource, ResourcesManage.class);
                    subject.setResourcesNum(resourcesNum);
                }
            }
        }

        map.put("data",subjectArea);
        map.put("count",count);
        return map;
    }


    @Override
    public List<ResourcesListManage> getIndexNewResource() {
        Query query = new Query();
        //Display only the latest resource identification data
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        query.with(Sort.by(Sort.Direction.DESC, "approveTime"));
        query.skip(0);
        query.limit(6);
        List<ResourcesListManage> resourcesManage = mongoTemplate.find(query, ResourcesListManage.class, RESOURCE_COLLECTION_NAME);
        return resourcesManage;
    }

    @Override
    public EsDataPage getIndexAllResourceByES(String token,EsServiceParams esServiceParams) {
        instdbUrl.setMongoTemplate(this.mongoTemplate);
        instdbUrl.setSubjectAreaService(this.subjectAreaService);
        instdbUrl.setSettingService(this.settingService);

        String username = "";
        ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
        if (StringUtils.isNotBlank(token)) {
            //according totokenaccording to
            ConsumerDO consumerDO = null;
            String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
            if (StringUtils.isNotBlank(userIdFromToken)) {
                Optional<ConsumerDO> byId = userRepository.findById(userIdFromToken);
                if (byId.isPresent()) {
                    consumerDO = byId.get();
                    username = consumerDO.getEmailAccounts();
                }
            }
        }

        try {
            return elasticSearchUtil.searchDataPage(username,esServiceParams,client,instdbUrl);
        } catch (Exception e) {
            log.error("context",e);
        }
        return null;


    }

    @Override
    public List<SearchConfig>  getIndexSearchitems(String type) {
        List<SearchConfig> select = null;
        Object ifPresent = searchConfigCache.getIfPresent("searchConfig" + type);
        if (null != ifPresent) {
            select = (List<SearchConfig>) ifPresent;
            if (0 < select.size()) {
                return select;
            }
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("type").is(type));
        query.addCriteria(Criteria.where("status").is("1"));
        query.with(Sort.by(Sort.Direction.ASC, "sort"));
        select = mongoTemplate.find(query, SearchConfig.class);
        if (null != select && select.size() > 0) {
            searchConfigCache.put("searchConfig" + type, select);
        }
        return select;
    }

    @Override
    public Result getHistorySearch() {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.skip(0);
        query.limit(5);
        List<EsServiceParamsDb> esServiceParamsDbs = mongoTemplate.find(query, EsServiceParamsDb.class);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //Get to today's date  Get to today's date
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(new Date());
        cal1.add(Calendar.DATE, -30);
        String imptimeEnd = sdf.format(cal1.getTime());
        Query resourcesQuery = new Query();
        resourcesQuery.addCriteria(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(imptimeEnd)));
        mongoTemplate.remove(resourcesQuery, EsServiceParamsDb.class);

        return ResultUtils.success(esServiceParamsDbs);
    }


}
