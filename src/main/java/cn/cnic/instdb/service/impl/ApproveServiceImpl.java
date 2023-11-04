package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.commentNotice.Notice;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.*;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.repository.ApproveRepository;
import cn.cnic.instdb.repository.ResourcesManageRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.*;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.HTTP;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @Auther wdd
 * @Date 2021/3/23 20:23
 * @Desc Approval
 */
@Service
@Slf4j
public class ApproveServiceImpl implements ApproveService {

    public static final String COLLECTION_NAME = "approve";
    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();


    @Resource
    private SettingService settingService;

    @Resource
    private InstdbUrl instdbUrl;

    @Autowired
    private ApproveRepository approveRepository;

    @Autowired
    private ResourcesManageRepository resourcesManageRepository;

    @Resource
    private MongoUtil mongoUtil;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private AuthService authService;
    @Resource
    private UserService userService;

    @Resource
    private ExternalInterService externalInterService;

    @Resource
    private EsDataService esDataService;

    @Autowired
    @Lazy
    private AsyncDeal asyncDeal;


    @Override
    public void save(ResourcesManage manage) {
        Approve approve = new Approve();
        approve.setName(manage.getName());
        approve.setNameEn(manage.getName_en());
        //approve.setType(Constant.Comment.RESOURCE_PUBLISHING);
        approve.setResourcesId(manage.getId());
        approve.setApplyAuthor(manage.getPublish().getName());
        approve.setApplyEmail(manage.getPublish().getEmail());
        approve.setCreateTime(LocalDateTime.now());
        approve.setApprovalStatus(Constant.Approval.PENDING_APPROVAL);
        approve.setResourceType(manage.getResourceType());
        //Default is unclaimed status
        approve.setClaimStatus(Constant.Approval.NO);
        approve.setDownloadFileFlag(manage.getDownloadFileFlag());
        approveRepository.save(approve);
    }

    @Override
    public Result approveSubmit(String token, String resourcesId, String approvalStatus, String reason, String rejectApproval, MultipartFile file) {

        ConsumerDO consumerDO = authService.getUserBytoken(token);
        if (null == consumerDO) {
            return ResultUtils.error("ENVIRONMENT_EXCEPTION");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        query.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        Approve approve = mongoTemplate.findOne(query, Approve.class);
        if (null == approve) {
            return ResultUtils.error("RESOURCE_NO_WAIT");
        }
        if (!Constant.Approval.YES.equals(approve.getClaimStatus())) {
            return ResultUtils.error("CLAIM_NO");
        }
        if (!consumerDO.getEmailAccounts().equals(approve.getClaimEmail())) {
            return ResultUtils.error("CLAIM_NO_YOU");
        }
        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(resourcesId));
        query2.addCriteria(Criteria.where("status").is(Constant.Approval.PENDING_APPROVAL));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query2, ResourcesManage.class);
        if (null != resourcesManage && !Constant.Approval.PENDING_APPROVAL.equals(resourcesManage.getStatus())) {
            return ResultUtils.error("RESOURCE_APPROVED_REPEAT");
        }
        if (!Constant.VERSION_FLAG.equals(resourcesManage.getDownloadFileFlag())) {
            return ResultUtils.error("RESOURCE_APPROVED_NO_FILE");
        }
        //Update resource record table
        Update updateResources = new Update();
        //Update Approval Record Form
        Update update = new Update();

        //Create a form for uploading files
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
        //Prevent Chinese garbled characters
        ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
        //Approval status  Approval statuscstrApproval statusdoi
        if (approvalStatus.equals(Constant.Approval.ADOPT)) {
            //doi and cstrand
            String cstr = resourcesManage.getCstr();
            String doi = resourcesManage.getDoi();
            if (StringUtils.isNotBlank(doi) && Constant.APPLY.equals(doi)) {
                CenterAccount centerConf = settingService.getCenterConf();
                if (StringUtils.isBlank(centerConf.getDoiType()) || Constant.CASDC.equals(centerConf.getDoiType())) {
                    doi = externalInterService.registerDOI(resourcesManage.getId());
                } else if (Constant.DATACITE.equals(centerConf.getDoiType())) {
                    doi = externalInterService.dataciteDoi(resourcesManage.getId());
                } else if (Constant.CHINA_DOI.equals(centerConf.getDoiType())) {
                    doi = externalInterService.registerChinaDOI(resourcesManage.getId());
                }
                if (doi.equals("302")) {
                    return ResultUtils.error("DOI_AUTHOR_NULL");
                } else if (doi.equals("403")) {
                    return ResultUtils.error("DOI_QUERY_CONFIG");
                } else if (doi.equals("500")) {
                    return ResultUtils.error("DOI_ERROR");
                }
                updateResources.set("doi", doi);
                resourcesManage.setDoi(doi);
                if (updateResources.getUpdateObject().size() > 0) {
                    mongoTemplate.updateFirst(query2, updateResources, ResourcesManage.class);
                }
            }
            if (StringUtils.isNotBlank(cstr) && Constant.APPLY.equals(cstr)) {
                //applicationcstr
                cstr = externalInterService.applyCSTR(resourcesManage.getId(),resourcesManage.getDoi());
                if (cstr.equals("-1")) {
                    return ResultUtils.error("CSTR_QUERY_ORG");
                } else if (cstr.equals("-2")) {
                    return ResultUtils.error("CSTR_CONFIG");
                } else if (cstr.equals("-3")) {
                    return ResultUtils.error("CSTR_QUERY_CONFIG");
                }
                updateResources.set("cstr", cstr);
                resourcesManage.setCstr(cstr);
                if (updateResources.getUpdateObject().size() > 0) {
                    mongoTemplate.updateFirst(query2, updateResources, ResourcesManage.class);
                }
            }
            //If approved  If approvedcstrIf approveddoiIf approved
            if (StringUtils.isNotBlank(resourcesManage.getCstr())) {
                entityBuilder.addTextBody("CSTR", StringUtils.isNotBlank(resourcesManage.getCstr()) ? resourcesManage.getCstr() : "");
            }
            if (StringUtils.isNotBlank(resourcesManage.getDoi())) {
                entityBuilder.addTextBody("DOI", StringUtils.isNotBlank(resourcesManage.getDoi()) ? resourcesManage.getDoi() : "");
            }
            entityBuilder.addTextBody("detailsUrl", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + resourcesManage.getId());
        }
        if (StringUtils.isNotBlank(reason) && StringUtils.isNotBlank(rejectApproval)) {
            entityBuilder.addPart("reason", new StringBody(reason, contentType));
            entityBuilder.addPart("rejectApproval", new StringBody(rejectApproval, contentType));
        }
        entityBuilder.addTextBody("resourceId", resourcesId);
        entityBuilder.addTextBody("approvalStatus", approvalStatus);
        if (null != file && !file.isEmpty()) {
            entityBuilder.addPart("file", new FileBody(FileUtils.multipartFileToFile(file), contentType));//Add uploaded files
            //The attachment is also saved in theidb
            String upload = FileUtils.upload(file, instdbUrl.getResourcesRejectFilePath(), CommonUtils.getCode(8));
            //Verify the upload status based on the returned value uploaded
            Map<String, Object> mapUploadFile = JSON.parseObject(upload);
            String codes = (String) mapUploadFile.get("code");
            if ("500".equals(codes)) {
                log.error("Reject attachment uploadidbReject attachment upload");
                return ResultUtils.error("SYSTEM_ERROR");
            }
            String saveFileName = (String) mapUploadFile.get("fileName");
            update.set("rejectFilePath", saveFileName);
            update.set("rejectFileName", file.getOriginalFilename());
        }

