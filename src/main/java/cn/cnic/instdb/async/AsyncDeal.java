package cn.cnic.instdb.async;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.Resources;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.service.EsDataService;
import cn.cnic.instdb.service.ExternalInterService;
import cn.cnic.instdb.service.InstdbApiService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class AsyncDeal {


    @Resource
    private EmailUtils emailUtils;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private InstdbApiService instdbApiService;

    @Resource
    private EsDataService esDataService;

//    @Resource
//    private MongoDatabase mongoDatabase;

    @Resource
    private ExternalInterService externalInterService;


    /**
     * Email sending Email sending
     *
     * @param toEmail
     * @param attachment
     * @param emailModel
     */
    @Async
    public void send(ToEmail toEmail, Map<String, Object> attachment, EmailModel emailModel) {

        //Encapsulation template information
        attachment.put("title", emailModel.getTitle());
        if (emailModel.getMessage().contains("name")) {
            emailModel.setMessage(emailModel.getMessage().replaceAll("name", attachment.get("name").toString()));
        }
        if (emailModel.getMessage().contains("email")) {
            emailModel.setMessage(emailModel.getMessage().replaceAll("email", attachment.get("email").toString()));
        }

        if (emailModel.getMessage().contains("resourceName")) {
            emailModel.setMessage(emailModel.getMessage().replaceAll("resourceName", attachment.get("resourceName").toString()));
        }
        if (emailModel.getMessage().contains("password")) {
            emailModel.setMessage(emailModel.getMessage().replaceAll("password", attachment.get("password").toString()));
        }

        if (emailModel.getCall().contains("toEmail")) {
            emailModel.setCall(emailModel.getCall().replaceAll("toEmail", attachment.get("toEmail").toString()));
        }

        attachment.put("button", emailModel.getButton());
        attachment.put("alert", emailModel.getAlert());
        attachment.put("alertTo", emailModel.getAlertTo());
        attachment.put("end", emailModel.getEnd());
        attachment.put("last", emailModel.getLast());

        emailUtils.sendTemplateMail(toEmail, attachment, emailModel);
        return;
    }


    /**
     * scidbdata processing data processing
     *
     * @param journalZhList
     * @param apiKey
     */
    @Async
    public void setScidbCommunityData(JSONArray journalZhList, String apiKey) {

        for (int i = 0; i < journalZhList.size(); i++) {
            JSONObject jsonObject = journalZhList.getJSONObject(i);
            //Check if the data has been synchronized
            Query query = new Query();
            String dataSetId = jsonObject.getString("dataSetId");
            query.addCriteria(Criteria.where("_id").is(dataSetId));
            ResourcesManage resourcesManages = mongoTemplate.findOne(query, ResourcesManage.class);
            if (null != resourcesManages) {
                //Determine if the versions are consistent  Determine if the versions are consistent
                if (!resourcesManages.getVersion().equals(jsonObject.getString("version"))) {
                    jsonObject.put("resourcesId", resourcesManages.getResourcesId());
                } else {
                    continue;
                }
            }
            setData(jsonObject, apiKey);
        }
    }

    private void setData(JSONObject jsonObject, String apiKey) {

        Resources resource = new Resources();

        //Corresponding template name
        resource.setTemplateName("scidbTemplate");
        //Publisher Information Publisher Information
//        Resources.Publish publish = new Resources.Publish();
//        publish.setEmail("wangdongdong0224@163.com");
//        publish.setName("Wang Dongdong");
//        publish.setOrg("General Center of Chinese Academy of Sciences");
//        resource.setPublish(publish);
//       // Publisher InstitutionidPublisher Institution
//        Resources.Organization organization = new Resources.Organization();
//        organization.setId("61add156fde4bb2cff4a3b50");
//        organization.setName("Chinese Academy of Sciences Scientific Data Center");
//        resource.setOrganization(organization);
        //Resources.CallbackUrl callbackUrl = new Resources.CallbackUrl();
//        //Data approval callback address
//        callbackUrl.setOnSuccess("");
//        //Data approval synchronization modification address  Data approval synchronization modification address
//        callbackUrl.setOnUpdate("");
//        resource.setCallbackUrl(callbackUrl);

        //The type of dataset The type of dataset
        //11&data set", "13&data set", "14&data set", "15&data set", "16&data set", "17&data set", "19&data set", "99&data set"
        resource.setResourceType("11");
        //Resource unique tags maintained by the data publishing end,Resource unique tags maintained by the data publishing end
        resource.setRootId("");

        Map<String, Object> json = new HashMap<>();
        json.put("scidb_communityId", apiKey);
        //  json.put("organization", resource.getOrganization());
        json.put("templateName", resource.getTemplateName());
        //  json.put("publish", resource.getPublish());
        //  json.put("callbackUrl", resource.getCallbackUrl());

        json.put("versionFlag", Constant.VERSION_FLAG);
        json.put("version", jsonObject.getString("version"));
        json.put("createTime", DateUtils.getLocalDateTimeByString(jsonObject.getString("dataSetPublishDate")));
        json.put("_id", jsonObject.getString("dataSetId"));
        json.put("status", Constant.Approval.ADOPT);
        json.put("resourceType", "11");
        json.put("downloadFileFlag", "false");

        json.put("resourcesId", jsonObject.containsKey("resourcesId") && null != jsonObject.get("resourcesId") ? jsonObject.get("resourcesId") : CommonUtils.generateUUID());


        Map<String, Object> typeMap = new HashMap<>(1);
        typeMap.put("type", "0");
        json.put("dataType", typeMap);
        json.put("fileIsZip", "no");

        if (null != jsonObject.get("copyRight")) {
            JSONObject copyRight = jsonObject.getJSONObject("copyRight");
            json.put("license", copyRight.getString("name"));
        }

        json.put("fileCount", jsonObject.getIntValue("count"));
        json.put("storageNum", jsonObject.getLongValue("size"));
        json.put("doi", jsonObject.getString("doi"));
        json.put("cstr", jsonObject.getString("cstr"));

        json.put("approveTime", DateUtils.getLocalDateTimeByString(jsonObject.getString("dataSetUpdateDate")));


        if (null != jsonObject.get("shareStatus")) {
            String shareStatus = jsonObject.getString("shareStatus");
            LinkedHashMap privacyPolicy = new LinkedHashMap();
            if ("EMBARGO".equals(shareStatus)) {
                privacyPolicy.put("type", Constant.PrivacyPolicy.PROTECT);
                if (null != jsonObject.get("protectDay") && org.apache.commons.lang3.StringUtils.isNotBlank(jsonObject.getString("protectDay"))) {
                    String protectDay = jsonObject.getString("protectDay");
                    LocalDateTime localDateTimeByString = DateUtils.getLocalDateTimeByString(protectDay);
                    String dateTimeString2 = DateUtils.getDateTimeString2(localDateTimeByString);
                    privacyPolicy.put("openDate", dateTimeString2);
                }
            } else if ("PUBLIC".equals(shareStatus)) {
                privacyPolicy.put("type", Constant.PrivacyPolicy.OPEN);
            } else if ("RESTRICTED".equals(shareStatus)) {
                privacyPolicy.put("type", Constant.PrivacyPolicy.CONDITION);
            } else {
                privacyPolicy.put("type", Constant.PrivacyPolicy.NOTOPEN);
            }
            json.put("privacyPolicy", privacyPolicy);
        }


        //Image processing
        if (jsonObject.containsKey("coverUrl") && null != jsonObject.get("coverUrl")) {
            String image = CommonUtils.urlToBase64(jsonObject.getString("coverUrl"));
            if (org.apache.commons.lang3.StringUtils.isNotBlank(image)) {
                //Image conversion
                CommonUtils.base64ToFile(instdbUrl.getResourcesPicturePath(), image, json.get("_id").toString());
                json.put("image",true);
            }
        }

        if (jsonObject.containsKey("titleZh") && null != jsonObject.get("titleZh")) {
            json.put("name", jsonObject.get("titleZh"));
        }
        if (jsonObject.containsKey("titleEn") && null != jsonObject.get("titleEn")) {
            json.put("name_en", jsonObject.get("titleEn"));
        }
        if (jsonObject.containsKey("introductionZh") && null != jsonObject.get("introductionZh")) {
            json.put("description", jsonObject.get("introductionZh").toString().replaceAll("<p>",""));
        }
        if (jsonObject.containsKey("introductionEn") && null != jsonObject.get("introductionEn")) {
            json.put("description_en", jsonObject.get("introductionEn"));
        }
        //Corresponding author email
        if (jsonObject.containsKey("correspondent") && null != jsonObject.get("correspondent")) {
            JSONArray correspondent = jsonObject.getJSONArray("correspondent");
            if (null != correspondent && correspondent.size() > 0) {
                json.put("correspondingAuthor", correspondent.get(0));
            }
        }

        if (jsonObject.containsKey("keywordEn") && null != jsonObject.get("keywordEn")) {
            JSONArray keywordEn = jsonObject.getJSONArray("keywordEn");
            json.put("keywords_en", keywordEn);
        }
        if (jsonObject.containsKey("keywordZh") && null != jsonObject.get("keywordZh")) {
            JSONArray keywordZh = jsonObject.getJSONArray("keywordZh");
            json.put("keywords", keywordZh);
        }

        if (jsonObject.containsKey("taxonomy") && null != jsonObject.get("taxonomy")) {
            List<String> subject = new ArrayList<>();
            JSONArray subjects = jsonObject.getJSONArray("taxonomy");
            if (null != subjects && subjects.size() > 0) {
                for (int i = 0; i < subjects.size(); i++) {
                    JSONObject o = subjects.getJSONObject(i);
                    subject.add(o.getString("nameZh"));
                }
                json.put("subject", subject);
            }
        }

        if (jsonObject.containsKey("author") && null != jsonObject.get("author")) {
            List<Map> listAuthor = new ArrayList<>();
            JSONArray authors = jsonObject.getJSONArray("author");
            if (null != authors && authors.size() > 0) {
                for (int i = 0; i < authors.size(); i++) {
                    Map mapAuthor = new HashMap();
                    JSONObject o = authors.getJSONObject(i);
                    mapAuthor.put("name", StringUtils.isNotBlank(o.getString("nameZh")) ? o.getString("nameZh") : "");
                    mapAuthor.put("en_name", StringUtils.isNotBlank(o.getString("nameEn")) ? o.getString("nameEn") : "");
                    mapAuthor.put("email", StringUtils.isNotBlank(o.getString("email")) ? o.getString("email") : "");
                    if (null != o.get("organizations")) {
                        JSONArray organizations = o.getJSONArray("organizations");
                        if (null != organizations && organizations.size() > 0) {
                            List<Map> listOrg = new ArrayList<>();
                            for (int x = 0; x < organizations.size(); x++) {
                                Map mapOrg = new HashMap();
                                JSONObject org = organizations.getJSONObject(x);
                                mapOrg.put("name", StringUtils.isNotBlank(org.getString("nameZh")) ? org.getString("nameZh") : "");
                                mapOrg.put("en_name", StringUtils.isNotBlank(org.getString("nameEn")) ? org.getString("nameEn") : "");
                                listOrg.add(mapOrg);
                            }
                            mapAuthor.put("Organization", listOrg);
                        }
                    }
                    listAuthor.add(mapAuthor);
                }
                json.put("author", listAuthor);
            }
        }

        if (jsonObject.containsKey("papers") && null != jsonObject.get("papers")) {
            List<Map> listpapers = new ArrayList<>();
            JSONArray papers = jsonObject.getJSONArray("papers");
            if (null != papers && papers.size() > 0) {
                for (int i = 0; i < papers.size(); i++) {

                    JSONObject o = papers.getJSONObject(i);
                    if (StringUtils.isBlank(o.getString("titleZh"))) {
                        continue;
                    }
                    Result result = externalInterService.accessDataInfo("", "Paper", o.getString("titleZh"));

                    JSONArray data = (JSONArray) result.getData();
                    if (null != data && data.size() > 0) {
                        for (int j = 0; j < data.size(); j++) {
                            Map mapPaper = new HashMap();
                            JSONObject ooo = data.getJSONObject(j);
                            mapPaper.put("@type", "Paper");
                            mapPaper.put("@id", ooo.getString("id"));
                            mapPaper.put("name", ooo.getString("zh_Name"));
                            mapPaper.put("en_name", ooo.getString("en_Name"));
                            mapPaper.put("periodical", StringUtils.isNotBlank(o.getString("journalZh")) ? o.getString("journalZh") : "");
                            mapPaper.put("doi", StringUtils.isNotBlank(o.getString("doi")) ? o.getString("doi") : "");
                            mapPaper.put("url", StringUtils.isNotBlank(o.getString("url")) ? o.getString("url") : "");
                            mapPaper.put("publishStatus", StringUtils.isNotBlank(o.getString("state")) ? o.getString("state") : "");
                            listpapers.add(mapPaper);
                        }
                    } else {
                        Map<String, Object> mapParam = new HashMap<>();
                        mapParam.put("zh_Name", o.getString("titleZh"));
                        mapParam.put("en_Name", StringUtils.isNotBlank(o.getString("titleEn")) ? o.getString("titleEn") : "");
                        mapParam.put("doi", StringUtils.isNotBlank(o.getString("doi")) ? o.getString("doi") : "");
                        mapParam.put("url", StringUtils.isNotBlank(o.getString("url")) ? o.getString("url") : "");
                        mapParam.put("periodical", StringUtils.isNotBlank(o.getString("journalZh")) ? o.getString("journalZh") : "");
                        mapParam.put("volume_number", StringUtils.isNotBlank(o.getString("manuscriptNo")) ? o.getString("manuscriptNo") : "");
                        mapParam.put("issue_number", StringUtils.isNotBlank(o.getString("journalCode")) ? o.getString("journalCode") : "");
                        Result result1 = externalInterService.paperAdd(mapParam);
                        if (200 == result1.getCode()) {
                            JSONObject data1 = (JSONObject) result1.getData();
                            if (null != data1 && null != data1.get("object")) {
                                Map mapPaper = new HashMap();
                                JSONObject ooo = (JSONObject) data1.get("object");
                                mapPaper.put("@type", "Paper");
                                mapPaper.put("@id", ooo.getString("id"));
                                mapPaper.put("name", ooo.getString("zh_Name"));
                                mapPaper.put("en_name", ooo.getString("en_Name"));
                                mapPaper.put("periodical", ooo.getString("periodical"));
                                mapPaper.put("doi", ooo.getString("doi"));
                                mapPaper.put("url", ooo.getString("url"));
                                mapPaper.put("volume_number", ooo.getString("volume_number"));
                                mapPaper.put("issue_number", ooo.getString("issue_number"));
                                listpapers.add(mapPaper);
                            }
                        }
                    }
                }
            }
            if (listpapers.size() > 0) {
                json.put("paper", listpapers);
            }

        }

        //Project processing
        if (jsonObject.containsKey("funding") && null != jsonObject.get("funding")) {
            List<Map> listproject = new ArrayList<>();
            JSONArray funding = jsonObject.getJSONArray("funding");
            if (null != funding && funding.size() > 0) {
                for (int i = 0; i < funding.size(); i++) {

                    JSONObject o = funding.getJSONObject(i);
                    if (StringUtils.isBlank(o.getString("funding_nameEn"))) {
                        continue;
                    }
                    Result result = externalInterService.accessDataInfo("", "Project", o.getString("funding_nameZh"));

                    JSONArray data = (JSONArray) result.getData();
                    if (null != data && data.size() > 0) {
                        for (int j = 0; j < data.size(); j++) {
                            Map mapPaper = new HashMap();
                            JSONObject ooo = data.getJSONObject(j);
                            mapPaper.put("@type", "Project");
                            mapPaper.put("@id", ooo.getString("id"));
                            mapPaper.put("name", ooo.getString("zh_Name"));
                            mapPaper.put("en_name", ooo.getString("en_Name"));
                            listproject.add(mapPaper);
                        }
                    } else {
                        Map<String, Object> mapParam = new HashMap<>();
                        mapParam.put("zh_Name", o.getString("funding_nameZh"));
                        mapParam.put("en_Name", StringUtils.isNotBlank(o.getString("funding_nameEn")) ? o.getString("funding_nameEn") : "");
                        mapParam.put("fundType", StringUtils.isNotBlank(o.getString("type")) ? o.getString("type") : "");
                        mapParam.put("identifier", StringUtils.isNotBlank(o.getString("funding_code")) ? o.getString("funding_code") : "");
                        Result result1 = externalInterService.projectAdd(mapParam);
                        if (200 == result1.getCode()) {
                            JSONObject data1 = (JSONObject) result1.getData();
                            if (null != data1 && null != data1.get("object")) {
                                Map mapPaper = new HashMap();
                                JSONObject ooo = (JSONObject) data1.get("object");
                                mapPaper.put("@type", "Project");
                                mapPaper.put("@id", ooo.getString("id"));
                                mapPaper.put("name", ooo.getString("zh_Name"));
                                mapPaper.put("en_name", ooo.getString("en_Name"));
                                mapPaper.put("identifier", ooo.getString("identifier"));
                                listproject.add(mapPaper);
                            }
                        }
                    }
                }
                if (listproject.size() > 0) {
                    json.put("project", listproject);
                }
            }
        }

        if (jsonObject.containsKey("referenceLink") && null != jsonObject.get("referenceLink")) {
            json.put("citation", jsonObject.get("referenceLink"));
        }
        json.put("dataSetSource", "scidb");
        Map<String, Object> jsonNew = new HashMap<>();
        jsonNew.putAll(json);

        json.put("approveTime", DateUtils.getDateTimeString2((LocalDateTime) json.get("approveTime")));
        json.put("rootId", json.get("_id").toString());
        json.remove("_id");
        json.remove("callbackUrl");
        json.remove("downloadFileFlag");
        json.remove("organization");
        json.remove("publish");
        json.remove("fileIsZip");
        //preservees
        String esId = esDataService.add(json);
        if ("500".equals(esId)) {
            log.error(json.get("doi") + "Dataset SaveesDataset Save ");
            return;
        } else {
            jsonNew.put("esSync", Constant.Approval.YES);
            jsonNew.put("es_id", esId);
            //Save resource information first Save resource information firstjson
            Map<String, Object> save = mongoTemplate.save(jsonNew, Constant.RESOURCE_COLLECTION_NAME);
        }
        log.info("Data save completed，Data save completedftpData save completed");
        getDataFile(jsonObject.getString("doi"), jsonObject.getString("dataSetId"), apiKey);
        json.clear();
        jsonNew.clear();
    }

    @Async
    public void getDataFile(String doi, String dataSetId, String api_key) {

        if (StringUtils.isBlank(doi) || StringUtils.isBlank(dataSetId) || StringUtils.isBlank(api_key)) {
            log.error("doi " + doi + "  dataSetId " + dataSetId + " api_key " + api_key + " Empty value  Empty value");
            return;
        }
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doGetScidb(Constant.ScidbUrl.FTPBYDOI + doi, api_key);
            Map resultMap = JSONObject.parseObject(result, Map.class);
            int code = (int) resultMap.get("code");
            if (code != 20000) {
                log.error("doi： " + doi + " queryftpquery，query：" + code + resultMap.get("message"));
                return;
            }

            log.info("ProcessingftpProcessing");
            JSONObject data = (JSONObject) resultMap.get("data");
            String host = data.getString("host");
            int port = data.getIntValue("port");
            String user = data.getString("user");
            String pwd = data.getString("pwd");
            String remoteBaseDir = "/";
            String localPath = instdbUrl.getResourcesFilePath() + dataSetId + "/";
            //Go download
            int num = FtpFileUtils.downloadFile(host, port, user, pwd, remoteBaseDir, localPath);
            log.info("ftpFile download complete");
            if (num > 0) {
                instdbApiService.uploadCompleted(dataSetId);
            } else {
                log.error("doi： " + doi + "ftpFile download failed" + data.toJSONString() + "  \t \n");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("doi： " + doi + "scidb ftpFile processing failed" + e.getMessage());
            return;
        }
    }


    @Async
    public void deleteDirectory(String dir) {
        FileUtils.deleteDirectory(dir);
    }

    public static void main(String[] args) {
        String dataSetId = "b35f90111ac344398dfb3d946234d00d";
        String localPath_bak = "E:\\dataCenter\\" + dataSetId + "_bak";
        FileUtils.deleteFileAndDir(localPath_bak);
    }

}
