package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.commentNotice.Notice;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.ApplyAccessApproval;
import cn.cnic.instdb.model.resources.ApplyAccessSubmit;
import cn.cnic.instdb.model.resources.ResourceAccess;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.Template;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.repository.ResourcesManageRepository;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ApplyAccessService;
import cn.cnic.instdb.service.AuthService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther wdd
 * @Date 2021/3/23 20:23
 * @Desc Approval
 */
@Service
@Slf4j
public class ApplyAccessServiceImpl implements ApplyAccessService {

    public static final String COLLECTION_NAME = "applyAccess_template";


    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();


    @Resource
    private InstdbUrl instdbUrl;


    @Autowired
    private ResourcesManageRepository resourcesManageRepository;

    @Resource
    private MongoUtil mongoUtil;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private JwtTokenUtils jwtTokenUtils;
    @Resource
    private UserRepository userRepository;

    @Resource
    private AuthService authService;


    @Autowired
    private AsyncDeal asyncDeal;


    @Override
    public Result resourceAccessRequest(String token, ApplyAccessSubmit applyAccessSubmit) {
// || stringutils.isblank(applyaccesssubmit.getstarttime()) || stringutils.isblank(applyaccesssubmit.getendtime())
        if (StringUtils.isBlank(applyAccessSubmit.getResourceId())
                || StringUtils.isBlank(applyAccessSubmit.getOrg()) || StringUtils.isBlank(applyAccessSubmit.getPhone())) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

//        if (!DateUtils.belongCalendar(applyAccessSubmit.getStartTime(), applyAccessSubmit.getEndTime())) {
//            return ResultUtils.error("TIME_VERIFICATION");
//        }

        Optional<ResourcesManage> byId = resourcesManageRepository.findById(applyAccessSubmit.getResourceId());
        if (!byId.isPresent()) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        String type = byId.get().getPrivacyPolicy().getType();
        if (!Constant.PrivacyPolicy.CONDITION.equals(type)) {
            return ResultUtils.error("RESOURCE_NOT_APPLY");
        }

        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();


        //Prohibit duplicate submissions
        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(byId.get().getId()));
        query.addCriteria(Criteria.where("applyEmail").is(consumerDO.getEmailAccounts()));
        query.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        ResourceAccess resourceAccess = mongoTemplate.findOne(query, ResourceAccess.class);
        if (null != resourceAccess) {
            return ResultUtils.error("RESOURCE_REPEAT_APPLY");
        }


        //Generate approved records
        ResourceAccess access = new ResourceAccess();
        if (null != applyAccessSubmit.getResources() && applyAccessSubmit.getResources().size() > 0) {
            List<Template.Group> resources = applyAccessSubmit.getResources();
            for (Template.Group group : resources) {
                List<Template.Resource> resource = group.getResources();
                if (null != resource && resource.size() > 0) {
                    for (Template.Resource data : resource) {
                        String multiply = data.getMultiply();
                        if (StringUtils.isNotBlank(multiply) && multiply.contains(":")) {
                            String[] split = multiply.split(":");
                            if (Integer.parseInt(split[0]) > 0) {
                                if (null == data.getValue()) {
                                    return ResultUtils.error("TEMPLATE_REQUIRED");
                                }
                            }
                        }
                        if ("startDate".equals(data.getType())) {
                            if (null != data.getValue() && StringUtils.isNotBlank(data.getValue().toString()) && data.getValue().toString().contains("&")) {
                                String time = data.getValue().toString();
                                access.setStartTime(time.split("&")[0]);
                                access.setEndTime(time.split("&")[1]);
                                if (!DateUtils.belongCalendar(time.split("&")[0], time.split("&")[1])) {
                                    return ResultUtils.error("TIME_VERIFICATION");
                                }
                            }
                        }
                    }
                } else {
                    return ResultUtils.error("TEMPLATE_DATA_NULL");
                }
            }
            access.setTemplateData(applyAccessSubmit.getResources());

        } else {
            return ResultUtils.error("TEMPLATE_DATA_NULL");
        }