        update.set("approvalStatus", approvalStatus);
        update.set("approvalTime", LocalDateTime.now());
        update.set("approvalAuthor", consumerDO.getName());
        update.set("approvalEmail", consumerDO.getEmailAccounts());
        update.set("approvalAuthorEn", consumerDO.getEnglishName());
        update.set("reason", reason);
        update.set("rejectApproval", rejectApproval);
        update.set("esSync", Constant.Approval.NO);

        if (approvalStatus.equals(Constant.Approval.ADOPT)) {
            updateResources.set("status", approvalStatus);
            updateResources.set("approvalAuthor", consumerDO.getName());

            Query query3 = new Query();
            query3.addCriteria(Criteria.where("_id").is(resourcesId));
            Map dataMap = mongoTemplate.findOne(query3, Map.class, Constant.RESOURCE_COLLECTION_NAME);
            dataMap.put("approveTime", DateUtils.getDateString(LocalDate.now()));
            updateResources.set("approveTime", LocalDateTime.now());
            //Custom Publishing Time
            CenterAccount centerAccounts = mongoTemplate.findOne(new Query(), CenterAccount.class);
            if (null != centerAccounts && null != centerAccounts.getIsCustomDate() && Constant.Approval.YES.equals(centerAccounts.getIsCustomDate())) {
                //datePublishedExisting situation
                if (dataMap.containsKey("datePublished") && null != dataMap.get("datePublished")) {
                    String datePublished = dataMap.get("datePublished").toString();
                    try {
                        LocalDateTime localDateTimeByString2 = DateUtils.getLocalDateTimeByString2(datePublished);
                        updateResources.set("approveTime", localDateTimeByString2);
                        dataMap.put("approveTime", DateUtils.getDateTimeString2(localDateTimeByString2));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResultUtils.error("SYSTEM_ERROR");
                    }
                }
            }


            dataMap.put("rootId", dataMap.get("_id").toString());
            if (dataMap.containsKey("doi") && Constant.APPLY.equals(dataMap.get("doi").toString())) {
                dataMap.put("doi", resourcesManage.getDoi());
            }
            if (dataMap.containsKey("cstr") && Constant.APPLY.equals(dataMap.get("cstr").toString())) {
                dataMap.put("cstr", resourcesManage.getCstr());
            }
            dataMap.put("status", Constant.Approval.ADOPT);
            dataMap.remove("_id");
            dataMap.remove("callbackUrl");
            dataMap.remove("sendFileList");
            dataMap.remove("downloadFileFlag");
            dataMap.remove("json_id_content");
            dataMap.remove("@type");
            dataMap.remove("@context");
            dataMap.remove("organization");
            dataMap.remove("publish");
            dataMap.remove("fileIsZip");


            if (dataMap.containsKey("createTime")) {
                Date createTime = (Date) dataMap.get("createTime");
                dataMap.put("createTime", DateUtils.getDateString(createTime));
            }
            //preservees
            String esId = esDataService.add(dataMap);
            if ("500".equals(esId)) {
                return ResultUtils.error("RESOURCE_APPROVED_ES");
            }else {
                update.set("esSync", Constant.Approval.YES);
                updateResources.set("esSync", Constant.Approval.YES);
                updateResources.set("es_id", esId);
            }

            //Callbackds
            String dataSpace = sendDataSpace(resourcesManage, entityBuilder);
            if ("200".equals(dataSpace)) {
            } else {
                esDataService.delete(esId);
                if ("500" == dataSpace) {
                    return ResultUtils.error("RESOURCE_APPROVED_TO_URL");
                } else {
                    return ResultUtils.error(dataSpace);
                }
            }

            Criteria criteriaOld = Criteria.where("resourcesId").is(resourcesManage.getResourcesId());
            Query queryOld = new Query();
            criteriaOld.and("_id").ne(resourcesId);
            queryOld.addCriteria(criteriaOld);
            List<ResourcesManage> list = mongoTemplate.find(queryOld, ResourcesManage.class);
            if (null != list && list.size() > 0) {
                //Clear the latest tags from previous versions of information
                for (ResourcesManage data : list) {
                    Criteria criteriaUpdate = Criteria.where("_id").is(data.getId());
                    Query queryUpdate = new Query();
                    queryUpdate.addCriteria(criteriaUpdate);
                    Update update1 = new Update();
                    update1.set("versionFlag", "");
                    mongoTemplate.updateFirst(queryUpdate, update1, ResourcesManage.class);
                }
            }

            mongoTemplate.upsert(query, update, COLLECTION_NAME);
            //Update Resources
            mongoTemplate.upsert(query2, updateResources, ResourcesManage.class);
            //Make Trusted  Make Trusted、Make Trusted、Make Trusted、Make Trusted
            externalInterService.setCredible(resourcesId);
            dataMap.clear();
        } else if (approvalStatus.equals(Constant.Approval.NO)) {

            //Callbackds
            String dataSpace = sendDataSpace(resourcesManage, entityBuilder);
            if ("200".equals(dataSpace)) {
            } else {
                if (updateResources.getUpdateObject().size() > 0) {
                    mongoTemplate.upsert(query2, updateResources, ResourcesManage.class);
                }
                if ("500" == dataSpace) {
                    return ResultUtils.error("RESOURCE_APPROVED_TO_URL");
                } else {
                    return ResultUtils.error(dataSpace);
                }
            }

            //Delete Dataset
            mongoTemplate.remove(query2, ResourcesManage.class);
            //Delete the corresponding file
            asyncDeal.deleteDirectory(instdbUrl.getResourcesFilePath() + resourcesManage.getId());
            FileUtils.deleteFile(instdbUrl.getResourcesPicturePath() + resourcesManage.getId() + Constant.PNG);
            //Delete structured content
            Query structuredQuery = new Query();
            structuredQuery.addCriteria(Criteria.where("resourceId").is(resourcesId));
            List<Map> result = mongoTemplate.find(structuredQuery, Map.class, Constant.TABLE_NAME);
            if (null != result && result.size() > 0) {
                for (Map map : result) {
                    mongoTemplate.dropCollection(map.get("tableName").toString());
                }
                mongoTemplate.remove(structuredQuery, Constant.TABLE_NAME);
            }

            //Delete Dataset File Content
            Query removeQuery = new Query();
            removeQuery.addCriteria(Criteria.where("resourcesId").is(resourcesId));
            mongoTemplate.remove(removeQuery, ResourceFileTree.class);
            mongoTemplate.upsert(query, update, COLLECTION_NAME);
            //Expert comments are marked as invalid
            Update resourcesReviewUpdate = new Update();
            resourcesReviewUpdate.set("deadlineStatus", Constant.Approval.OFFLINE);
            mongoTemplate.updateMulti(removeQuery, resourcesReviewUpdate, ResourcesReview.class);

           // 2023year8year24year16:32:02  year  setResourcesIdyear  yearidyearsetResourcesId year  year
            Update reject = new Update();
            reject.set("resourceId", resourcesId);
            reject.set("correlationId", resourcesManage.getResourcesId());
            mongoTemplate.upsert(new Query(), reject, "rejectionRecord");

        }



