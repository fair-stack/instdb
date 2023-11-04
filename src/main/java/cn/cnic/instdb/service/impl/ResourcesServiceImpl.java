package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.commentNotice.Notice;
import cn.cnic.instdb.model.config.BasicConfigurationVo;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.*;
import cn.cnic.instdb.model.special.SpecialVo;
import cn.cnic.instdb.model.system.*;
import cn.cnic.instdb.repository.ResourcesManageRepository;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.*;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.jsonldjava.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Auther wdd
 * @Date 2021/3/10 23:39
 * @Desc Resource Publishing
 */
@Service
@Slf4j
public class ResourcesServiceImpl implements ResourcesService {

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    public static final String COLLECTION_NAME = "resources_manage";

    //Temporarily definedtoken
    public static final String token = "instdb";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    @Autowired
    private ResourcesManageRepository resourcesManageRepository;

    @Autowired
    private ApproveService approveService;

    @Autowired
    private SubjectAreaService subjectAreaService;


    @Autowired
    @Lazy
    private AsyncDeal asyncDeal;

    @Resource
    private MongoUtil mongoUtil;

    @Resource
    private JwtTokenUtils jwtTokenUtils;
    @Resource
    private UserRepository userRepository;

    @Resource
    private ExternalInterService externalInterService;

    @Resource
    private EsDataService esDataService;

    @Resource
    private InstdbApiService instdbApiService;

    @Resource
    private SettingService settingService;

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private TransportClient client;


