package cn.cnic.instdb.utils;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.elasticsearch.EsParams;
import cn.cnic.instdb.elasticsearch.EsServiceParams;
import cn.cnic.instdb.elasticsearch.EsServiceParamsDb;
import cn.cnic.instdb.model.resources.FollowResources;
import cn.cnic.instdb.model.system.Template;
import cn.cnic.instdb.model.system.TemplateConfig;
import cn.cnic.instdb.result.EsDataPage;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ElasticSearchUtil {
    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    public static final String RESOURCE_COLLECTION_NAME = "resources_manage";

    @Resource
    private InstdbUrl instdbUrl;

    public EsDataPage searchDataPage(String username, EsServiceParams esServiceParams, TransportClient client, InstdbUrl instdb) throws Exception {

        IndicesExistsResponse response = client.admin().indices().exists(new IndicesExistsRequest().indices(new String[]{Constant.RESOURCE_COLLECTION_NAME})).actionGet();
        if (!response.isExists()) {
            return new EsDataPage();
        }

        this.instdbUrl = instdb;
        SearchRequest request = new SearchRequest(Constant.RESOURCE_COLLECTION_NAME);
        SearchSourceBuilder searchRequestBuilder = new SearchSourceBuilder();
        request.types(Constant.RESOURCE_COLLECTION_NAME);
        request.searchType(SearchType.QUERY_THEN_FETCH);

        //Filter condition settings
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //sort field
        if (null != esServiceParams.getSorts() && esServiceParams.getSorts().size() > 0) {
            addSort(searchRequestBuilder, esServiceParams.getSorts());
        }

        // Filter condition processing
        if (esServiceParams.getEsParameter() != null && esServiceParams.getEsParameter().size() > 0) {
            createQueryBuilder(boolQuery, esServiceParams.getEsParameter());
        }
        //Add parameters for this condition by default Add parameters for this condition by defaultesAdd parameters for this condition by default
        boolQuery.must(QueryBuilders.termQuery("versionFlag.keyword", Constant.VERSION_FLAG));
        //Only query these states
        boolQuery.must(QueryBuilders.termsQuery("status", Constant.Approval.ADOPT, Constant.Approval.PENDING_APPROVAL));

        //Highlight 
        if (esServiceParams.getHighlight() != null && esServiceParams.getHighlight().size() > 0) {
//        if(StringUtils.isNotBlank(instdbUrl.getEsHighlight()) && instdbUrl.getEsHighlight().contains(",")){
            addHighlight(searchRequestBuilder, esServiceParams.getHighlight());
        }

        searchRequestBuilder.query(QueryBuilders.matchAllQuery());
        searchRequestBuilder.query(boolQuery);

        //polymerization
        if (esServiceParams.getAggregations() != null && esServiceParams.getAggregations().size() > 0) {
            addAggregation(searchRequestBuilder, esServiceParams.getAggregations());
        }

        // Paging application
        searchRequestBuilder.from(esServiceParams.getPage() * esServiceParams.getPageSize()).size(esServiceParams.getPageSize());

        // Set whether to sort by query matching degree
        searchRequestBuilder.explain(true);

        //Printed content Printed content Elasticsearch head Printed content Kibana  Printed content
        //log.info("\n{}", searchRequestBuilder);

        // Perform Search,Perform Search
        request.source(searchRequestBuilder);

        SearchResponse searchResponse = client.search(request).get();

        long totalHits = searchResponse.getHits().totalHits;

        if (searchResponse.status().getStatus() == 200) {
            // Parsing Objects
            List<Map<String, Object>> sourceList = setSearchResponse(searchResponse, esServiceParams.getHighlight());
            if (null != sourceList && sourceList.size() > 0) {
                for (Map<String, Object> map : sourceList) {
                    //Whether to follow
                    if (StringUtils.isNotBlank(username)) {
                        Query queryF = new Query();
                        queryF.addCriteria(Criteria.where("username").is(username));
                        queryF.addCriteria(Criteria.where("resourcesId").is(map.get("rootId").toString()));
                        FollowResources followResources = instdb.getMongoTemplate().findOne(queryF, FollowResources.class);
                        map.put("follow", null == followResources ? "no" : "yes");
                    } else {
                        map.put("follow", "no");
                    }

                    //Disciplinary processing
                    if (map.containsKey("subject") && null != map.get("subject")) {
                        if (!map.containsKey("subjectEn") && null == map.get("subjectEn")) {
                            List<String> subject = (List<String>) map.get("subject");
                            List<String> subjectEn = instdb.getSubjectAreaService().getSubjectByName(subject, Constant.Language.chinese);
                            map.put("subjectEn", subjectEn);
                            UpdateRequest requestEs = new UpdateRequest();
                            XContentBuilder contentBuilder = XContentFactory.jsonBuilder()
                                    .startObject()
                                    .field("subjectEn", subjectEn)//Modifying Fields and Content
                                    .endObject();
                            requestEs.index(Constant.RESOURCE_COLLECTION_NAME)
                                    .type(Constant.RESOURCE_COLLECTION_NAME)
                                    .id(map.get("id").toString())//To modifyid
                                    .doc(contentBuilder);
                            client.update(requestEs).get();
                        }
                    }
                    if (map.containsKey("templateName") && null != map.get("templateName")) {
                        Query query = new Query();
                        query.addCriteria(Criteria.where("name").is(map.get("templateName").toString()));
                        query.addCriteria(Criteria.where("state").is("0"));
                        List<TemplateConfig> templateConfigs = instdb.getMongoTemplate().find(query, TemplateConfig.class);
                        if (null != templateConfigs && templateConfigs.size() > 0) {
                            map.put("templateValues", getTemplateValues(templateConfigs));
                        }
                    }

                }

            }
            List<Map<String, Object>> aggregationList = new ArrayList<>();
            //Aggregation processing
            if (esServiceParams.getAggregations().size() > 0) {
                aggregationList = getAggregationList(searchResponse, esServiceParams.getAggregations());
            }
            //Record Retrieval History
            if (esServiceParams.getEsParameter().size() > 0 && totalHits > 0) {
                addHistorySearch(esServiceParams.getEsParameter(), totalHits, username);
            }
            return new EsDataPage(esServiceParams.getPage(), esServiceParams.getPageSize(), (int) totalHits, sourceList, aggregationList);
        }
        return null;
    }

    /**
     * Obtain attributes from the template key&type&title
     *
     * @param templateConfigs
     * @return
     */
    private Map getTemplateValues(List<TemplateConfig> templateConfigs) {
        Map TemplateMap = new HashMap();
        TemplateConfig template = templateConfigs.get(0);
        Template temp = instdbUrl.getSettingService().getTemplate(template.getCode());
        List<Template.Group> group = temp.getGroup();
        for (Template.Group gro : group) {
            List<Template.Resource> resources = gro.getResources();
            for (Template.Resource resource : resources) {
                String iri = resource.getIri().substring((resource.getIri().lastIndexOf("/") + 1), resource.getIri().length());
                String language = resource.getLanguage();
                if (resource.getType().equals("text") || resource.getType().equals("textarea") || resource.getType().equals("select") || resource.getType().equals("date")) {
                    if (!iri.equals("name")) {
                        if (StringUtils.isNotBlank(language) && "en".equals(language) && !TemplateMap.containsKey(iri + "_en")) {
                            TemplateMap.put(iri + "_en&" + resource.getType(), resource.getTitle());
                        } else {
                            if (!TemplateMap.containsKey(iri)) {
                                TemplateMap.put(iri + "&" + resource.getType(), resource.getTitle());
                            }
                        }
                    }
                }
            }
        }
        return TemplateMap;
    }


    private void addHistorySearch(List<EsServiceParams.EsParameter> esParameter, long totalHits, String username) {
        EsServiceParamsDb serviceParamsDb = new EsServiceParamsDb();
        List<EsServiceParamsDb.EsParameter> list = new ArrayList<>();
        for (EsServiceParams.EsParameter parameter : esParameter) {
            EsServiceParamsDb.EsParameter data = new EsServiceParamsDb.EsParameter();
            data.setConnector(parameter.getConnector());
            data.setFieldType(parameter.getFieldType());
            data.setOperator(parameter.getOperator());
            data.setField(parameter.getField());
            data.setFieldName(parameter.getFieldName());
            data.setValue(parameter.getValue());
            list.add(data);
        }
        serviceParamsDb.setEsParameter(list);
        serviceParamsDb.setCreateTime(LocalDateTime.now());
        serviceParamsDb.setResultNum(totalHits);
        serviceParamsDb.setUsername(username);
        instdbUrl.getMongoTemplate().save(serviceParamsDb);
    }

    /**
     * join join join
     *
     * @param searchRequestBuilder
     * @param aggregations
     * @return
     */
    private void addAggregation(SearchSourceBuilder searchRequestBuilder, List<EsServiceParams.Aggregation> aggregations) {
        for (EsServiceParams.Aggregation aggregation : aggregations) {
            if (StringUtils.isBlank(aggregation.getField())) {
                continue;
            }
            String filed = parameterSuffix(aggregation.getField(), aggregation.getFieldType());
            if (aggregation.getFieldType().equals("year")) {
                DateHistogramAggregationBuilder fieldBuilder = AggregationBuilders.dateHistogram(filed).field(filed).dateHistogramInterval(DateHistogramInterval.YEAR).order(BucketOrder.count(false));
                searchRequestBuilder.aggregation(fieldBuilder);
            }else if (aggregation.getFieldType().equals("date")) {
                DateHistogramAggregationBuilder fieldBuilder = AggregationBuilders.dateHistogram(filed).field(filed).dateHistogramInterval(DateHistogramInterval.DAY).order(BucketOrder.count(false));
                searchRequestBuilder.aggregation(fieldBuilder);
            } else {
                AggregationBuilder threeAgg = AggregationBuilders.terms(filed).field(filed + ".keyword").size(aggregation.getSize());
                searchRequestBuilder.aggregation(threeAgg);
            }
        }
    }

    private String parameterSuffix(String field, String fieldType) {
        if ("project".equals(fieldType) || "author".equals(fieldType) || "org".equals(fieldType) || "paper".equals(fieldType)) {
            field = field + ".name";
        } else if ("privacyPolicy".equals(fieldType)) {
            field = field + ".type";
        } else {
            field = field;
        }
        return field;
    }

    /**
     * Highlight Field Processing
     *
     * @param searchRequestBuilder
     * @param highlights
     */
    private void addHighlight(SearchSourceBuilder searchRequestBuilder, List<EsServiceParams.Highlight> highlights) {
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for (EsServiceParams.Highlight highlight : highlights) {
            if (StringUtils.isNotBlank(highlight.getHighlightField()) && StringUtils.isNotBlank(highlight.getColor())) {
                switch (highlight.getColor()) {
                    case "red":
                        highlightBuilder.field(new HighlightBuilder.Field(highlight.getHighlightField()).preTags(Constant.TitleColor.COLOR_RED).postTags(Constant.TitleColor.POST_TAGS).fragmentSize(10000).fragmentOffset(0));
                        break;
                    case "green":
                        highlightBuilder.field(new HighlightBuilder.Field(highlight.getHighlightField()).preTags(Constant.TitleColor.COLOR_Green).postTags(Constant.TitleColor.POST_TAGS).fragmentSize(10000).fragmentOffset(0));
                        break;
                }
            }
        }
        searchRequestBuilder.highlighter(highlightBuilder);
    }


    /**
     * sort
     *
     * @param searchRequestBuilder
     * @param sorts
     */
    private void addSort(SearchSourceBuilder searchRequestBuilder, List<EsServiceParams.Sort> sorts) {
        //searchRequestBuilder.sort(sortField, SortOrder.DESC);
        //searchRequestBuilder.sort(new FieldSortBuilder(sorts.getField()).order(SortOrder.DESC));//You can also press the_idYou can also press the
        for (EsServiceParams.Sort sort : sorts) {
            if (StringUtils.isBlank(sort.getField())) {
                sort.setField("approveTime");
                sort.setOrder("desc");
            }
            if ("asc".equals(sort.getOrder())) {
                searchRequestBuilder.sort(SortBuilders.fieldSort(sort.getField()).order(SortOrder.ASC));
            } else {
                searchRequestBuilder.sort(SortBuilders.fieldSort(sort.getField()).order(SortOrder.DESC));
            }
        }
    }

    /**
     * Aggregation processing
     *
     * @param response
     * @return
     */
    public List<Map<String, Object>> getAggregationList(SearchResponse response, List<EsServiceParams.Aggregation> list) {
        List<Map<String, Object>> listMap = new ArrayList();
        if (null != list && list.size() > 0) {
            Aggregations aggregations = response.getAggregations();
            if (0 == aggregations.getAsMap().size()) {
                return listMap;
            }
            Map<String, Aggregation> aggregationMap = aggregations.asMap();
            for (EsServiceParams.Aggregation aggregation : list) {
                if (StringUtils.isNotBlank(aggregation.getField())) {
                    List<Map<String, Object>> termsList = new ArrayList<>();
                    Map<String, Aggregation> asMap = aggregations.getAsMap();
                    if (aggregation.getFieldType().equals("year") || aggregation.getFieldType().equals("date")) {
                        Histogram count = (Histogram) aggregationMap.get(aggregation.getField());
                        int size = 0;
                        for (Histogram.Bucket bucket : count.getBuckets()) {
                            String keys = bucket.getKey().toString();
                            if (keys != null && !keys.equals("") && !keys.equals("null")) {
                                size ++ ;
                                Map<String, Object> termsMap = new HashMap<>();
                                termsMap.put("name", keys.substring(0, 10));
                                termsMap.put("sign", keys.substring(0, 10));
                                if (aggregation.getFieldType().equals("year")) {
                                    termsMap.put("name", keys.substring(0, 4));
                                    termsMap.put("sign", keys.substring(0, 4));
                                }
                                termsMap.put("value", bucket.getDocCount());
                                termsMap.put("field", aggregation.getField());
                                termsMap.put("fieldType", aggregation.getFieldType());
                                termsList.add(termsMap);
                                if(size == 10){
                                    break;
                                }
                            }
                        }
                    } else {

                        String filed = parameterSuffix(aggregation.getField(), aggregation.getFieldType());
                        Terms terms = (Terms) asMap.get(filed);
                        List<? extends Terms.Bucket> buckets = terms.getBuckets();
                        for (Terms.Bucket bucket : buckets) {
                            Map<String, Object> termsMap = new HashMap<>();
                            String appName = bucket.getKey().toString();
                            if (appName != null && !appName.equals("") && !appName.equals("null")) {
                                termsMap.put("sign", appName);
                                termsMap.put("name", appName);
                                termsMap.put("value", bucket.getDocCount());
                                termsMap.put("field", aggregation.getField());
                                termsMap.put("fieldType", aggregation.getFieldType());
                                termsList.add(termsMap);
                            }
                        }
                    }

                    Map<String, Object> fieldMap = new HashMap<>();
                    if ("resourceType".equals(aggregation.getField())) {
                        termsList = setResourceType(termsList, Constant.LanguageStatus.RESOURCE_TYPES);
                    }
                    if ("privacyPolicy".equals(aggregation.getField())) {
                        termsList = setResourceType(termsList, Constant.LanguageStatus.PRIVACYPOLICY);
                    }
                    fieldMap.put("list", termsList);
                    fieldMap.put("name", aggregation.getName());
                    listMap.add(fieldMap);
                }


            }
        }
        return listMap;
    }


    private List<Map<String, Object>> setResourceType(List<Map<String, Object>> termsList, String type) {
        List<Map<String, Object>> objMapList = new ArrayList();
        if (termsList.size() > 0) {

            String datas[] = null;
            if (Constant.LanguageStatus.PRIVACYPOLICY.equals(type)) {
                datas = Constant.PrivacyPolicy_TYPES;
            } else if (Constant.LanguageStatus.ROLE.equals(type)) {
                datas = Constant.ROLE_TYPES;
            } else if (Constant.LanguageStatus.RESOURCE_TYPES.equals(type)) {
                datas = Constant.RESOURCE_TYPES;
            }

            for (String resourceType : datas) {
                Map objMap = new HashMap();
                objMap.put("name", resourceType.split("&")[0]);
                objMap.put("sign", CommonUtils.getValueByType(resourceType.split("&")[0], type));
                if (null != termsList && termsList.size() > 0) {
                    for (Map<String, Object> map : termsList) {
                        if (map.get("sign").equals(resourceType.split("&")[0])) {
                            objMap.put("value", map.get("value"));
                            objMap.put("field", map.get("field"));
                            objMap.put("fieldType", map.get("fieldType"));
                        }
                    }
                }
                if (!objMap.containsKey("value")) {
                    objMap.put("value", 0);
                }
                objMapList.add(objMap);
            }
        }
        return objMapList;
    }

    /**
     * Highlight Result Set Highlight Result Set
     *
     * @param searchResponse
     * @param highlights
     */
    private List<Map<String, Object>> setSearchResponse(SearchResponse searchResponse, List<EsServiceParams.Highlight> highlights) {

        List<Map<String, Object>> sourceList = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits()) {
            StringBuffer stringBuffer = new StringBuffer();
            searchHit.getSourceAsMap().put("id", searchHit.getId());
            if (null != highlights && highlights.size() > 0) {

                Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();

                for (EsServiceParams.Highlight highlight : highlights) {
                    if (StringUtils.isNotBlank(highlight.getHighlightField())) {
                        HighlightField highlightField1 = highlightFields.get(highlight.getHighlightField());
                        if (null != highlightField1) {
                            Text[] text = searchHit.getHighlightFields().get(highlight.getHighlightField()).getFragments();
                            if (text != null) {
                                for (Text str : text) {
                                    stringBuffer.append(str.string());
                                }
                                //ergodic ergodic，ergodic ergodic
                                searchHit.getSourceAsMap().put(highlight.getHighlightField(), stringBuffer.toString());
                            }
                        }
                    }
                }
            }
            sourceList.add(searchHit.getSourceAsMap());
        }
        return sourceList;
    }

    public void createQueryBuilder(BoolQueryBuilder boolQuery, List<EsServiceParams.EsParameter> parameters) {

        //Initialize aleftInitialize alist
        List<EsServiceParams.EsParameter> parametersLeft = new ArrayList<>();
        List<EsParams> parametersNew = new ArrayList<>();

        Iterator<EsServiceParams.EsParameter> iterator = parameters.iterator();
        while (iterator.hasNext()){
            EsParams parameterNew = new EsParams();
            EsServiceParams.EsParameter v = iterator.next();
            //Filter out all incorrect parameters
            if (StringUtils.isBlank(v.getField()) || StringUtils.isBlank(v.getFieldType()) || StringUtils.isBlank(v.getConnector()) || StringUtils.isBlank(v.getOperator())) {
                iterator.remove();
            }
            //Add to initialization on the leftlistAdd to initialization on the left  Add to initialization on the left
            if (StringUtils.isNotBlank(v.getType()) && "left".equals(v.getType())) {
                parametersLeft.add(v);
                iterator.remove();
            }else {
                parameterNew.setField(v.getField());
                parameterNew.setFieldType(v.getFieldType());
                parameterNew.setConnector(v.getConnector());
                parameterNew.setOperator(v.getOperator());
                parameterNew.setValue(v.getValue());
                parametersNew.add(parameterNew);
            }
        }

        //Processing initializationleft List
        if(parametersLeft.size()>0){
            Map<String, List<EsServiceParams.EsParameter>> gslist = parametersLeft.stream().collect(Collectors.groupingBy(EsServiceParams.EsParameter::getField));
            for (Map.Entry<String, List<EsServiceParams.EsParameter>> entry : gslist.entrySet()) {
                List<EsServiceParams.EsParameter> value = entry.getValue();

                if (null != value && value.size() > 0) {
                    EsParams parameterNew = new EsParams();
                    if (value.size() == 1) {
                        for (EsServiceParams.EsParameter v : value) {
                            parameterNew.setConnector(v.getConnector());
                            parameterNew.setOperator(v.getOperator());
                            parameterNew.setFieldType(v.getFieldType());
                            parameterNew.setValue(v.getValue());
                        }
                    } else {
                        List<String> list = new ArrayList<>();
                        for (EsServiceParams.EsParameter v : value) {
                            parameterNew.setConnector(v.getConnector());
                            parameterNew.setOperator(v.getOperator());
                            parameterNew.setFieldType(v.getFieldType());
                            list.add(v.getValue());
                        }
                        parameterNew.setValues(list);
                    }
                    parameterNew.setField(entry.getKey());
                    parametersNew.add(parameterNew);
                }
            }
        }

        //Start processing parameters
        for (EsParams parameter : parametersNew) {

            if (StringUtils.isBlank(parameter.getField()) || StringUtils.isBlank(parameter.getFieldType()) || StringUtils.isBlank(parameter.getConnector()) || StringUtils.isBlank(parameter.getOperator())) {
                continue;
            }

            switch (parameter.getFieldType()) {
                case "date":
                    if (null != parameter.getValues() && parameter.getValues().size() > 0) {
                        List<String> objYear = parameter.getValues();
                        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                        for (String v : objYear) {
                            LocalDateTime localDateTimeByString2 = DateUtils.getLocalDateTimeByString2(v);
                            LocalDateTime localDateTime = localDateTimeByString2.plusDays(1);
                            String dateTimeString = DateUtils.getDateTimeString2(localDateTime);
                            boolQuery.should(QueryBuilders.rangeQuery(parameter.getField()).gte(v).lt(dateTimeString).timeZone("Asia/Shanghai"));
                        }
                        boolQuery.must(boolQueryBuilder);
                    } else {
                        LocalDateTime localDateTimeByString2 = DateUtils.getLocalDateTimeByString2(parameter.getValue());
                        LocalDateTime localDateTime = localDateTimeByString2.plusDays(1);
                        String dateTimeString = DateUtils.getDateTimeString2(localDateTime);
                        switch (parameter.getConnector()) {
                            case "and":
                                boolQuery.must(QueryBuilders.rangeQuery(parameter.getField()).gte(parameter.getValue()).lt(dateTimeString).timeZone("Asia/Shanghai"));
                                break;
                            case "or":
                                boolQuery.should(QueryBuilders.rangeQuery(parameter.getField()).gte(parameter.getValue()).lt(dateTimeString).timeZone("Asia/Shanghai"));
                                break;
                            case "not":
                                boolQuery.mustNot(QueryBuilders.rangeQuery(parameter.getField()).gte(parameter.getValue()).lt(dateTimeString).timeZone("Asia/Shanghai"));
                                break;
                        }
                        break;
                    }
                    break;
                case "year":
                    if (null != parameter.getValues() && parameter.getValues().size() > 0) {
                        List<String> objYear = parameter.getValues();
                        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                        for (String v : objYear) {
                            boolQueryBuilder.should(QueryBuilders.rangeQuery(parameter.getField()).format("8uuuu").gt(v).lt(Integer.parseInt(v) + 1));
                        }
                        boolQuery.must(boolQueryBuilder);
                    } else {
                        switch (parameter.getConnector()) {
                            case "and":
                                boolQuery.must(QueryBuilders.rangeQuery(parameter.getField()).format("8uuuu").gt(parameter.getValue()).lt(Integer.parseInt(parameter.getValue()) + 1));
                                break;
                            case "or":
                                boolQuery.should(QueryBuilders.rangeQuery(parameter.getField()).format("8uuuu").gt(parameter.getValue()).lt(Integer.parseInt(parameter.getValue()) + 1));
                                break;
                            case "not":
                                boolQuery.mustNot(QueryBuilders.rangeQuery(parameter.getField()).format("8uuuu").gt(parameter.getValue()).lt(Integer.parseInt(parameter.getValue()) + 1));
                                break;
                        }
                        break;
                    }
                    break;
                default:
                    String filed = parameterSuffix(parameter.getField(), parameter.getFieldType());

                    List obj = new ArrayList();
                    if (null != parameter.getValues() && parameter.getValues().size() > 0) {
                        obj = parameter.getValues();
                    } else {
                        obj.add(parameter.getValue());
                    }
                    switch (parameter.getOperator()) {
                        case "EQ":
                            switch (parameter.getConnector()) {
                                case "and":
                                    boolQuery.must(QueryBuilders.termsQuery(filed + ".keyword", obj));
                                    break;
                                case "or":
                                    boolQuery.should(QueryBuilders.termsQuery(filed + ".keyword", obj));
                                    break;
                                case "not":
                                    boolQuery.mustNot(QueryBuilders.termsQuery(filed + ".keyword", obj));
                                    break;
                            }
                            break;
                        case "LK":
                            switch (parameter.getConnector()) {
                                case "and":
                                    boolQuery.must(QueryBuilders.matchPhraseQuery(filed, obj));
                                    break;
                                case "or":
                                    boolQuery.should(QueryBuilders.matchPhraseQuery(filed, obj));
                                    break;
                                case "not":
                                    boolQuery.mustNot(QueryBuilders.matchPhraseQuery(filed, obj));
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
    }

}
