package cn.cnic.instdb.service.impl;

import cn.chinadoi.api.action.UploadFile;
import cn.chinadoi.api.dto.UploadResult;
import cn.cnic.instdb.cacheLoading.CacheLoading;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.exception.CommonException;
import cn.cnic.instdb.model.center.Doi;
import cn.cnic.instdb.model.center.Org;
import cn.cnic.instdb.model.config.EmailConfig;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.model.system.SubjectData;
import cn.cnic.instdb.model.system.ValuesResult;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ExternalInterService;
import cn.cnic.instdb.service.IndexService;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.utils.CommonUtils;
import cn.cnic.instdb.utils.DateUtils;
import cn.cnic.instdb.utils.HttpClient;
import cn.cnic.instdb.utils.InstdbUrl;
import cn.pid21.client.model.Creator;
import cn.pid21.client.model.SoftWareMetadata;
import cn.pid21.client.model.SoftWareWrapper;
import cn.pid21.client.model.datapid.BaseMetadata;
import cn.pid21.client.model.datapid.Identifier;
import cn.pid21.client.model.datapid.MetaDataWrapper;
import cn.pid21.client.model.datapid.Subject;
import cn.pid21.client.util.JacksonUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class ExternalInterServiceImpl implements ExternalInterService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Resource
    private SettingService settingService;
    @Resource
    private IndexService indexService;
    /* Address of the main center */
    @Value("${data.centerHost}")
    private String centerHost;

    @Resource
    private InstdbUrl instdbUrl;