        //Generate notifications
        if (StringUtils.isNotBlank(resourcesManage.getPublish().getEmail())) {
            String type = approvalStatus.equals(Constant.Approval.ADOPT) ? Constant.Comment.APPROVAL_ADOPT : Constant.Comment.APPROVAL_NO;
            Notice notice = new Notice();
            notice.setUsername(resourcesManage.getPublish().getEmail());
            notice.setType(type);
            notice.setContent("The data resources you submitted for publication《" + resourcesManage.getName() + "》The data resources you submitted for publication！");
            String name_en = StringUtils.isNotBlank(resourcesManage.getName_en()) ? resourcesManage.getName_en() : resourcesManage.getName();
            notice.setContentEn("The data resource \"" + name_en + "\" you submitted and released has been approved ！");
            notice.setTitle("Data resource approval completion reminder");
            notice.setTitleEn("Reminder of data resource approval completion");
            notice.setIs_read("1");
            notice.setCreateTime(LocalDateTime.now());
            notice.setResourcesId(resourcesManage.getId());
            mongoTemplate.insert(notice);
            //  commentNoticeService.addNotice(resourcesManage.getPublish().getEmail(), type, "The data resources you submitted for publication《" + resourcesManage.getName() + "》The data resources you submitted for publication！",resourcesManage.getId(),"The data resources you submitted for publication");
        }
        //After approval is completed, also delete the generated account password  After approval is completed, also delete the generated account passwordftpAfter approval is completed, also delete the generated account password
        Query queryFtp = new Query();
        queryFtp.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        mongoTemplate.remove(queryFtp, FtpUser.class);

