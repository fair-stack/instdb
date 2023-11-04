package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncResource;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.elasticsearch.EsServiceParams;
import cn.cnic.instdb.model.commentNotice.Comment;
import cn.cnic.instdb.model.config.BasicConfigurationVo;
import cn.cnic.instdb.model.openApi.Apis;
import cn.cnic.instdb.model.openApi.SecretKey;
import cn.cnic.instdb.model.resources.FtpUser;
import cn.cnic.instdb.model.resources.Resources;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.model.system.Template;
import cn.cnic.instdb.model.system.TemplateConfig;
import cn.cnic.instdb.result.EsDataPage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.service.*;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Auther: wdd
 * @Date: 2021/05/28/15:32
 * @Description:
 */
@Service
@Slf4j
public class InstdbApiServiceImpl implements InstdbApiService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

//    @Resource
//    private MongoDatabase mongoDatabase;

    @Resource
    private TransportClient client;

    @Resource
    private MongoUtil mongoUtil;


    @Resource
    private SettingService settingService;

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private ExternalInterService externalInterService;

    @Autowired
    private AsyncResource asyncResource;

    @Autowired
    private ResourcesService resourcesService;


    @Override
    public Map entry(String secretKey) {
        Map<String, Object> map = new HashMap<>();
        long stringTime = System.currentTimeMillis();
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("token", SMS4.Encryption(secretKey + "&" + stringTime));
        ticket.put("expires", 7200);
        map.put("ticket", ticket);
        List<Map<String, String>> serviceList = getServiceList();
        map.put("serviceList", serviceList);
        return map;
    }

    @Override
    public List<Map<String, Object>> datasetList(String publishDate) {
        List<Map<String, Object>> list = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("status").in(Constant.Approval.ADOPT, Constant.Approval.OFFLINE));
       // query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        query.addCriteria(Criteria.where("dataSetSource").ne("scidb"));
        query.with(Sort.by(Sort.Direction.DESC, "approveTime"));
        if (StringUtils.isNotBlank(publishDate)) {
            LocalDateTime localDateTime = DateUtils.getLocalDateTimeByString2(publishDate);
            LocalDateTime localDateTime1 = localDateTime.plusDays(1);
            Criteria criteria = new Criteria();
            query.addCriteria(criteria.andOperator(Criteria.where("approveTime").gte(localDateTime),
                    Criteria.where("approveTime").lt(localDateTime1)));
        }
        List<ResourcesManage> resourcesManage = mongoTemplate.find(query, ResourcesManage.class);
        if (null != resourcesManage) {
            for (ResourcesManage resources : resourcesManage) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", resources.getId());
                map.put("publishDate", DateUtils.getDateTimeString2(resources.getApproveTime()));
                map.put("downloadUrl", instdbUrl.getCallHost() + "/api/resourcesDownloadFileAll?id=" + resources.getId());
                list.add(map);
            }
        }
        return list;
    }

    @Override
    public List<Map<String, String>> getServiceList() {
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

        CenterAccount centerConf = settingService.getCenterConf();
        if (null == centerConf || StringUtils.isBlank(centerConf.getHost())) {
            return list;
        }
        String ip = centerConf.getHost() + "/api";
        String version = "1.0";

        map.put("version", version);
        map.put("name", "ENTRY");
        map.put("url", ip + "/fair/entry");
        list.add(map);

        Map<String, String> mapA2 = new HashMap<>();
        mapA2.put("version", version);
        mapA2.put("name", "DATASET_LIST");
        mapA2.put("url", ip + "/fair/dataset/list");
        list.add(mapA2);

        Map<String, String> mapA4 = new HashMap<>();
        mapA4.put("version", Constant.Api.version);
        mapA4.put("name", "GET_DATASET_DETAILS");
        mapA4.put("url", ip + "/fair/dataset/details");
        list.add(mapA4);

        Map<String, String> mapA6 = new HashMap<>();
        mapA6.put("version", version);
        mapA6.put("name", "DATASET_INFO");
        mapA6.put("url", ip + "/fair/dataset/info");
        list.add(mapA6);

        Map<String, String> mapA5 = new HashMap<>();
        mapA5.put("version", version);
        mapA5.put("name", "GET_TEMPLATES");
        mapA5.put("url", ip + "/fair/getTemplates");
        list.add(mapA5);

        Map<String, String> mapA31 = new HashMap<>();
        mapA31.put("version", version);
        mapA31.put("name", "DATASET_PUBLISH");
        mapA31.put("url", ip + "/fair/dataset/publish");
        list.add(mapA31);

        Map<String, String> mapA32 = new HashMap<>();
        mapA32.put("version", version);
        mapA32.put("name", "DATASET_CANCEL");
        mapA32.put("url", ip + "/fair/dataset/cancel");
        list.add(mapA32);

        Map<String, String> mapA33 = new HashMap<>();
        mapA33.put("version", version);
        mapA33.put("name", "UPLOAD_COMPLETED");
        mapA33.put("url", ip + "/fair/uploadCompleted");
        list.add(mapA33);

        return list;
    }

    @Override
    public List<Map<String, String>> getDataTemplate() {

        List<Map<String, String>> result = new ArrayList<>();

        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.addCriteria(Criteria.where("state").is("0"));
        List<TemplateConfig> templateConfig = mongoTemplate.find(query, TemplateConfig.class);
        if (null != templateConfig && templateConfig.size() > 0) {
            CenterAccount centerConf = settingService.getCenterConf();
            for (TemplateConfig template : templateConfig) {
                Map<String, String> map = new HashMap<>();
                map.put("type", template.getType() + "-" + template.getTypeName());
                map.put("name", template.getName());
                map.put("url", centerConf.getHost() + "/api/getTemplatesByName?name=" + template.getCode());
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public Map getMetaData(String identifier) {

        Map result = new HashMap();
        result.put("code", 500);

        if (StringUtils.isBlank(identifier)) {
            result.put("errorMsg", "identifierCannot be empty");
            return result;
        }
        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("cstr").is(identifier), Criteria.where("doi").is(identifier), Criteria.where("_id").is(identifier)));
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        Map map = mongoTemplate.findOne(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null == map) {
            result.put("errorMsg", "Resource metadata information not matched");
            return result;
        }

        Map faird = new HashMap();
        faird.put("description", "" + map.get("description"));
        faird.put("keywords", CommonUtils.listToStr((List<String>) map.get("keywords"), ";"));
        faird.put("subject", CommonUtils.listToStr((List<String>) map.get("subject"), ";"));
        faird.put("url", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + map.get("_id"));
        faird.put("version", map.get("version"));
        faird.put("language", "zh");
        if (map.containsKey("license") && null != map.get("license")) {
            faird.put("license", Arrays.asList(CommonUtils.getLicense("" + map.get("license"))));
        }

        List<Map> listTitle = new ArrayList<>();
        Map titleZh = new HashMap();
        titleZh.put("title", map.get("name"));
        titleZh.put("language", "zh");
        listTitle.add(titleZh);
        if (map.containsKey("name_en") && null != map.get("name_en")) {
            Map titleEn = new HashMap();
            titleEn.put("title", map.get("name_en"));
            titleEn.put("language", "en");
            listTitle.add(titleEn);
        }
        faird.put("title", listTitle);

        List<Map> identifiers = new ArrayList<>();
        if (map.containsKey("doi") && null != map.get("doi")) {
            Map doi = new HashMap();
            doi.put("id", map.get("doi"));
            doi.put("type", "DOI");
            identifiers.add(doi);
        }
        if (map.containsKey("cstr") && null != map.get("cstr")) {
            Map cstr = new HashMap();
            cstr.put("id", map.get("cstr"));
            cstr.put("type", "CSTR");
            identifiers.add(cstr);
        }
        Map id = new HashMap();
        id.put("id", map.get("_id"));
        id.put("type", "CUSTOM");
        identifiers.add(id);
        faird.put("identifier", identifiers);

        List<Map> authorList = new ArrayList();
        if (map.containsKey("author") && null != map.get("author")) {
            ArrayList author = (ArrayList) map.get("author");
            if (null != author && author.size() > 0) {
                for (int i = 0; i < author.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) author.get(i);
                    Map creators = new HashMap();
                    creators.put("creatorName", object.get("name").toString());
                    if (null != object.get("Organization")) {
                        try {
                            LinkedHashMap organization = (LinkedHashMap) object.get("Organization");
                            if (null != organization && organization.containsKey("name")) {
                                creators.put("affiliation", organization.get("name").toString());
                            }
                        }catch (Exception e){
                            ArrayList organization = (ArrayList) object.get("Organization");
                            if (null != organization && null != organization.get(0)) {
                                LinkedHashMap o = (LinkedHashMap) organization.get(0);
                                creators.put("affiliation", o.get("name").toString());
                            }
                        }
                    }
                    authorList.add(creators);
                }
            }
            faird.put("creators", authorList);
        }

        List<Map> dates = new ArrayList<>();
        Map datesCreated = new HashMap();
        datesCreated.put("type", "Created");
        datesCreated.put("dateTime", DateUtils.getDateString1((Date) map.get("createTime")));
        dates.add(datesCreated);

        Map datesIssued = new HashMap();
        datesIssued.put("type", "Issued");
        datesIssued.put("dateTime", DateUtils.getDateString1((Date) map.get("approveTime")));
        dates.add(datesIssued);

        Map datesUpdated = new HashMap();
        datesUpdated.put("type", "Updated");
        datesUpdated.put("dateTime", DateUtils.getDateString1((Date) map.get("approveTime")));
        dates.add(datesUpdated);
        faird.put("dates", dates);


        List<String> listFormat = new ArrayList<>();
        LinkedHashMap<String, Object> fileFormat = (LinkedHashMap) map.get("fileFormat");
        if (null != fileFormat && fileFormat.size() > 0) {
            for (Map.Entry<String, Object> entry : fileFormat.entrySet()) {
                listFormat.add(entry.getKey());
            }
        } else if (map.containsKey("fileFormatNew") && null != map.get("fileFormatNew")) {
            ArrayList fileFormatNew = (ArrayList) map.get("fileFormatNew");
            if (null != fileFormatNew && fileFormatNew.size() > 0) {
                for (int i = 0; i < fileFormatNew.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) fileFormatNew.get(i);
                    if (null != object && object.containsKey("name")) {
                        listFormat.add("" + object.get("name"));
                    }
                }
            }
        }
        if (listFormat.size() > 0) {
            faird.put("format", CommonUtils.listToStr(listFormat, ";"));
        }

        CenterAccount centerConf = settingService.getCenterConf();
        Map publisher = new HashMap();
        publisher.put("name", centerConf.getOrgName());
        publisher.put("url", centerConf.getHost());
        faird.put("publisher", publisher);
        Resources.Publish publish = (Resources.Publish) map.get("publish");
        if (null != publish) {
            faird.put("email", Arrays.asList(publish.getEmail()));
        } else if (map.containsKey("correspondingAuthor") && null != map.get("correspondingAuthor")) {
            faird.put("email", Arrays.asList(map.get("correspondingAuthor")));
        }
        result.put("code", 200);
        result.put("data", JSONObject.toJSON(faird));
        return result;
    }


    @Override
    public EsDataPage datasetSearch(String param, String filters, String startDate, String endDate, Integer page, Integer pageSize) {

        EsServiceParams esServiceParams = new EsServiceParams();
        List<EsServiceParams.Aggregation> aggregationList = new ArrayList<>();
        List<EsServiceParams.EsParameter> esParameterList = new ArrayList<>();
        EsServiceParams.EsParameter esParameter = new EsServiceParams.EsParameter();

        if (StringUtils.isNotBlank(param)) {
            esParameter.setOperator("ALL");
            esParameter.setField(instdbUrl.getEsSearchField());
            esParameter.setValue(param);
            esParameterList.add(esParameter);
        }
        if (StringUtils.isNotBlank(startDate)) {
            esParameter.setOperator("GTE");
            esParameter.setField(Constant.RESOURCE_CREATE_TIME);
            esParameter.setValue(startDate);
            esParameterList.add(esParameter);
        }
        if (StringUtils.isNotBlank(endDate)) {
            esParameter.setOperator("LTE");
            esParameter.setField(Constant.RESOURCE_CREATE_TIME);
            esParameter.setValue(endDate);
            esParameterList.add(esParameter);
        }

        esServiceParams.setEsParameter(esParameterList);

        if (StringUtils.isNotBlank(filters)) {
            String[] split = filters.split(",");
            if (null != split && split.length > 0) {
                for (String s : split) {
                    EsServiceParams.Aggregation aggregation = new EsServiceParams.Aggregation();
                    aggregation.setField(s);
                    aggregation.setSize(10);
                    aggregation.setFieldType("");
                    aggregationList.add(aggregation);
                }
                esServiceParams.setAggregations(aggregationList);
            }
        }

        esServiceParams.setPage(page);
        esServiceParams.setPageSize(pageSize);

        ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
        try {
            return elasticSearchUtil.searchDataPage("", esServiceParams, client, instdbUrl);
        } catch (Exception e) {
            log.error("context", e);
        }
        return null;
    }


    /**
     * checktypecheck
     *
     * @param type
     * @return
     */
    private boolean checkType(String type) {
        switch (type) {
            case "doi":
                return true;
            case "cstr":
                return true;
            case "id":
                return true;
            default:
                return false;
        }
    }

    private BasicDBObject setParameter(String type, String data) {
        BasicDBObject query = new BasicDBObject();
        query.put("status", Constant.Approval.ADOPT);
        switch (type) {
            case "doi":
                query.put("doi", data);
                break;
            case "cstr":
                query.put("cstr", data);
                break;
            case "id":
                query.put("_id", data);
                break;
        }
        return query;
    }

    @Override
    public Map getDetailsOld(String id) {

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(id));
        Map map = mongoTemplate.findOne(query2, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null != map) {
            map.remove("callbackUrl");
            map.remove("json_id_content");
            map.remove("es_id");
            map.remove("versionFlag");
            map.remove("downloadFileFlag");
            map.remove("templateName");
            map.remove("dataType");
            map.remove("fileIsZip");
            map.remove("showFile");
            map.remove("approvalAuthor");
            map.put("image", instdbUrl.getCallHost() + "/api/datasetLogo/" + id + ".png");
        }
        return map;
    }


    @Override
    public Map getDetails(String id, String version) {
        Map json = new HashMap();
        Map map = mongoTemplate.findOne(new Query(Criteria.where("_id").is(id)), Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null == map) {
            return json;
        }
        CenterAccount centerConf = settingService.getCenterConf();
        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();

        LinkedHashMap context = new LinkedHashMap();
        context.put("casdc", "http://casdc.cn/md/");
        context.put("schema", "https://schema.org/");
        context.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        context.put("dc", "http://purl.org/dc/elements/1.1/");
        context.put("dcmi", "http://purl.org/dc/dcmitype/");
        context.put("dct", "http://datacite.org/schema/kernel-4/");
        context.put("dcterms", "http://purl.org/dc/terms/");
        context.put("dcat", "http://www.w3.org/ns/dcat#");
        context.put("@language", "zh");
        json.put("@context", context);
        //Identification storage
        JSONArray sidentifierArray = new JSONArray();
        if (map.get("resourceType").toString().equals("11")) {
            //data set
            json.put("@type", "dcmi:Dataset");
            json.put("dc:type", "dcmi:Dataset");
        } else if (map.get("resourceType").toString().equals("16")) {
            //patent
            json.put("@type", "http://purl.org/coar/resource_type/c_15cd");
            json.put("dc:type", "http://purl.org/coar/resource_type/c_15cd");
            //Patentee
            if (map.containsKey("provider") && null != map.get("provider")) {
                ArrayList provider = (ArrayList) map.get("provider");
                if (null != provider && provider.size() > 0) {
                    for (int i = 0; i < provider.size(); i++) {
                        LinkedHashMap object = (LinkedHashMap) provider.get(i);
                        String name = object.get("name").toString();
                        json.put("schema:copyrightHolder", name);
                        json.put("schema:provider", name);
                    }
                }
            }


            if (map.containsKey("patentNumber") && null != map.get("patentNumber")) {
                JSONObject object = new JSONObject();
                object.put("@type", "https://casdc.cn/md/patentNumber");
                object.put("@id", map.get("patentNumber").toString());
                sidentifierArray.add(object);
            }

        } else if (map.get("resourceType").toString().equals("19")) {
            //software
            json.put("@type", "http://purl.org/coar/resource_type/c_5ce6");
            json.put("dc:type", "http://purl.org/coar/resource_type/c_5ce6");

            //Submitting Institution
            if (map.containsKey("provider") && null != map.get("provider")) {
                ArrayList provider = (ArrayList) map.get("provider");
                if (null != provider && provider.size() > 0) {
                    for (int i = 0; i < provider.size(); i++) {
                        LinkedHashMap object = (LinkedHashMap) provider.get(i);
                        String name = object.get("name").toString();
                        json.put("schema:provider", name);
                        json.put("schema:copyrightHolder", name);
                    }
                }
            }

        } else if (map.get("resourceType").toString().equals("14")) {
            //paper
            json.put("@type", "http://purl.org/coar/resource_type/c_2df8fbb1");
            json.put("dc:type", "http://purl.org/coar/resource_type/c_2df8fbb1");
            //First author submission unit
            if (map.containsKey("provider") && null != map.get("provider")) {
                ArrayList provider = (ArrayList) map.get("provider");
                if (null != provider && provider.size() > 0) {
                    for (int i = 0; i < provider.size(); i++) {
                        LinkedHashMap object = (LinkedHashMap) provider.get(i);
                        String name = object.get("name").toString();
                        json.put("schema:provider", name);
                        break;
                    }
                }
            }


            //Processing of papers and journals
            Map paperData = new HashMap();
            if (map.containsKey("paper") && null != map.get("paper")) {
                ArrayList paper = (ArrayList) map.get("paper");
                if (null != paper && paper.size() > 0) {
                    for (int i = 0; i < paper.size(); i++) {
                        LinkedHashMap o = (LinkedHashMap) paper.get(i);
                        paperData.put("schema:identifier", o.get("doi"));
                        paperData.put("schema:name", o.get("periodical"));
                        paperData.put("schema:publisher", null != json.get("schema:provider") ? json.get("schema:provider").toString() : "");
                        paperData.put("@id", o.get("url"));
                        break;
                    }
                }
            } else {
                paperData.put("schema:identifier", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + id);
                paperData.put("schema:name", null != map.get("periodical") ? map.get("periodical").toString() : "");
                paperData.put("schema:publisher", null != json.get("schema:provider") ? json.get("schema:provider").toString() : "");
                paperData.put("@id", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + id);
            }
            if (json.size() > 0) {
                json.put("schema:isPartOf", paperData);
            }


        } else if (map.get("resourceType").toString().equals("13")) {
            //report
            json.put("@type", "http://purl.org/coar/resource_type/c_93fc");
            json.put("dc:type", "http://purl.org/coar/resource_type/c_93fc");
            //Drafting unit
            if (map.containsKey("provider") && null != map.get("provider")) {
                ArrayList provider = (ArrayList) map.get("provider");
                if (null != provider && provider.size() > 0) {
                    for (int i = 0; i < provider.size(); i++) {
                        LinkedHashMap object = (LinkedHashMap) provider.get(i);
                        String name = object.get("name").toString();
                        json.put("schema:provider", name);
                    }
                }
            }

        } else if (map.get("resourceType").toString().equals("15")) {
            //work ？ work ？
            json.put("@type", "http://purl.org/coar/resource_type/c_2f33");
            json.put("dc:type", "http://purl.org/coar/resource_type/c_2f33");

            if (map.containsKey("ISBN") && null != map.get("ISBN")) {
                JSONObject object = new JSONObject();
                object.put("@type", "ISBN");
                object.put("@id", map.get("ISBN").toString());
                sidentifierArray.add(object);
            }

            //Drafting unit
            if (map.containsKey("provider") && null != map.get("provider")) {
                ArrayList provider = (ArrayList) map.get("provider");
                if (null != provider && provider.size() > 0) {
                    for (int i = 0; i < provider.size(); i++) {
                        LinkedHashMap object = (LinkedHashMap) provider.get(i);
                        String name = object.get("name").toString();
                        json.put("schema:provider", name);
                    }
                }
            }

        }else if (map.get("resourceType").toString().equals("43")) {
            json.put("@type", "http://purl.org/coar/resource_type/MW8G-3CR8");
            json.put("dc:type", "http://purl.org/coar/resource_type/MW8G-3CR8");
        } else{
            //other
            json.put("@type", "http://purl.org/coar/resource_type/c_1843");
            json.put("dc:type", "http://purl.org/coar/resource_type/c_1843");
        }

        if (!json.containsKey("@id") && map.containsKey("cstr") && null != map.get("cstr")) {
            if (!Constant.APPLY.equals(map.get("cstr").toString())) {
                json.put("@id", "https://cstr.cn/" + map.get("cstr").toString());
            }
        }
        if (!json.containsKey("@id") && map.containsKey("doi") && null != map.get("doi")) {
            if (!Constant.APPLY.equals(map.get("doi").toString())) {
                json.put("@id", "https://www.doi.org/" + map.get("doi").toString());
            }
        }

        if (!json.containsKey("@id")) {
            json.put("@id", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + id);
        }


        Template template = null;

        if ("scidbTemplate".equals(map.get("templateName").toString()) && null != map.get("dataSetSource") && "scidb".equals(map.get("dataSetSource").toString())) {
            final String subjectURL = "/data/DbMetadata_scidb.xml";
            File resourceFile = FileUtils.getResourceFile(subjectURL);
            if (resourceFile.exists()) {
                template = XmlTemplateUtil.getTemplate(resourceFile);
                FileUtils.deleteFile(resourceFile.getPath());
            }
        } else {
//            final String URL = "src/main/resources/templates/111.xml";
//             template = XmlTemplateUtil.getTemplateInfo(URL);
            template = settingService.getTemplate(map.get("templateName").toString());
        }
        if (null == template) {
            return json;
        }

        //Template data conversion
        List<Template.Group> groups = template.getGroup();

        for (Template.Group group : groups) {
            List<Template.Resource> resources = group.getResources();
            for (Template.Resource resource : resources) {
                String iri = resource.getIri().substring((resource.getIri().lastIndexOf("/") + 1), resource.getIri().length());

                if (map.containsKey(iri)) {
                    String type = resource.getType();
                    switch (type) {
                        case "author":
                            JSONArray authorDataArray = new JSONArray();
                            JSONObject authorData = new JSONObject();
                            //author
                            if (map.containsKey("author") && null != map.get("author")) {
                                ArrayList author = (ArrayList) map.get("author");
                                if (null != author && author.size() > 0) {
                                    JSONArray array = new JSONArray();
                                    for (int i = 0; i < author.size(); i++) {
                                        JSONObject info = new JSONObject();
                                        LinkedHashMap object = (LinkedHashMap) author.get(i);
                                        String typeInfo = null != object.get("@type") ? "Organization".equals(object.get("@type")) ? "casdc:Organization" : "schema:Person" : "schema:Person";
                                        info.put("@type", typeInfo);
                                        if (null == object.get("@id")) {
                                            info.put("schema:name", object.get("name"));
                                            info.put("schema:email", object.get("email"));
                                            array.add(info);
                                            continue;
                                        }
                                        String authorId = object.get("@id").toString();
                                        String typeData = null != object.get("@type") ? object.get("@type").toString() : "Person";
                                        if ("Person".equals(typeData)) {
                                            if (null != object.get("Organization")) {
                                                LinkedHashMap organization = (LinkedHashMap) object.get("Organization");
                                                if (organization.containsKey("name") && null != organization.get("name")) {
                                                    info.put("schema:name", object.get("name"));
                                                    info.put("schema:email", object.get("email"));
                                                    info.put("schema:worksFor", organization.get("name"));
                                                    array.add(info);
                                                    continue;
                                                }
                                            }
                                        } else if ("Organization".equals(typeData)) {
                                            info.put("schema:name", object.get("name"));
                                            array.add(info);
                                            continue;
                                        }
                                        Result result = null;
                                        if ("Person".equals(typeData)) {
                                            result = externalInterService.accessDataInfo(authorId, "Person", null);
                                        } else {
                                            result = externalInterService.accessDataInfo(authorId, "Organization", null);
                                        }
                                        if (null != result && 200 == result.getCode()) {
                                            JSONArray data = (JSONArray) result.getData();
                                            if (null != data && data.size() > 0) {
                                                for (int j = 0; j < data.size(); j++) {
                                                    JSONObject o = data.getJSONObject(j);
                                                    if ("Person".equals(typeData) && "Person".equals(o.getString("type"))) {
                                                        info.put("schema:name", o.getString("zh_Name"));
                                                        info.put("schema:email", o.getString("email"));
                                                        if (o.containsKey("employment") && null != o.get("employment")) {
                                                            JSONArray org = (JSONArray) o.get("employment");
                                                            if (null != org && org.size() > 0) {
                                                                for (int x = 0; x < org.size(); x++) {
                                                                    JSONObject oo = org.getJSONObject(x);
                                                                    info.put("schema:worksFor", oo.getString("zh_Name"));
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        info.put("schema:name", o.getString("zh_Name"));
                                                        info.put("schema:description", o.getString("description"));
                                                    }
                                                }
                                                array.add(info);
                                            }
                                        } else {
                                            info.put("schema:name", object.get("name"));
                                            info.put("schema:email", object.get("email"));
                                            array.add(info);
                                        }
                                    }
                                    if (array.size() > 0) {
                                        authorData.put("@list", array);
                                    }
                                }
                                authorDataArray.add(authorData);
                                json.put("schema:author", authorDataArray);
                            }
                            break;
                        case "project":
                            ArrayList project = null;
                            if (map.containsKey("project") && null != map.get("project")) {
                                project = (ArrayList) map.get("project");
                            } else if (map.containsKey("fundingReferences") && null != map.get("fundingReferences")) {
                                project = (ArrayList) map.get("fundingReferences");
                            }
                            if (null != project && project.size() > 0) {
                                JSONArray projectDataArray = new JSONArray();
                                for (int i = 0; i < project.size(); i++) {
                                    JSONObject info = new JSONObject();
                                    LinkedHashMap object = (LinkedHashMap) project.get(i);
                                    if (null == object.get("@id")) {
                                        continue;
                                    }
                                    String projectId = object.get("@id").toString();
                                    Result Project = externalInterService.accessDataInfo(projectId, "Project", null);
                                    if (200 == Project.getCode()) {
                                        JSONArray data = (JSONArray) Project.getData();
                                        if (null != data && data.size() > 0) {
                                            for (int j = 0; j < data.size(); j++) {
                                                JSONObject o = data.getJSONObject(j);
                                                info.put("@id", o.getString("identifier"));
                                                info.put("@type", o.getString("fundType"));
                                                info.put("schema:name", o.getString("zh_Name"));
                                            }
                                            projectDataArray.add(info);
                                        }
                                    }
                                }
                                if (projectDataArray.size() > 0) {
                                    json.put("dct:fundingReferences", projectDataArray);
                                }
                            }
                            break;
                        case "image":
                            json.put("schema:image", instdbUrl.getCallHost() + "/api/datasetLogo/" + id + ".png");
                            break;
                        case "textTabMany":
                            JSONArray keywordsArray = new JSONArray();
                            if (null != map.get(iri + "_en") && null != map.get(iri)) {
                                List<String> keywords = (ArrayList) map.get(iri);
                                if (null != keywords && keywords.size() > 0) {
                                    for (String k : keywords) {
                                        JSONObject object = new JSONObject();
                                        object.put("@value", k);
                                        object.put("@language", "zh");
                                        keywordsArray.add(object);
                                    }
                                }
                                List<String> keywords_en = (ArrayList) map.get(iri + "_en");
                                if (null != keywords_en && keywords_en.size() > 0) {
                                    for (String k : keywords_en) {
                                        JSONObject object = new JSONObject();
                                        object.put("@value", k);
                                        object.put("@language", "en");
                                        keywordsArray.add(object);
                                    }
                                }
                                json.put("schema:" + iri, keywordsArray);
                            } else if (null != map.get(iri) && !json.containsKey("schema:" + iri)) {
                                List<String> keywords = (ArrayList) map.get(iri);
                                if (null != keywords && keywords.size() > 0) {
                                    for (String k : keywords) {
                                        JSONObject object = new JSONObject();
                                        object.put("@value", k);
                                        object.put("@language", "zh");
                                        keywordsArray.add(object);
                                    }
                                }
                                json.put("schema:" + iri, keywordsArray);
                            }
                            break;
                        default:
                            if (null != map.get(iri + "_en") && null != map.get(iri)) {
                                List<Map<String, Object>> name = new ArrayList<>();
                                Map<String, Object> nameMap = new HashMap<>();
                                nameMap.put("@value", map.get(iri));
                                nameMap.put("@language", "zh");
                                name.add(nameMap);
                                Map<String, Object> nameMap1 = new HashMap<>();
                                nameMap1.put("@value", map.get(iri + "_en"));
                                nameMap1.put("@language", "en");
                                name.add(nameMap1);
                                json.put("schema:" + iri, name);
                            } else if (null != map.get(iri) && !json.containsKey("schema:" + iri)) {
                                json.put("schema:" + iri, map.get(iri));
                            }
                            break;
                    }
                }
            }
        }

        if (map.containsKey("subject") && null != map.get("subject")) {
            List<String> subjects = (ArrayList) map.get("subject");
            if (null != subjects && subjects.size() > 0) {
                if ("1.0".equals(version)) {
                    JSONObject object = new JSONObject();
                    object.put("@value", subjects);
                    object.put("@type", "GB/T 13745-2009");
                    json.put("dct:Subject", object);

                } else if ("1.1".equals(version)) {
                    List<Map<String, Object>> subjectsList = new ArrayList<>();
                    for (String sub : subjects) {
                        Map<String, Object> subjectMap = new HashMap<>();
                        subjectMap.put("@value", sub);
                        subjectMap.put("@type", "https://casdc.cn/md/subjectType/GBT13745");
                        subjectsList.add(subjectMap);
                    }
                    json.put("dct:Subject", subjectsList);
                }
            }
        }


        if (map.containsKey("privacyPolicy")) {
            LinkedHashMap object = new LinkedHashMap();
            try {
                ResourcesManage.PrivacyPolicy privacyPolicy = (ResourcesManage.PrivacyPolicy) map.get("privacyPolicy");
                if (null != privacyPolicy) {
                    object.put("type", privacyPolicy.getType());
                    if (StringUtils.isNotBlank(privacyPolicy.getOpenDate())) {
                        object.put("openDate", privacyPolicy.getOpenDate());
                    }
                    if (StringUtils.isNotBlank(privacyPolicy.getCondition())) {
                        object.put("condition", privacyPolicy.getCondition());
                    }
                }
            } catch (Exception e) {
                object = (LinkedHashMap) map.get("privacyPolicy");
            }

            map.put("privacyPolicy", object);
            String type = object.get("type").toString();
            if (Constant.PrivacyPolicy.OPEN.equals(type)) {
                json.put("dcterms:accessRights", "http://purl.org/coar/access_right/c_abf2");
            } else if (Constant.PrivacyPolicy.NOTOPEN.equals(type)) {
                json.put("dcterms:accessRights", "http://purl.org/coar/access_right/c_14cb");
            } else if (Constant.PrivacyPolicy.PROTECT.equals(type)) {
                json.put("dcterms:accessRights", "http://purl.org/coar/access_right/c_f1cf");
                json.put("casdc:dateAvailable", object.get("openDate"));
            } else if (Constant.PrivacyPolicy.CONDITION.equals(type)) {
                json.put("dcterms:accessRights", "http://purl.org/coar/access_right/c_16ec");
            }
        }

        json.put("dcat:byteSize", map.get("storageNum"));
        json.put("casdc:fileNumber", map.get("fileCount"));
        if (Constant.Approval.ADOPT.equals(map.get("status"))) {
            json.put("schema:creativeWorkStatus", "http://purl.org/coar/version/c_970fb48d4fbd8a85");
        } else if (Constant.Approval.OFFLINE.equals(map.get("status"))) {
            json.put("schema:creativeWorkStatus", "http://inspire.ec.europa.eu/registry/status/retired");
        }

        if (map.containsKey("license") && null != map.get("license")) {
            json.put("schema:license", CommonUtils.getLicense(map.get("license").toString()));
        }
//        if (!json.containsKey("schema:license")) {
//            if (map.containsKey("privacyPolicy")) {
//                LinkedHashMap object = (LinkedHashMap) map.get("privacyPolicy");
//                String type = object.get("type").toString();
//                if (Constant.PrivacyPolicy.OPEN.equals(type)) {
//                    json.put("schema:license", "https://creativecommons.org/licenses/by/4.0/");
//                } else if (Constant.PrivacyPolicy.PROTECT.equals(type)) {
//                    json.put("schema:license", "https://creativecommons.org/licenses/by/4.0/");
//                } else if (Constant.PrivacyPolicy.CONDITION.equals(type)) {
//                    json.put("schema:license", "https://creativecommons.org/licenses/by-nc-nd/4.0/");
//                }
//            }
//        }


        if (map.containsKey("doi") && null != map.get("doi")) {
            JSONObject object = new JSONObject();
            object.put("@type", "dct:DOI");
            object.put("@id", "https://www.doi.org/" + map.get("doi").toString());
            sidentifierArray.add(object);
        }
        if (map.containsKey("cstr") && null != map.get("cstr")) {
            JSONObject object = new JSONObject();
            object.put("@type", "https://www.iana.org/assignments/uri-schemes/prov/cstr");
            object.put("@id", "https://cstr.cn/" + map.get("cstr").toString());
            sidentifierArray.add(object);
        }

        if (sidentifierArray.size() > 0) {
            json.put("schema:identifier", sidentifierArray);
        }


        Map publisher = new TreeMap();
        publisher.put("@id", "https://cstr.cn/" + centerConf.getCstr());
        publisher.put("@type", "casdc:Organization");
        publisher.put("schema:name", centerConf.getOrgName());
        json.put("schema:publisher", publisher);


        Map sourceOrganization = new TreeMap();
        sourceOrganization.put("@id", "http://semweb.casdc.cn/resource/casorgs");
        sourceOrganization.put("schema:name", centerConf.getOrgName());
        sourceOrganization.put("schema:logo", instdbUrl.getCallHost() + "/api/banaer_icoLogo/" + basicConfig.getIcoLogo());
        json.put("schema:sourceOrganization", sourceOrganization);

        json.put("schema:datePublished", DateUtils.LocalDateTimeasISO8601((Date) map.get("approveTime")));

        json.put("casdc:originalStatement", "yes");
        json.put("casdc:privacyProtection", "yes");
        json.put("casdc:sensitiveContent", "yes");

        json.put("schema:version", map.get("version").toString());

        if (!json.containsKey("schema:url")) {
            json.put("schema:url", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + id);
        }

//        if (json.get("dcterms:accessRights").equals("http://purl.org/coar/access_right/c_abf2")) {
//            Query query = new Query();
//            query.addCriteria(Criteria.where("resourcesId").is(id)).addCriteria(Criteria.where("authType").ne("part")).addCriteria(Criteria.where("auth").is(Constant.GENERAL));
//            FtpUser ftpUser = mongoTemplate.findOne(query, FtpUser.class);
//            Map<String, String> ftpInfo = new HashMap<>();
//            if (null != ftpUser) {
//                ftpInfo.put("casdc:username", ftpUser.getUsername());
//                ftpInfo.put("casdc:password", ftpUser.getPassword());
//                ftpInfo.put("casdc:ftpUrl", instdbUrl.getFtpHost());
//            } else {
//                resourcesService.createFtpUser(instdbUrl.getFtpHost(), id, Constant.GENERAL, ftpInfo);
//            }
//            json.put("casdc:ftpUrlInfo", ftpInfo);
//        }

        json.remove("schema:privacyPolicy");
        json.remove("schema:subject");
        json.remove("schema:doi");
        json.remove("schema:cstr");

        return json;
    }


    @Override
    public PageHelper getDatasetStatus(Integer page, Integer pageSize) {
        List<Map> list = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        long count = mongoTemplate.count(query, Comment.class);
        mongoUtil.start(page, pageSize, query);
        List<ResourcesManage> resourcesManage = mongoTemplate.find(query, ResourcesManage.class);
        if (null != resourcesManage && resourcesManage.size() > 0) {
            for (ResourcesManage data : resourcesManage) {
                Map<String, Object> map = new HashMap<>();
                map.put("resourcesId", data.getId());
                map.put("updateTime", DateUtils.getDateTimeString2(data.getApproveTime()));
                list.add(map);
            }
        }
        return mongoUtil.pageHelper(count, list);
    }


    public static void main(String[] args) {


        File file = new File("E:\\GameDownload");
        System.out.println(file.exists());
        System.out.println(file.isDirectory());
        System.out.println(file.listFiles().length);

//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
//        String s = sdf.format(new Date());
//        System.out.println(s);
    }

    @Override
    public Map<String, Object> getDatasetInfo(String id) {
        Map<String, Object> map = new HashMap<>();
        Query query = new Query();
        Criteria criteria = Criteria.where("_id").is(id);
        query.addCriteria(criteria);
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null != resourcesManage && Constant.Approval.ADOPT.equals(resourcesManage.getStatus())) {
            map.put("visitCount", resourcesManage.getVisitNum());
            map.put("fileSize", resourcesManage.getStorageNum());
            map.put("fileCount", resourcesManage.getFileCount());
            map.put("downloadCount", resourcesManage.getDownloadNum());
            map.put("publishDate", DateUtils.getDateTimeString(resourcesManage.getApproveTime()));
        }
        return map;
    }

    @Override
    public int checkToken(HttpServletRequest request) {
        int success = 200;
        String requestURI = request.getRequestURI();

        //Interface Service PermissionstokenInterface Service Permissions,Interface Service Permissions
        String secretKey = request.getHeader("secretKey");
        //cookies validate
        if ("/fair/entry".equals(requestURI)) {
            if (StringUtils.isBlank(secretKey)) {
                return 501;
            }
            Query query = new Query();
            query.addCriteria(Criteria.where("status").is("0"));
            query.addCriteria(Criteria.where("value").is(secretKey));
            SecretKey secretKeyObj = mongoTemplate.findOne(query, SecretKey.class);
            if (null == secretKeyObj) {
                log.error(secretKey + ": Authorization code query failed,Authorization code query failed");
                return 503;
            }
        } else if (Constant.useList(Constant.Api.includeUrls, requestURI)) {


            String token = request.getHeader("token");
            String version = request.getHeader("version");
            if (StringUtils.isBlank(token) || StringUtils.isBlank(version)) {
                return 501;
            }
//            if (!Constant.Api.version.equals(version)) {
//                return 504;
//            }
            String decrypt = SMS4.Decrypt(token);
            if (StringUtils.isBlank(decrypt)) {
                return 503;
            }

            Query query = new Query();
            query.addCriteria(Criteria.where("status").is("0"));
            query.addCriteria(Criteria.where("value").is(decrypt.split("&")[0]));
            SecretKey secretKeyObj = mongoTemplate.findOne(query, SecretKey.class);

            if (null == secretKeyObj) {
                log.error(decrypt + ": Authorization code verificationtokenAuthorization code verification");
                return 503;
            }

            int result = TokenProccessor.decryptTokeCode(token, secretKeyObj.getValue());
            if (200 != result) {
                return result;
            }

            Query query1 = new Query();
            query1.addCriteria(Criteria.where("url").regex(requestURI));
            query1.addCriteria(Criteria.where("authorizationList").elemMatch(Criteria.where("id").is(secretKeyObj.getId())));
            Apis apis = mongoTemplate.findOne(query1, Apis.class);
            if (null == apis) {
                log.error(requestURI + "Authorization authority verification failed Authorization authority verification failed");
                return 505;
            } else if ("1".equals(apis.getStatus())) {
                log.error("APIOffline，Offline");
                return 507;
            } else {
                List<Apis.Authorization> authorizationList = apis.getAuthorizationList();
                if (null != authorizationList && authorizationList.size() > 0) {
                    Apis.Authorization authorization;
                    if (StringUtils.isNotBlank(secretKeyObj.getApplicationName())) {
                        authorization = authorizationList.stream().filter(s -> Objects.equals(s.getApplicationName(), secretKeyObj.getApplicationName())).findFirst().orElse(null);
                    } else {
                        authorization = authorizationList.stream().filter(s -> Objects.equals(s.getId(), secretKeyObj.getOrganId())).findFirst().orElse(null);
                    }
                    if (null != authorization) {
                        if (Constant.SHORT_TERM.equals(authorization.getType()) && StringUtils.isNotBlank(authorization.getOpenDate())) {
                            if (!DateUtils.belongCalendar(DateUtils.getDateString(new Date()), authorization.getOpenDate())) {
                                log.error(requestURI + "apiExpiration of Authorization Time Expiration of Authorization Time");
                                return 506;
                            }
                        }
                    }
                }
            }

        }
        return success;
    }

    @Override
    public Map<String, Object> uploadCompleted(String resourceId) {
        log.info("=========================The notification file has arrived====================================");
        Map<String, Object> map = new HashMap<>();
        map.put("result", false);
        //Update resource download file status
        Query query = new Query();
        Criteria criteria = Criteria.where("_id").is(resourceId);
        query.addCriteria(criteria);
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null == resourcesManage) {
            return map;
        }

        if (null != resourcesManage && !Constant.VERSION_FLAG.equals(resourcesManage.getDownloadFileFlag())) {

            //Yes NozipYes No
            String fileIsZip = resourcesManage.getFileIsZip();
            if (StringUtils.isBlank(fileIsZip)) {
                fileIsZip = "yes";
            }

            if ("yes".equals(fileIsZip)) {
                File file = new File(instdbUrl.getResourcesFilePath() + resourceId, resourceId + ".zip");
                if (null != file && file.exists()) {
                    log.info("=========================Resource file start processing==================================");
                    //After the update is completed  After the update is completed
                    log.info("=========================Start extracting files====================================");
                    FileUtils.unZip(file, instdbUrl.getResourcesFilePath() + resourceId);
                    log.info("=========================File decompression completed====================================");
                } else {
                    log.info("=========================file does not exist，file does not exist====================================");
                    map.put("result", false);
                    return map;
                }
            } else {
                File file = new File(instdbUrl.getResourcesFilePath() + resourceId);
                if (null != file && file.exists() && file.isDirectory() && file.listFiles().length > 0) {
                } else {
                    log.info("=========================file does not exist，file does not exist====================================");
                    map.put("result", false);
                    return map;
                }
            }

            List<String> filterFile = new ArrayList<>();
            //Structured processing
            //0 data file  1  data file  2 data file+data file
            if (null != resourcesManage.getDataType() && resourcesManage.getDataType().size() > 0) {
                log.info("=========================Start processing files====================================");
                Map<String, Object> dataType = resourcesManage.getDataType();
                //0 Only data file processing
                if ("0".equals(dataType.get("type").toString())) {
                    asyncResource.dataFileHandle(filterFile, resourceId, query, fileIsZip);
                    setStatus(resourceId);
                    //Structured only
                } else if ("1".equals(dataType.get("type").toString()) && null != dataType.get("fileList")) {
                    asyncResource.structuredHandle(query, dataType, resourceId);
                    setStatus(resourceId);
                    //data file+data file
                } else if ("2".equals(dataType.get("type").toString()) && null != dataType.get("fileList")) {
                    List<String> fileLists = (List<String>) dataType.get("fileList");
                    asyncResource.structuredHandle(query, dataType, resourceId);
                    for (String f : fileLists) {
                        filterFile.add(f + ".xlsx");
                    }
                    asyncResource.dataFileHandle(filterFile, resourceId, query, fileIsZip);
                    setStatus(resourceId);
                }
            } else {
                asyncResource.dataFileHandle(filterFile, resourceId, query, fileIsZip);
                setStatus(resourceId);
            }

        } else {
            setStatus(resourceId);
            log.info("=========================File transfer completion status，File transfer completion status====================================");
        }
        map.put("result", true);
        return map;
    }


    private void setStatus(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("downloadFileFlag", Constant.VERSION_FLAG);
        mongoTemplate.updateFirst(query, update, ResourcesManage.class);
        //Delete the generated account password
        Query queryFtp = new Query();
        queryFtp.addCriteria(Criteria.where("resourcesId").is(id));
        mongoTemplate.remove(queryFtp, FtpUser.class);
        //log.info("============================Push and data status processing completed=================================");
    }


}