//    @Resource
//    private MongoDatabase mongoDatabase;

    @Override
    public String dataciteDoi(String resourcesId) {
        ResourcesManage resourcesManage = getResourcesManage(resourcesId);
        if (null == resourcesManage) {
            return "500";
        }
        if (null == resourcesManage.getAuthor()) {
            log.error("No author information，No author informationdoiNo author information");
            return "302";
        }
        CenterAccount centerConf = getCenterConf();

        if (StringUtils.isBlank(centerConf.getRepositoryID()) || StringUtils.isBlank(centerConf.getDoiPassword())
                || StringUtils.isBlank(centerConf.getDoiPrefiex()) || StringUtils.isBlank(centerConf.getDoiCode())) {
            return "403";
        }

        Map<String, Object> map = new HashMap<>();
        map.put("titles", new String[]{resourcesManage.getName()});

        List<String> listAuthor = new ArrayList<>();
        net.sf.json.JSONArray authors = resourcesManage.getAuthor();
        if (null != authors && authors.size() > 0) {
            for (int i = 0; i < authors.size(); i++) {
                net.sf.json.JSONObject jsonObject = authors.getJSONObject(i);
                listAuthor.add(jsonObject.getString("name"));
            }
        }
        map.put("creators", listAuthor);
        map.put("publisher", resourcesManage.getOrganization().getName());
        map.put("publicationYear", DateUtils.getCurrentYear());
        String resourceType = resourcesManage.getResourceType();
        String resourceTypeGeneral = "11".equals(resourceType) ? "Dataset" : "13".equals(resourceType) ? "Report" : "14".equals(resourceType) ? "Dissertation"
                : "17".equals(resourceType) ? "Standard" : "19".equals(resourceType) ? "Software" : "Other";
        map.put("resourceTypeGeneral", resourceTypeGeneral);
        map.put("url", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId());
        map.put("schemaVersion", resourcesManage.getVersion());
        //  10.82256/casdc.0000007
        String doi = centerConf.getDoiPrefiex() + "/" + centerConf.getDoiCode() + "." + CommonUtils.getCode(centerConf.getCstrLength());
        map.put("doi", doi);
        String registerDoiJson = getRegisterDoiJson(map);
        log.info("request dataCite request : " + registerDoiJson);
        HttpClient httpClient = new HttpClient();
        try {
            //skiphttps skip datacite skipdoiskip
            String response = httpClient.doPostToDoi(instdbUrl.getDataciteDoiUrl(), registerDoiJson, centerConf.getRepositoryID() + ":" + centerConf.getDoiPassword());
            log.info("request dataCite request : " + response);
            JSONObject parseObject = JSONObject.parseObject(response);
            JSONArray results = parseObject.getJSONArray("errors");
            if (null != results && results.size() > 0) {
                //An error occurred
                log.error("registerdoiregister");
                return "403";
            } else {
                //Return to correct
                return doi;
            }
        } catch (Exception e) {
            log.error("registerdoiregister，register");
            e.printStackTrace();
            return "500";
        }
    }

    /**
     * registerdoi json register
     *
     * @author wdd
     */
    public static String getRegisterDoiJson(Map<String, Object> dataCiteDoiBo) {
        Gson gson = new Gson();
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        Map<String, Object> attributesMap = new LinkedHashMap<>();
        List<Map<String, Object>> creatorsList = new ArrayList<>();
        List<Map<String, Object>> titlesList = new ArrayList<>();
        Map<String, Object> typesMap = new LinkedHashMap<>();
        //types
        typesMap.put("resourceTypeGeneral", dataCiteDoiBo.get("resourceTypeGeneral"));
        //creators
        Map<String, Object> nameMap = new LinkedHashMap<>();
        List<String> creators = (List<String>) dataCiteDoiBo.get("creators");
        for (String name : creators) {
            if (StringUtils.isNotBlank(name)) {
                nameMap.put("name", name);
                creatorsList.add(nameMap);
            }
        }

        //titles
        Map<String, Object> titleMap = new LinkedHashMap<>();
        String[] titles = (String[]) dataCiteDoiBo.get("titles");
        for (String title : titles) {
            if (StringUtils.isNotBlank(title)) {
                titleMap.put("title", title);
                titlesList.add(titleMap);
            }
        }
        //attributes
        //Default registration type to publish status Default registration type to publish status
        attributesMap.put("event", "publish");
        attributesMap.put("doi", dataCiteDoiBo.get("doi"));
        attributesMap.put("creators", creatorsList);
        attributesMap.put("titles", titlesList);
        attributesMap.put("publisher", dataCiteDoiBo.get("publisher"));
        attributesMap.put("publicationYear", Integer.parseInt(dataCiteDoiBo.get("publicationYear").toString()));
        attributesMap.put("types", typesMap);
        attributesMap.put("url", dataCiteDoiBo.get("url"));
        attributesMap.put("schemaVersion", dataCiteDoiBo.get("schemaVersion"));
        //data
        dataMap.put("id", dataCiteDoiBo.get("doi"));
        dataMap.put("type", "dois");
        dataMap.put("attributes", attributesMap);
        jsonMap.put("data", dataMap);
        return gson.toJson(jsonMap);
    }

    @Override
    public Result accessDataInfo(String id, String type, String keyword) {
        boolean b = checkType(type);
        if (!b) {
            return ResultUtils.error(type + "Type parameter mismatch");
        }
        CenterAccount centerConf = getCenterConf();

        if (null == centerConf || StringUtils.isBlank(centerConf.getUsername()) || StringUtils.isBlank(centerConf.getPassword())) {
            return ResultUtils.error("CENTER_SET");
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("account", centerConf.getUsername());
        paramMap.put("password", centerConf.getPassword());
        paramMap.put("modelType", type);
        paramMap.put("keyword", keyword);
        paramMap.put("id", id);

        return publicQuery(paramMap);
    }

    private boolean checkType(String type) {
        switch (type) {
            case "Person":
                return true;
            case "Organization":
                return true;
            case "Project":
                return true;
            case "Paper":
                return true;
            default:
                return false;
        }
    }

    private ResourcesManage getResourcesManage(String resourcesId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(resourcesId));
        ResourcesManage manage = mongoTemplate.findOne(query, ResourcesManage.class);
        return manage;
    }

    @Override
    public String registerDOI(String resourcesId) {

        Doi doi = new Doi();
        ResourcesManage resourcesManage = getResourcesManage(resourcesId);
        if (null == resourcesManage) {
            return "500";
        }
        if (null == resourcesManage.getAuthor()) {
            log.error("No author information，No author informationdoiNo author information");
            return "302";
        }

        List<String> listAuthor = new ArrayList<>();
        net.sf.json.JSONArray authors = resourcesManage.getAuthor();
        if (null != authors && authors.size() > 0) {
            for (int i = 0; i < authors.size(); i++) {
                net.sf.json.JSONObject jsonObject = authors.getJSONObject(i);
                listAuthor.add(jsonObject.getString("name"));
            }
        }

        CenterAccount centerConf = getCenterConf();
        doi.setAccount(centerConf.getUsername());
        doi.setPassword(centerConf.getPassword());
        doi.setTitles(new String[]{resourcesManage.getName()});
        doi.setCreators(listAuthor.toArray(new String[listAuthor.size()]));
        doi.setPublisher(resourcesManage.getOrganization().getName());
        doi.setPublicationYear("2022");
        doi.setSchemaVersion(resourcesManage.getVersion());
        doi.setUrl(instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId());
        String resourceType = resourcesManage.getResourceType();
        String resourceTypeGeneral = "11".equals(resourceType) ? "Dataset" : "13".equals(resourceType) ? "Report" : "14".equals(resourceType) ? "Dissertation"
                : "17".equals(resourceType) ? "Standard" : "19".equals(resourceType) ? "Software" : "Other";
        doi.setResourceTypeGeneral(resourceTypeGeneral);

        String json = JSONObject.toJSONString(doi);
        log.info(json);
        Result responseResult = publicAdd(JSON.toJSONString(doi), centerHost + "/api/v1/registerDOI");
        if (responseResult.getCode() != 200) {
            // return responseResult.getMessage();
            return "403";
        } else {
            JSONObject data = (JSONObject) responseResult.getData();
            return data.getString("doi");
        }
    }

    @Override
    public String registerChinaDOI(String resourcesId) {

        CenterAccount centerConf = getCenterConf();

        if (StringUtils.isBlank(centerConf.getRepositoryID()) || StringUtils.isBlank(centerConf.getDoiPassword())
                || StringUtils.isBlank(centerConf.getDoiPrefiex()) || StringUtils.isBlank(centerConf.getDoiCode())) {
            return "403";
        }

        ResourcesManage resourcesManage = getResourcesManage(resourcesId);
        if (null == resourcesManage) {
            return "500";
        }

        if (null == resourcesManage.getAuthor()) {
            log.error("No author information，No author informationdoiNo author information");
            return "302";
        }

        net.sf.json.JSONArray authors = resourcesManage.getAuthor();
        String authorsName = "";
        if (null != authors && authors.size() > 0) {
            for (int i = 0; i < authors.size(); i++) {
                net.sf.json.JSONObject jsonObject = authors.getJSONObject(i);
                authorsName += jsonObject.getString("name") + ",";
            }
        }

        CenterAccount centerConf1 = settingService.getCenterConf();
        EmailConfig emailConfig = settingService.getEmailConfig();

        //  10.82256/casdc.0000007
        String doi = centerConf.getDoiPrefiex() + "/" + centerConf.getDoiCode() + "." + CommonUtils.getCode(centerConf.getCstrLength());

        //After going online, it needs to be annotated above，After going online, it needs to be annotated above
        /**
         * ChineseDOIChinese：http://www.chinadoi.cn/portal/newsAction!help.action?type=4
         * Generate locally to uploadXMLGenerate locally to upload
         * uploadXMLuploadDOIupload
         * Save file directory toMongoDB
         * Return Results
         */
        File file = null;
        XMLWriter writer = null;
        try {
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            /**
             * Generate locally to uploadXMLGenerate locally to upload
             */
            org.dom4j.Document document = DocumentHelper.createDocument();
            Element doi_batch = document.addElement("doi_batch").addAttribute("version", "1.0.0");

            //head
            Element head = doi_batch.addElement("head");
            head.addElement("doi_batch_id").setText(resourcesManage.getId());
            head.addElement("timestamp").setText(timestamp);
            Element depositor = head.addElement("depositor");
            depositor.addElement("name").setText(centerConf1.getOrgName());
            depositor.addElement("email_address").setText(emailConfig.getUsername());
            head.addElement("registrant").setText(centerConf1.getOrgName());

            //body
            Element body = doi_batch.addElement("body");
            Element science_data = body.addElement("science_data");

            //database
            Element database = science_data.addElement("database");
            {
                //contributors
                Element contributors = database.addElement("contributors");
                Element person_name = contributors.addElement("person_name").addAttribute("sequence", "first").addAttribute("language", "en").addAttribute("contributor_role", "author");
                person_name.setText(authorsName.substring(0, authorsName.length() - 1));
                Element organization = contributors.addElement("organization").addAttribute("sequence", "first").addAttribute("contributor_role", "Creator");
                organization.setText(resourcesManage.getOrganization().getName());
                //titles
                Element titles = database.addElement("titles");
                titles.addElement("title").setText(resourcesManage.getName());
                //   titles.addElement("subtitle").setText(resourcesManage.getName());
                //description
                Element description = database.addElement("description");
                description.setText(resourcesManage.getDescription());
                //publisher
                Element publisher = database.addElement("publisher");
                publisher.addElement("publisher_name").setText(resourcesManage.getPublish().getName());
                publisher.addElement("publisher_place").setText(resourcesManage.getPublish().getOrg());
                //doi_data
                Element doi_data = database.addElement("doi_data");
                doi_data.addElement("doi").setText(doi);
                doi_data.addElement("timestamp").setText(timestamp);
                doi_data.addElement("resource").setText("<![CDATA[" + instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId() + "]]>");
            }

//                //dataset   This doesn't need to be This doesn't need to bedoiThis doesn't need to be
//                Element dataset = science_data.addElement("dataset").addAttribute("dataset_type", "record");
//                {
//                    //contributors
//                    Element contributors = dataset.addElement("contributors");
//                    Element person_name = contributors.addElement("person_name").addAttribute("sequence", "first").addAttribute("contributor_role", "author");
//                    person_name.setText(doiBo.getPerson_name());
//                    Element organization = contributors.addElement("organization").addAttribute("sequence", "first").addAttribute("contributor_role", "Creator");
//                    organization.setText(doiBo.getOrganization());
//                    //titles
//                    Element titles = dataset.addElement("titles");
//                    titles.addElement("title").setText(doiBo.getTitle());
//                    titles.addElement("subtitle").setText(doiBo.getSubtitle());
//                    //dataset_date
//                    Element dataset_date = dataset.addElement("dataset_date");
//                    Element creation_date = dataset_date.addElement("creation_date");
//                    creation_date.addElement("year").setText(year);
//                    Element publication_date = dataset_date.addElement("publication_date").addAttribute("media_type", "online");
//                    publication_date.addElement("year").setText(year);
//                    Element update_date = dataset_date.addElement("update_date");
//                    update_date.addElement("year").setText(year);
//                    dataset.addElement("item_number").setText("science0001");
//                    Element format = dataset.addElement("format").addAttribute("MIME_type", "image");
//                    format.setText("text");
//                    Element doi_data = dataset.addElement("doi_data");
//                    doi_data.addElement("doi").setText(doiBo.getDoi() + "/1");
//                    doi_data.addElement("timestamp").setText(timestamp);
//                    doi_data.addElement("resource").setText("<![CDATA[" + doiBo.getResource() + "]]>");
//                }

            //Set GenerationxmlSet Generation
            OutputFormat format = OutputFormat.createPrettyPrint();
            //Set encoding format
            format.setEncoding("UTF-8");

            String fileName = CommonUtils.getCode(8) + ".xml";
            file = new File(instdbUrl.getResourcesTempFilePath(), fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            writer = new XMLWriter(new FileOutputStream(file), format);
            //Set whether to escape，Set whether to escape
            writer.setEscapeText(false);
            writer.write(document);
            writer.close();

            /**
             * uploadDOIuploadXMLuploadDOIupload
             */
            //Last item：Last item，Last item：1Last item“Last itemDOILast item”，2Last item“Last item”，3Last item“Last item”，4Last item“Last item”
            UploadResult uploadResult = new UploadFile().upload(file.getAbsolutePath(), centerConf.getRepositoryID(), centerConf.getDoiPassword(), 3);
            if (null != uploadResult && null != uploadResult.getNewFileNames() && uploadResult.getNewFileNames().size() > 0) {
                return doi;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return "500";
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //Delete files
            if (null != file && file.exists()) {
                file.delete();
            }
        }
        return "500";
    }

    @Override
    public Result checkDoi(String doiCode) {
        if (StringUtils.isEmpty(doiCode)) {
            return ResultUtils.error("doiParameter error!");
        }
        HttpClient httpClient = new HttpClient();

        CenterAccount centerConf = getCenterConf();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("account", centerConf.getUsername());
        paramMap.put("password", centerConf.getPassword());
        paramMap.put("doiCode", doiCode);

        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), centerHost + "/api/v1/checkDOI");
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success((String) resultMap.get("message"));
    }

    @Override
    public Result findTemplateById(String id) {
        HttpClient httpClient = new HttpClient();
        List<NameValuePair> params = new ArrayList<>();
        CenterAccount centerConf = getCenterConf();
        params.add(new BasicNameValuePair("id", id));
        params.add(new BasicNameValuePair("account", centerConf.getUsername()));
        params.add(new BasicNameValuePair("password", centerConf.getPassword()));
        String result = "";
        try {
            result = httpClient.doGetWayTwo(params, centerHost + "/api/v1/findTemplateById",new HashMap<>());
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        if (code != 200) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success((String) resultMap.get("message"), resultMap.get("result"));

    }

    @Override
    public Result accessOrgList(String pid) {

        List<NameValuePair> params = new ArrayList<>();
        CenterAccount centerConf = getCenterConf();
        if (StringUtils.isNotBlank(pid)) {
            params.add(new BasicNameValuePair("id", pid));
            params.add(new BasicNameValuePair("key", ""));
        }
        params.add(new BasicNameValuePair("account", centerConf.getUsername()));
        params.add(new BasicNameValuePair("password", centerConf.getPassword()));

        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doGetWayTwo(params, centerHost + "/api/v1/findPubDataByTips",new HashMap<>());
        } catch (Exception e) {
            log.info(e.getMessage());
            return ResultUtils.error("Error in obtaining the list of publishing institutions!");
        }

        Map resultMap = JSONObject.parseObject(result, Map.class);
        if (resultMap == null) {
            return ResultUtils.error("Parsing failed");
        }
        if (StringUtils.isEmpty(result)) {
            return ResultUtils.error("Parsing failed");
        }
        Object code = resultMap.get("code");
        Object success = resultMap.get("success");
        if (code == null || success == null) {
            return ResultUtils.error("Parsing failed");
        }
        if ((int) code != 200 || !(boolean) success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        List<Map> result1 = (List<Map>) resultMap.get("result");
        return ResultUtils.success(result1);
    }

    @Override
    public void setCredible(String resourcesId) {

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(resourcesId));
        Map map = mongoTemplate.findOne(query2, Map.class, Constant.RESOURCE_COLLECTION_NAME);

        CenterAccount centerConf = getCenterConf();
        Map<String, Object> mapParam = new HashMap<>();
        mapParam.put("account", centerConf.getUsername());
        mapParam.put("password", centerConf.getPassword());
        mapParam.put("status", "credible");
        //author
        if (map.containsKey("author") && null != map.get("author")) {
            ArrayList author = (ArrayList) map.get("author");
            if (null != author && author.size() > 0) {
                for (int i = 0; i < author.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) author.get(i);
                    String authorId = object.get("@id").toString();
                    mapParam.put("id", authorId);
                    publicAdd(JSON.toJSONString(mapParam), centerHost + "/api/v1/addPersonModel");
                }
            }
        }

        //project
        if (map.containsKey("project") && null != map.get("project")) {
            ArrayList project = (ArrayList) map.get("project");
            if (null != project && project.size() > 0) {
                for (int i = 0; i < project.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) project.get(i);
                    String projectId = object.get("@id").toString();
                    mapParam.put("id", projectId);
                    publicAdd(JSON.toJSONString(mapParam), centerHost + "/api/v1/addProjectModel");
                }
            }
        }
        //paper
        if (map.containsKey("paper") && null != map.get("paper")) {
            ArrayList paper = (ArrayList) map.get("paper");
            if (null != paper && paper.size() > 0) {
                for (int i = 0; i < paper.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) paper.get(i);
                    String paperId = object.get("@id").toString();
                    mapParam.put("id", paperId);
                    publicAdd(JSON.toJSONString(mapParam), centerHost + "/api/v1/addPaperModel");
                }
            }
        }


    }

    @Override
    public Result paperAdd(Map<String, Object> mapParam) {
        CenterAccount centerConf = getCenterConf();
        mapParam.put("account", centerConf.getUsername());
        mapParam.put("password", centerConf.getPassword());
        mapParam.put("status", "credible");
        return publicAdd(JSON.toJSONString(mapParam), centerHost + "/api/v1/addPaperModel");
    }

    @Override
    public Result projectAdd(Map<String, Object> mapParam) {
        CenterAccount centerConf = getCenterConf();
        mapParam.put("account", centerConf.getUsername());
        mapParam.put("password", centerConf.getPassword());
        mapParam.put("status", "credible");
        return publicAdd(JSON.toJSONString(mapParam), centerHost + "/api/v1/addProjectModel");
    }

    @Override
    public Result orgAdd(Org org) {
        //Parameter verification
        List<String> validation = CommonUtils.validation(org);
        if (validation.size() > 0) {
            return ResultUtils.error(validation.toString());
        }
        CenterAccount centerConf = getCenterConf();
        org.setAccount(centerConf.getUsername());
        org.setPassword(centerConf.getPassword());
        return publicAdd(JSON.toJSONString(org), centerHost + "/api/v1/addOrgModel");
    }

    @Override
    public Result bandOrg(String host, String cstr, String orgId) {
        HttpClient httpClient = new HttpClient();
        CenterAccount centerConf = getCenterConf();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("account", centerConf.getUsername());
        paramMap.put("password", centerConf.getPassword());
        paramMap.put("ip", host);
        paramMap.put("orgId", orgId);
        paramMap.put("cstr", cstr);
        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), centerHost + "/api/v1/addPublisherModelNew");
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        //boolean success = (boolean) resultMap.get("success");
        if (code != 200 /*|| !success*/) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success((String) resultMap.get("message"), resultMap.get("result"));
    }

    @Override
    public Result syncTemplate(String orgId, String id, String name, String type, String typeName, File file) {
        HttpClient httpClient = new HttpClient();
        CenterAccount centerConf = getCenterConf();

        //Prevent Chinese garbled characters
        ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);

        //Create a form for uploading files
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
        entityBuilder.addTextBody("id", id);
        entityBuilder.addTextBody("account", centerConf.getUsername());
        entityBuilder.addTextBody("password", centerConf.getPassword());
        entityBuilder.addPart("name", new StringBody(name, contentType));
        entityBuilder.addTextBody("type", type);
        entityBuilder.addPart("typeName", new StringBody(typeName, contentType));
        entityBuilder.addTextBody("publisherId", orgId);

        //Prevent Chinese garbled characters
        entityBuilder.addPart("file", new FileBody(file));//Add uploaded files
        HttpEntity httpEntity = entityBuilder.build();

        String result = "";
        try {
            result = httpClient.upload(httpEntity, centerHost + "/api/v1/insertPubTemplate");
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success((String) resultMap.get("message"));
    }

    /**
     * Handle account passwords separately Handle account passwords separately
     *
     * @return
     */
    private CenterAccount getCenterConf() {

        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        Object acc = cacheLoading.loadingCenter();
        if (acc == null) {
            throw new CommonException(500, "Not joined the scientific data center！");
        }
        CenterAccount centerConf = (CenterAccount) acc;

        if (centerConf.isNetwork()) {
            if (StringUtils.isBlank(centerConf.getUsername()) || StringUtils.isBlank(centerConf.getPassword())) {
                throw new CommonException(500, "The account password of the main center is empty,The account password of the main center is empty");
            }
        }
        return centerConf;
    }

    @Override
    public Result deleteTemplate(String id) {
        HttpClient httpClient = new HttpClient();
        CenterAccount centerConf = getCenterConf();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("account", centerConf.getUsername());
        paramMap.put("password", centerConf.getPassword());
        paramMap.put("id", id);
        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), centerHost + "/api/v1/removePubTemplate");
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success((String) resultMap.get("message"));
    }

    @Override
    public Result findOrgByAccount(String account, String password) {
        HttpClient httpClient = new HttpClient();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("account", account);
        paramMap.put("password", password);
        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), centerHost + "/api/v1/findOrgByAccount");
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success("", resultMap.get("result"));
    }

    /**
     * applicationcstr
     *
     * @param resourcesId
     */
    @Override
    public String applyCSTR(String resourcesId,String doi) {
        ResourcesManage manage = getResourcesManage(resourcesId);
        if (null == manage) {
            log.error("Data resource query failed");
            return "-1";
        }

        //Inquiry of institutional information
        Result result1 = accessOrgList(manage.getOrganization().getId());
        if (null == result1.getData() && 200 != result1.getCode()) {
            log.error("Publishing institution query failed");
            return "-1";
        }


        JSONArray array = (JSONArray) result1.getData();
        JSONObject data = (JSONObject) array.get(0);

        if (null == data) {
            log.error("CSTRScience and technology resource identification organization query failed");
            return "-1";
        }
        CenterAccount centerConf = settingService.getCenterConf();
        if (StringUtils.isBlank(centerConf.getCstr()) || StringUtils.isBlank(centerConf.getSecret()) || StringUtils.isBlank(centerConf.getClientId())
                || StringUtils.isBlank(centerConf.getCstrCode()) || 0 >= centerConf.getCstrLength()) {
            log.error("CSTRThe organization code for scientific and technological resource identification is not configured，The organization code for scientific and technological resource identification is not configured");
            return "-2";
        }

        //cstrThere's a request to continue the transmission on the soft side19
        String cstrNum = "43".equals(manage.getResourceType()) ? "19" : manage.getResourceType();
        String cstr = centerConf.getCstr() + "." + cstrNum + "." + centerConf.getCstrCode() + "." + CommonUtils.getCode(centerConf.getCstrLength());
        Query queryCstr = new Query();
        queryCstr.addCriteria(Criteria.where("cstr").is(cstr));
        long count = mongoTemplate.count(queryCstr, ResourcesManage.class);
        if (count > 0) {
            cstr = centerConf.getCstr() + "." + cstrNum + "." + centerConf.getCstrCode() + "." + CommonUtils.getCode(centerConf.getCstrLength());
        }
        manage.setDoi(doi);
        manage.setCstr(cstr);

        String result = null;
        try {
            String json = "";
            String cstrUrl = "";
            //Soft Book Registration

            if ("43".equals(manage.getResourceType())) {
                List<SoftWareMetadata> softWareMetadataList = Lists.newArrayList();
                SoftWareMetadata softWareMetadata = registerCstrSftcopyright(manage);
                cstrUrl = "/openapi/v2/pid-cstr-service/sftcopyright/batch.register";
                softWareMetadataList.add(softWareMetadata);
                //Set up bulk registration cstr Set up bulk registration
                SoftWareWrapper wrapper = new SoftWareWrapper();
                wrapper.setMetadatas(softWareMetadataList);
                //distribution5distribution
                wrapper.setPrefix(centerConf.getCstr());
                //Convert to XML/JSON Convert to
                json = JacksonUtils.create("json").convert(wrapper);
            } else {
                List<BaseMetadata> metadataList = Lists.newArrayList();
                BaseMetadata baseMetadata = new BaseMetadata();
                baseMetadata = registerCstrDataSet(manage);
                metadataList.add(baseMetadata);
                //Set up bulk registration cstr Set up bulk registration
                MetaDataWrapper wrapper = new MetaDataWrapper();
                wrapper.setMetadatas(metadataList);
                //distribution5distribution
                wrapper.setPrefix(centerConf.getCstr());
                //Convert to XML/JSON Convert to
                json = JacksonUtils.create("json").convert(wrapper);
                cstrUrl = "/openapi/v2/pid-cstr-service/cstr.batch.register";
            }
            log.info("json cstr metadata list is : {}", json);
            HttpClient httpClient = new HttpClient();
            result = httpClient.doPostCstr(instdbUrl.getCstrUrl() + cstrUrl, json, centerConf.getClientId(), centerConf.getSecret());
            JSONObject resultJsonObject = JSON.parseObject(result);
            if ((Integer) resultJsonObject.get("code") == 200) {
                //login was successful
                return cstr;
            } else {
                JSONArray message = resultJsonObject.getJSONArray("message");
                if (null != message && message.size() > 0) {
                    log.error(message.getJSONObject(0).getString("content"));
                }
            }
        } catch (Exception e) {
            log.error(result);
            e.printStackTrace();
            return "-3";
        }
        log.error("context", "cstr Registration results：" + result);
        return "-3";
    }


    //Universal registration
    private BaseMetadata registerCstrDataSet( ResourcesManage manage ) {

        //Theme and classification
        Subject subject = new Subject();
        subject.setKeyWordsCN(manage.getKeywords());
        subject.setKeyWordsEN(manage.getKeywords_en());


        List<String> subjectDB = manage.getSubject();
        if (null != subjectDB && subjectDB.size() > 0) {
            List<String> list = new ArrayList<>();
            for (String str : subjectDB) {
                SubjectData subjectData = mongoTemplate.findOne(new Query().addCriteria(new Criteria().orOperator(
                        Criteria.where("one_rank_name").is(str), Criteria.where("two_rank_name").is(str), Criteria.where("three_rank_name").is(str))), SubjectData.class);
                if (null != subjectData) {
                    if (!list.contains(subjectData.getOne_rank_no())) {
                        list.add(subjectData.getOne_rank_no());
                    }
                }
            }
            subject.setSubjectName(list);
            subject.setSubjectNameStandard("01");
        }

        List<String> ipc = manage.getIpc();
        if (null != ipc && ipc.size() > 0) {
            subject.setSubjectName(ipc);
            subject.setSubjectNameStandard("03");
        }


        List<Creator> listCreator = new ArrayList<>();
        net.sf.json.JSONArray authors = manage.getAuthor();
        if (null != authors && authors.size() > 0) {
            for (int i = 0; i < authors.size(); i++) {
                net.sf.json.JSONObject jsonObject = authors.getJSONObject(i);
                Creator creator = new Creator();
                creator.setCreatorNameCN(jsonObject.getString("name"));
                listCreator.add(creator);
            }
        }

        if (StringUtils.isNotBlank(manage.getDescription())) {
            if (manage.getDescription().length() > 1024) {
                manage.setDescription(manage.getDescription().substring(0, 1020));
            }
        } else {
            manage.setDescription("testa");
        }
        //Alternative identifier
        Identifier identifier = new Identifier();
        if(StringUtils.isNotBlank(manage.getDoi())){
            identifier.setIdentifierType("04");
            identifier.setIdentifierValue(manage.getDoi());
        }

        // Building a registration metadata organization
        BaseMetadata metadata = BaseMetadata.builder().resourceChineseName(manage.getName())
                .resourceName(StringUtils.isNotBlank(manage.getName_en()) ? manage.getName_en() : "")
                .identifier("CSTR:"+manage.getCstr())
                .resourceType(CommonUtils.getValueByType(manage.getResourceType(),Constant.LanguageStatus.RESOURCE_TYPES))
                // -> *Required ：Required ，Required cstrRequired 
                .urls(Arrays.asList(instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + manage.getId()))
                .identificationStatus(2)
                .descriptionCN(manage.getDescription())
                .descriptionEN(StringUtils.isNotBlank(manage.getDescription_en()) ? manage.getDescription_en() : "")
                .resourceType(manage.getResourceType())
                //.submitOrgName(0 < manage.getSubject().size() ? manage.getSubject().toString() : "test")

                .registerOrganizationCN(manage.getOrganization().getName())

                // .keywords(0 < manage.getKeywords().size() ? manage.getKeywords().toString() : "test")

                // -> *Required ：Required ，cstrRequired ，Required 
                .submitOrgName(manage.getOrganization().getName())
                //doi
                // .alternativeIdentifiers(Arrays.asList(identifier))
                .publicationDate(new Date())
                .shareChannel("1")
                .shareRange("02")
                .process("normal")
                .creators(listCreator)
                .subjectClassifications(Arrays.asList(subject)) //Theme and classification
                .build();
        if(null != identifier && StringUtils.isNotBlank(identifier.getIdentifierType())){
            metadata.setAlternativeIdentifiers(Arrays.asList(identifier));
        }

        return metadata;
    }

    //Soft Book Registration
    private SoftWareMetadata registerCstrSftcopyright( ResourcesManage manage) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(manage.getId()));
        Map map = mongoTemplate.findOne(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);

        //Software Creator
        List authorList = new ArrayList();
        if (map.containsKey("author") && null != map.get("author")) {
            ArrayList author = (ArrayList) map.get("author");
            if (null != author && author.size() > 0) {
                for (int i = 0; i < author.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) author.get(i);
                    authorList.add(object.get("name").toString());
                }
            }
        }
        //copyright owner
        List copyrightHolderList = new ArrayList();
        if (map.containsKey("copyrightHolder") && null != map.get("copyrightHolder")) {
            ArrayList author = (ArrayList) map.get("copyrightHolder");
            if (null != author && author.size() > 0) {
                for (int i = 0; i < author.size(); i++) {
                    LinkedHashMap object = (LinkedHashMap) author.get(i);
                    copyrightHolderList.add(object.get("name").toString());
                }
            }
        }
        String registrationDate = null != map.get("registrationDate") ? map.get("registrationDate").toString() : "";
        String registrationNo = null != map.get("patentNumber") ? map.get("patentNumber").toString() : "";
        String dateCreated = null != map.get("dateCreated") ? map.get("dateCreated").toString() : "";
        String datePublished = null != map.get("datePublished") ? map.get("datePublished").toString() : "";
        String certificateNo = null != map.get("certificateNo") ? map.get("certificateNo").toString() : "";

        SoftWareMetadata metadata = new SoftWareMetadata();
        metadata.setSoftWareName(manage.getName());
        metadata.setCopyRightOwner(copyrightHolderList);
        metadata.setRegistrationDate(DateUtils.LocalDateTimeasDate(DateUtils.getLocalDateTimeByString2(registrationDate)));
        metadata.setUrls(Arrays.asList(instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + manage.getId()));
        metadata.setResourceType("43");
        metadata.setRegistrationNo(registrationNo);
        metadata.setIdentificationStatus(2);
        metadata.setCertificateNo(certificateNo);
        metadata.setSoftWareCreator(authorList);
        if (StringUtils.isNotBlank(dateCreated)) {
            metadata.setDevCompletionDate(dateCreated);
        }
        if (StringUtils.isNotBlank(datePublished)) {
            metadata.setPublicationDate(DateUtils.LocalDateTimeasDate(DateUtils.getLocalDateTimeByString2(datePublished)));
        }