        //Update user information
        if (!applyAccessSubmit.getOrg().equals(consumerDO.getOrgChineseName()) || !applyAccessSubmit.getPhone().equals(consumerDO.getTelephone())) {
            Query queryUser = new Query();
            queryUser.addCriteria(Criteria.where("_id").is(consumerDO.getId()));
            Update update = new Update();
            update.set("orgChineseName", applyAccessSubmit.getOrg());
            update.set("telephone", applyAccessSubmit.getPhone());
            mongoTemplate.updateFirst(queryUser, update, ConsumerDO.class);
        }


        access.setName(byId.get().getName());
        access.setNameEn(byId.get().getName_en());
        access.setApplyAuthor(consumerDO.getName());
        access.setApplyAuthorEn(consumerDO.getEnglishName());
        access.setApplyEmail(consumerDO.getEmailAccounts());
        access.setCreateTime(LocalDateTime.now());
        access.setTemplate("yes");
        access.setId(CommonUtils.generateUUID());
        access.setResourcesId(byId.get().getId());
        access.setResourceType(byId.get().getResourceType());
        //Set approver
        access.setApprovalEmail(byId.get().getPublish().getEmail());
        access.setApprovalAuthor(byId.get().getPublish().getName());
        access.setApprovalStatus(Constant.Approval.PENDING_APPROVAL);
        mongoTemplate.save(access);


        //Store the email account to be sent
        List<String> listEmail = new ArrayList<>();
        //Add a publisher's first
        listEmail.add(byId.get().getPublish().getEmail());
        //Batch Add Notification
        List<Notice> listNotice = new ArrayList<>();
        //Create a separate object for the publisher
        Notice notice = new Notice();
        notice.setUsername(byId.get().getPublish().getEmail());
        notice.setType(Constant.Comment.ACCESS_APPROVAL_REMINDER);
        notice.setContent(consumerDO.getName() + "(" + consumerDO.getEmailAccounts() + ")Apply for access to data resources《" + byId.get().getName() + "》,Apply for access to data resources！");
        String name_en = StringUtils.isNotBlank(byId.get().getName_en()) ? byId.get().getName_en() : byId.get().getName();
        notice.setContentEn(consumerDO.getName() + "(" + consumerDO.getEmailAccounts() + ")Please approve the application for accessing the data resource " + name_en + " ！");
        notice.setTitle("Request to access data resources pending approval reminder");
        notice.setTitleEn("Application for access to data resources pending approval reminder");
        notice.setIs_read("1");
        notice.setCreateTime(LocalDateTime.now());
        notice.setResourcesId(byId.get().getId());
        listNotice.add(notice);
        //Push to each administrator auditor
        Query queryConsumerDO = new Query();
        queryConsumerDO.addCriteria(Criteria.where("roles").in(Constant.ADMIN, Constant.ROLE_APPROVE)).addCriteria(Criteria.where("state").is(1));
        List<ConsumerDO> userList = mongoTemplate.find(queryConsumerDO, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null != userList && userList.size() > 0) {
            for (ConsumerDO user : userList) {
                //Send Message Notification
                //Push to each administrator Push to each administratoruserPush to each administratoramdinPush to each administratorcode  Push to each administrator
                Notice noticeAdmin = new Notice();
                noticeAdmin.setUsername((user.getEmailAccounts()));
                noticeAdmin.setType(Constant.Comment.ACCESS_APPROVAL_REMINDER);
                noticeAdmin.setContent(consumerDO.getName() + "(" + consumerDO.getEmailAccounts() + ")Apply for access to data resources《" + byId.get().getName() + "》,Apply for access to data resources！");
                String name_en1 = StringUtils.isNotBlank(byId.get().getName_en()) ? byId.get().getName_en() : byId.get().getName();
                noticeAdmin.setContentEn(consumerDO.getName() + "(" + consumerDO.getEmailAccounts() + ")Please approve the application for accessing the data resource " + name_en1 + " ！");
                noticeAdmin.setTitle("Request to access data resources pending approval reminder");
                noticeAdmin.setTitleEn("Application for access to data resources pending approval reminder");
                noticeAdmin.setIs_read("1");
                noticeAdmin.setCreateTime(LocalDateTime.now());
                noticeAdmin.setResourcesId(byId.get().getId());
                listNotice.add(noticeAdmin);
                listEmail.add(user.getEmailAccounts());
            }
        }