        return ResultUtils.success("APPROVAL_SUCCESS");
    }

    String sendDataSpace(ResourcesManage resourcesManage, MultipartEntityBuilder entityBuilder){
        //Callback interface address if not empty
        if (null != resourcesManage.getCallbackUrl() && StringUtils.isNotBlank(resourcesManage.getCallbackUrl().getOnSuccess())) {
            //calldataspacecall
            Result callback = submitDataspce(entityBuilder, resourcesManage.getCallbackUrl().getOnSuccess());
            net.sf.json.JSONObject objBody = null;
            try {
                objBody = net.sf.json.JSONObject.fromObject(callback.getData());
            } catch (Exception e) {
                e.printStackTrace();
                return "500";
              //  return ResultUtils.error("RESOURCE_APPROVED_TO_URL");
            }
            if (200 != callback.getCode() || 0 == objBody.size()) {
                return "500";
               // return ResultUtils.error("RESOURCE_APPROVED_TO_URL");
            }
            String code = objBody.optString("code");
            if (StringUtils.isNotBlank(code) && "-1".equals(code)) {
                log.error("Approval callback" + resourcesManage.getCallbackUrl() + "Approval callback：" + objBody.optString("message"));
                //return ResultUtils.error(objBody.optString("message"));
                return objBody.optString("message");
            }
        }
        return "200";
    }

    public static void main(String[] args) {
      //  File file = new File("C:\\Users\\wangdongdong\\Desktop\\parameter.txt");
        //Create a form for uploading files
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
        //Prevent Chinese garbled characters
        ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
        entityBuilder.addPart("reason", new StringBody("Disagree", contentType));
        entityBuilder.addTextBody("resourceId", "e55ea37e752d4477b2438f8d5a770012");
        entityBuilder.addTextBody("approvalStatus", "adopt");


        entityBuilder.addTextBody("CSTR", "38458.11.16160309612061");
        entityBuilder.addTextBody("DOI", "10.57841/casdc.0000037");
            entityBuilder.addTextBody("detailsUrl", "http://sdc.ustc.edu.cn/dataDetails/e55ea37e752d4477b2438f8d5a770012");

//        if (null != file) {
//            log.info("Here comes the file" + file.getName() + file.getParentFile());
//            entityBuilder.addPart("file", new FileBody(file, contentType));//Add uploaded files
//        }
        Result callback = submitDataspce(entityBuilder, "http://127.0.0.1:8082/api/audit.callback");
        net.sf.json.JSONObject objBody = net.sf.json.JSONObject.fromObject(callback.getData());
        log.info(callback.toString());
        if (200 != callback.getCode() || 0 == objBody.size()) {
            throw new RuntimeException("Approval callback interface call failed,Approval callback interface call failed!");
        }
        //JSONObject objBody = (JSONObject) callback.getData();
        String code = objBody.optString("code");
        if (StringUtils.isNotBlank(code) && "-1".equals(code)) {
            log.error("Approval callback return result：" + objBody.optString("message"));
            throw new RuntimeException(objBody.optString("message"));
        }

    }

    private static Result submitDataspce(MultipartEntityBuilder entityBuilder, String url) {
        HttpClient httpClient = new HttpClient();
        //Prevent Chinese garbled characters
        HttpEntity httpEntity = entityBuilder.build();
        String result = "";
        try {
            result = httpClient.upload(httpEntity, url);
        } catch (Exception e) {
            log.info(e.getMessage());
            ResultUtils.error("RESOURCE_APPROVED_TO_URL");
        }
        return ResultUtils.success("", result);
    }

    @Override
    public List<Approve> toApprove(String resourcesOnlyId, String version) {

        //Go for approval Go for approval Go for approval
        Criteria criteria = Criteria.where("resourcesId").is(resourcesOnlyId);
        criteria.and("version").is(version);
        Object[] o = new Object[]{Constant.Approval.ADOPT, Constant.Approval.NO}; //Include all,Include all
        criteria.and("approvalStatus").in(o);
        Query query = new Query();
        query.addCriteria(criteria);
        List<Approve> approveList = mongoTemplate.find(query, Approve.class);
        return approveList;


    }

    @Override
    public PageHelper findApproveList(Map<String, Object> condition) {

        String lang = tokenCache.getIfPresent("lang");
        Query query = new Query();
        HashMap<String, Object> countMap = new HashMap<>();
        String tag = condition.get("tag").toString();
        setParam(query, condition, countMap);
        long count = mongoTemplate.count(query, Approve.class);
        mongoUtil.start(Integer.parseInt(condition.get("pageOffset").toString()), Integer.parseInt(condition.get("pageSize").toString()), query);
        List<Approve> approves = mongoTemplate.find(query, Approve.class);
        for (Approve approve : approves) {
            approve.setDownloadFileFlag(Constant.Language.english.equals(lang) ? "unaccomplished" : "Transfer not completed");
            //Query whether data transmission is complete
            if (!Constant.VERSION_FLAG.equals(approve.getDownloadFileFlag()) || 0 == approve.getStorageNum() || StringUtils.isBlank(approve.getPrivacyPolicy())) {
                Query queryResources = new Query();
                queryResources.addCriteria(Criteria.where("_id").is(approve.getResourcesId()));
                ResourcesManage resourcesManage = mongoTemplate.findOne(queryResources, ResourcesManage.class);
                Query queryApprove = new Query();
                Criteria criteria = Criteria.where("_id").is(approve.getId());
                queryApprove.addCriteria(criteria);
                //If it is already completed, update it If it is already completed, update it
                if (null != resourcesManage && Constant.VERSION_FLAG.equals(resourcesManage.getDownloadFileFlag())) {
                    Update update = new Update();
                    update.set("downloadFileFlag", Constant.VERSION_FLAG);
                    update.set("storageNum", resourcesManage.getStorageNum());
                    update.set("privacyPolicy", resourcesManage.getPrivacyPolicy().getType());

                    mongoTemplate.updateFirst(queryApprove, update, Approve.class);
                    approve.setDownloadFileFlag(Constant.Language.english.equals(lang) ? "completed" : "Transfer completed");
                    approve.setStorageNum(resourcesManage.getStorageNum());
                    approve.setPrivacyPolicy(resourcesManage.getPrivacyPolicy().getType());
                }
                if (null != resourcesManage && StringUtils.isBlank(approve.getPrivacyPolicy())) {
                    Update update = new Update();
                    update.set("privacyPolicy", resourcesManage.getPrivacyPolicy().getType());
                    mongoTemplate.updateFirst(queryApprove, update, Approve.class);
                    approve.setPrivacyPolicy(resourcesManage.getPrivacyPolicy().getType());
                }

            }
            approve.setApplyAuthor(approve.getApplyAuthor());

            if (!"openApi".equals(tag)) {
                if (!approve.getApprovalStatus().equals(Constant.Approval.PENDING_APPROVAL)) {
                    //Add evaluation records
                    Query queryEvaluat = new Query();
                    queryEvaluat.addCriteria(Criteria.where("approvalId").is(approve.getId()));
                    queryEvaluat.addCriteria(Criteria.where("resourcesId").is(approve.getResourcesId()));
                    List<ResourcesReview> review = mongoTemplate.find(queryEvaluat, ResourcesReview.class);
                    if (null != review && review.size() > 0) {
                        for (ResourcesReview r : review) {
                            //Calculate link expiration time
                            Date tomorrowday = DateUtils.tomorrowdayByDate(7 * 24, DateUtils.LocalDateTimeasDate(r.getCreateTime()));
                            //Determine if the link expiration time has expired
                            String dateString = DateUtils.getDateString(tomorrowday);
                            boolean effectiveDate = DateUtils.isEffectiveDate(new Date(), dateString);
                            r.setDeadlineStatus(effectiveDate ? Constant.Approval.YES : Constant.Approval.NO);
                        }
                    }
                    approve.setResourcesReviews(review);
                }
            }

            //Expert evaluation information query
            Query queryEvaluat = new Query();
            queryEvaluat.addCriteria(Criteria.where("resourcesId").is(approve.getResourcesId()));
            //Total number to be evaluated
            long countResourcesReview = mongoTemplate.count(queryEvaluat, ResourcesReview.class);
            queryEvaluat.addCriteria(Criteria.where("status").ne(Constant.Approval.PENDING_APPROVAL));
            long countResourcesReviewWait = mongoTemplate.count(queryEvaluat, ResourcesReview.class);
            approve.setResourcesReview(countResourcesReviewWait + "/" + countResourcesReview);

            // approve.setApplyAuthor(approve.getApplyAuthor()+"("+approve.getApplyEmail()+")");
            //  approve.setResourceType(CommonUtils.getResourceType(approve.getResourceType()));
            //approve.setApprovalStatus(CommonUtils.getValueByType(approve.getApprovalStatus(),Constant.LanguageStatus.APPROVAL));
        }

        return mongoUtil.pageHelper(count, approves, countMap);
    }


    /**
     * Set query parameters for approval list
     *
     * @param query
     * @param condition
     * @param countMap
     */
    void setParam(Query query, Map<String, Object> condition, Map<String, Object> countMap) {

        String token = condition.get("token").toString();

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("PERMISSION_FORBIDDEN"));
        String username = jwtTokenUtils.getUsernameFromToken(token);
        //Not for administrators and approvers, just check the records that need to be approved by oneself
        //Administrators and auditors can see all the data
        if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
            query.addCriteria(Criteria.where("approvalEmail").is(username));
        }

        String tag = condition.get("tag").toString();
        String applyAuthor = condition.get("applyAuthor").toString();

        String approvalAuthor = condition.get("approvalAuthor").toString();
        String applyEmail = condition.get("applyEmail").toString();
        String claimAuthor = condition.get("claimAuthor").toString();
        String claimStatus = condition.get("claimStatus").toString();
        String resourceType = condition.get("resourceType").toString();
        String approvalStatus = condition.get("approvalStatus").toString();
        String identifier = condition.get("identifier").toString();
        String sort = condition.get("sort").toString();
        String resourcesId = condition.get("resourcesId").toString();

        String name = condition.get("name").toString();

        String startDate = condition.get("startDate").toString();
        String endDate = condition.get("endDate").toString();

        if (StringUtils.isNotBlank(tag) && "all".equals(tag)) {
            if (StringUtils.isNotBlank(approvalStatus)) {
                query.addCriteria(Criteria.where("approvalStatus").is(approvalStatus));
            } else {
                Object[] o = new Object[]{Constant.Approval.REVOKE, Constant.Approval.ADOPT, Constant.Approval.NO}; //Include all,Include all
                query.addCriteria(Criteria.where("approvalStatus").in(o));
            }
            if (StringUtils.isNotBlank(sort) && sort.contains("&")) {
                String[] s = sort.split("&");
                if (s[0].equals("asc")) {
                    query.with(Sort.by(Sort.Direction.ASC, s[1]));
                } else if (s[0].equals("desc")) {
                    query.with(Sort.by(Sort.Direction.DESC, s[1]));
                }
            }else {
                //Sort approval records by approval time
                query.with(Sort.by(Sort.Direction.DESC, "approvalTime"));
            }

            if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
                Criteria criteria = new Criteria();
                query.addCriteria(criteria.andOperator(Criteria.where("approvalTime").gte(DateUtils.getLocalDateTimeByString2(startDate)),
                        Criteria.where("approvalTime").lte(DateUtils.getLocalDateTimeByString2(endDate))));
            } else if (StringUtils.isNotBlank(startDate)) {
                query.addCriteria(Criteria.where("approvalTime").gte(DateUtils.getLocalDateTimeByString2(startDate)));
            } else if (StringUtils.isNotBlank(endDate)) {
                query.addCriteria(Criteria.where("approvalTime").lte(DateUtils.getLocalDateTimeByString2(endDate)));
            }
        } else if (StringUtils.isNotBlank(tag) && "openApi".equals(tag)) {
            if (StringUtils.isNotBlank(sort) && sort.contains("&")) {
                String[] s = sort.split("&");
                if (s[0].equals("asc")) {
                    query.with(Sort.by(Sort.Direction.ASC, s[1]));
                } else if (s[0].equals("desc")) {
                    query.with(Sort.by(Sort.Direction.DESC, s[1]));
                }
            }else {
                //Sort approval records by approval time
                query.with(Sort.by(Sort.Direction.DESC, "approvalTime"));
            }
        } else {
            //Query pending approval records
            query.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
            if (StringUtils.isNotBlank(sort) && sort.contains("&")) {
                String[] s = sort.split("&");
                if (s[0].equals("asc")) {
                    query.with(Sort.by(Sort.Direction.ASC, s[1]));
                } else if (s[0].equals("desc")) {
                    query.with(Sort.by(Sort.Direction.DESC, s[1]));
                }
            }else {
                query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            }
            if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
                Criteria criteria = new Criteria();
                query.addCriteria(criteria.andOperator(Criteria.where("createTime").gte(DateUtils.getLocalDateTimeByString2(startDate)),
                        Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(endDate))));
            } else if (StringUtils.isNotBlank(startDate)) {
                query.addCriteria(Criteria.where("createTime").gte(DateUtils.getLocalDateTimeByString2(startDate)));
            } else if (StringUtils.isNotBlank(endDate)) {
                query.addCriteria(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(endDate)));
            }
        }
        if (StringUtils.isNotBlank(name)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(name) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("name").regex(pattern));
        }

        if (StringUtils.isNotBlank(identifier)) {
            query.addCriteria(Criteria.where("identifier").is(identifier.trim()));
        }

        if (StringUtils.isNotBlank(resourcesId)) {
            query.addCriteria(Criteria.where("resourcesId").is(resourcesId.trim()));
        }

        //Applicant fuzzy search Applicant fuzzy search,Applicant fuzzy search
        if (StringUtils.isNotBlank(applyAuthor)) {
            if (applyAuthor.contains(",")) {
                String[] applyAuthors = applyAuthor.split(",");
                Criteria criteria = new Criteria();
                List<Criteria> criteria1List = new ArrayList<>();
                for (String author : applyAuthors) {
                    Criteria applyAuthor1 = Criteria.where("applyAuthor").regex(author);
                    criteria1List.add(applyAuthor1);
                }
                Criteria[] criteria1 = new Criteria[criteria1List.size()];
                criteria1List.toArray(criteria1);
                criteria.orOperator(criteria1);
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where("applyAuthor").regex(applyAuthor));
            }
        }

        //Approver fuzzy search Approver fuzzy search,Approver fuzzy search
        if (StringUtils.isNotBlank(approvalAuthor)) {
            if (approvalAuthor.contains(",")) {
                String[] approvalAuthors = approvalAuthor.split(",");
                Criteria criteria = new Criteria();
                List<Criteria> criteria1List = new ArrayList<>();
                for (String author : approvalAuthors) {
                    Criteria applyAuthor1 = Criteria.where("approvalAuthor").regex(author);
                    criteria1List.add(applyAuthor1);
                }
                Criteria[] criteria1 = new Criteria[criteria1List.size()];
                criteria1List.toArray(criteria1);
                criteria.orOperator(criteria1);
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where("approvalAuthor").regex(approvalAuthor));
            }
        }


        if (StringUtils.isNotBlank(applyEmail)) {
            query.addCriteria(Criteria.where("applyEmail").regex(applyEmail));
        }

        //Claimant fuzzy search Claimant fuzzy search,Claimant fuzzy search
        if (StringUtils.isNotBlank(claimAuthor)) {
            if (claimAuthor.contains(",")) {
                String[] claimAuthors = claimAuthor.split(",");
                Criteria criteria = new Criteria();
                List<Criteria> criteria1List = new ArrayList<>();
                for (String author : claimAuthors) {
                    Criteria applyAuthor1 = Criteria.where("claimAuthor").regex(author);
                    criteria1List.add(applyAuthor1);
                }
                Criteria[] criteria1 = new Criteria[criteria1List.size()];
                criteria1List.toArray(criteria1);
                criteria.orOperator(criteria1);
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where("claimAuthor").regex(claimAuthor));
            }
        }

        if (StringUtils.isNotBlank(claimStatus)) {
            query.addCriteria(Criteria.where("claimStatus").is(claimStatus));
        }
        if (StringUtils.isNotBlank(resourceType)) {
            query.addCriteria(Criteria.where("resourceType").is(resourceType));
        }

        //Check the list of pending approval and approval records specifically
        Query queryWait = new Query();
        Query queryAll = new Query();
        Object[] o = new Object[]{Constant.Approval.REVOKE, Constant.Approval.ADOPT, Constant.Approval.NO}; //Include all,Include all
        if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
            queryWait.addCriteria(Criteria.where("approvalEmail").is(username));
            queryAll.addCriteria(Criteria.where("approvalEmail").is(username));
        }
        queryAll.addCriteria(Criteria.where("approvalStatus").in(o));
        queryWait.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        //Total Pending Approval
        long waitCount = mongoTemplate.count(queryWait, Approve.class);
        //Total number of approval records
        long allCount = mongoTemplate.count(queryAll, Approve.class);
