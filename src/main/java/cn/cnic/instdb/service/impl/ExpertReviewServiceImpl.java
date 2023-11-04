package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.*;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ExpertReviewService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
* @Auther  wdd
* @Date  2021/3/10 23:39
* @Desc  Resource Publishing
*/
@Service
@Slf4j
public class ExpertReviewServiceImpl implements ExpertReviewService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private AsyncDeal asyncDeal;

    private final Cache<String, String> check = CaffeineUtil.getCHECK();


    @Override
    public Result saveExpert(String username, String email, String org) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(org)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        if (StringUtils.isNotBlank(email)) {
            if (!CommonUtils.isEmail(email)) {
                return ResultUtils.errorBefore(email, "EMAIL_INCORRECT");
            }
            Query query = new Query();
            query.addCriteria(Criteria.where("email").is(email));
            Expert expertData = mongoTemplate.findOne(query, Expert.class);
            if(null != expertData){
                return ResultUtils.errorBefore(email, "CONTENT_ALREADY_EXISTS");
            }
        }

        Expert expert = new Expert();
        expert.setEmail(email);
        expert.setUsername(username);
        expert.setOrg(org);
        Expert save = mongoTemplate.save(expert);
        return ResultUtils.success("ADD_SUCCESS",save.getId());
    }

    @Override
    public Result updateExpert(Expert expert) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(expert.getId()));
        Expert expertData = mongoTemplate.findOne(query, Expert.class);
        if (null == expertData) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        Update update = new Update();

        if (StringUtils.isNotBlank(expert.getUsername())) {
            update.set("username", expert.getUsername());
        }
        if (StringUtils.isNotBlank(expert.getEmail())) {
            update.set("email", expert.getEmail());
        }
        if (StringUtils.isNotBlank(expert.getOrg())) {
            update.set("org", expert.getOrg());
        }

        mongoTemplate.updateFirst(query,update, Expert.class);
        return ResultUtils.success();
    }

    @Override
    public Result getExpert(String username) {
        Query query = new Query();
        query.limit(10);
        query.with(Sort.by(Sort.Direction.DESC, "username"));
        if (StringUtils.isNotBlank(username)) {
            query.addCriteria(Criteria.where("username").regex(username));
        }
        List<Expert> experts = mongoTemplate.find(query, Expert.class);
        return ResultUtils.success(experts);
    }

    @Override
    public Result createPrivacyLink(String resourcesId, List<String> ids) {

        if (StringUtils.isBlank(resourcesId) || null == ids || 0 == ids.size()) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Criteria criteria = Criteria.where("_id").is(resourcesId);
        Query query = new Query();
        query.addCriteria(criteria);
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error("RESOURCE_DOES_NOT_EXIST");
        }

        //Approved items are not allowed for further evaluation
        if (!Constant.Approval.PENDING_APPROVAL.equals(resourcesManage.getStatus())) {
            return ResultUtils.error("RESOURCE_APPROVED_REVIEW");
        }

        Query queryApproval = new Query();
        queryApproval.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        queryApproval.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        Approve approve = mongoTemplate.findOne(queryApproval, Approve.class);
        if (null == approve) {
            return ResultUtils.error("APPROVED_REVIEW_NOT_EXIST");
        }

        List<ResourcesReview> list = new ArrayList<>();
        //Generate evaluation information
        for (String id : ids) {
            Criteria criteriaExpert = Criteria.where("_id").is(id);
            Query queryExpert = new Query();
            queryExpert.addCriteria(criteriaExpert);
            Expert expert = mongoTemplate.findOne(queryExpert, Expert.class);
            if(null == expert){
                return ResultUtils.error("EXPERT_GET_INFO");
            }


            Query queryEvaluat = new Query();
            queryEvaluat.addCriteria(Criteria.where("expertId").is(id));
            queryEvaluat.addCriteria(Criteria.where("resourcesId").is(resourcesId));
           // queryEvaluat.addCriteria(Criteria.where("status").is(Constant.Approval.PENDING_APPROVAL));
            ResourcesReview review = mongoTemplate.findOne(queryEvaluat, ResourcesReview.class);
            if (null != review) {
                if(Constant.Approval.PENDING_APPROVAL.equals(review.getStatus())){
                    //Calculate link expiration time
                    Date tomorrowday = DateUtils.tomorrowdayByDate(7 * 24, DateUtils.LocalDateTimeasDate(review.getCreateTime()));
                    //Determine if the link expiration time has expired
                    String dateString = DateUtils.getDateString(tomorrowday);
                    boolean effectiveDate = DateUtils.isEffectiveDate(new Date(), dateString);
                    //Determine if it is invalid  Determine if it is invalid
                    if (!effectiveDate) {
                        continue;
                    }
                }else {
                    continue;
                }
            }

            ResourcesReview resourcesReview = new ResourcesReview();
            resourcesReview.setResourcesId(resourcesId);
            resourcesReview.setApprovalId(approve.getId());
            resourcesReview.setExpertId(expert.getId());
            resourcesReview.setUsername(expert.getUsername());
            resourcesReview.setEmail(expert.getEmail());
            resourcesReview.setOrg(expert.getOrg());
            resourcesReview.setCreateTime(LocalDateTime.now());
            resourcesReview.setStatus(Constant.Approval.PENDING_APPROVAL);
            String uuid = CommonUtils.generateUUID();
            resourcesReview.setId(uuid);
            long stringTime = System.currentTimeMillis();
            String code = SMS4.Encryption(uuid+"&" + resourcesId + "&" + stringTime);
            String url = instdbUrl.getCallHost() + instdbUrl.getReview() + code;
            resourcesReview.setUrl(url);
            list.add(resourcesReview);

            if(StringUtils.isNotBlank(expert.getEmail())){
                Map<String, Object> attachment = new HashMap<>();
                attachment.put("resourceName", resourcesManage.getName());
                attachment.put("url", url);
                attachment.put("email", expert.getEmail());
                attachment.put("name", expert.getUsername());
                ToEmail toEmail = new ToEmail();
                toEmail.setTos(new String[]{expert.getEmail()});
                asyncDeal.send(toEmail,attachment, EmailModel.EMAIL_RESOURCES_REVIEW());
            }
        }
        if (null != list && list.size() > 0) {
            mongoTemplate.insertAll(list);
        }
        return ResultUtils.success("RESOURCE_SEND_REVIEW");

    }

    @Override
    public Result reCreatePrivacyLink(String resourcesId, String id) {

        Query query1 = new Query();
        query1.addCriteria(Criteria.where("_id").is(resourcesId));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query1, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error("RESOURCE_DOES_NOT_EXIST");
        }
        //Approved items are not allowed for further evaluation
        if (!Constant.Approval.PENDING_APPROVAL.equals(resourcesManage.getStatus())) {
            return ResultUtils.error("RESOURCE_APPROVED_REVIEW");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        ResourcesReview review = mongoTemplate.findOne(query, ResourcesReview.class);
        if(null == review){
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if(!Constant.Approval.PENDING_APPROVAL.equals(review.getStatus())){
            return ResultUtils.error("RESOURCE_REVIEW_CREATED");
        }


        //Calculate link expiration time
        Date tomorrowday = DateUtils.tomorrowdayByDate(7 * 24, DateUtils.LocalDateTimeasDate(review.getCreateTime()));
        //Determine if the link expiration time has expired
        String dateString = DateUtils.getDateString(tomorrowday);
        boolean effectiveDate = DateUtils.isEffectiveDate(new Date(), dateString);
        if (effectiveDate) {
            return ResultUtils.error("LINK_INVALID");
        }

//        long stringTime = System.currentTimeMillis();
//        String code = SMS4.Encryption(review.getId()+"&" + resourcesId + "&" + stringTime);
//        String url = instdbUrl.getCallHost() + instdbUrl.getReview() + code;
//
//        Update update = new Update();
//        update.set("url",url);
//        mongoTemplate.updateFirst(query,update, ResourcesReview.class);

        if(StringUtils.isNotBlank(review.getEmail())){
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("resourceName", resourcesManage.getName());
            attachment.put("url", review.getUrl());
            attachment.put("email", review.getEmail());
            attachment.put("name", review.getUsername());
            ToEmail toEmail = new ToEmail();
            toEmail.setTos(new String[]{review.getEmail()});
            asyncDeal.send(toEmail,attachment, EmailModel.EMAIL_RESOURCES_REVIEW());
            return ResultUtils.success("RESOURCE_SEND_REVIEW");
        }
        return ResultUtils.error("FAILED");
    }


    @Override
    public Result resourcesReview(String status, String reason,String resourcesId, HttpServletRequest request, HttpServletResponse response) {

        if (StringUtils.isBlank(status) || StringUtils.isBlank(reason) || StringUtils.isBlank(resourcesId)) {
            return ResultUtils.error("APPROVE_REASON");
        }

        CommonUtils.setLangToReq(request);
        String ifPresent = check.getIfPresent(resourcesId + Constant.REVIEW);
        if (StringUtils.isBlank(ifPresent)) {
            return ResultUtils.error("LINK_ERROR");
        }
        String[] split = ifPresent.split("&");
        if (StringUtils.isBlank(split[1]) || StringUtils.isBlank(split[0])) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        String cookieVal = CommonUtils.getCookie(request, split[1]);
        if (StringUtils.isBlank(cookieVal)) {
            return ResultUtils.error("LINK_ERROR");
        } else {
            String decrypt = RSAEncrypt.decrypt(cookieVal);
            if (!decrypt.equals(split[1])) {
                return ResultUtils.error("CODE_ERROR");
            }
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(split[1]));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error(504, "RESOURCE_DOES_NOT_EXIST");
        }
        //Approved items are not allowed for further evaluation
        if (!Constant.Approval.PENDING_APPROVAL.equals(resourcesManage.getStatus())) {
            return ResultUtils.error("RESOURCE_APPROVED_REVIEW");
        }

        Query queryEvaluat = new Query();
        queryEvaluat.addCriteria(Criteria.where("_id").is(split[0]));
        ResourcesReview resourcesReview = mongoTemplate.findOne(queryEvaluat, ResourcesReview.class);
        if (null == resourcesReview) {
            return ResultUtils.error("RESOURCE_CLOSE_REVIEW");
        }

        //Evaluated items are not allowed to be evaluated again
        if (!Constant.Approval.PENDING_APPROVAL.equals(resourcesReview.getStatus()) && null != resourcesReview.getReviewTime()) {
            return ResultUtils.error("RESOURCE_REVIEW");
        }
        //Verify the validity period of the link
        String dateTimeString = DateUtils.getDateTimeString(resourcesReview.getCreateTime());
        if (!DateUtils.judgmentDate(dateTimeString, 7*24)) {
            return ResultUtils.error("RESOURCE_URL_REVIEW");
        }

        Update update = new Update();
        update.set("status",status);
        update.set("reason",reason);
        update.set("reviewTime",LocalDateTime.now());
        //Link failure
        update.set("deadlineStatus",Constant.Approval.OFFLINE);
        mongoTemplate.findAndModify(queryEvaluat, update,new FindAndModifyOptions().returnNew(true).upsert(true), ResourcesReview.class);


        //Removecooklie
        Cookie cookie = CommonUtils.setCookie(split[1], "", 0, 1);
        response.addCookie(cookie);
        check.invalidate(resourcesId + Constant.REVIEW);

        Query queryApprove = new Query();
        queryApprove.addCriteria(Criteria.where("resourcesId").is(split[1]));
        queryApprove.addCriteria(Criteria.where("claimStatus").is(Constant.Approval.YES));
        queryApprove.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        Approve approve = mongoTemplate.findOne(queryApprove, Approve.class);

        //Send an email to the claimant after the review is completed
        if (null != approve && StringUtils.isNotBlank(approve.getClaimEmail())) {
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("resourceName", resourcesManage.getName());
            attachment.put("name", approve.getClaimAuthor());
            attachment.put("url", instdbUrl.getCallHost() + "/center/approve");
            ToEmail toEmail = new ToEmail();
            toEmail.setTos(new String[]{approve.getClaimEmail()});
            asyncDeal.send(toEmail, attachment, EmailModel.EMAIL_RESOURCES_REVIEW_FINISH());
        }

        return ResultUtils.success("RESOURCE_REVIEW_MSG");
    }

    @Override
    public Result getResourcesReview(String resourcesId) {
        Map<String, Object> map = new HashMap<>();
        Criteria criteria = Criteria.where("_id").is(resourcesId);
        Query query = new Query();
        query.addCriteria(criteria);
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error(504, "RESOURCE_DOES_NOT_EXIST");
        }
        Query queryEvaluat = new Query();
        queryEvaluat.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        List<ResourcesReview> resourcesReviews = mongoTemplate.find(queryEvaluat, ResourcesReview.class);
        ArrayList<ResourcesReview> collect1 = new ArrayList<>();
        if (null != resourcesReviews && resourcesReviews.size() > 0) {
            collect1 = resourcesReviews.stream().sorted(Comparator.comparing(ResourcesReview::getCreateTime).reversed()).collect(Collectors.collectingAndThen(Collectors.toCollection(()
                    -> new TreeSet<>(Comparator.comparing(ResourcesReview::getExpertId))), ArrayList::new));

            for (ResourcesReview resourcesReview : resourcesReviews) {
                resourcesReview.setStatus(resourcesReview.getStatus().equals(Constant.Approval.PENDING_APPROVAL) ? "To be reviewed" : resourcesReview.getStatus().equals(Constant.Approval.ADOPT) ? "To be reviewed" : "To be reviewed");
                //Calculate link expiration time
                Date tomorrowday = DateUtils.tomorrowdayByDate(7 * 24, DateUtils.LocalDateTimeasDate(resourcesReview.getCreateTime()));
                resourcesReview.setDeadline(DateUtils.DateasLocalDateTime(tomorrowday));
                //Determine if the link expiration time has expired
                String dateString = DateUtils.getDateString(tomorrowday);
                boolean effectiveDate = DateUtils.isEffectiveDate(new Date(), dateString);
                resourcesReview.setDeadlineStatus(effectiveDate ? Constant.Approval.YES : Constant.Approval.NO);
                //In case of failure and evaluation  In case of failure and evaluation In case of failure and evaluation
                if (resourcesReview.getDeadlineStatus().equals(Constant.Approval.YES)) {
                    if ("To be reviewed".equals(resourcesReview.getStatus())) {
                        resourcesReview.setStatus("Not Reviewed");
                        if (collect1.size() > 0) {
                            for (ResourcesReview collect : collect1) {
                                if (resourcesReview.getId().equals(collect.getId())) {
                                    resourcesReview.setInvitable("yes");
                                }
                            }
                        }
                    }
                }
            }

        }

        queryEvaluat.addCriteria(Criteria.where("status").ne(Constant.Approval.PENDING_APPROVAL));
        //Number of pending evaluations
        long count = mongoTemplate.count(queryEvaluat, ResourcesReview.class);
        map.put(Constant.Approval.PENDING_APPROVAL, count);
        map.put("count", resourcesReviews.size());
        map.put("list", resourcesReviews);
        return ResultUtils.success(map);
    }

    @Override
    public Result deleteResourcesReview(String id) {
        Criteria criteriaEvaluat = Criteria.where("_id").is(id);
        Query queryEvaluat = new Query();
        queryEvaluat.addCriteria(criteriaEvaluat);
        ResourcesReview resourcesReview = mongoTemplate.findOne(queryEvaluat, ResourcesReview.class);
        if (null == resourcesReview) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if (!Constant.Approval.PENDING_APPROVAL.equals(resourcesReview.getStatus())) {
            return ResultUtils.error("RESOURCE_REVIEW_DELETE");
        }
        mongoTemplate.remove(queryEvaluat,ResourcesReview.class);
        return ResultUtils.success("DELETE_SUCCESS");
    }
}