//        metadata.setGetRightWay("Original acquisition");
//        metadata.setClaims("All rights");
        metadata.setIdentifier("CSTR:"+manage.getCstr());


        return metadata;
    }

    @Override
    public Result checkCstr(String cstrCode) {
        if (StringUtils.isEmpty(cstrCode)) {
            return ResultUtils.error("cstrParameter error!");
        }
        if("dev".equals(instdbUrl.getProfilesActive())){
            return ResultUtils.success();
        }
        URL ur = null;
        try {
            ur = new URL(instdbUrl.getCstrUrl()+"/openapi/v2/pid-cstr-service/detail?identifier=CSTR:" + cstrCode);
            InputStream instr = ur.openStream();
            String s, str;
            BufferedReader in = new BufferedReader(new InputStreamReader(instr, StandardCharsets.UTF_8));
            StringBuffer sb = new StringBuffer();
            while ((s = in.readLine()) != null) {
                sb.append(s);
            }
            str = new String(sb);
            JSONObject jsonObject = JSONObject.parseObject(str);
            if ((Integer) jsonObject.get("code") == 200) {
                //Registration code is valid
                return ResultUtils.success();
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultUtils.error("fail");
    }


    /**
     * Public Query
     *
     * @param paramMap
     * @return
     */
    private Result publicQuery(Map<String, Object> paramMap) {
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), centerHost + "/api/v1/findDataByTips");
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        if (StringUtils.isEmpty(result)) {
            return ResultUtils.error("Parsing failed");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        if (resultMap == null) {
            return ResultUtils.error("Parsing failed");
        }
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success(resultMap.get("result"));
    }

    private Result publicAdd(String paramJson, String path) {
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(paramJson, path);
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success((String) resultMap.get("message"), resultMap.get("result"));
    }


    private Result getUrl(String path) {
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doGetWayTwo(path,new HashMap<>());
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("Failed to call the main center interface!");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtils.error((String) resultMap.get("message"));
        }
        return ResultUtils.success();
    }

    //Every day23.59Every day
    @Scheduled(cron = "00 59 23 * * ? ")
    public void getIndexStatisticsNumToFairman() {
        log.info("getIndexStatisticsNumToFairmanThe data extraction task has started");
        Map<String, Object> indexStatisticsNum = indexService.getIndexStatisticsNum();
        if (null != indexStatisticsNum && indexStatisticsNum.size() > 0) {
            CenterAccount centerConf = getCenterConf();
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("userName", centerConf.getUsername());
            paramMap.put("softwareId", "619a4a7fbe34efa5543d8bfa");
            paramMap.put("softwareName", "InstDB");
            paramMap.put("softwareVersion", "1.2.7");
            Map<String, Object> param = new HashMap<>();
            param.put("resourcesCount", indexStatisticsNum.get("totalResources"));// Number of resources
            param.put("topicsCount", indexStatisticsNum.get("specialCount"));// Number of topics
            param.put("downloadCount", indexStatisticsNum.get("totalDownload"));// Number of downloads
            param.put("totalStorage", indexStatisticsNum.get("totalStorage")); // Total Storage
            param.put("visitCount", indexStatisticsNum.get("totalPage")); // Visits
            param.put("usersCount", indexStatisticsNum.get("totalUser")); // Number of users
            param.put("cstrCount", 0);// cstrNumber of registrations

            //Total number of resource files
            Aggregation aggTotalFiles = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)),
                    Aggregation.group()
                            .sum("fileCount").as("longValue"));
            AggregationResults<ValuesResult> resultsTotalFiles = mongoTemplate.aggregate(aggTotalFiles, "resources_manage", ValuesResult.class);
            List<ValuesResult> totalFiles = resultsTotalFiles.getMappedResults();
            long totalFilesNum = 0;
            if (null != totalFiles && totalFiles.size() > 0) {
                totalFilesNum = totalFiles.get(0).getLongValue();
            }
            param.put("totalFiles", totalFilesNum); // Total number of files

            //Calculate download volume
            Aggregation downloadStorage = Aggregation.newAggregation(
                    Aggregation.group()
                            .sum("downloadStorage").as("longValue"));
            AggregationResults<ValuesResult> resultsDownloadStorage = mongoTemplate.aggregate(downloadStorage, "access_records", ValuesResult.class);
            List<ValuesResult> downloadStorages = resultsDownloadStorage.getMappedResults();
            long downloadVolume = 0;
            if (null != downloadStorages && downloadStorages.size() > 0) {
                downloadVolume = downloadStorages.get(0).getLongValue();
            }
            param.put("downloadVolume", downloadVolume); // Download volume

            //First level discipline dimension statistics
            param.put("firstSubject", getSubjectStatistics());
            //file format
            param.put("top20FileFormat", getfileFormatStatistics());
            paramMap.put("softwareData", param);

            //cstrNumber of registrations
            if (StringUtils.isNotBlank(centerConf.getCstr())) {
                Query query = new Query();
                query.addCriteria(Criteria.where("cstr").regex(centerConf.getCstr()));
                long cstrCount = mongoTemplate.count(query, ResourcesManage.class);
                param.put("cstrCount", cstrCount);
            }

            log.info(JSON.toJSONString(paramMap));
            HttpClient httpClient = new HttpClient();
            String result = "";
            try {
                result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), instdbUrl.getMarketUrl() + "/api/v2/open/software");
                log.info("getIndexStatisticsNumToFairmanThe data extraction task has ended" + result);
            } catch (Exception e) {
                log.info(e.getMessage());
                log.info("getIndexStatisticsNumToFairmanThe data extraction task has ended，The data extraction task has ended" + result);
            }
        }
    }

    /**
     * First level discipline dimension statistics
     */
    public List<Map> getSubjectStatistics() {
        List<AggregationOperation> aggList = new ArrayList<>();
        //Only display approved and published
        aggList.add(Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)));
        aggList.add(Aggregation.match(Criteria.where("versionFlag").is(Constant.VERSION_FLAG)));
        List<ValuesResult> xuekesss = new ArrayList<>();
        aggList.add(Aggregation.unwind("subject"));
        aggList.add(Aggregation.group("subject")
                .max("subject").as("name")
                .count().as("value"));
        aggList.add(Aggregation.sort(Sort.Direction.DESC, "value"));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<ValuesResult> document = mongoTemplate.aggregate(aggregation, "resources_manage", ValuesResult.class);
        List<Map> list = new ArrayList<>();
        if (null != document.getMappedResults() && document.getMappedResults().size() > 0) {
            xuekesss = document.getMappedResults();
            for (ValuesResult str : xuekesss) {
                SubjectData subjectData = mongoTemplate.findOne(new Query(Criteria.where("one_rank_name").is(str.getName())), SubjectData.class);
                if (null != subjectData) {
                    Map map = new HashMap();
                    map.put("firstSubject", subjectData.getOne_rank_name());
                    map.put("resourcesCount", str.getValue());
                    //Calculate the number of files under the discipline
                    Aggregation aggTotalFiles = Aggregation.newAggregation(
                            Aggregation.match(Criteria.where("subject").in(subjectData.getOne_rank_name())),
                            Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)),
                            Aggregation.group()
                                    .sum("fileCount").as("value"));
                    AggregationResults<ValuesResult> resultsTotalFiles = mongoTemplate.aggregate(aggTotalFiles, "resources_manage", ValuesResult.class);
                    List<ValuesResult> totalFiles = resultsTotalFiles.getMappedResults();
                    Integer totalFilesNum = 0;
                    if (null != totalFiles && totalFiles.size() > 0) {
                        totalFilesNum = totalFiles.get(0).getValue();
                    }
                    map.put("filesCount", totalFilesNum);
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * Top20file format file format
     */
    public List<Map> getfileFormatStatistics() {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        List<Map> resourcesManages = mongoTemplate.find(query, Map.class, "resources_manage");
        List<Map> list = new ArrayList<>();
        if (null != resourcesManages && resourcesManages.size() > 0) {
            Map mapData = new HashMap();
            for (Map map : resourcesManages) {
                if (map.containsKey("fileFormat") && null != map.get("fileFormat")) {
                    LinkedHashMap<String, Object> fileFormat = (LinkedHashMap) map.get("fileFormat");
                    if (null != fileFormat && fileFormat.size() > 0) {
                        for (Map.Entry<String, Object> entry : fileFormat.entrySet()) {
                            if (mapData.containsKey(entry.getKey())) {
                                mapData.put(entry.getKey(), (Integer) mapData.get(entry.getKey()) + (Integer) entry.getValue());
                            } else {
                                mapData.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }

                //fileFormatNewsituation   situation 2023situation6situation28situation15:12:28 wddsituation
                //PreviousfileFormatPreviouskeyPrevious，Previous$  Previous，PreviousfileFormatNewPrevious
                if (map.containsKey("fileFormatNew") && null != map.get("fileFormatNew")) {
                    ArrayList fileFormatNew = (ArrayList) map.get("fileFormatNew");
                    if (null != fileFormatNew && fileFormatNew.size() > 0) {
                        for (int i = 0; i < fileFormatNew.size(); i++) {
                            LinkedHashMap object = (LinkedHashMap) fileFormatNew.get(i);
                            if (mapData.containsKey(object.get("name"))) {
                                mapData.put(object.get("name"), (Integer) mapData.get(object.get("name")) + (Integer) object.get("count"));
                            } else {
                                mapData.put(object.get("name"), object.get("count"));
                            }
                        }
                    }
                }
            }
            if (mapData.size() > 0) {
                Map<String, Integer> map = CommonUtils.sortMap(mapData);
                int num = 0;
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    if (20 == num) {
                        break;
                    }
                    LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap();
                    linkedHashMap.put("fileFormat", entry.getKey());
                    linkedHashMap.put("filesCount", entry.getValue());
                    list.add(linkedHashMap);
                    num++;
                }
            }
        }
        return list;
    }
}