    @Override
    public Map<String, String> dataRelease(Resources resources) {

        Map<String, String> map = new HashMap<>();
        log.info("-------------------a dataset has been submitted-----------------------");
        JSONObject metadata = resources.getMetadata();
        ResourcesManage manage = new ResourcesManage();
        BeanUtils.copyProperties(resources, manage);

        //PublishedResourcesOnlyIdPublished，Published，Published
        if (StringUtils.isNotBlank(resources.getRootId())) {
            checkSubmit(resources.getRootId(), manage);
        } else {
            //Description is newly released  Description is newly releasedtrue
            manage.setVersionFlag(Constant.VERSION_FLAG);
            manage.setResourcesId(CommonUtils.generateUUID());
            manage.setId(CommonUtils.generateUUID());
        }

        Map<String, Object> json = new HashMap<>();
        Map<String, Object> metadataJson = null;
        try {
            //Special field processing Special field processing
            metadataJson = (Map<String, Object>) JsonUtils.fromString(metadata.toString());
            for (Map.Entry<String, Object> entry : metadataJson.entrySet()) {
                //String type defaults to directput
                if (entry.getValue() instanceof String) {
                    json.put(entry.getKey(), entry.getValue());
                } else if (entry.getValue() instanceof ArrayList) {
                    List<Object> lists = (List<Object>) entry.getValue();
                    if (null != lists && lists.size() > 0) {
                        for (Object list : lists) {
                            //The main difference here islistThe main difference here is
                            if (list instanceof LinkedHashMap) {
                                Map<String, Object> listEn = (Map<String, Object>) list;
                                if (null != listEn.get("@language") && "zh".equals(listEn.get("@language"))) {
                                    json.put(entry.getKey(), listEn.get("@value"));
                                } else if (null != listEn.get("@language") && "en".equals(listEn.get("@language"))) {
                                    json.put(entry.getKey() + "_en", listEn.get("@value"));
                                } else {
                                    json.put(entry.getKey(), entry.getValue());
                                }
                            } else {
                                json.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                } else if (entry.getValue() instanceof LinkedHashMap) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }

            //There is a separate field to savejsonThere is a separate field to save
           // json.put("json_id_content", metadata.toString());

            //Markings for the latest version Markings for the latest versiontrue
            json.put("versionFlag", manage.getVersionFlag());
            json.put("version", manage.getVersion());
            json.put("resourcesId", manage.getResourcesId());
            json.put("createTime", LocalDateTime.now());
            json.put("_id", manage.getId());
            json.put("status", Constant.Approval.PENDING_APPROVAL);
            //Identification of whether the resource file has been downloaded and completed falseIdentification of whether the resource file has been downloaded and completed  true Identification of whether the resource file has been downloaded and completed
            if (null != manage.getDataType() && manage.getDataType().size() > 0 && null != manage.getDataType().get("type")) {
                json.put("dataType", manage.getDataType());
                json.put("downloadFileFlag", "false");
                Map<String, Object> dataType = manage.getDataType();
                if ("-1".equals(dataType.get("type").toString())) {
                    json.put("downloadFileFlag", Constant.VERSION_FLAG);
                }
            } else {
                json.put("downloadFileFlag", "false");
            }
            json.put("fileIsZip", manage.getFileIsZip());
            json.put("resourceType", manage.getResourceType());
            json.put("organization", manage.getOrganization());
            json.put("templateName", manage.getTemplateName());
            json.put("publish", manage.getPublish());
            json.put("callbackUrl", manage.getCallbackUrl());

            //Image conversion
            if (json.containsKey("image") && null != json.get("image")) {
                List image = (List) json.get("image");
                if (null != image && image.size() > 0) {
                    CommonUtils.base64ToFile(instdbUrl.getResourcesPicturePath(), image.get(0).toString(), manage.getId());
                    json.put("image", true);
                }
            }

            //Privacy policy is empty
            checkPrivacyPolicy(json);
            if (null != json.get("name_en")) {
                manage.setName_en(json.get("name_en").toString());
            } else {
                manage.setName_en(json.get("name").toString());
            }
            manage.setName(json.get("name").toString());
        } catch (IOException e) {
            log.error("context", e);
            throw new RuntimeException("Publishing failed,Publishing failed");

        }

        LinkedHashMap privacyPolicyNap = new LinkedHashMap();
        try {
            ResourcesManage.PrivacyPolicy privacyPolicy = (ResourcesManage.PrivacyPolicy) json.get("privacyPolicy");
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
            privacyPolicyNap = (LinkedHashMap) json.get("privacyPolicy");
        }
        json.put("privacyPolicy",privacyPolicyNap);

        //Save resource information first Save resource information firstjson
        mongoTemplate.save(json, Constant.RESOURCE_COLLECTION_NAME);
        //Generate approved records
        approveService.save(manage);
        //Update project information
        esDataService.updateProject(json.get("_id").toString());
        //Email and message prompts
        sendEmail(manage);
        map.put("resourceId", manage.getId());
        //establishftpestablishdataspace
        createFtpUser(instdbUrl.getFtpHost(), manage.getId(), "", map);
        return map;
    }

    private void sendEmail(ResourcesManage manage) {
        //Send an email to the administrator
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("resourceName", manage.getName());
        attachment.put("name", manage.getPublish().getName());
        attachment.put("email", manage.getPublish().getEmail());
        attachment.put("url", instdbUrl.getCallHost() + "/center/approve");
        Query query = new Query();
        query.addCriteria(Criteria.where("roles").in(Constant.ADMIN, Constant.ROLE_APPROVE)).addCriteria(Criteria.where("state").is(1));
        List<ConsumerDO> userList = mongoTemplate.find(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null != userList && userList.size() > 0) {
            List<String> listEmail = new ArrayList<>();
            List<Notice> listNotice = new ArrayList<>();
            for (ConsumerDO user : userList) {
                //Send Message Notification
                //Push to each administrator Push to each administratoruserPush to each administratoramdinPush to each administratorcode  Push to each administrator
                Notice notice = new Notice();
                notice.setUsername((user.getEmailAccounts()));
                notice.setType(Constant.Comment.APPROVAL_REMINDER);
                notice.setContent(manage.getPublish().getName() + "(" + manage.getPublish().getEmail() + ")Submitted data resources《" + manage.getName() + "》,Submitted data resources！");
                String name_en = StringUtils.isNotBlank(manage.getName_en()) ? manage.getName_en() : manage.getName();
                notice.setContentEn(manage.getPublish().getName() + "(" + manage.getPublish().getEmail() + ") Submitted the data resource \"" + name_en + "\",please approve！");
                notice.setTitle("Data resource pending approval reminder");
                notice.setTitleEn("Reminder of pending approval of data resources");
                notice.setIs_read("1");
                notice.setCreateTime(LocalDateTime.now());
                notice.setResourcesId(manage.getId());
                listEmail.add(user.getEmailAccounts());
                listNotice.add(notice);
            }
            if (listEmail.size() > 0) {
                mongoTemplate.insertAll(listNotice);
                ToEmail toEmail = new ToEmail();
                toEmail.setTos(listEmail.toArray(new String[listEmail.size()]));
                asyncDeal.send(toEmail, attachment, EmailModel.EMAIL_RESOURCES_APPROVE());
            }
        }

        log.info("resource submission, approval record, message record generated");
    }

    private static void checkPrivacyPolicy(Map<String, Object> json) {
        //Privacy policy is empty
        if (!json.containsKey("privacyPolicy") && null == json.get("privacyPolicy")) {
            LinkedHashMap<String, Object> privacyPolicy = new LinkedHashMap<>();
            privacyPolicy.put("type", Constant.PrivacyPolicy.OPEN);
            json.put("privacyPolicy", privacyPolicy);
        } else {
            LinkedHashMap map = (LinkedHashMap) json.get("privacyPolicy");
            if (null != map.get("type") && map.get("type").toString().equals(Constant.PrivacyPolicy.PROTECT)) {
                if (null == map.get("openDate") || StringUtils.isBlank(map.get("openDate").toString())) {
                    throw new RuntimeException("Data access permission protection period time cannot be empty");
                }
            }
            if (null != map.get("type") && map.get("type").toString().equals(Constant.PrivacyPolicy.CONDITION)) {
                if (null == map.get("condition") || StringUtils.isBlank(map.get("condition").toString())) {
                    throw new RuntimeException("Data restricted access application conditions cannot be empty");
                }
            }
        }
    }

    @Override
    public void resourcesDownloadJsonLd(String id, HttpServletResponse response) {
        Map details = instdbApiService.getDetails(id,"1.1");
        String path = instdbUrl.getResourcesTempFilePath() + DateUtils.dateToStr_yyyyMMddHHMMss(new Date()) + ".json";
        if (null != details && details.size() > 0) {
            String JsonString = JSON.toJSONString(details);
            String s = JsonFormatTool.formatJson(JsonString);
            FileUtils.writeJsonToFile(s, path);
            FileUtils.downloadFile(path, response,"");
        }
    }


    /**
     * Verify if you have permission to download
     * @param token
     * @param resourcesManage
     * @param request
     * @return
     */
    private boolean checkDownload(String token, ResourcesManage resourcesManage, HttpServletRequest request) {
        String username = "";
        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
        ResourcesManage.PrivacyPolicy privacyPolicy = resourcesManage.getPrivacyPolicy();
        String status = resourcesManage.getStatus();
        //Login free and open for direct download
        if (null != basicConfig && StringUtils.isNotBlank(basicConfig.getDownloadPower()) && "yes".equals(basicConfig.getDownloadPower())) {
            if (Constant.PrivacyPolicy.OPEN.equals(privacyPolicy.getType())) {
                return true;
            }
        }
        //Verify if it is an expert review download
        String cookie = CommonUtils.getCookie(request, resourcesManage.getId());
        if (StringUtils.isNotBlank(cookie)) {
            String decrypt = RSAEncrypt.decrypt(cookie);
            if (StringUtils.isNotBlank(decrypt) && decrypt.equals(resourcesManage.getId())) {
                return true;
            }
        }
        //tokenNot empty case
        if (StringUtils.isNotBlank(token) && null != token && !"null".equals(token)) {
            List<String> roles = jwtTokenUtils.getRoles(token);
            //Direct release over the tube
            if (null != roles && roles.contains(Constant.ADMIN)) {
                return true;
            }
            String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
            if (StringUtils.isNotBlank(userIdFromToken)) {
                Optional<ConsumerDO> user = userRepository.findById(userIdFromToken);
                if (!user.isPresent()) {
                    return false;
                }
                username = user.get().getEmailAccounts();
            }
            //Pending Review Pending Review
            if (Constant.Approval.PENDING_APPROVAL.equals(status)) {
                if (null != roles && roles.contains(Constant.ROLE_APPROVE)) {
                    return true;
                }
            }
            if (Constant.PrivacyPolicy.OPEN.equals(resourcesManage.getPrivacyPolicy().getType())) {
                return true;
            } else if (Constant.PrivacyPolicy.NOTOPEN.equals(resourcesManage.getPrivacyPolicy().getType())) {
            } else if (Constant.PrivacyPolicy.PROTECT.equals(resourcesManage.getPrivacyPolicy().getType())) {
                //Protection period Protection period
                String protectDate = resourcesManage.getPrivacyPolicy().getOpenDate();
                boolean effectiveDate = DateUtils.isEffectiveDate(new Date(), protectDate);
                if (effectiveDate) {
                    return true;
                }
            } else if (Constant.PrivacyPolicy.CONDITION.equals(resourcesManage.getPrivacyPolicy().getType())) {
                Query queryResourceAccess = new Query();
                queryResourceAccess.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
                queryResourceAccess.addCriteria(Criteria.where("applyEmail").is(username));
                queryResourceAccess.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.ADOPT));
                ResourceAccess one = mongoTemplate.findOne(queryResourceAccess, ResourceAccess.class);
                if (null != one) {

                    //Visit duration  Visit duration Visit duration  Visit duration
                    if ("range".equals(one.getAccessPeriod()) && StringUtils.isNotBlank(one.getStartTime()) && StringUtils.isNotBlank(one.getEndTime())) {
                        boolean between = DateUtils.isBetween(DateUtils.getLocalDateTimeByString2(one.getStartTime()), DateUtils.getLocalDateTimeByString2(one.getEndTime()));
                        if (!between) {
                            return false;
                        }
                    } else if ("unlimited".equals(one.getAccessPeriod())) {
                    } else {
                        return false;
                    }

                    //Access data permissions  Access data permissions
                    if ("unlimited".equals(one.getAccessData())) {
                    } else if ("range".equals(one.getAccessData()) && null != one.getFilesId() && one.getFilesId().size() > 0) {
                        Query query = new Query();
                        query.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
                        query.addCriteria(Criteria.where("_id").in(one.getFilesId()));
                        List<ResourceFileTree> list = mongoTemplate.find(query, ResourceFileTree.class);
                        if (null != list && list.size() > 0) {
                        }
                    } else {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void resourcesDownloadFile(String token, String resourcesId, String fileId, HttpServletRequest request, HttpServletResponse response) {

        Optional<ResourcesManage> byId = resourcesManageRepository.findById(resourcesId);
        Assert.isTrue(byId.isPresent(), I18nUtil.get("DATA_QUERY_EXCEPTION"));

        String username = jwtTokenUtils.getUsernameFromToken(token);
        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
        //Determine whether to enable matching email suffixes before downloading
        if (StringUtils.isNotBlank(basicConfig.getEmailDownloadPower()) && Constant.Approval.YES.equals(basicConfig.getEmailDownloadPower()) && StringUtils.isNotBlank(basicConfig.getEmailSuffixDownload())) {
            int code = checkFtpEmailSuffix(basicConfig.getEmailSuffixDownload(), username);
            if (500 == code) {
                log.error(I18nUtil.get("FTP_DOWNLOAD_EMAILSUFFIX"));
                throw new RuntimeException(I18nUtil.get("FTP_DOWNLOAD_EMAILSUFFIX"));
            }
        }

        ResourcesManage resourcesManage = byId.get();
        boolean b = checkDownload(token, resourcesManage, request);
        if (!b) {
            throw new RuntimeException(I18nUtil.get("PERMISSION_DENIED"));
        }

        Query queryFile = new Query(Criteria.where("_id").is(fileId));
        ResourceFileTree resourceFileTree = mongoTemplate.findOne(queryFile, ResourceFileTree.class);
        if (null != resourceFileTree && StringUtils.isNotBlank(resourceFileTree.getFilePath())) {
            if (Constant.Approval.ADOPT.equals(byId.get().getStatus())) {
                //Interface Download Count Settings
                Query query = new Query(Criteria.where("_id").is(byId.get().getId()));
                Update update = new Update();
                update.inc("downloadNum", 1);
                setAccessRecords(request, resourcesId, "download", query, update,username,resourceFileTree.getSize());
                mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), ResourcesManage.class);
            }
            FileUtils.downloadFile(resourceFileTree.getFilePath(), response,"");
        }
    }

    @Override
    public void resourcesDownloadFileAll(String token, String id, HttpServletRequest request, HttpServletResponse response) {

//        if (StringUtils.isNotBlank(token) && null != token && !"null".equals(token)) {
//            String username = "";
//            String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
//            if (StringUtils.isNotBlank(userIdFromToken)) {
//                Optional<ConsumerDO> byId = userRepository.findById(userIdFromToken);
//                if (byId.isPresent()) {
//                    username = byId.get().getEmailAccounts();
//                }
//            }
//            Assert.isTrue(StringUtils.isNotBlank(username), I18nUtil.get("LOGIN_EXCEPTION"));
//        } else {
//            String cookie = CommonUtils.getCookie(request, id);
//            if (StringUtils.isBlank(cookie) || !cookie.equals(id)) {
//                throw new RuntimeException(I18nUtil.get("PERMISSION_DENIED"));
//            }
//        }
//        Optional<ResourcesManage> byId = resourcesManageRepository.findById(id);
//        Assert.isTrue(byId.isPresent(), I18nUtil.get("DATA_QUERY_EXCEPTION"));
//
//        //Interface Download Count Settings
//        Query query = new Query(Criteria.where("_id").is(byId.get().getId()));
//        Update update = new Update();
//        update.inc("downloadNum", 1);
//        if (Constant.Approval.ADOPT.equals(byId.get().getStatus())) {
//            mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), ResourcesManage.class);
//        }
//
//        String mulu = byId.get().getId();
//        String path = instdbUrl.getResourcesFilePath() + mulu + "/" + mulu + ".zip";
//        log.info("Download all file paths" + path);
//        FileUtils.downloadFile(path, response);
    }


    /**
     * Determine whether the current user meets the specified email suffix
     * @param emailSuffixDownload
     * @param username
     * @return
     */
    private int checkFtpEmailSuffix(String emailSuffixDownload, String username) {
        if (StringUtils.isBlank(username)) {
            return 500;
        }
        if (emailSuffixDownload.contains(";")) {
            String[] split = emailSuffixDownload.split(";");
            for (String str : split) {
                if (username.contains(str)) {
                    return 200;
                }
            }
        } else if (username.contains(emailSuffixDownload)) {
            return 200;
        }
        return 500;
    }

    @Override
    public Result resourcesFtpDownloadFile(String token, String id, HttpServletRequest request) {
        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
        //judgeftpjudge
        if(StringUtils.isNotBlank(basicConfig.getFtpSwitch()) && Constant.Approval.NO.equals(basicConfig.getFtpSwitch())){
            return ResultUtils.error("FTP_DOWNLOAD_CLOSE");
        }
        String username = jwtTokenUtils.getUsernameFromToken(token);
        //Determine whether to enable matching email suffixes before downloading
        if (StringUtils.isNotBlank(basicConfig.getEmailDownloadPower()) && Constant.Approval.YES.equals(basicConfig.getEmailDownloadPower()) && StringUtils.isNotBlank(basicConfig.getEmailSuffixDownload())) {
            int code = checkFtpEmailSuffix(basicConfig.getEmailSuffixDownload(), username);
            if (500 == code) {
                return ResultUtils.error("FTP_DOWNLOAD_EMAILSUFFIX");
            }
        }
        Map<String, String> map = new HashMap<>();
        Optional<ResourcesManage> byId = resourcesManageRepository.findById(id);
        Assert.isTrue(byId.isPresent(), I18nUtil.get("DATA_QUERY_EXCEPTION"));
        ResourcesManage resourcesManage = byId.get();
        boolean b = checkDownload(token, resourcesManage, request);
        if (!b) {
            return ResultUtils.error("PERMISSION_DENIED");
        }


        if (Constant.PrivacyPolicy.CONDITION.equals(resourcesManage.getPrivacyPolicy().getType())) {
            Query queryResourceAccess = new Query();
            queryResourceAccess.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
            queryResourceAccess.addCriteria(Criteria.where("applyEmail").is(username));
            queryResourceAccess.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.ADOPT));
            ResourceAccess one = mongoTemplate.findOne(queryResourceAccess, ResourceAccess.class);
            if (null != one) {
                //If it is a partial file, it can be viewed  If it is a partial file, it can be viewed
                if ("range".equals(one.getAccessData()) && null != one.getFilesId() && one.getFilesId().size() > 0) {
                    Query query = new Query();
                    query.addCriteria(Criteria.where("resourcesId").is(id));
                    query.addCriteria(Criteria.where("_id").in(one.getFilesId()));
                    List<ResourceFileTree> list = mongoTemplate.find(query, ResourceFileTree.class);
                    if (null != list && list.size() > 0) {
                        map.put("realUsers", username);
                        createFtpUser(instdbUrl.getFtpHost(), id, Constant.GENERAL, map);
                        return ResultUtils.success(map);
                    }
                }
            }
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(id)).addCriteria(Criteria.where("authType").ne("part")).addCriteria(Criteria.where("auth").is(Constant.GENERAL));
        FtpUser ftpUser = mongoTemplate.findOne(query, FtpUser.class);
        if (null != ftpUser) {
            map.put("username", ftpUser.getUsername());
            map.put("password", ftpUser.getPassword());
            map.put("ftpUrl", instdbUrl.getFtpHost());
        } else {
            createFtpUser(instdbUrl.getFtpHost(), id, Constant.GENERAL, map);
        }
        return ResultUtils.success(map);
    }

    private void checkSubmit(String resourcesOnlyId, ResourcesManage manage) {
        Criteria criteria = Criteria.where("_id").is(resourcesOnlyId);
        Query query = new Query();
        query.addCriteria(criteria);
        //inspectresourcesOnlyIdinspect  inspect
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class, COLLECTION_NAME);
        if (null != resourcesManage) {
            if (Constant.Approval.PENDING_APPROVAL.equals(resourcesManage.getStatus())) {
                throw new RuntimeException(I18nUtil.get("RESOURCE_VERSION_APPLY"));
            }
            //New version record
            manage.setVersionFlag(Constant.VERSION_FLAG);
            manage.setResourcesId(resourcesManage.getResourcesId());
            manage.setId(CommonUtils.generateUUID());
        } else {
            //If the transmittedrootIdIf the transmitted If the transmitted：1.If the transmitted，If the transmittedrootidIf the transmitted。
            // 2.Indicates that the dataset has been rejected，Indicates that the dataset has been rejectedid，Indicates that the dataset has been rejected，Indicates that the dataset has been rejectedscidbIndicates that the dataset has been rejected，Indicates that the dataset has been rejectedid
            //If empty If empty If emptyid
            // 2023year8year24year16:32:02  year  setResourcesIdyear  yearidyearsetResourcesId year  year

            Query queryrejectionRecord = new Query();
            queryrejectionRecord.addCriteria(Criteria.where("resourceId").is(resourcesOnlyId));
            Map reject = mongoTemplate.findOne(queryrejectionRecord, Map.class, "rejectionRecord");
            if (null != reject && null != reject.get("correlationId")) {
                manage.setResourcesId(reject.get("correlationId").toString());
                mongoTemplate.remove(queryrejectionRecord, "rejectionRecord");
            } else {
                manage.setResourcesId(CommonUtils.generateUUID());
            }
            manage.setId(resourcesOnlyId);
            manage.setVersionFlag(Constant.VERSION_FLAG);
        }
    }

    @Override
    public Result getResourcesDetails(String token, String id, Integer examine, HttpServletRequest request) {
        if (StringUtils.isBlank(id)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        Query query1 = new Query();
        query1.addCriteria(Criteria.where("_id").is(id));

        Map map = mongoTemplate.findOne(query1, Map.class, Constant.RESOURCE_COLLECTION_NAME);

        if (null == map) {
            return ResultUtils.error(504, "DATA_QUERY_EXCEPTION");
        }

        //Approved access is normal
        if (Constant.Approval.ADOPT.equals(map.get("status").toString())) {
            examine = 0;
        }

        //according totokenaccording to
        ConsumerDO consumerDO = null;
        String username = "";
        List<String> roles = new ArrayList<>();
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        if (StringUtils.isNotBlank(userIdFromToken)) {
            Optional<ConsumerDO> byId = userRepository.findById(userIdFromToken);
            if (byId.isPresent()) {
                consumerDO = byId.get();
                username = consumerDO.getEmailAccounts();
                roles = jwtTokenUtils.getRoles(token);
                if (Constant.Approval.ADOPT.equals(map.get("status").toString())) {
                    //Add History
                    ResourcesHistory history = new ResourcesHistory();
                    history.setResourcesId(id);
                    history.setUsername(username);
                    history.setCreateTime(LocalDateTime.now());
                    mongoTemplate.save(history);
                }
            }
        }

        if (!Constant.Approval.ADOPT.equals(map.get("status").toString())) {
            if (3 != examine) {
                if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
                    return ResultUtils.error(504, "RESOURCE_DOES_NOT_EXIST");
                }
            } else if (3 == examine) {//Control review link access
                String cookie = CommonUtils.getCookie(request, id);
                String decrypt = RSAEncrypt.decrypt(cookie);
                if (StringUtils.isBlank(decrypt) || !decrypt.equals(id)) {
                    return ResultUtils.error(504, "DATA_QUERY_EXCEPTION");
                }
            }
        }

        //Query the list of other versions under the same resource
        //Add query criteria
        Query query2 = new Query();
        query2.addCriteria(Criteria.where("resourcesId").is(map.get("resourcesId")));
        query2.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query2.with(Sort.by(Sort.Direction.DESC, "createTime"));

        List<Map> list = mongoTemplate.find(query2, Map.class, Constant.RESOURCE_COLLECTION_NAME);
//        if(roles.size()>0 && roles.contains(Constant.ADMIN) && 1 == examine){
//            //Administrators can view Administrators can view
//            queryOne.put("status", Constant.Approval.ADOPT);
//            queryOne.put("status", Constant.Approval.PENDING_APPROVAL);
//        }else{
//            queryOne.put("status", Constant.Approval.ADOPT);
//        }
        List<ResourcesManageVo.ResourcesVersion> versionList = new ArrayList<>();
        if (null != list) {
            for (Map<String, Object> resources : list) {
                if (id.equals(resources.get("_id").toString())) {
                    continue;
                }
                ResourcesManageVo.ResourcesVersion versions = new ResourcesManageVo.ResourcesVersion();
                versions.setCreateTime(DateUtils.DateasLocalDateTime((Date) resources.get("createTime")));
                if (null != resources.get("doi")) {
                    versions.setDoi(resources.get("doi").toString());
                }
                if (null != resources.get("cstr")) {
                    versions.setCstr(resources.get("cstr").toString());
                }
                versions.setName(resources.get("name").toString());
                versions.setName_en(null != resources.get("name_en") ? resources.get("name_en").toString() : "");
                versions.setVersion(resources.get("version").toString());
                versions.setId(resources.get("_id").toString());
                Resources.Publish publish = (Resources.Publish) resources.get("publish");
                versions.setAuthor(publish.getName());
                versionList.add(versions);
            }
            map.put("versionList", versionList);
        }


        //doiandcstrand and
        if ((1 == examine || 2 == examine || 3 == examine) && !Constant.Approval.ADOPT.equals(map.get("status").toString())) {
            setDoiAndCstr(map);
        }


        //Whether to follow
        if (StringUtils.isNotBlank(username)) {
            Query queryF = new Query();
            queryF.addCriteria(Criteria.where("username").is(username));
            queryF.addCriteria(Criteria.where("resourcesId").is(map.get("_id").toString()));
            long followResources = mongoTemplate.count(queryF, FollowResources.class);
            map.put("follow", 0 == followResources ? "no" : "yes");
        } else {
            map.put("follow", "no");
        }

        //Reference Link
        if (Constant.Approval.ADOPT.equals(map.get("status").toString())) {
            map.put("reference_link", createReferenceLink(map));
        }
        if (0 == examine && Constant.Approval.ADOPT.equals(map.get("status").toString())) {
            //Interface Access Count Settings
            Update update = new Update();
            update.set("visitNum", (null == map.get("visitNum") ? 0 : (int) map.get("visitNum")) + 1);
            setAccessRecords(request, id, "visit", query1, update,username,0);
        }

        LinkedHashMap privacyPolicyNap = new LinkedHashMap();
        try {
            ResourcesManage.PrivacyPolicy privacyPolicy = (ResourcesManage.PrivacyPolicy) map.get("privacyPolicy");
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
            privacyPolicyNap = (LinkedHashMap) map.get("privacyPolicy");
        }

        map.put("examine",examine);
        //Privacy Policy Processing
        setPrivacyPolicy(privacyPolicyNap, map, username, roles, request);
        // return vo;
        map.remove("json_id_content");
        map.remove("callbackUrl");
        //Publish time format conversion
        map.put("createTime", DateUtils.getDateString((Date) map.get("createTime")));
        if (null != map.get("approveTime")) {
            map.put("approveTime", DateUtils.getDateString((Date) map.get("approveTime")));
        }

        //Template bound based on resource dataidTemplate bound based on resource data
        Template template = null;
        if ("scidbTemplate".equals(map.get("templateName").toString()) && null != map.get("dataSetSource") && "scidb".equals(map.get("dataSetSource").toString())) {
            final String subjectURL = "/data/DbMetadata_scidb.xml";
            File resourceFile = FileUtils.getResourceFile(subjectURL);
            if (resourceFile.exists()) {
                template = XmlTemplateUtil.getTemplate(resourceFile);
                FileUtils.deleteFile(resourceFile.getPath());
            }
        } else {
            template = settingService.getTemplate(map.get("templateName").toString());
        }
        if (null == template) {
            return ResultUtils.error(504, "RESOURCE_TEMPLATE_DATA_QUERY_ERROR");
        }

        //Template data conversion
        List<Template.Group> groups = template.getGroup();

        for (Template.Group group : groups) {
            List<Template.Resource> resources = group.getResources();
            for (Template.Resource resource : resources) {
                String iri = resource.getIri().substring((resource.getIri().lastIndexOf("/") + 1), resource.getIri().length());
                if (map.containsKey(iri)) {
                    try {
                        String language = resource.getLanguage();
                        if (StringUtils.isNotBlank(language) && "en".equals(language)) {
                            resource.setValue(map.get(iri + "_en"));
                        } else {
                            resource.setValue(map.get(iri));
                        }
                    } catch (Exception e) {
                        return ResultUtils.error("RESOURCE_TEMPLATE_PARSING_ERROR");
                    }
                }
            }
        }
//        Document privacyPolicy = (Document) map.get("privacyPolicy");
//        privacyPolicy.put("type",CommonUtils.getValueByType(privacyPolicy.getString("type"), Constant.LanguageStatus.PRIVACYPOLICY));
        //Approval return content processing
        if (1 == examine && Constant.Approval.PENDING_APPROVAL.equals(map.get("status").toString())) {
            setApprove(map);
        }
        map.put("templateData", groups);
        //Disciplinary processing
        if (map.containsKey("subject") && null != map.get("subject")) {
            if (!map.containsKey("subjectEn") && null == map.get("subjectEn")) {
                List<String> subject = (List<String>) map.get("subject");
                List<String> subjectEn = subjectAreaService.getSubjectByName(subject, Constant.Language.chinese);
                map.put("subjectEn", subjectEn);
                Update update = new Update();
                update.set("subjectEn", subjectEn);
                mongoTemplate.updateFirst(query1, update, ResourcesManage.class);
            }
        }

        return ResultUtils.success(map);
    }

    private void setApprove(Map<String, Object> map) {
        Query queryApprove = new Query();
        queryApprove.addCriteria(Criteria.where("resourcesId").is(map.get("_id").toString()));
        queryApprove.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        Approve approve = mongoTemplate.findOne(queryApprove, Approve.class);
        map.put("approvalAuthor", approve.getClaimAuthor());
    }


    /**
     * doiandcstrand
     *
     * @param map
     */
    private void setDoiAndCstr(Map<String, Object> map) {
        if (map.containsKey("doi") && null != map.get("doi")) {
            String doi = map.get("doi").toString();
            if (StringUtils.isNotBlank(doi) && Constant.APPLY.equals(doi)) {
                //doi = "Automatically allocate after approval";
                doi = "Automatically allocate after approval";
            } else if (StringUtils.isNotBlank(doi)) {
                //To verifydoiTo verify
                Result result = externalInterService.checkDoi(doi);
                if (200 != result.getCode()) {
                    doi = doi + "：" + result.getMessage();
                }
            }
            map.put("doi", doi);
        }
        if (map.containsKey("cstr") && null != map.get("cstr")) {
            String cstr = map.get("cstr").toString();
            if (StringUtils.isNotBlank(cstr) && cstr.contains(Constant.APPLY)) {
                //cstr = "Automatically allocate after approval";
                cstr = "Automatically allocate after approval";
            } else if (StringUtils.isNotBlank(cstr)) {
                if (!cstr.contains(".")) {
                    cstr = cstr + "：" + "Format error";
                } else {
                    Result result = externalInterService.checkCstr(cstr);
                    if (200 != result.getCode()) {
                        cstr = cstr + "：" + result.getMessage();
                    }
                }
            }
            map.put("cstr", cstr);
        }

    }

    /**
     * Generate reference links according to rules
     *
     * @param map
     * @return
     */
    private String createReferenceLink(Map<String, Object> map) {
        //Reference Link
        //Generate Rules
        //author.author（author）.author[author],author.author[author],author.author;author.
        String authorStr = "";
        ArrayList author = (ArrayList) map.get("author");
        if (null != map.get("author") && null != author && author.size() > 0) {
            for (int i = 0; i < author.size(); i++) {
                LinkedHashMap object = (LinkedHashMap) author.get(i);
                authorStr += object.get("name") + ";";
            }
            //Remove the last character;
            authorStr = authorStr.substring(0, authorStr.length() - 1);
        }

        //Propagation time  Propagation time
        String chuanboTime = "";
        if (null != map.get("approveTime")) {
            chuanboTime = "," + DateUtils.getDateString((Date) map.get("approveTime"));
        }

        String cstr = map.containsKey("cstr") ? ".CSTR:" + map.get("cstr").toString() : "";
        String url = map.containsKey("cstr") ? "https://cstr.cn/" + map.get("cstr").toString() : instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + map.get("_id").toString();

        //Communication institutions
        Resources.Organization organization = (Resources.Organization) map.get("organization");
        String organizationName = null != organization && null != organization.getName() ? organization.getName() : "";
        String reference_link = authorStr + "." + map.get("name").toString() + "(" + map.get("version").toString() + ")." + organizationName + "[Create an organization]," + DateUtils.getDateString((Date) map.get("createTime")) + "." +
                organizationName + "[Communication institutions]" + chuanboTime + cstr + ";" + url;
        return reference_link;
    }


    /**
     * Set the file of the dataset according to privacy policy
     *
     * @param privacyPolicy
     * @param map
     * @param username
     * @param request
     */
    private void setPrivacyPolicy(LinkedHashMap privacyPolicy, Map<String, Object> map, String username, List<String> roles, HttpServletRequest request) {
        map.put("showFile", false);
        String type = privacyPolicy.get("type").toString();
        //Administrator Direct Release
        if (roles.size() > 0 && roles.contains(Constant.ADMIN)) {
            map.put("showFile", true);
            return;
        }
        //Expert evaluation for direct release
        String cookie = CommonUtils.getCookie(request, map.get("_id").toString());
        if (StringUtils.isNotBlank(cookie) && RSAEncrypt.decrypt(cookie).equals(map.get("_id").toString())) {
            map.put("showFile", true);
            return;
        }
        //Approval for direct release
        if (roles.size() > 0 && (1 == (Integer) map.get("examine") || 2 == (Integer) map.get("examine")) && roles.contains(Constant.ROLE_APPROVE)) {
            //Reviewer approval available
            map.put("showFile", true);
            return;
        }

        //Resource Restricted Types
        if (Constant.PrivacyPolicy.CONDITION.equals(type)) {
            Criteria criteriaApprove = Criteria.where("applyEmail").is(username);
            criteriaApprove.and("resourcesId").is(map.get("_id"));
            Query queryApprove = new Query();
            queryApprove.addCriteria(criteriaApprove);
            queryApprove.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.ADOPT));
            //Query approved application access records
            ResourceAccess approve = mongoTemplate.findOne(queryApprove, ResourceAccess.class);
            //Query access records for applications pending approval
            Query queryApprove1 = new Query();
            queryApprove1.addCriteria(criteriaApprove);
            queryApprove1.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
            ResourceAccess approve1 = mongoTemplate.findOne(queryApprove1, ResourceAccess.class);
            if (null != approve && Constant.Approval.ADOPT.equals(approve.getApprovalStatus())) {
                //Access period verification
                if ("range".equals(approve.getAccessPeriod()) && StringUtils.isNotBlank(approve.getStartTime()) && StringUtils.isNotBlank(approve.getEndTime())) {
                    boolean between = DateUtils.isBetween(DateUtils.getLocalDateTimeByString2(approve.getStartTime()), DateUtils.getLocalDateTimeByString2(approve.getEndTime()));
                    if (!between) {
                        //Request access time expiration prompt
                        map.put(Constant.PrivacyPolicy.CONDITION + "TimeExpired", true);
                        boolean effectiveDate = DateUtils.isEffectiveDate(new Date(), approve.getEndTime());
                        //After the visit time exceeds  After the visit time exceeds
                        if (effectiveDate) {
                            Update updateResourceAccess = new Update();
                            Query queryResourceAccess = new Query();
                            queryResourceAccess.addCriteria(Criteria.where("_id").is(approve.getId()));
                            updateResourceAccess.set("approvalStatus", Constant.Approval.OFFLINE);
                            mongoTemplate.updateFirst(queryResourceAccess, updateResourceAccess, ResourceAccess.class);
                        }
                    } else {
                        map.put("showFile", true);
                    }
                } else {
                    map.put("showFile", true);
                }
                //Explain that the application for access has been approved Explain that the application for access has been approved
            } else if (null != approve1 && Constant.Approval.PENDING_APPROVAL.equals(approve1.getApprovalStatus())) {
                //Status Pending Approval Status Pending Approval
                map.put(Constant.PrivacyPolicy.CONDITION + Constant.Approval.PENDING_APPROVAL, true);
                //  }  else if (roles.size() > 0 && roles.contains(Constant.ADMIN) && 1 == examine || 2 == examine || 3 == examine) {
            }
        } else if (Constant.PrivacyPolicy.PROTECT.equals(type)) {
            //Protection period Protection period
            String protectDate = privacyPolicy.get("openDate").toString();
            boolean effectiveDate = DateUtils.isEffectiveDate(new Date(), protectDate);
            if (effectiveDate) {
                //Only approved items can be changed
                if (Constant.Approval.ADOPT.equals(map.get("status").toString())) {
                    Update update = new Update();
                    update.set("privacyPolicy.type", Constant.PrivacyPolicy.OPEN);
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(map.get("_id")));
                    mongoTemplate.updateFirst(query, update, ResourcesManage.class);
                }
                //Explain that the application for access has been approved Explain that the application for access has been approved
                map.put("showFile", true);
            }
        } else if (Constant.PrivacyPolicy.OPEN.equals(type)) {
            //Opening period  Opening period
            //Here's what's given Here's what's given
            map.put("showFile", true);
        }
    }


    @Override
    public Result emailShare(String token, String resourcesId, String email) {
        if (StringUtils.isBlank(email)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        if (!CommonUtils.isEmail(email)) {
            return ResultUtils.error("EMAIL_INCORRECT");
        }

        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();

        Criteria criteria = Criteria.where("_id").is(resourcesId);
        Query query = new Query();
        query.addCriteria(criteria);
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error(504, "DATA_QUERY_EXCEPTION");
        }

        if (!Constant.Approval.ADOPT.equals(resourcesManage.getStatus())) {
            return ResultUtils.error("RESOURCE_APPROVED_SHARE");
        }

        Map<String, Object> attachment = new HashMap<>();
        attachment.put("name", consumerDO.getName() + "(" + consumerDO.getEmailAccounts() + ")");
        attachment.put("resourceName", resourcesManage.getName());
        attachment.put("url", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId());
        attachment.put("toEmail", email);
        ToEmail toEmail = new ToEmail();
        toEmail.setTos(new String[]{email});
        asyncDeal.send(toEmail, attachment, EmailModel.EMAIL_RESOURCES_SHARE());
        return ResultUtils.success("RESOURCE_EMAIL_SHARE");

    }

    @Override
    public Result setFollow(String token, String resourcesId, String value) {
        if (StringUtils.isBlank(value) || StringUtils.isBlank(resourcesId)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Query queryF = new Query();
        queryF.addCriteria(Criteria.where("_id").is(resourcesId));
        ResourcesManage resourcesManage = mongoTemplate.findOne(queryF, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if (!Constant.Approval.ADOPT.equals(resourcesManage.getStatus())) {
            return ResultUtils.error("RESOURCE_APPROVED_FOLLOW");
        }
        Update update = new Update();
        String username = tokenCache.getIfPresent(token);
        if ("yes".equals(value)) {
            FollowResources followResources = new FollowResources();
            followResources.setCreateTime(LocalDateTime.now());
            followResources.setUsername(username);
            followResources.setResourcesId(resourcesId);
            followResources.setResourcesName(resourcesManage.getName());
            followResources.setResourceType(resourcesManage.getResourceType());
            mongoTemplate.save(followResources);

            //Update the field values of resources after following Update the field values of resources after following+1
            update.set("followNum", resourcesManage.getFollowNum() + 1);
            mongoTemplate.upsert(queryF, update, ResourcesManage.class);

        } else if ("no".equals(value)) {
            Query query = new Query();
            query.addCriteria(Criteria.where("username").is(username));
            query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
            mongoTemplate.remove(query, FollowResources.class);
            //Update the field values of resources after following Update the field values of resources after following-1
            update.set("followNum", (0 == resourcesManage.getFollowNum() ? 1 : resourcesManage.getFollowNum()) - 1);
            mongoTemplate.upsert(queryF, update, ResourcesManage.class);
        } else {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        return ResultUtils.success("yes".equals(value) ? "FOLLOW_SUCCESS" : "UNFOLLOW_SUCCESS");
    }

    @Override
    public PageHelper getMyFollow(String token, String resourcesType, Integer pageOffset, Integer pageSize, String sort) {
        String username = tokenCache.getIfPresent(token);
        //Set query criteria
        Query queryResources = new Query();
        queryResources.addCriteria(Criteria.where("username").is(username));
        //Total number of queries
        long count = mongoTemplate.count(queryResources, FollowResources.class);
        if (StringUtils.isNotBlank(resourcesType)) {
            queryResources.addCriteria(Criteria.where("resourceType").is(resourcesType));
        }
        if (StringUtils.isNotBlank(sort) && sort.contains("_")) {
            String[] s = sort.split("_");
            if (s[1].equals("asc")) {
                queryResources.with(Sort.by(Sort.Direction.ASC, s[0]));
            } else if (s[1].equals("desc")) {
                queryResources.with(Sort.by(Sort.Direction.DESC, s[0]));
            }
        }

        List<ResourcesListManage> listManages = new ArrayList<>();

        mongoUtil.start(pageOffset, pageSize, queryResources);
        List<FollowResources> followResourcesList = mongoTemplate.find(queryResources, FollowResources.class);
        if (null != followResourcesList && followResourcesList.size() > 0) {
            for (FollowResources follow : followResourcesList) {
                Query resourcesQuery = new Query();
                resourcesQuery.addCriteria(Criteria.where("_id").is(follow.getResourcesId()));
                //according toidaccording to
                ResourcesListManage resourcesManage = mongoTemplate.findOne(resourcesQuery, ResourcesListManage.class, COLLECTION_NAME);
                if (null != resourcesManage) {
                    //Requirement modification Requirement modification Requirement modification
                    if (StringUtils.isBlank(follow.getResourceType())) {
                        Update update = new Update();
                        Query queryUpdate = new Query();
                        queryUpdate.addCriteria(Criteria.where("_id").is(follow.getId()));
                        update.set("resourceType", resourcesManage.getResourceType());
                        mongoTemplate.upsert(queryUpdate, update, FollowResources.class);
                    }
                    String lang = tokenCache.getIfPresent("lang");
                    if (Constant.Language.english.equals(lang)) {
//                    if (StringUtils.isNotBlank(resourcesManage.getName_en())) {
//                        resourcesManage.setName(resourcesManage.getName_en());
//                    }
//                    if (null != resourcesManage.getKeywords_en()) {
//                        resourcesManage.setKeywords(resourcesManage.getKeywords_en());
//                    }
                        if (null != resourcesManage.getAuthor() && resourcesManage.getAuthor().size() > 0) {
                            JSONArray author = resourcesManage.getAuthor();
                            for (int i = 0; i < author.size(); i++) {
                                net.sf.json.JSONObject o = (net.sf.json.JSONObject) author.get(i);
                                if (null != o.get("en_name") && StringUtils.isNotBlank(o.getString("en_name"))) {
                                    o.put("name", o.getString("en_name"));
                                }
                            }
                        }
                    }
                    listManages.add(resourcesManage);
                }
            }
        }
        //First, obtain the types of all data resources
        Map<String, String> map = new HashMap<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("username").is(username));
        List<FollowResources> followResources = mongoTemplate.find(query, FollowResources.class);
        if (null != followResources && followResources.size() > 0) {
            Map<String, List<FollowResources>> collect = followResources.stream().collect(Collectors.groupingBy(FollowResources::getResourceType));
            if (null != collect && collect.size() > 0) {
                for (Map.Entry<String, List<FollowResources>> entry : collect.entrySet()) {
                    map.put(entry.getKey() + "-" + entry.getValue().size(), "");
                }
                CommonUtils.addResourceType(map, Constant.LanguageStatus.RESOURCE_TYPES);
            }
        }

        return mongoUtil.pageHelper(count, listManages, map);
    }

    @Override
    public PageHelper getMyRelease(String token, String status, String resourcesType, Integer pageOffset, Integer pageSize, String sort) {
        String username = tokenCache.getIfPresent(token);
        //First, obtain the status types of all data resources
        Map<String, Object> map = new HashMap<>();
        Map<String, String> mapType = new HashMap<>();
        Map<String, String> mapStatus = new HashMap<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("publish.email").is(username));
        List<ResourcesManage> resourcesManage = mongoTemplate.find(query, ResourcesManage.class);
        if (null != resourcesManage && resourcesManage.size() > 0) {
            Map<String, List<ResourcesManage>> collect = resourcesManage.stream().collect(Collectors.groupingBy(ResourcesManage::getResourceType));
            if (null != collect && collect.size() > 0) {
                for (Map.Entry<String, List<ResourcesManage>> entry : collect.entrySet()) {
                    mapType.put(entry.getKey() + "-" + entry.getValue().size(), "");
                }
                CommonUtils.addResourceType(mapType, Constant.LanguageStatus.RESOURCE_TYPES);
            }
            //Completion status
            Map<String, List<ResourcesManage>> collectStatus = resourcesManage.stream().collect(Collectors.groupingBy(ResourcesManage::getStatus));
            if (null != collectStatus && collectStatus.size() > 0) {
                for (Map.Entry<String, List<ResourcesManage>> entry : collectStatus.entrySet()) {
                    mapStatus.put(entry.getKey() + "-" + entry.getValue().size(), "");
                }
                CommonUtils.addResourceType(mapStatus, Constant.LanguageStatus.STATUS);
            }

            map.put("mapType", mapType);
            map.put("mapStatus", mapStatus);
        }

        //Set query criteria
        Query queryResources = new Query();
        queryResources.addCriteria(Criteria.where("publish.email").is(username));
        long count = mongoTemplate.count(queryResources, ResourcesManage.class);
        if (StringUtils.isNotBlank(status)) {
            queryResources.addCriteria(Criteria.where("status").is(status));
        }
        if (StringUtils.isNotBlank(resourcesType)) {
            queryResources.addCriteria(Criteria.where("resourceType").is(resourcesType));
        }
        if (StringUtils.isNotBlank(sort) && sort.contains("_")) {
            String[] s = sort.split("_");
            if (s[1].equals("asc")) {
                query.with(Sort.by(Sort.Direction.ASC, s[0]));
            } else if (s[1].equals("desc")) {
                query.with(Sort.by(Sort.Direction.DESC, s[0]));
            }
        }
        mongoUtil.start(pageOffset, pageSize, queryResources);
        List<ResourcesListManage> resourcesListManages = mongoTemplate.find(queryResources, ResourcesListManage.class, COLLECTION_NAME);
        return mongoUtil.pageHelper(count, resourcesListManages, map);
    }


    @Override
    public void resourceUpdateByAdmin(String token, ResourcesManageUpdate resourcesManageUpdate) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        List<String> roles = consumerDO.getRoles();
        Assert.isTrue(null != roles, I18nUtil.get("LOGIN_EXCEPTION"));
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(resourcesManageUpdate.getId()));
        query2.addCriteria(Criteria.where("status").is(Constant.Approval.PENDING_APPROVAL));

        Map dataMap = mongoTemplate.findOne(query2, Map.class, Constant.RESOURCE_COLLECTION_NAME);

        Assert.isTrue(dataMap.size() > 0, I18nUtil.get("PARAMETER_ERROR"));

        //Prohibit modification after approval
        Assert.isTrue(Constant.Approval.PENDING_APPROVAL.equals(dataMap.get("status").toString()), "This resource has already been approved,This resource has already been approved");

        List<ResourcesManageUpdate.Resources> resourcesList = resourcesManageUpdate.getResources();
        for (ResourcesManageUpdate.Resources resources : resourcesList) {
            List<Template.Resource> resources1 = resources.getResources();
            for (Template.Resource resource : resources1) {
                if (null == resource.getValue()) {
                    continue;
                }

                String iri = resource.getIri().substring((resource.getIri().lastIndexOf("/") + 1), resource.getIri().length());
                String language = resource.getLanguage();
                switch (resource.getType()) {
                    //Processing of Text Fields
                    case "text":
                    case "textarea":
                    case "date":
                    case "radio":
                    case "select":
                        if (!"en".equals(language) && dataMap.containsKey(iri) && null != dataMap.get(iri) && !dataMap.get(iri).toString().equals(resource.getValue())) {
                            dataMap.put(iri, resource.getValue());
                        }
                        if ("en".equals(language) && dataMap.containsKey(iri + "_en")
                                && null != dataMap.get(iri + "_en") && !dataMap.get(iri).toString().equals(resource.getValue())) {
                            dataMap.put(iri + "_en", resource.getValue());
                        }
                        break;
                    case "selectMany":
                    case "textTabMany":
                    case "textMany":
                    case "subject":
                        List<String> value = (List) resource.getValue();
                        if (null != value && value.size() > 0) {
                            dataMap.put(iri, value);
                        }
                        break;
                    case "privacyPolicy":
                        LinkedHashMap privacyPolicyMap = (LinkedHashMap) resource.getValue();
                        dataMap.put("privacyPolicy", privacyPolicyMap);
                        break;
                }
            }
            if (dataMap.containsKey("doi") && null != dataMap.get("doi") && dataMap.get("doi").toString().equals("DOI registration is not open")) {
                dataMap.put("doi", Constant.APPLY);
            }
            if (dataMap.containsKey("cstr") && null != dataMap.get("cstr") && dataMap.get("cstr").toString().equals("Automatically allocate after approval")) {
                dataMap.put("cstr", Constant.APPLY);
            }
        }
        mongoTemplate.save(dataMap, Constant.RESOURCE_COLLECTION_NAME);
        log.info("Successfully edited and modified the data resource before approval,Successfully edited and modified the data resource before approval!");
    }


    @Override
    public List<Template.Group> getTemplateByResourcesId(String token, String resourcesId) {

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(resourcesId));
        Map dataMap = mongoTemplate.findOne(query2, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        Assert.isTrue(dataMap.size() > 0, I18nUtil.get("DATA_QUERY_EXCEPTION"));
        setDoiAndCstr(dataMap);
        //Template bound based on resource dataidTemplate bound based on resource data
        Template template = settingService.getTemplate(dataMap.get("templateName").toString());
        //Template data conversion
        List<Template.Group> groups = template.getGroup();
        //Template and data processing  Template and data processing
        for (Template.Group group : groups) {
            List<Template.Resource> resources = group.getResources();
            for (Template.Resource resource : resources) {
                String iri = resource.getIri().substring((resource.getIri().lastIndexOf("/") + 1), resource.getIri().length());
                String language = resource.getLanguage();
                if (StringUtils.isNotBlank(language) && "en".equals(language) && dataMap.containsKey(iri + "_en") && null != dataMap.get(iri)) {
                    resource.setValue(dataMap.get(iri + "_en"));
                } else if (dataMap.containsKey(iri) && null != dataMap.get(iri)) {
                    resource.setValue(dataMap.get(iri));
                }
            }
        }
        return groups;
    }


    @Override
    public void createFtpUser(String ftpHost, String resourcesId, String auth, Map<String, String> map) {
        String username = CommonUtils.getCode(6);
        String password = CommonUtils.getCode(6);
        FtpUser ftpUser = new FtpUser();
        ftpUser.setCreateTime(LocalDateTime.now());
        if (map.containsKey("realUsers") && null != map.get("realUsers")) {
            ftpUser.setRealUsers(map.get("realUsers"));
        }
        ftpUser.setHomedirectory(instdbUrl.getResourcesFilePath() + resourcesId);
        ftpUser.setPassword(password);
        ftpUser.setUsername(username);
        ftpUser.setResourcesId(resourcesId);
        ftpUser.setAuth(auth);
        mongoTemplate.save(ftpUser);
        map.put("username", username);
        map.put("ftpUrl", ftpHost);
        //  map.put("ftpUrl", "ftp://127.0.0.1:2121");
        map.put("password", password);
    }

    @Override
    public PageHelper getResourceFileTree(String token, String resourcesId, int id, String fileName, Integer pageOffset, Integer pageSize, String sort, HttpServletRequest request) {

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(resourcesId));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query2, ResourcesManage.class);
        if (!resourcesManage.getDownloadFileFlag().equals(Constant.VERSION_FLAG)) {
            List<ResourceFileTree> list = new ArrayList<>();
            mongoUtil.start(pageOffset, pageSize, query2);
            return mongoUtil.pageHelper(0L, list);
        }

        boolean b = checkDownload(token, resourcesManage, request);
        if (!b) {
            if (Constant.PrivacyPolicy.OPEN.equals(resourcesManage.getPrivacyPolicy().getType())) {
            } else {
                List<ResourceFileTree> list = new ArrayList<>();
                mongoUtil.start(pageOffset, pageSize, query2);
                return mongoUtil.pageHelper(0L, list);
            }
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        if (StringUtils.isNotBlank(fileName)) {
            query.addCriteria(Criteria.where("fileName").regex(fileName));
        }

        if (StringUtils.isNotBlank(sort) && sort.contains("_")) {
            String[] s = sort.split("_");
            if (s[1].equals("asc")) {
                query.with(Sort.by(Sort.Direction.ASC, s[0]));
            } else if (s[1].equals("desc")) {
                query.with(Sort.by(Sort.Direction.DESC, s[0]));
            }
        }

        if (Constant.PrivacyPolicy.CONDITION.equals(resourcesManage.getPrivacyPolicy().getType())) {
            Token token1 = jwtTokenUtils.getToken(token);
            if (null != token1 && StringUtils.isNotBlank(token1.getEmailAccounts())) {
                Query queryResourceAccess = new Query();
                queryResourceAccess.addCriteria(Criteria.where("resourcesId").is(resourcesId));
                queryResourceAccess.addCriteria(Criteria.where("applyEmail").is(token1.getEmailAccounts()));
                queryResourceAccess.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.ADOPT));
                ResourceAccess one = mongoTemplate.findOne(queryResourceAccess, ResourceAccess.class);

                if (null != one && "range".equals(one.getAccessData()) && null != one.getFilesId() && one.getFilesId().size() > 0) {
                    query.addCriteria(Criteria.where("_id").in(one.getFilesId()));
                }else {
                    query.addCriteria(Criteria.where("pid").is(id));
                }
            }else if(b){
                query.addCriteria(Criteria.where("pid").is(id));
                //bbytrueby by
            } else{
                mongoUtil.start(pageOffset, pageSize, query2);
                return mongoUtil.pageHelper(0L, new ArrayList<>(), "Unrecognized identity information");
            }
        }else  {
            query.addCriteria(Criteria.where("pid").is(id));
        }
        long count = mongoTemplate.count(query, ResourceFileTree.class);
        mongoUtil.start(pageOffset, pageSize, query);
        List<ResourceFileTree> list = mongoTemplate.find(query, ResourceFileTree.class);
        for (ResourceFileTree f : list) {
            f.setFilePath(null);
        }
        return mongoUtil.pageHelper(count, list);
    }

    @Override
    public PageHelper getResourceFiles(String resourcesId, int pid, String fileName, Integer pageOffset, Integer pageSize) {

        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));

        if (StringUtils.isNotBlank(fileName) && 0 < pid) {
            query.addCriteria(Criteria.where("fileName").regex(fileName));
            query.addCriteria(Criteria.where("pid").is(pid));
        } else if (StringUtils.isNotBlank(fileName)) {
            query.addCriteria(Criteria.where("fileName").regex(fileName));
        } else if (0 == pid) {
            query.addCriteria(Criteria.where("pid").is(0));
        } else {
            query.addCriteria(Criteria.where("pid").is(pid));
        }

        long count = mongoTemplate.count(query, ResourceFileTree.class);
        mongoUtil.start(pageOffset, pageSize, query);
        List<ResourceFileTree> list = mongoTemplate.find(query, ResourceFileTree.class);
        return mongoUtil.pageHelper(count, list);
    }

    @Override
    public List<Approve> getApproveLog(String resourcesId) {
        List<Approve> list = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        query.addCriteria(Criteria.where("approvalAuthor").ne("").ne(null));
        query.addCriteria(Criteria.where("approvalStatus").nin(Constant.Approval.PENDING_APPROVAL));
        List<Approve> approves = mongoTemplate.find(query, Approve.class);
        //There is only one default approval record  There is only one default approval record There is only one default approval record
        if (null != approves && approves.size() > 0) {
            for (Approve approve : approves) {
                if (Constant.Comment.RESOURCE_PUBLISHING.equals(approve.getType()) && !approve.getApprovalStatus().equals(Constant.Approval.PENDING_APPROVAL)) {
                    //Add evaluation records
                    Query queryEvaluat = new Query();
                    queryEvaluat.addCriteria(Criteria.where("approvalId").is(approve.getId()));
                    queryEvaluat.addCriteria(Criteria.where("resourcesId").is(approve.getResourcesId()));
                    List<ResourcesReview> review = mongoTemplate.find(queryEvaluat, ResourcesReview.class);
                    approve.setResourcesReviews(review);
                }
            }
            return approves;
        }
        return list;
    }

    @Override
    public Result getResourceRecommend(String id) {
        List<Map<String, Object>> list = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        Query queryResource = new Query();
        queryResource.skip(0);
        queryResource.limit(6);
        queryResource.addCriteria(Criteria.where("_id").nin(id));
        queryResource.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));

        boolean flag = false;
        if (null != resourcesManage.getSubject() && resourcesManage.getSubject().size() > 0) {
            queryResource.addCriteria(Criteria.where("subject").in(resourcesManage.getSubject()));
            flag = true;
        } else if (null != resourcesManage.getKeywords() && resourcesManage.getKeywords().size() > 0) {
            queryResource.addCriteria(Criteria.where("keywords").in(resourcesManage.getKeywords()));
            flag = true;
        }
        List<ResourcesManage> resourcesManages = new ArrayList<>();
        if (flag) {
            resourcesManages = mongoTemplate.find(queryResource, ResourcesManage.class);
        }
        if (null != resourcesManages && resourcesManages.size() > 0) {
            for (ResourcesManage data : resourcesManages) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", data.getId());
                map.put("name", data.getName());
                map.put("name_en", data.getName_en());
                map.put("keywords", data.getKeywords());
                map.put("keywords_en", data.getKeywords_en());
                list.add(map);
            }
        }
        return ResultUtils.success(list);
    }

   private void setQueryResources(String displayStatus, String name, String resourceType,
                                  String privacyPolicy,
                                  String startDate, String endDate, String publishName, String version, Query query,List<String> listStatus,String identifier,String sort,String templateName){

       if (StringUtils.isNotBlank(version)) {
           query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
           query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
       } else {
           if (StringUtils.isNotBlank(displayStatus)) {
               query.addCriteria(Criteria.where("status").is(displayStatus));
           } else {
               query.addCriteria(Criteria.where("status").in(listStatus));
           }
       }

       if (StringUtils.isNotBlank(resourceType)) {
           query.addCriteria(Criteria.where("resourceType").is(resourceType));
       }

       if (StringUtils.isNotBlank(identifier)) {
           query.addCriteria(new Criteria().orOperator(
                   Criteria.where("project").elemMatch(Criteria.where("identifier").is(identifier)), Criteria.where("fundingReferences").elemMatch(Criteria.where("identifier").is(identifier))
                   , Criteria.where("project").elemMatch(Criteria.where("name").is(identifier)), Criteria.where("fundingReferences").elemMatch(Criteria.where("name").is(identifier))));
       }

       if (StringUtils.isNotBlank(privacyPolicy)) {
           query.addCriteria(Criteria.where("privacyPolicy.type").is(privacyPolicy));
       }

       if (StringUtils.isNotBlank(templateName)) {
           query.addCriteria(Criteria.where("templateName").is(templateName));
       }


       if (StringUtils.isNotBlank(name)) {
           Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(name) + ".*$", Pattern.CASE_INSENSITIVE);
           query.addCriteria(Criteria.where("name").regex(pattern));
       }
       if (StringUtils.isNotBlank(publishName)) {
           query.addCriteria(new Criteria().orOperator(
                   Criteria.where("publish.name").regex(publishName), Criteria.where("publish.email").is(publishName)));
       }

       if (StringUtils.isNotBlank(sort) && sort.contains("&")) {
           String[] s = sort.split("&");
           if (s[0].equals("asc")) {
               query.with(Sort.by(Sort.Direction.ASC, s[1]));
           } else if (s[0].equals("desc")) {
               query.with(Sort.by(Sort.Direction.DESC, s[1]));
           }
       }else {
           query.with(Sort.by(Sort.Direction.DESC, "approveTime"));
       }

       if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
           Criteria criteria = new Criteria();
           query.addCriteria(criteria.andOperator(Criteria.where("approveTime").gte(DateUtils.getLocalDateTimeByString2(startDate)),
                   Criteria.where("approveTime").lte(DateUtils.getLocalDateTimeByString2(endDate))));
       } else if (StringUtils.isNotBlank(startDate)) {
           query.addCriteria(Criteria.where("approveTime").gte(DateUtils.getLocalDateTimeByString2(startDate)));
       } else if (StringUtils.isNotBlank(endDate)) {
           query.addCriteria(Criteria.where("approveTime").lte(DateUtils.getLocalDateTimeByString2(endDate)));
       }

   }

    @Override
    public PageHelper resourcesListManage(String displayStatus, String name, String resourceType,
                                          String privacyPolicy, Integer pageOffset, Integer pageSize,
                                          String startDate, String endDate, String publishName, String version,String identifier,String sort,String templateName) {
        Map<String, Object> map = new HashMap<>();
        Query query = new Query();
        List<String> listStatus = new ArrayList<>();
        listStatus.add(Constant.Approval.ADOPT);
        listStatus.add(Constant.Approval.OFFLINE);

        setQueryResources(displayStatus, name, resourceType,
                privacyPolicy,
                startDate, endDate, publishName, version, query, listStatus,identifier,sort,templateName);


        Query queryCurrent = new Query();
        queryCurrent.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        queryCurrent.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        long currentCount = mongoTemplate.count(queryCurrent, ResourcesManage.class);

        Query queryHistory = new Query();
        queryHistory.addCriteria(Criteria.where("status").in(listStatus));
        //   queryHistory.addCriteria(Criteria.where("versionFlag").ne(Constant.VERSION_FLAG));
        long historytCount = mongoTemplate.count(queryHistory, ResourcesManage.class);
        map.put("currentCount", currentCount);
        map.put("historytCount", historytCount);



        long count = mongoTemplate.count(query, ResourcesManage.class);
        mongoUtil.start(pageOffset, pageSize, query);
        List<ResourcesManage> list = mongoTemplate.find(query, ResourcesManage.class, COLLECTION_NAME);
        return mongoUtil.pageHelper(count, list, map);
    }


    @Override
    public Result upAndDown(String token, String id, String type) {

        if (StringUtils.isBlank(id)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        if (!type.equals(Constant.Approval.ADOPT) && !type.equals(Constant.Approval.OFFLINE)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class, COLLECTION_NAME);
        if (null == resourcesManage) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if (resourcesManage.getStatus().equals(type)) {
            return ResultUtils.error(type.equals(Constant.Approval.ADOPT) ? "RESOURCE_ALREADY_ONLINE" : "RESOURCE_ALREADY_OFFLINE");
        }


        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        String resourcesId = resourcesManage.getResourcesId();
        Query queryResources = new Query(Criteria.where("resourcesId").is(resourcesId)).addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT)).addCriteria(Criteria.where("_id").ne(id));
        List<ResourcesManage> resourcesManages = mongoTemplate.find(queryResources, ResourcesManage.class);
        if (null != resourcesManages && resourcesManages.size() > 0) {
            resourcesManages = resourcesManages.stream().sorted(Comparator.comparing(ResourcesManage::getVersion).reversed()).collect(Collectors.toList());
            ResourcesManage resourcesManage1 = resourcesManages.get(0);
            if (type.equals(Constant.Approval.OFFLINE)) {
                if (!resourcesManage1.getVersionFlag().equals(Constant.VERSION_FLAG)) {
                    return ResultUtils.error(501, "Confirm delisting" + resourcesManage.getVersion() + "Confirm delisting？Confirm delisting " + resourcesManage1.getVersion() + "Confirm delisting");
                } else {
                    return upAndDownBefore(token, id, type);
                }
            } else if (type.equals(Constant.Approval.ADOPT)) {
                return ResultUtils.error(501, "UPANDDOWN_MSG1");
            }

        } else {
            if (type.equals(Constant.Approval.OFFLINE)) {
                return ResultUtils.error(501, "UPANDDOWN_MSG");
            } else if (type.equals(Constant.Approval.ADOPT)) {
                return upAndDownBefore(token, id, type);
            }
        }
        return ResultUtils.success();
    }

    @Override
    public PageHelper getResourcesHistory(String token, Integer pageOffset, Integer pageSize) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        Assert.isTrue(null != consumerDO, I18nUtil.get("LOGIN_EXCEPTION"));
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.addCriteria(Criteria.where("username").is(consumerDO.getEmailAccounts()));
        long count = mongoTemplate.count(query, ResourcesHistory.class);
        mongoUtil.start(pageOffset, pageSize, query);

        List<ResourcesHistory> list = mongoTemplate.find(query, ResourcesHistory.class);
        List<ResourcesListManage> manageList = new ArrayList<>();
        if (null != list && list.size() > 0) {
            for (ResourcesHistory history : list) {
                Query resourcesQuery = new Query();
                resourcesQuery.addCriteria(Criteria.where("_id").is(history.getResourcesId()));
                ResourcesListManage resourcesManage = mongoTemplate.findOne(resourcesQuery, ResourcesListManage.class, COLLECTION_NAME);
                manageList.add(resourcesManage);
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //Get to today's date  Get to today's date
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(new Date());
        cal1.add(Calendar.DATE, -30);
        String imptimeEnd = sdf.format(cal1.getTime());
        Query resourcesQuery = new Query();
        resourcesQuery.addCriteria(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(imptimeEnd)));
        mongoTemplate.remove(resourcesQuery, ResourcesHistory.class);

        return mongoUtil.pageHelper(count, null, manageList);
    }

    @Override
    public Result getStructured(String id) {


        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(id));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query2, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.success();
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("resourceId").is(id));
        List<Map> result = mongoTemplate.find(query, Map.class, Constant.TABLE_NAME);

        if (!resourcesManage.getDownloadFileFlag().equals(Constant.VERSION_FLAG)) {
            return ResultUtils.success();
        }

        if (null != result && result.size() > 0) {
            for (Map map : result) {
                if (map.containsKey("filepath") && null != map.get("filepath")) {
                    map.put("filepath", "");
                }
            }
        }
        return ResultUtils.success(result);
    }

    @Override
    public PageHelper getStructuredData(String id, String name, String content, Integer pageOffset, Integer pageSize) {
        Query query = new Query();
        query.addCriteria(Criteria.where("resourceId").is(id));
        query.addCriteria(Criteria.where("tableName").is(name));
        Map structured = mongoTemplate.findOne(query, Map.class, Constant.TABLE_NAME);
        if (null != structured) {
            List<String> heads = (List<String>) structured.get("head");
            if (null != heads && heads.size() > 0) {
                Query queryData = new Query();
                if (StringUtils.isNotBlank(content)) {
                    List<Criteria> orCriterias = new ArrayList<>();
                    heads.forEach((k) -> {
                        if (k != null) {
                            orCriterias.add(Criteria.where(k).regex(content));
                        }
                    });
                    Criteria criteria = new Criteria();
                    criteria.orOperator(orCriterias.toArray(new Criteria[0]));
                    queryData.addCriteria(criteria);
                }

                long count = mongoTemplate.count(queryData, name);
                mongoUtil.start(pageOffset, pageSize, queryData);
                List<Map> list = mongoTemplate.find(queryData, Map.class, name);
                return mongoUtil.pageHelper(count, null, list);
            }
        }
        List<Map> list = new ArrayList<>();
        return mongoUtil.pageHelper(0, null, list);
    }

    @Override
    public void structuredDownloadFile(String token, String resourcesId, String tableName, HttpServletRequest request, HttpServletResponse response) {

        Optional<ResourcesManage> byId = resourcesManageRepository.findById(resourcesId);
        Assert.isTrue(byId.isPresent(), I18nUtil.get("DATA_QUERY_EXCEPTION"));

        String username = jwtTokenUtils.getUsernameFromToken(token);
        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
        //Determine whether to enable matching email suffixes before downloading
        if (StringUtils.isNotBlank(basicConfig.getEmailDownloadPower()) && Constant.Approval.YES.equals(basicConfig.getEmailDownloadPower()) && StringUtils.isNotBlank(basicConfig.getEmailSuffixDownload())) {
            int code = checkFtpEmailSuffix(basicConfig.getEmailSuffixDownload(), username);
            if (500 == code) {
                log.error(I18nUtil.get("FTP_DOWNLOAD_EMAILSUFFIX"));
                throw new RuntimeException(I18nUtil.get("FTP_DOWNLOAD_EMAILSUFFIX"));
            }
        }

        ResourcesManage resourcesManage = byId.get();
        boolean b = checkDownload(token, resourcesManage, request);
        if (!b) {
            throw new RuntimeException(I18nUtil.get("PERMISSION_DENIED"));
        }

        //Interface Download Count Settings
        Query query = new Query(Criteria.where("_id").is(byId.get().getId()));
        if (Constant.Approval.ADOPT.equals(byId.get().getStatus())) {
            Update update = new Update();
            update.inc("downloadNum", 1);
            mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), ResourcesManage.class);
        }
        Query queryStructured = new Query();
        queryStructured.addCriteria(Criteria.where("tableName").is(tableName));
        queryStructured.addCriteria(Criteria.where("resourceId").is(resourcesId));
        Map structured = mongoTemplate.findOne(queryStructured, Map.class, Constant.TABLE_NAME);

        if (null != structured && structured.containsKey("filepath") && null != structured.get("filepath")) {
            FileUtils.downloadFile(structured.get("filepath").toString(), response,"");
        }
    }

    @Override
    public Result getStatisticsResourcesMonth(String resourcesId) {
        Map<String, Object> map = new HashMap<>();
        //Get every month1Get every month
        LocalDate localDate = LocalDate.now().withDayOfMonth(1);
        String[] months = new String[6];
        //applyset，apply；
        Set<String> monthSet = new TreeSet<>();
        //Obtain the last day of each month within the past year；
        for (int i = 0; i < 6; i++) {
            //Last day of each month；
            LocalDate lastDayOfMonth = localDate.minusMonths(i).minusDays(0);
            //Format Last Day；"yyyy-MM-dd"
            months[i] = lastDayOfMonth.format(DateTimeFormatter.ISO_LOCAL_DATE);
            //Format Adult Month： "yyyy-MM",Format Adult Month Format Adult Month；
            monthSet.add(lastDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        map.put("time", monthSet);

        List<Integer> downloadNumList = new ArrayList<>();
        List<Integer> visitNumList = new ArrayList<>();

        for (String month : monthSet) {
            Query query = new Query();
            query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
            query.addCriteria(Criteria.where("createTime").regex(month));
            List<AccessRecords> accessRecords = mongoTemplate.find(query, AccessRecords.class);
            int downloadNum = accessRecords.stream().mapToInt(AccessRecords::getDownloadNum).sum();
            int visitNum = accessRecords.stream().mapToInt(AccessRecords::getVisitNum).sum();
            downloadNumList.add(downloadNum);
            visitNumList.add(visitNum);
        }
        map.put("downloadNumList", downloadNumList);
        map.put("visitNumList", visitNumList);
        return ResultUtils.success(map);
    }


    @Override
    public Result getStatisticsResourcesDay(String resourcesId) {

        Map<String, Object> map = new HashMap<>();
        //format date
        List<Date> lDate = DateUtils.getTimeInterval(new Date(), 30);//Get all this weekdate
        List<String> lists = new ArrayList<>();
        for (Date date : lDate) {
            lists.add(DateUtils.getDateString(date));
        }
        map.put("time", lists);
        List<Integer> downloadNumList = new ArrayList<>();
        List<Integer> visitNumList = new ArrayList<>();
        for (String day : lists) {
            Query query = new Query();
            query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
            query.addCriteria(Criteria.where("createTime").regex(day));
            List<AccessRecords> accessRecords = mongoTemplate.find(query, AccessRecords.class);
            int downloadNum = accessRecords.stream().mapToInt(AccessRecords::getDownloadNum).sum();
            int visitNum = accessRecords.stream().mapToInt(AccessRecords::getVisitNum).sum();
            downloadNumList.add(downloadNum);
            visitNumList.add(visitNum);
        }
        map.put("downloadNumList", downloadNumList);
        map.put("visitNumList", visitNumList);
        return ResultUtils.success(map);
    }

    @Override
    public Result getResourcesMap(String resourcesId) {
        Map resultMaps = new HashMap(2);
        for (int x = 0; x < 2; x++) {
            String field = x == 0 ? "visitNum" : "downloadNum";
            Map resultMap = new HashMap(2);
            List<Map> listProvinces = new ArrayList<>();
            List<Map> listProvinces1 = new ArrayList<>();

            Aggregation agg = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("resourcesId").is(resourcesId)),
                    Aggregation.group("name")
                            .sum(field).as("name").count().as("value"));
            AggregationResults<ValuesResult> results = mongoTemplate.aggregate(agg, "access_records", ValuesResult.class);
            Document rawResults = results.getRawResults();
            ArrayList<Document> result = (ArrayList) rawResults.get("results");
            if (null != result && result.size() > 0) {
                for (Document data : result) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", data.get("_id"));
                    map.put("selected", false);
                    map.put("value", data.get("name"));
                    listProvinces.add(map);
                }
                Map<String, Object> mapChina = new HashMap<>();
                for (Document data : result) {
                    Map<String, Object> map = new HashMap<>();
                    //Determine if it belongs to China
                    if (Arrays.asList(Constant.CHINESE_PROVINCES).contains(data.get("_id").toString())) {

                        if (mapChina.containsKey("name") && null != mapChina.get("name") && "China".equals(mapChina.get("name"))) {
                            mapChina.put("name", "China");
                            mapChina.put("selected", true);
                            mapChina.put("value", (int) data.get("name") + (int) mapChina.get("value"));
                        } else {
                            mapChina.put("name", "China");
                            mapChina.put("selected", true);
                            mapChina.put("value", data.get("name"));
                        }

                    } else {
                        map.put("name", data.get("_id"));
                        map.put("selected", false);
                        map.put("value", data.get("name"));
                    }
                    listProvinces1.add(map);
                }

                if (mapChina.size() > 0) {
                    listProvinces1.add(mapChina);
                }

            } else {
                return ResultUtils.success(resultMap);
            }

            List<Map> listResult = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String data[] = i == 0 ? Constant.CHINESE_PROVINCES : Constant.ABROAD;
                String dataEn[] = i == 0 ? Constant.CHINESE_PROVINCES_EN : Constant.ABROAD_EN;
                List<Map> listMap = i == 0 ? listProvinces : listProvinces1;
                boolean existence = false;
                for (int j = 0; j < data.length ; j++) {
                    if (listMap.size() > 0) {
                        for (Map mapData : listMap) {
                            Map<String, Object> map = new HashMap<>(3);
                            if (mapData.containsKey("name") && null != mapData.get("name") && data[j].equals(mapData.get("name"))) {
                                map.put("name", mapData.get("name"));
                                map.put("name_en", dataEn[j]);
                                map.put("selected", false);
                                map.put("value", mapData.get("value"));
                                listResult.add(map);
                                existence = true;
                                continue;
                            }
                        }
                    }
                    if (existence) {
                        existence = false;
                        continue;
                    }
                    Map<String, Object> map = new HashMap<>(3);
                    map.put("name", data[j]);
                    map.put("name_en", dataEn[j]);
                    map.put("selected", false);
                    map.put("value", 0);
                    listResult.add(map);
                }
                if (i == 0) {
                    resultMap.put("domestic", listResult);
                    listResult = new ArrayList<>();
                    continue;
                } else {
                    resultMap.put("abroad", listResult);
                    listResult = new ArrayList<>();
                    break;
                }
            }
            if (x == 0) {
                resultMaps.put("visit", resultMap);
                resultMap = new HashMap();
                continue;
            } else {
                resultMaps.put("download", resultMap);
                resultMap = new HashMap();
                break;
            }
        }

        return ResultUtils.success(resultMaps);
    }

    @Override
    public Result getCitationDetail(String cstr) {
        CenterAccount centerConf = settingService.getCenterConf();
        HttpClient httpClient = new HttpClient();
        String url = instdbUrl.getCstrUrl() + "/openapi/v2/pid-common-service/citations/getCitationDetail?identifier=CSTR:" + cstr;
        String result = "";
        try {
            result = httpClient.doGetCstr(url, centerConf.getClientId(), centerConf.getSecret());
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResultUtils.success();
        }
        JSONObject resultJsonObject = JSON.parseObject(result);
        if (null != resultJsonObject) {
            if (200 == (Integer) resultJsonObject.get("code")) {
                JSONObject data = resultJsonObject.getJSONObject("data");
                com.alibaba.fastjson.JSONArray citationsDetail = data.getJSONArray("citationsDetail");
                if (null != citationsDetail && citationsDetail.size() > 0) {
                    return ResultUtils.success(citationsDetail);
                }
            } else {
                return ResultUtils.success(resultJsonObject.getString("message"));
            }
        }
        return ResultUtils.success();
    }

    @Override
    public Result upAndDownBefore(String token, String id, String type) {
        if (!type.equals(Constant.Approval.ADOPT) && !type.equals(Constant.Approval.OFFLINE)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class, COLLECTION_NAME);
        if (null == resourcesManage) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if (resourcesManage.getStatus().equals(type)) {
            return ResultUtils.error(type.equals(Constant.Approval.ADOPT) ? "RESOURCE_ALREADY_ONLINE" : "RESOURCE_ALREADY_OFFLINE");
        }

        String status = type.equals(Constant.Approval.ADOPT) ? Constant.Approval.ADOPT : Constant.Approval.OFFLINE;
        String resourcesId = resourcesManage.getResourcesId();
        Query queryResources = new Query(Criteria.where("resourcesId").is(resourcesId)).addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT)).addCriteria(Criteria.where("_id").ne(id));
        List<ResourcesManage> resourcesManages = mongoTemplate.find(queryResources, ResourcesManage.class);
        try {
            if (null != resourcesManages && resourcesManages.size() > 0) {
                resourcesManages = resourcesManages.stream().sorted(Comparator.comparing(ResourcesManage::getVersion).reversed()).collect(Collectors.toList());
                ResourcesManage resourcesManage1 = resourcesManages.get(0);
                if (type.equals(Constant.Approval.OFFLINE)) {
                    if (!resourcesManage1.getVersionFlag().equals(Constant.VERSION_FLAG)) {
                        //Modify the status of the current version
                        Update updateCurrent = new Update();
                        updateCurrent.set("versionFlag", Constant.VERSION_FLAG);
                        Query queryCurrent = new Query().addCriteria(Criteria.where("_id").is(resourcesManage1.getId()));
                        mongoTemplate.updateFirst(queryCurrent, updateCurrent, ResourcesManage.class);
                        updateEsStatus("versionFlag", Constant.VERSION_FLAG, resourcesManage1.getEs_id());
                    }
                    //Modify the data status to be removed from the shelves
                    Update updateNow = new Update();
                    updateNow.set("status", status);
                    updateNow.set("versionFlag", "");
                    mongoTemplate.updateFirst(query, updateNow, ResourcesManage.class);
                    updateEsStatus("versionFlag", "", resourcesManage.getEs_id());
                    updateEsStatus("status", status, resourcesManage.getEs_id());
                } else if (type.equals(Constant.Approval.ADOPT)) {
                    Update update = new Update();
                    update.set("status", status);
                    update.set("versionFlag", Constant.VERSION_FLAG);
                    mongoTemplate.updateFirst(query, update, ResourcesManage.class);
                    updateEsStatus("versionFlag", Constant.VERSION_FLAG, resourcesManage.getEs_id());
                    updateEsStatus("status", status, resourcesManage.getEs_id());
                    //Modify the data status of other versions
                    for (ResourcesManage resources : resourcesManages) {
                        Query queryResources1 = new Query().addCriteria(Criteria.where("_id").is(resources.getId()));
                        Update updateResources = new Update();
                        updateResources.set("versionFlag", "");
                        mongoTemplate.updateFirst(queryResources1, updateResources, ResourcesManage.class);
                        updateEsStatus("versionFlag", "", resources.getEs_id());
                    }
                    //   return ResultUtils.error("This data will become the current version，This data will become the current version，This data will become the current version");
                }

            } else {
                Update update = new Update();
                update.set("status", status);
                update.set("versionFlag", type.equals(Constant.Approval.ADOPT) ? Constant.VERSION_FLAG : "");
                mongoTemplate.updateFirst(query, update, ResourcesManage.class);
                updateEsStatus("status", status, resourcesManage.getEs_id());
                if (type.equals(Constant.Approval.ADOPT)) {
                    updateEsStatus("versionFlag", Constant.VERSION_FLAG, resourcesManage.getEs_id());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtils.error(type.equals(Constant.Approval.ADOPT) ? "RESOURCE_ONLINE_FAIL" : "RESOURCE_OFFLINE_FAIL");
        }
        //To remove the generated account password, you need to delete it  To remove the generated account password, you need to delete itftpTo remove the generated account password, you need to delete it
        if (type.equals(Constant.Approval.OFFLINE)) {
            Query queryFtp = new Query();
            queryFtp.addCriteria(Criteria.where("resourcesId").is(id));
            mongoTemplate.remove(queryFtp, FtpUser.class);
        }

        return ResultUtils.success(type.equals(Constant.Approval.ADOPT) ? "RESOURCE_ONLINE_SUCCESS" : "RESOURCE_OFFLINE_SUCCESS");
    }

    @Override
    public Result getResourceList(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT)).addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        if (StringUtils.isNotBlank(name)) {
            query.addCriteria(Criteria.where("name").regex(name));
        }
        query.skip(0);
        query.limit(5);
        query.with(Sort.by(Sort.Direction.DESC, "approveTime"));
        List<ResourcesManage> resourcesManages = mongoTemplate.find(query, ResourcesManage.class);
        return ResultUtils.success(resourcesManages);
    }

    /**
     * Get Template Fields
     * @return
     */
    @Override
    public Map<String, String> getTemplateInfo() {
        Map<String, String> map = new LinkedHashMap();
        Map<String, Object> condition = new HashMap<>();
        condition.put("pageOffset", 0);
        condition.put("pageSize", 20);
        condition.put("name", "");
        condition.put("sort", "");
        PageHelper templateConfigAll = settingService.getTemplateConfigAll(condition);
        if (null != templateConfigAll.getList() && templateConfigAll.getList().size() > 0) {
            List<TemplateConfig> list = templateConfigAll.getList();
            //Determine if there is anyscidbDetermine if there is any
            Query query1 = new Query();
            query1.addCriteria(Criteria.where("templateName").is("scidbTemplate"));
            query1.addCriteria(Criteria.where("dataSetSource").is("scidb"));
            long count = mongoTemplate.count(query1, ResourcesManage.class);
            Template templateScidb = null;
            if (count > 0) {
                final String subjectURL = "/data/DbMetadata_scidb.xml";
                File resourceFile = FileUtils.getResourceFile(subjectURL);
                if (resourceFile.exists()) {
                    templateScidb = XmlTemplateUtil.getTemplate(resourceFile);
                    TemplateConfig config = new TemplateConfig();
                    config.setName("scidbTemplate");
                    list.add(config);
                }
            }
            for (TemplateConfig template : list) {
                Template tem = null;
                if ("scidbTemplate".equals(template.getName())) {
                    tem = templateScidb;
                } else {
                    tem = settingService.getTemplate(template.getCode());
                }
                List<Template.Group> group = tem.getGroup();
                for (Template.Group gro : group) {
                    List<Template.Resource> resources = gro.getResources();
                    for (Template.Resource resource : resources) {
                        String iri = resource.getIri().substring((resource.getIri().lastIndexOf("/") + 1), resource.getIri().length());
                        String language = resource.getLanguage();
                        if (StringUtils.isNotBlank(language) && "en".equals(language) && !map.containsKey(iri + "_en")) {
                            map.put(iri + "_en&" + resource.getType(), resource.getTitle());
                        } else {
                            if (!map.containsKey(iri)) {
                                map.put(iri + "&" + resource.getType(), resource.getTitle());
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    @Override
    public Result getJsonld(String id) {
        LinkedHashMap json = new LinkedHashMap();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        query.addCriteria(Criteria.where("dataSetSource").ne("scidb"));
        Map map = mongoTemplate.findOne(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null == map) {
            return ResultUtils.success();
        }
        if (!map.containsKey("license") || null == map.get("license")) {
            return ResultUtils.success();
        }
        CenterAccount centerConf = settingService.getCenterConf();
        json.put("@context", "https://schema.org");
        json.put("@type", "Dataset");
        json.put("@id", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + id);
        json.put("version", "V1");
        json.put("identifier", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + id);
        json.put("url", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + id);
        json.put("name", null != map.get("name_en") ?map.get("name_en"):map.get("name"));
        json.put("description", null != map.get("description_en") ?map.get("description_en"):map.get("description"));
        json.put("abstract", null != map.get("description_en") ?map.get("description_en"):map.get("description"));
        if (map.containsKey("license") && null != map.get("license")) {
            json.put("license", CommonUtils.getLicense(map.get("license").toString()));
        }

        Map publisher = new TreeMap();
        publisher.put("@url", centerConf.getHost());
        publisher.put("@type", "Organization");
        publisher.put("name", centerConf.getOrgName());
        json.put("publisher", publisher);

        Map includedInDataCatalog = new TreeMap();
        includedInDataCatalog.put("url", centerConf.getHost());
        includedInDataCatalog.put("@type", "DataCatalog");
        includedInDataCatalog.put("name", centerConf.getOrgName());
        json.put("includedInDataCatalog", includedInDataCatalog);

        List<String> keywordsList = new ArrayList();
        List<String> keywords = (List<String>) map.get("keywords");
        List<String> keywords_en = (List<String>) map.get("keywords_en");
        if (null != keywords_en && keywords_en.size() > 0) {
            keywordsList = keywords_en;
        } else if (null != keywords && keywords.size() > 0) {
            keywordsList = keywords;
        }

        //author
        if (map.containsKey("author") && null != map.get("author")) {
            ArrayList author = (ArrayList) map.get("author");
            if (null != author && author.size() > 0) {
                List<Map> authorList = new ArrayList();
                for (int i = 0; i < author.size(); i++) {
                    Map<String, String> authorMap = new HashMap<>();
                    LinkedHashMap object = (LinkedHashMap) author.get(i);
                    authorMap.put("@type", object.get("@type").toString());
                    authorMap.put("name", object.get("name").toString());
                    authorList.add(authorMap);
                }
                json.put("creator", authorList);
            }
        }

        json.put("keywords",  CommonUtils.listToStr(keywordsList,";"));
        json.put("isAccessibleForFree",true);
        json.put("conditionsOfAccess", map.get("version"));
        json.put("conditionsOfAccess", "unrestricted");
        json.put("datePublished", DateUtils.getDateTimeString((Date) map.get("approveTime")));
        json.put("inLanguage", "en_US");

        Map size = new TreeMap();
        size.put("value", map.get("storageNum"));
        size.put("@type", "QuantitativeValue");
        size.put("unitText", "bytes");
        json.put("size", size);

        String JsonString = JSON.toJSONString(json);
        return ResultUtils.success(JsonString);
    }

    @Override
    public void exportResourceData(HttpServletResponse response, String displayStatus, String name, String resourceType, String privacyPolicy, String startDate, String endDate, String publishName, String version,String token,String identifier,String templateName) {

        if (StringUtils.isBlank(token)) {
            log.error("tokenIs empty");
            return;
        } else {
            List<String> roles = jwtTokenUtils.getRoles(token);
            if (!roles.contains(Constant.ADMIN)) {
                log.error("Not an administrator, unable to export");
                return;
            }
        }
        Query query = new Query();
        List<String> listStatus = new ArrayList<>();
        listStatus.add(Constant.Approval.ADOPT);
        listStatus.add(Constant.Approval.OFFLINE);
        setQueryResources(displayStatus, name, resourceType,
                privacyPolicy,
                startDate, endDate, publishName, version, query, listStatus,identifier,"",templateName);

        List<Map> dataList = mongoTemplate.find(query, Map.class, COLLECTION_NAME);
        log.info("Export Data："+dataList.size());
        if (null != dataList && dataList.size() > 0) {

            // Called before exporting data
            FileUtils.initCellMaxTextLength();

            Map<String, String> map = getTemplateInfo();
            if (map.size() == 0) {
                log.info("Data resource exportmapData resource export");
                return;
            }
            map.put("approveTime&dateStr", "Release time");
            map.put("version&custom", "version");
            map.put("publish&publish", "Name of publisher");
            map.put("visitNum&num", "Visits");
            map.put("downloadNum&num", "Download volume");
            map.put("followNum&num", "Collection volume");
            map.put("storageNum&storageNum", "Storage capacity");
            map.put("fileCount&num", "Number of files");
            map.put("templateName&text", "Template Name");
            map.put("dataSetUrl&url", "data seturl");

            //Calculate the total amount of storage
            long storageNum = 0;

            XSSFWorkbook export = null;
            try {
                export = new XSSFWorkbook();
                XSSFSheet sheet = export.createSheet("data set");
                XSSFRow row1 = sheet.createRow(0);
                int irow = 0;
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    row1.createCell(irow).setCellValue(entry.getValue());
                    irow++;
                }
                int storageNumRow = 0;
                for (int i = 0; i < dataList.size(); i++) {
                    Map resourcesManage = dataList.get(i);
                    XSSFRow row = sheet.createRow(i + 1);

                    int datai = 0;
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        String[] split = entry.getKey().split("&");

                        switch (split[1]) {
                            case "url":
                                row.createCell(datai).setCellValue(instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.get("_id"));
                                break;
                            case "publish":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    try {
                                        Resources.Publish publish = (Resources.Publish) resourcesManage.get(split[0]);
                                        row.createCell(datai).setCellValue(publish.getName());
                                    } catch (Exception e) {
                                        row.createCell(datai).setCellValue("");
                                    }
                                } else {
                                    row.createCell(datai).setCellValue("");
                                }
                                break;
                            case "num":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    row.createCell(datai).setCellValue(resourcesManage.get(split[0]).toString());
                                } else {
                                    row.createCell(datai).setCellValue("0");
                                }
                                break;
                            case "dateStr":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    try {
                                        LocalDateTime o = (LocalDateTime) resourcesManage.get(split[0]);
                                        row.createCell(datai).setCellValue(DateUtils.getDateTimeString2(o));
                                    } catch (Exception e) {
                                        Date o = (Date) resourcesManage.get(split[0]);
                                        row.createCell(datai).setCellValue(DateUtils.getDateString(o));
                                    }
                                } else {
                                    row.createCell(datai).setCellValue("");
                                }
                                break;
                            case "storageNum":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    try {
                                        long o = (long) resourcesManage.get(split[0]);
                                        storageNum += o;
                                        String s = FileUtils.readableFileSize(o);
                                        row.createCell(datai).setCellValue(s);
                                    } catch (Exception e) {
                                        row.createCell(datai).setCellValue("0");
                                    }
                                    storageNumRow = datai;
                                } else {
                                    row.createCell(datai).setCellValue("0");
                                }
                                break;
                            //Processing of Text Fields
                            case "custom":
                            case "DOI":
                            case "CSTR":
                            case "text":
                            case "textarea":
                            case "date":
                            case "radio":
                            case "select":
                            case "license":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    row.createCell(datai).setCellValue(resourcesManage.get(split[0]).toString());
                                } else {
                                    row.createCell(datai).setCellValue("");
                                }
                                break;
                            case "selectMany":
                            case "textTabMany":
                            case "textMany":
                            case "subject":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    try {
                                        List<String> list = (List) resourcesManage.get(split[0]);
                                        row.createCell(datai).setCellValue(CommonUtils.listToStr(list,";"));
                                    } catch (Exception e) {
                                        row.createCell(datai).setCellValue("");
                                    }
                                } else {
                                    row.createCell(datai).setCellValue("");
                                }
                                break;
                            case "privacyPolicy":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    LinkedHashMap privacyPolicyMap = new LinkedHashMap();
                                    try {
                                        ResourcesManage.PrivacyPolicy privacyPolicy1 = (ResourcesManage.PrivacyPolicy) resourcesManage.get(split[0]);
                                        if (null != privacyPolicy1) {
                                            privacyPolicyMap.put("type", privacyPolicy1.getType());
                                        }
                                    } catch (Exception e) {
                                        privacyPolicyMap = (LinkedHashMap) resourcesManage.get(split[0]);
                                    }
                                    row.createCell(datai).setCellValue(CommonUtils.getValueByType(privacyPolicyMap.get("type").toString(), Constant.LanguageStatus.PRIVACYPOLICY));
                                } else {
                                    row.createCell(datai).setCellValue("");
                                }
                                break;
                            case "paper":
                            case "project":
                            case "author":
                            case "org":
                                if (resourcesManage.containsKey(split[0]) && null != resourcesManage.get(split[0])) {
                                    ArrayList list = (ArrayList) resourcesManage.get(split[0]);
                                    if (null != list && list.size() > 0) {
                                        StringBuffer sb = new StringBuffer();
                                        for (int iii = 0; iii < list.size(); iii++) {
                                            LinkedHashMap object = (LinkedHashMap) list.get(iii);
                                            if (object.containsKey("name") && null != object.get("name")) {
                                                if ("paper".equals(split[1])) {
                                                    sb.append(object.get("name").toString() + "||" + object.get("en_name"));
                                                } else {
                                                    sb.append(object.get("name").toString());
                                                }
                                            }
                                            if (list.size() > 1) {
                                                sb.append("\r\n");
                                            }
                                            if (iii == 5) {
                                                break;
                                            }
                                        }
                                        row.createCell(datai).setCellValue(sb.toString());
                                    }
                                } else {
                                    row.createCell(datai).setCellValue("");
                                }
                                break;
                        }
                        datai++;
                    }
                }

                XSSFRow row = sheet.createRow(dataList.size() + 1);
                row.createCell(storageNumRow).setCellValue("Total Dataset Storage："+storageNum);

                //Obtain output flow based on the response ancestor
                OutputStream outputStream = null;
                try {
                    //Declare the corresponding text type
                    response.setContentType("application/application/vnd.ms-excel");
                    //set name
                    String filename = "Data resources" + DateUtils.dateToStr_yyyyMMddHHMMss(new Date()) + ".xlsx";
                    response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
                    outputStream = response.getOutputStream();
                    //Calling browser functions through output streamsexcelCalling browser functions through output streams
                    export.write(outputStream);
                    outputStream.close();
                    export.close();
                } catch (Exception e) {
                    log.error("context", e);
                } finally {
                    try {
                        outputStream.close();
                        export.close();
                    } catch (Exception e) {
                    }
                }

            } catch (Exception e) {
                log.debug("-------------  File export failed!  -------------------");
                e.printStackTrace();
            }
        }
        return;
    }

    @Override
    public Result getResourceGroupList() {

        Map<String,Object> mapList = new HashMap<>();

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        query.addCriteria(Criteria.where("versionFlag").is(Constant.VERSION_FLAG));
        List<ResourcesListManage> resourcesManage = mongoTemplate.find(query, ResourcesListManage.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null != resourcesManage && resourcesManage.size() > 0) {


            //Resource Type
            Map<String, List<ResourcesListManage>> collect = resourcesManage.stream()
                    .filter(item-> StringUtils.isNotBlank(item.getResourceType())).collect(Collectors.groupingBy(ResourcesListManage::getResourceType));
            if (null != collect && collect.size() > 0) {
                Map<String,String> map = new HashMap<>();
                for (Map.Entry<String, List<ResourcesListManage>> entry : collect.entrySet()) {
                    map.put(entry.getKey(), "");
                }
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String resourceType = CommonUtils.getValueByType(entry.getKey(),Constant.LanguageStatus.RESOURCE_TYPES);
                    entry.setValue(resourceType);
                }
                mapList.put("resourceType",map);
            }


            //license agreement
            Map<String, List<ResourcesListManage>> collectLicense = resourcesManage.stream()
                    .filter(item-> StringUtils.isNotBlank(item.getLicense())).collect(Collectors.groupingBy(ResourcesListManage::getLicense));
            if (null != collectLicense && collectLicense.size() > 0) {
                Map<String,String> map = new HashMap<>();
                for (Map.Entry<String, List<ResourcesListManage>> entry : collectLicense.entrySet()) {
                    map.put(entry.getKey(), "");
                }
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    entry.setValue(entry.getKey());
                }
                mapList.put("license",map);
            }

            //Shared Scope
            Map<ResourcesManage.PrivacyPolicy, List<ResourcesListManage>> collectPrivacyPolicy = resourcesManage.stream()
                    .filter(item-> null != item.getPrivacyPolicy()).collect(Collectors.groupingBy(ResourcesListManage::getPrivacyPolicy));
            if (null != collectPrivacyPolicy && collectPrivacyPolicy.size() > 0) {
                Map<String,String> map = new HashMap<>();
                for (Map.Entry<ResourcesManage.PrivacyPolicy, List<ResourcesListManage>> entry : collectPrivacyPolicy.entrySet()) {
                    map.put(entry.getKey().getType(), "");
                }
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String resourceType = CommonUtils.getValueByType(entry.getKey(),Constant.LanguageStatus.PRIVACYPOLICY);
                    entry.setValue(resourceType);
                }
                mapList.put("privacyPolicy",map);
            }

            //Year
            Map<LocalDateTime, List<ResourcesListManage>> collectYear = resourcesManage.stream()
                    .filter(item-> null != item.getApproveTime()).collect(Collectors.groupingBy(ResourcesListManage::getApproveTime));
            if (null != collectYear && collectYear.size() > 0) {
                Map<String,String> map = new HashMap<>();
                for (Map.Entry<LocalDateTime, List<ResourcesListManage>> entry : collectYear.entrySet()) {
                    map.put(DateUtils.getDateTimeString3(entry.getKey()), "");
                }
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    entry.setValue(entry.getKey());
                }
                mapList.put("year",map);
            }

        }

        //Topic List
        Query query1 = new Query();
        query1.with(Sort.by(Sort.Direction.DESC, "createTime"));
        List<SpecialVo> list = mongoTemplate.find(query1, SpecialVo.class, "special");
        if (null != list && list.size() > 0) {
            List<Map> listSpecia = new ArrayList<>();
            for (SpecialVo specialVo : list) {
                Map map = new HashMap();
                map.put("id", specialVo.getId());
                map.put("name", specialVo.getSpecialName());
                listSpecia.add(map);
            }
            mapList.put("special", listSpecia);
        }

        return ResultUtils.success(mapList);
    }


    private String updateEsStatus(String field, String value, String esId) {
        UpdateRequest request = new UpdateRequest();
        XContentBuilder contentBuilder = null;
        try {
            contentBuilder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field(field, value)//Modifying Fields and Content
                    .endObject();
            request.index(Constant.RESOURCE_COLLECTION_NAME)
                    .type(Constant.RESOURCE_COLLECTION_NAME)
                    .id(esId)//To modifyid
                    .doc(contentBuilder);
            UpdateResponse updateResponse = client.update(request).get();
            if (200 == updateResponse.status().getStatus()) {
                return "200";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "500";
        }
        return "500";
    }


    private void setAccessRecords(HttpServletRequest request, String resourcesId, String type, Query query1, Update update,String username,long size) {

        String ipAddr = IPUtil.getIpAddr(request);
        String cityInfo = IPUtil.getIpPossession(ipAddr);
        String dateString = DateUtils.getDateString(new Date());
        Query queryAccess = new Query(Criteria.where("resourcesId").is(resourcesId)).addCriteria(Criteria.where("name").is(cityInfo)).addCriteria(Criteria.where("ip").is(ipAddr)).addCriteria(Criteria.where("createTime").is(dateString));
        AccessRecords one = mongoTemplate.findOne(queryAccess, AccessRecords.class);

        HttpSession session = request.getSession();
        if (null == session.getAttribute(session.getId() + "_" + resourcesId) && "visit".equals(type)) {
            mongoTemplate.updateFirst(query1, update, ResourcesManage.class);
            session.setAttribute(session.getId() + "_" + resourcesId, resourcesId);
            if (one == null) {
                AccessRecords accessRecords = new AccessRecords();
                accessRecords.setIp(ipAddr);
                accessRecords.setName(cityInfo);
                accessRecords.setUsername(username);
                accessRecords.setVisitNum("visit".equals(type) ? 1 : 0);
                accessRecords.setDownloadNum("download".equals(type) ? 1 : 0);
                accessRecords.setCreateTime(dateString);
                accessRecords.setResourcesId(resourcesId);
                mongoTemplate.save(accessRecords);
            } else {
                //Interface Download Count Settings
                Query queryUpdateAccess = new Query(Criteria.where("_id").is(one.getId()));
                Update updateAccess = new Update();
                updateAccess.inc("download".equals(type) ? "downloadNum" : "visitNum", 1);
                mongoTemplate.findAndModify(queryUpdateAccess, updateAccess, new FindAndModifyOptions().returnNew(true).upsert(true), AccessRecords.class);
            }
        } else if ("download".equals(type)) {
            if (one == null) {
                AccessRecords accessRecords = new AccessRecords();
                accessRecords.setIp(ipAddr);
                accessRecords.setName(cityInfo);
                accessRecords.setUsername(username);
                accessRecords.setVisitNum("visit".equals(type) ? 1 : 0);
                accessRecords.setDownloadNum("download".equals(type) ? 1 : 0);
                accessRecords.setDownloadStorage(size);
                accessRecords.setCreateTime(dateString);
                accessRecords.setResourcesId(resourcesId);
                mongoTemplate.save(accessRecords);
            } else {

                //Interface Download Count Settings
                Query queryUpdateAccess = new Query(Criteria.where("_id").is(one.getId()));
                Update updateAccess = new Update();
                updateAccess.inc("downloadNum", 1);
                updateAccess.inc("downloadStorage", size);
                mongoTemplate.findAndModify(queryUpdateAccess, updateAccess, new FindAndModifyOptions().returnNew(true).upsert(true), AccessRecords.class);
            }
        }
    }

}