//        //Number of pending approval records applied for access
//        queryWait.addCriteria(Criteria.where("type").is(Constant.Comment.APPLY_REMINDER));
//        long waitCountVisit = mongoTemplate.count(queryWait, Approve.class);
//        countMap.put("waitCountVisit", waitCountVisit);
//        //Number of approval records applied for access
//        queryAll.addCriteria(Criteria.where("type").is(Constant.Comment.APPLY_REMINDER));
//        long allCountVisit = mongoTemplate.count(queryAll, Approve.class);
//        countMap.put("allCountVisit", allCountVisit);
        countMap.put("allCount", allCount);
        countMap.put("waitCount", waitCount);
    }

    @Override
    public Map<String, Object> revokeApprove(String resourceId) {
        Map<String, Object> map = new HashMap<>();
        map.put("result", false);
        //Verify if it has been approved Verify if it has been approved Verify if it has been approved
        //Verify if it exists
        //Delete without approval

        Criteria criteria = Criteria.where("_id").is(resourceId);
        Query query = new Query();
        query.addCriteria(criteria);
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);

        if (null == resourcesManage) {
            return map;
        }
        if (Constant.Approval.ADOPT.equals(resourcesManage.getStatus()) || Constant.Approval.NO.equals(resourcesManage.getStatus())) {
            return map;
        }
        Query queryApprove = new Query();
        queryApprove.addCriteria(Criteria.where("resourcesId").is(resourceId));
        queryApprove.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        Approve approve = mongoTemplate.findOne(queryApprove, Approve.class);

        if (null != approve && StringUtils.isNotBlank(approve.getClaimStatus()) && Constant.Approval.YES.equals(approve.getClaimStatus())) {
            return map;
        }

        //Delete Approval Record
        approveRepository.delete(approve);
        //Delete Resources
        resourcesManageRepository.delete(resourcesManage);
        //Delete the corresponding file
        asyncDeal.deleteDirectory(instdbUrl.getResourcesFilePath() + resourcesManage.getId());

        //Delete structured content
        Query structuredQuery = new Query();
        structuredQuery.addCriteria(Criteria.where("resourceId").is(resourceId));
        List<Map> result = mongoTemplate.find(structuredQuery, Map.class, Constant.TABLE_NAME);
        if (null != result && result.size() > 0) {
            for (Map maps : result) {
                mongoTemplate.dropCollection(maps.get("tableName").toString());
            }
            mongoTemplate.remove(structuredQuery, Constant.TABLE_NAME);
        }

        //Delete Dataset File Content
        Query removeQuery = new Query();
        removeQuery.addCriteria(Criteria.where("resourcesId").is(resourceId));
        mongoTemplate.remove(removeQuery, ResourceFileTree.class);
        map.put("result", true);
        //Generate notifications
        //Push to each administrator Push to each administratoruserPush to each administratoramdinPush to each administratorcode  Push to each administrator

        Query queryRole = new Query();
        queryRole.addCriteria(Criteria.where("roles").in(Constant.ADMIN, Constant.ROLE_APPROVE)).addCriteria(Criteria.where("state").is(1));
        List<ConsumerDO> userList = mongoTemplate.find(queryRole, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null != userList && userList.size() > 0) {
            List<String> listEmail = new ArrayList<>();
            List<Notice> listNotice = new ArrayList<>();
            for (ConsumerDO user : userList) {
                Notice notice = new Notice();
                notice.setUsername((user.getEmailAccounts()));
                notice.setType(Constant.Comment.APPROVAL_REVOKE);
                notice.setContent(resourcesManage.getPublish().getName() + "(" + resourcesManage.getPublish().getEmail() + ")Submitted data resources《" + resourcesManage.getName() + "》Submitted data resources。");
                String name_en = StringUtils.isNotBlank(resourcesManage.getName_en()) ? resourcesManage.getName_en() : resourcesManage.getName();
                notice.setContentEn(resourcesManage.getPublish().getName() + "(" + resourcesManage.getPublish().getEmail() + ") The submitted data resource \"" + name_en + "\" has withdrawn its approval。");
                notice.setTitle("Reminder for revoking approval of data resources");
                notice.setTitleEn("Reminder of revoking approval of data resources");
                notice.setIs_read("1");
                notice.setCreateTime(LocalDateTime.now());
                notice.setResourcesId(null);
                listEmail.add(user.getEmailAccounts());
                listNotice.add(notice);
            }
            if (listEmail.size() > 0) {
                mongoTemplate.insertAll(listNotice);
                //Send an email to the administrator
                Map<String, Object> attachment = new HashMap<>();
                attachment.put("resourceName", resourcesManage.getName());
                attachment.put("name", resourcesManage.getPublish().getName() + "(" + resourcesManage.getPublish().getEmail() + ")");
                ToEmail toEmail = new ToEmail();
                toEmail.setTos(listEmail.toArray(new String[listEmail.size()]));
                asyncDeal.send(toEmail, attachment, EmailModel.EMAIL_RESOURCES_REVOKE());
            }
        }

        return map;
    }


    @Override
    public Result claim(String token, String id, String status) {
        if (StringUtils.isBlank(id) || StringUtils.isBlank(status)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
            return ResultUtils.error("CLAIM_NO_ADMIN");
        }
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        if (null == consumerDO) {
            return ResultUtils.error("ENVIRONMENT_EXCEPTION");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Approve approveObj = mongoTemplate.findOne(query, Approve.class);

        if (null == approveObj) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Update update = new Update();
        update.set("claimStatus", status);
        update.set("claimAuthor", Constant.Approval.YES.equals(status) ? consumerDO.getName() : null);
        update.set("claimAuthorEn", Constant.Approval.YES.equals(status) ? consumerDO.getEnglishName() : null);
        update.set("claimEmail", Constant.Approval.YES.equals(status) ? consumerDO.getEmailAccounts() : null);
        if (Constant.Approval.ADOPT.equals(approveObj.getApprovalStatus())) {
            return ResultUtils.error("CLAIM_ADOPT");
        }
        if (StringUtils.isNotBlank(approveObj.getClaimStatus()) && Constant.Approval.YES.equals(approveObj.getClaimStatus())) {

            List<String> roles1 = consumerDO.getRoles();

            //Claimants can enter to cancel their claims
            if (consumerDO.getEmailAccounts().equals(approveObj.getClaimEmail()) || roles1.contains(Constant.ADMIN)) {
                mongoTemplate.updateFirst(query, update, Approve.class);
                return ResultUtils.success(Constant.Approval.YES.equals(status) ? "CLAIM_SUCCESS" : "CLAIM_CANCEL");
            }
            return ResultUtils.error("CLAIM_FAIL");
        }
        mongoTemplate.updateFirst(query, update, Approve.class);
        return ResultUtils.success(Constant.Approval.YES.equals(status) ? "CLAIM_SUCCESS" : "CLAIM_CANCEL");
    }

    @Override
    public Result adminClaim(String token, String id, String userEmail) {

        if (StringUtils.isBlank(id) || StringUtils.isBlank(userEmail)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("CLAIM_NO_ADMIN");
        }

        ConsumerDO consumerDO = userService.getUserInfoByName(userEmail);
        if (null == consumerDO) {
            return ResultUtils.error("ENVIRONMENT_EXCEPTION");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Approve approveObj = mongoTemplate.findOne(query, Approve.class);

        if (null == approveObj) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        if (Constant.Approval.ADOPT.equals(approveObj.getApprovalStatus())) {
            return ResultUtils.error("CLAIM_ADOPT");
        }

        Update update = new Update();
        update.set("claimStatus", Constant.Approval.YES);
        update.set("claimAuthor", consumerDO.getName());
        update.set("claimAuthorEn", consumerDO.getEnglishName());
        update.set("claimEmail", consumerDO.getEmailAccounts());
        mongoTemplate.updateFirst(query, update, Approve.class);
        return ResultUtils.success("CLAIM_SUCCESS");
    }

    @Override
    public void downloadRejectFile(String token, String id, HttpServletResponse response) {

        String username = tokenCache.getIfPresent(token);
        Assert.isTrue(StringUtils.isNotBlank(username), I18nUtil.get("ENVIRONMENT_EXCEPTION"));

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(roles.contains(Constant.ADMIN) || roles.contains(Constant.ROLE_APPROVE), I18nUtil.get("PERMISSION_FORBIDDEN"));

        Criteria criteria = Criteria.where("approvalStatus").is(Constant.Approval.NO);
        criteria.and("_id").is(id);
        Query query = new Query();
        query.addCriteria(criteria);
        Approve approve = mongoTemplate.findOne(query, Approve.class);
        if (null != approve && StringUtils.isNotBlank(approve.getRejectFilePath()) && StringUtils.isNotBlank(approve.getRejectFileName())) {
            FileUtils.downloadFile(instdbUrl.getResourcesRejectFilePath() + approve.getRejectFilePath(), response,"");
        }
    }

    @Override
    public void exportApprovalData(HttpServletResponse response, String token, String name, String applyAuthor, String applyEmail, String claimAuthor, String claimStatus, String resourceType, String approvalAuthor, String approvalStatus, String identifier, String startDate, String endDate) {
        if (StringUtils.isBlank(token)) {
            log.error("tokenIs empty");
            return;
        } else {
            List<String> roles = jwtTokenUtils.getRoles(token);
            if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
                log.error("Not an auditor and cannot be exported");
                return;
            }
        }

        Query query = new Query();

        if (StringUtils.isNotBlank(approvalStatus)) {
            query.addCriteria(Criteria.where("approvalStatus").is(approvalStatus));
        } else {
            Object[] o = new Object[]{Constant.Approval.REVOKE, Constant.Approval.ADOPT, Constant.Approval.NO}; //Include all,Include all
            query.addCriteria(Criteria.where("approvalStatus").in(o));
        }
        //Sort approval records by approval time
        query.with(Sort.by(Sort.Direction.DESC, "approvalTime"));
        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            Criteria criteria = new Criteria();
            query.addCriteria(criteria.andOperator(Criteria.where("approvalTime").gte(DateUtils.getLocalDateTimeByString2(startDate)),
                    Criteria.where("approvalTime").lte(DateUtils.getLocalDateTimeByString2(endDate))));
        } else if (StringUtils.isNotBlank(startDate)) {
            query.addCriteria(Criteria.where("approvalTime").gte(DateUtils.getLocalDateTimeByString2(startDate)));
        } else if (StringUtils.isNotBlank(endDate)) {
            query.addCriteria(Criteria.where("approvalTime").lte(DateUtils.getLocalDateTimeByString2(endDate)));
        }

        if (StringUtils.isNotBlank(name)) {
            query.addCriteria(Criteria.where("name").regex(name));
        }

        if (StringUtils.isNotBlank(identifier)) {
            query.addCriteria(Criteria.where("identifier").is(identifier));
        }

        //Applicant fuzzy search Applicant fuzzy search,Applicant fuzzy search
        if (StringUtils.isNotBlank(applyAuthor)) {
            if (applyAuthor.contains(",")) {
                String[] applyAuthors = applyAuthor.split(",");
                Criteria criteria = new Criteria();
                List<Criteria> criteria1List = new ArrayList<>();
                for (String author : applyAuthors) {
                    Criteria applyAuthor1 = Criteria.where("applyAuthor").regex(author);
                    criteria1List.add(applyAuthor1);
                }
                Criteria[] criteria1 = new Criteria[criteria1List.size()];
                criteria1List.toArray(criteria1);
                criteria.orOperator(criteria1);
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where("applyAuthor").regex(applyAuthor));
            }
        }

        //Approver fuzzy search Approver fuzzy search,Approver fuzzy search
        if (StringUtils.isNotBlank(approvalAuthor)) {
            if (approvalAuthor.contains(",")) {
                String[] approvalAuthors = approvalAuthor.split(",");
                Criteria criteria = new Criteria();
                List<Criteria> criteria1List = new ArrayList<>();
                for (String author : approvalAuthors) {
                    Criteria applyAuthor1 = Criteria.where("approvalAuthor").regex(author);
                    criteria1List.add(applyAuthor1);
                }
                Criteria[] criteria1 = new Criteria[criteria1List.size()];
                criteria1List.toArray(criteria1);
                criteria.orOperator(criteria1);
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where("approvalAuthor").regex(approvalAuthor));
            }
        }


        if (StringUtils.isNotBlank(applyEmail)) {
            query.addCriteria(Criteria.where("applyEmail").regex(applyEmail));
        }

        //Claimant fuzzy search Claimant fuzzy search,Claimant fuzzy search
        if (StringUtils.isNotBlank(claimAuthor)) {
            if (claimAuthor.contains(",")) {
                String[] claimAuthors = claimAuthor.split(",");
                Criteria criteria = new Criteria();
                List<Criteria> criteria1List = new ArrayList<>();
                for (String author : claimAuthors) {
                    Criteria applyAuthor1 = Criteria.where("claimAuthor").regex(author);
                    criteria1List.add(applyAuthor1);
                }
                Criteria[] criteria1 = new Criteria[criteria1List.size()];
                criteria1List.toArray(criteria1);
                criteria.orOperator(criteria1);
                query.addCriteria(criteria);
            } else {
                query.addCriteria(Criteria.where("claimAuthor").regex(claimAuthor));
            }
        }

        if (StringUtils.isNotBlank(claimStatus)) {
            query.addCriteria(Criteria.where("claimStatus").is(claimStatus));
        }
        if (StringUtils.isNotBlank(resourceType)) {
            query.addCriteria(Criteria.where("resourceType").is(resourceType));
        }

        List<Map> dataList = mongoTemplate.find(query, Map.class,"approve");


        log.info("Export Data："+dataList.size());
        if (null != dataList && dataList.size() > 0) {
            // Called before exporting data
           FileUtils.initCellMaxTextLength();

            List<String> listHead = new ArrayList<>();
            listHead.add("name&name&Dataset Name");
            listHead.add("resourceType&resourceType&Dataset Type");
            listHead.add("storageNum&storageNum&Dataset storage capacity");
            listHead.add("text&identifier&Dataset association project number");
            listHead.add("date&createTime&Submission time");
            listHead.add("text&applyAuthor&Name of publisher");
            listHead.add("text&applyEmail&Publisher's email");
            listHead.add("text&claimAuthor&Reviewer Name");
            listHead.add("text&claimEmail&Reviewer's email");
         //   listHead.add("claimStatus&claimStatus&Claimant Status");
//            listHead.add("text&approvalAuthor&Reviewer Name");
//            listHead.add("text&approvalEmail&Reviewer's email");
            listHead.add("approvalStatus&approvalStatus&Audit status");
            listHead.add("date&approvalTime&Audit time");
            listHead.add("reason&reason&Review comments");
            listHead.add("rejectApproval&rejectApproval&Reason for review rejection");
            listHead.add("expertReview&zhuanjia&expert review ");
            XSSFWorkbook export = null;
            try {
                export = new XSSFWorkbook();
                XSSFSheet sheet = export.createSheet("Approval Record");

                XSSFRow row1 = sheet.createRow(0);
                int irow = 0;
                for (String entry : listHead) {
                    if (entry.split("&")[0].equals("name") || entry.split("&")[0].equals("reason") || entry.split("&")[0].equals("rejectApproval")) {
                        sheet.setColumnWidth(irow, 3 * 2560);
                    } else if (entry.split("&")[0].equals("date")) {
                        sheet.setColumnWidth(irow, 3 * 2000);
                    } else if (entry.split("&")[0].equals("resourceType") || entry.split("&")[0].equals("claimStatus") || entry.split("&")[0].equals("approvalStatus")) {
                        sheet.setColumnWidth(irow, 3 * 1000);
                    }else {
                        sheet.setColumnWidth(irow, 3 * 1500);
                    }
                    row1.createCell(irow).setCellValue(entry.split("&")[2]);
                    irow++;
                }

                for (int i = 0; i < dataList.size(); i++) {
                    Map approve = dataList.get(i);
                    XSSFRow row = sheet.createRow(i + 1);

                    int datai = 0;
                    for (String entry : listHead) {

                        switch (entry.split("&")[0]) {
                            case "date":
                                try {
                                    Date date = (Date) approve.get(entry.split("&")[1]);
                                    String dateString = DateUtils.getDateTimeString(date);
                                    row.createCell(datai).setCellValue(dateString);
                                } catch (Exception e) {
                                    row.createCell(datai).setCellValue("");
                                }
                                datai++;
                                break;
                            case "resourceType":
                                try {
                                    row.createCell(datai).setCellValue(CommonUtils.getValueByType(approve.get(entry.split("&")[1]).toString(), Constant.LanguageStatus.RESOURCE_TYPES));
                                } catch (Exception e) {
                                    row.createCell(datai).setCellValue("");
                                }
                                datai++;
                                break;
                            case "approvalStatus":
                                try {
                                    String string = approve.get(entry.split("&")[1]).toString();
                                    if (Constant.Approval.ADOPT.equals(string)) {
                                        row.createCell(datai).setCellValue("Passed");
                                    } else if (Constant.Approval.NO.equals(string)) {
                                        row.createCell(datai).setCellValue("Rejected");
                                    } else if (Constant.Approval.REVOKE.equals(string)) {
                                        row.createCell(datai).setCellValue("Withdrawn");
                                    }
                                } catch (Exception e) {
                                    row.createCell(datai).setCellValue("");
                                }
                                datai++;
                                break;
                            case "claimStatus":
                                try {
                                    String string = approve.get(entry.split("&")[1]).toString();
                                    if (Constant.Approval.YES.equals(string)) {
                                        row.createCell(datai).setCellValue("Claimed");
                                    } else {
                                        row.createCell(datai).setCellValue("Unclaimed ");
                                    }
                                } catch (Exception e) {
                                    row.createCell(datai).setCellValue("");
                                }
                                datai++;
                                break;
                            case "expertReview":
                                try {
                                    String expertReview = getExpertReview(approve.get("_id").toString(), approve.get("resourcesId").toString());
                                    row.createCell(datai).setCellValue(expertReview);
                                } catch (Exception e) {
                                    row.createCell(datai).setCellValue("");
                                }
                                datai++;
                                break;
                            case "storageNum":
                                try {
                                    long o = (long) approve.get(entry.split("&")[1]);
                                    String s = FileUtils.readableFileSize(o);
                                    row.createCell(datai).setCellValue(s);
                                } catch (Exception e) {
                                    row.createCell(datai).setCellValue("0");
                                }
                                datai++;
                                break;
                            default:
                                try {
                                    row.createCell(datai).setCellValue(approve.get(entry.split("&")[1]).toString());
                                } catch (Exception e) {
                                    row.createCell(datai).setCellValue("");
                                }
                                datai++;
                                break;
                        }


                    }
                }

                //Obtain output flow based on the response ancestor
                OutputStream outputStream = null;
                try {
                    //Declare the corresponding text type
                    response.setContentType("application/application/vnd.ms-excel");
                    //set name
                    String filename = "Approval Record" + DateUtils.dateToStr_yyyyMMddHHMMss(new Date()) + ".xlsx";
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


    private String getExpertReview(String approvalId, String resourcesId) {
        //Add evaluation records
        Query queryEvaluat = new Query();
        queryEvaluat.addCriteria(Criteria.where("approvalId").is(approvalId));
        queryEvaluat.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        List<ResourcesReview> review = mongoTemplate.find(queryEvaluat, ResourcesReview.class);
        if (null != review && review.size() > 0) {
            StringBuffer sb = new StringBuffer();
            for (ResourcesReview r : review) {
                String status = r.getStatus().equals(Constant.Approval.ADOPT) ? "adopt" : r.getStatus().equals(Constant.Approval.NO) ? "adopt" : "adopt";
                sb.append(DateUtils.getDateTimeString(r.getCreateTime())).append("Invite experts").append(r.getUsername());
                if (StringUtils.isNotBlank(r.getEmail())) {
                    sb.append("(").append(r.getEmail()).append(")");
                }
                sb.append("Conduct evaluation，");
                if (null != r.getReviewTime()) {
                    sb.append("The evaluation time is：").append(DateUtils.getDateTimeString(r.getReviewTime())).append(" The evaluation time is：").append(status).append(" The evaluation time is：").append(r.getReason());
                } else {
                    sb.append("Invitation time is：").append(DateUtils.getDateTimeString(r.getCreateTime())).append(" Invitation time is：").append(status).append(" Invitation time is：").append(StringUtils.isBlank(r.getReason()) ? "" : r.getReason());
                }
                if (review.size() > 1) {
                    sb.append("\r\n");
                }
            }
            return sb.toString();
        }
        return "";
    }




}