        //Send an email to the administrator
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("resourceName", byId.get().getName());
        attachment.put("name", consumerDO.getName());
        attachment.put("email", consumerDO.getEmailAccounts());
        attachment.put("url", instdbUrl.getCallHost() + "/center/AccessApproval");
        //Batch Generate Notification
        mongoTemplate.insertAll(listNotice);
        ToEmail toEmail = new ToEmail();
        toEmail.setTos(listEmail.toArray(new String[listEmail.size()]));
        asyncDeal.send(toEmail, attachment, EmailModel.EMAIL_APPLY());
        return ResultUtils.success("RESOURCE_APPLY");

    }

    @Override
    public Result uploadDataTemplate(String token, MultipartFile file) {

//        Map data = mongoTemplate.findOne(new Query(), Map.class, COLLECTION_NAME);
//        if (null != data) {
//            return ResultUtils.error("APPLY_TEMPLATE_ONE");
//        }

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        if (null == file || file.isEmpty()) {
            return ResultUtils.error("FILE_IS_NULL");
        }

        if (!FileUtils.checkFileSizeIsLimit(file.getSize(), 2, "M")) {
            return ResultUtils.error("FILE_TOO_BIG5");
        }

        //Determine whether the file types are consistent
        String suffix = FileUtils.getSuffixByFileName(file.getOriginalFilename());
        if (!"xml".equals(suffix)) {
            return ResultUtils.error("FILE_FORMAT_XML");
        }

        //judgexmljudge
        if (!FileUtils.isXmlDocument(FileUtils.multipartFileToFile(file))) {
            return ResultUtils.error("FILE_CONTENT_XML");
        }

        Template template = XmlTemplateUtil.getTemplate(FileUtils.multipartFileToFile(file));
        if (null == template || null == template.getGroup() || 0 == template.getGroup().size()) {
            return ResultUtils.error("FILE_FORMAT_NO");
        }


        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        //Upload files
        String upload = FileUtils.upload(file, instdbUrl.getTemplateUrl(), CommonUtils.getCode(8));

        //Verify the upload status based on the returned value uploaded
        Map<String, Object> mapUploadFile = JSON.parseObject(upload);
        String codes = (String) mapUploadFile.get("code");
        if ("500".equals(codes)) {
            return ResultUtils.error("SYSTEM_ERROR");
        }
        String path = (String) mapUploadFile.get("url");
        Map map = new HashMap();
        String UUID = CommonUtils.generateUUID();
        map.put("name", template.getTemplateName());
        map.put("desc", template.getTemplateDesc());
        map.put("author", template.getTemplateAuthor());
        map.put("_id", UUID);
        map.put("path", path);
        map.put("state", Constant.Approval.NO);
        map.put("username", consumerDO.getName());
        map.put("userEmail", consumerDO.getEmailAccounts());
        map.put("createTime", LocalDateTime.now());
        mongoTemplate.insert(map, COLLECTION_NAME);

        Template templateInfo = XmlTemplateUtil.getTemplateInfo(path);
        if (null != templateInfo && null != templateInfo.getGroup()) {
            Map dataMap = new HashMap();
            dataMap.put("template", templateInfo.getGroup());
            dataMap.put("id", UUID);
            return ResultUtils.success(dataMap);
        }
        return ResultUtils.success();
    }

    @Override
    public Result submitFile(String token, String id) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        Query queryDel = new Query();
        queryDel.addCriteria(Criteria.where("_id").ne(id));
        List<Map> maps = mongoTemplate.find(queryDel, Map.class, COLLECTION_NAME);
        if (null != maps) {
            mongoTemplate.remove(queryDel, COLLECTION_NAME);
            for (Map map : maps) {
                //Delete files
                FileUtils.deleteFile(map.get("path").toString());
            }
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Map map = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        if (null != map) {
            Update update = new Update();
            update.set("state", Constant.Approval.YES);
            mongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return ResultUtils.success("FILE_UPLOAD");
        }
        return ResultUtils.error("PARAMETER_ERROR");
    }

    @Override
    public Result deleteTemplateById(String token, String id) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Map map = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        if (null != map) {
            mongoTemplate.remove(query, COLLECTION_NAME);
            FileUtils.deleteFile(map.get("path").toString());
        }
        return ResultUtils.success("DELETE_SUCCESS");
    }

    @Override
    public Result getMyApplyTemplate() {
        // 0Effective 1Effective
        Query query = new Query();
        query.addCriteria(Criteria.where("state").is(Constant.Approval.YES));
        Map map = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        if (null != map) {
            Template template = XmlTemplateUtil.getTemplateInfo(map.get("path").toString());
            if (null != template && null != template.getGroup()) {
                return ResultUtils.success(template.getGroup());
            }
        }
        return ResultUtils.success();
    }

    @Override
    public Result getMyApplyTemplateList() {

        Query queryDelete = new Query();
        queryDelete.addCriteria(Criteria.where("state").ne(Constant.Approval.YES));
        List<Map> result = mongoTemplate.find(queryDelete, Map.class, COLLECTION_NAME);
        if (null != result && result.size() > 0) {
            for (Map map : result) {
                if (map.containsKey("path") && null != map.get("path")) {
                    FileUtils.deleteFile(map.get("path").toString());
                }
            }
        }
        mongoTemplate.remove(queryDelete, COLLECTION_NAME);

        Query query = new Query();
        query.addCriteria(Criteria.where("state").is(Constant.Approval.YES));
        Map map = mongoTemplate.findOne(query, Map.class, COLLECTION_NAME);
        return ResultUtils.success(map);
    }


    @Override
    public Map getMyApply(String token, String status) {
        String username = tokenCache.getIfPresent(token);
        Map<String, Object> map = new HashMap<>();
        //First, obtain the status types of all data resources
        Map<String, String> statusTypes = new HashMap<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("applyEmail").is(username));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        List<ResourceAccess> approves = mongoTemplate.find(query, ResourceAccess.class);
        if (null != approves && approves.size() > 0) {
            Map<String, List<ResourceAccess>> collect = approves.stream().collect(Collectors.groupingBy(ResourceAccess::getApprovalStatus));
            if (null != collect && collect.size() > 0) {
                for (Map.Entry<String, List<ResourceAccess>> entry : collect.entrySet()) {
                    statusTypes.put(entry.getKey() + "-" + entry.getValue().size(), "");
                }
                for (Map.Entry<String, String> entry : statusTypes.entrySet()) {
                    for (String resourceType : Constant.STATUS_TYPES) {
                        String[] split = resourceType.split("&");
                        if (entry.getKey().contains("-") && split[0].equals(entry.getKey().split("-")[0])) {
                            entry.setValue(split[1]);
                        }
                    }
                }
            }
            if (statusTypes.size() > 0) {
                map.put("statusTypes", statusTypes);
            }
        }
        //Set query criteria
        if (StringUtils.isNotBlank(status)) {
            query.addCriteria(Criteria.where("approvalStatus").is(status));
        }
        long count = mongoTemplate.count(query, ResourceAccess.class);
        List<ResourceAccess> approveList = mongoTemplate.find(query, ResourceAccess.class);
        map.put("count", count);
        map.put("list", approveList);
        return map;
    }


    @Override
    public PageHelper getResourceAccess(String token, String tag, String name, String resourceType, String approvalAuthor, String applyAuthor, String approvalStatus, String startDate, String endDate, Integer pageOffset, Integer pageSize,String sort) {


        Query query = new Query();
        HashMap<String, Object> countMap = new HashMap<>();

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("PERMISSION_FORBIDDEN"));
        String username = jwtTokenUtils.getUsernameFromToken(token);

        if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
            query.addCriteria(Criteria.where("approvalEmail").is(username));
        }

        String time = "createTime";

        if (StringUtils.isNotBlank(tag) && "all".equals(tag)) {
            Object[] o = new Object[]{Constant.Approval.ADOPT, Constant.Approval.NO, Constant.Approval.OFFLINE}; //Include all,Include all
            if (StringUtils.isNotBlank(approvalStatus)) {
                query.addCriteria(Criteria.where("approvalStatus").is(approvalStatus));
            } else {
                query.addCriteria(Criteria.where("approvalStatus").in(o));
            }
            //Sort approval records by approval time
            time = "approvalTime";
        } else {
            //Query pending approval records
            query.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        }
        if (StringUtils.isNotBlank(sort) && sort.contains("&")) {
            String[] s = sort.split("&");
            if (s[0].equals("asc")) {
                query.with(Sort.by(Sort.Direction.ASC, s[1]));
            } else if (s[0].equals("desc")) {
                query.with(Sort.by(Sort.Direction.DESC, s[1]));
            }
        }else{
            query.with(Sort.by(Sort.Direction.DESC, time));
        }

        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            Criteria criteria = new Criteria();
            query.addCriteria(criteria.andOperator(Criteria.where(time).gte(DateUtils.getLocalDateTimeByString2(startDate)),
                    Criteria.where(time).lte(DateUtils.getLocalDateTimeByString2(endDate))));
        } else if (StringUtils.isNotBlank(startDate)) {
            query.addCriteria(Criteria.where(time).gte(DateUtils.getLocalDateTimeByString2(startDate)));
        } else if (StringUtils.isNotBlank(endDate)) {
            query.addCriteria(Criteria.where(time).lte(DateUtils.getLocalDateTimeByString2(endDate)));
        }

        if (StringUtils.isNotBlank(name)) {
            query.addCriteria(Criteria.where("name").regex(name));
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
        if (StringUtils.isNotBlank(resourceType)) {
            query.addCriteria(Criteria.where("resourceType").is(resourceType));
        }

        //Check the list of pending approval and approval records specifically
        Query queryWait = new Query();
        Query queryAll = new Query();
        Object[] o = new Object[]{Constant.Approval.ADOPT, Constant.Approval.NO, Constant.Approval.OFFLINE}; //Include all,Include all
        if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
            queryWait.addCriteria(Criteria.where("approvalEmail").is(username));
            queryAll.addCriteria(Criteria.where("approvalEmail").is(username));
        }
        queryAll.addCriteria(Criteria.where("approvalStatus").in(o));
        queryWait.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        //Total Pending Approval
        long waitCount = mongoTemplate.count(queryWait, ResourceAccess.class);
        //Total number of approval records
        long allCount = mongoTemplate.count(queryAll, ResourceAccess.class);
        countMap.put("allCount", allCount);
        countMap.put("waitCount", waitCount);

        long count = mongoTemplate.count(query, ResourceAccess.class);
        mongoUtil.start(pageOffset, pageSize, query);
        List<ResourceAccess> approves = mongoTemplate.find(query, ResourceAccess.class);
        return mongoUtil.pageHelper(count, approves, countMap);
    }

    @Override
    public Result resourceAccessApproval(String token, ApplyAccessApproval apply) {


        if (StringUtils.isEmpty(apply.getApprovalStatus()) || StringUtils.isEmpty(apply.getId())) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        if (Constant.Approval.NO.equals(apply.getApprovalStatus())) {
            if (StringUtils.isEmpty(apply.getReason())) {
                return ResultUtils.error("APPROVE_NO");
            }
        } else {
            if (StringUtils.isEmpty(apply.getAccessPeriod()) || StringUtils.isEmpty(apply.getAccessData())) {
                return ResultUtils.error("PARAMETER_ERROR");
            }
        }

        Set set = new HashSet();
        set.add(Constant.Approval.ADOPT);
        set.add(Constant.Approval.NO);
        if (!set.contains(apply.getApprovalStatus())) {
            return ResultUtils.error("PARAMETER_ERROR");
        }


        ConsumerDO consumerDO = authService.getUserBytoken(token);
        if (null == consumerDO) {
            return ResultUtils.error("ENVIRONMENT_EXCEPTION");
        }

        //Prohibit duplicate submissions
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(apply.getId()));
        ResourceAccess approveObj = mongoTemplate.findOne(query, ResourceAccess.class);
        if (null == approveObj) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

        if (!Constant.Approval.PENDING_APPROVAL.equals(approveObj.getApprovalStatus())) {
            return ResultUtils.error("RESOURCE_NO_WAIT");
        }


        Update update = new Update();
        update.set("approvalStatus", apply.getApprovalStatus());
        update.set("approvalTime", LocalDateTime.now());
        update.set("approvalAuthor", consumerDO.getName());
        update.set("approvalAuthorEn", consumerDO.getEnglishName());
        update.set("approvalEmail", consumerDO.getEmailAccounts());
        update.set("accessPeriod", apply.getAccessPeriod());
        update.set("accessData", apply.getAccessData());
        if (Constant.Approval.ADOPT.equals(apply.getApprovalStatus())) {
            if (!"unlimited".equals(apply.getAccessPeriod())) {
                if (StringUtils.isBlank(apply.getStartTime()) || StringUtils.isEmpty(apply.getEndTime())) {
                    return ResultUtils.error("PARAMETER_ERROR");
                } else {
                    if (!DateUtils.belongCalendar(apply.getStartTime(), apply.getEndTime())) {
                        return ResultUtils.error("TIME_VERIFICATION");
                    }
                    update.set("startTime", apply.getStartTime());
                    update.set("endTime", apply.getEndTime());
                }
            }
            if (!"unlimited".equals(apply.getAccessData())) {
                if (null == apply.getFilesId() || 0 == apply.getFilesId().size()) {
                    return ResultUtils.error("PARAMETER_ERROR");
                } else {
                    update.set("filesId", apply.getFilesId());
                }
            }
        } else {
            update.set("reason", apply.getReason());
        }

        mongoTemplate.upsert(query, update, ResourceAccess.class);

        //Send notifications
        if (StringUtils.isNotBlank(approveObj.getApplyEmail())) {

            boolean state = apply.getApprovalStatus().equals(Constant.Approval.ADOPT);

            Notice notice = new Notice();
            notice.setUsername(approveObj.getApplyEmail());
            String type = state ? Constant.Comment.APPROVAL_ADOPT : Constant.Comment.APPROVAL_NO;
            notice.setType(type);
            String status = state ? "Passed！" : "Passed，Passed";
            notice.setContent("The data resource you requested to access《" + approveObj.getName() + "》The data resource you requested to access" + status);
            String name_en = StringUtils.isNotBlank(approveObj.getNameEn()) ? approveObj.getNameEn() : approveObj.getName();
            String contentEn = state ? "The data resource \"" + name_en + "\" you applied for access has been approved。" :
                    "The approval of the data resource \"" + name_en + "\" you applied for access was rejected. Please check the reason for rejection in the application record。";
            notice.setContentEn(contentEn);
            String title = state ? "Reminder of approval for accessing data resources" : "Reminder of approval for accessing data resources";
            notice.setTitle(title);

            String titleEn = state ? "Reminder of approval of application for access to data resources" : "Application for access to data resources approval rejection reminder";
            notice.setTitleEn(titleEn);
            notice.setIs_read("1");
            notice.setCreateTime(LocalDateTime.now());
            notice.setResourcesId(approveObj.getId());
            mongoTemplate.insert(notice);


            Map<String, Object> attachment = new HashMap<>();
            attachment.put("name", approveObj.getApplyAuthor());
            attachment.put("resourceName", approveObj.getName());
            String url = state ? instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + approveObj.getResourcesId() : instdbUrl.getCallHost() + "/center/AccessApproval";
            attachment.put("url", url);
            ToEmail toEmail = new ToEmail();
            toEmail.setTos(new String[]{approveObj.getApplyEmail()});
            if (state) {
                asyncDeal.send(toEmail, attachment, EmailModel.EMAIL_APPLY_ADOPT());
            } else {
                asyncDeal.send(toEmail, attachment, EmailModel.EMAIL_APPLY_REJECT());
            }
        }
        return ResultUtils.success("APPROVAL_SUCCESS");
    }

    @Override
    public Result getResourceAccess(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ResourceAccess one = mongoTemplate.findOne(query, ResourceAccess.class);
        Map map = new HashMap();
        ConsumerDO byEmailAccounts = userRepository.findByEmailAccounts(one.getApplyEmail());
        map.put("name", one.getApplyAuthor());
        map.put("org", byEmailAccounts.getOrgChineseName());
        map.put("email", one.getApplyEmail());
        map.put("telephone", byEmailAccounts.getTelephone());
        one.setApplyAuthorInfo(map);
        return ResultUtils.success(one);
    }

    @Override
    public void downloadAccessTemplate(String token, HttpServletResponse response) {
        String username = tokenCache.getIfPresent(token);
        Assert.isTrue(StringUtils.isNotBlank(username), I18nUtil.get("ENVIRONMENT_EXCEPTION"));

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        final String url = "/data/accessTemplate.xml";
        File file = FileUtils.getResourceFile(url);
        if (!file.exists()) {
            log.error(url + "file cannot be found，file cannot be found！");
        } else {
            FileUtils.downloadFile(file.getPath(), response,"Dataset Request Access Sample Template");
        }
    }


}
