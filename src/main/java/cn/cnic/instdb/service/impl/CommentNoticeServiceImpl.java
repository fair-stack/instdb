package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.commentNotice.Comment;
import cn.cnic.instdb.model.commentNotice.CommentVo;
import cn.cnic.instdb.model.commentNotice.Notice;
import cn.cnic.instdb.model.commentNotice.UserSendEmail;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.AuthService;
import cn.cnic.instdb.service.CommentNoticeService;
import cn.cnic.instdb.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Auther wdd
 * @Date 2021/3/23 20:23
 * @Desc Comment notification
 */
@Service
public class CommentNoticeServiceImpl implements CommentNoticeService {

    public static final String COLLECTION_NAME = "comment";


    @Resource
    private MongoUtil mongoUtil;

    @Autowired
    private MongoTemplate mongoTemplate;


    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private AuthService authService;

    @Override
    public PageHelper findAllNotice(Map<String, Object> condition) {
        String content = condition.get("content").toString();
        String type = condition.get("type").toString();
        String token = condition.get("token").toString();
        String startDate = condition.get("startDate").toString();
        String endDate = condition.get("endDate").toString();

        ConsumerDO consumerDO = authService.getUserBytoken(token);

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("NEED_TOKEN"));

        Query query = new Query();

        query.with(Sort.by(Sort.Direction.DESC, "createTime"));

        //If it is an administrator  If it is an administratorusername If it is an administratoradminIf it is an administrator
//        if(roles.contains(Constant.ADMIN)){
//            List<String> list = new ArrayList<>();
//            list.add(Constant.ADMIN);
//            list.add(consumerDO.getEmailAccounts());
//            query.addCriteria(Criteria.where("username").in(list));
//        }else {
            //Otherwise, only check your own information
            query.addCriteria(Criteria.where("username").is(consumerDO.getEmailAccounts()));
      //  }

        if(StringUtils.isNotBlank(content)){
            query.addCriteria(Criteria.where("content").regex(content));
        }
        if(StringUtils.isNotBlank(type)){
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            Criteria criteria = new Criteria();
            query.addCriteria(criteria.andOperator(Criteria.where("createTime").gte(DateUtils.getLocalDateTimeByString2(startDate)),
                    Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(endDate))));
        }else if(StringUtils.isNotBlank(startDate)){
            query.addCriteria(Criteria.where("createTime").gte(DateUtils.getLocalDateTimeByString2(startDate)));
        }else if(StringUtils.isNotBlank(endDate)){
            query.addCriteria(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(endDate)));
        }
        long count = mongoTemplate.count(query, Notice.class);
        mongoUtil.start(Integer.parseInt(condition.get("pageOffset").toString()), Integer.parseInt(condition.get("pageSize").toString()), query);
        List<Notice> notice = mongoTemplate.find(query, Notice.class);


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //Get to today's date  Get to today's date
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(new Date());
        cal1.add(Calendar.DATE, -60);
        String imptimeEnd = sdf.format(cal1.getTime());
        Query resourcesQuery = new Query();
        resourcesQuery.addCriteria(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(imptimeEnd)));
        mongoTemplate.remove(resourcesQuery, Notice.class);


        return mongoUtil.pageHelper(count,notice);
    }



    @Override
    public Result getNoticesNum(String token) {
        Query query = new Query();
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        if (null == consumerDO) {
            return ResultUtils.error("ENVIRONMENT_EXCEPTION");
        }
        query.addCriteria(Criteria.where("username").is(consumerDO.getEmailAccounts()));
//        List<String> roles = jwtTokenUtils.getRoles(token);
//        //If it is an administrator  If it is an administratorusername If it is an administratoradminIf it is an administrator
//        if(roles.contains(Constant.ADMIN)){
//            List<String> list = new ArrayList<>();
//            list.add(Constant.ADMIN);
//            list.add(consumerDO.getEmailAccounts());
//            query.addCriteria(Criteria.where("username").in(list));
//        }else {
//            //Otherwise, only check your own information
//            query.addCriteria(Criteria.where("username").is(consumerDO.getEmailAccounts()));
//        }
        query.addCriteria(Criteria.where("is_read").is("1"));
        long num = mongoTemplate.count(query, Notice.class);
        return ResultUtils.success(num);
    }

    @Override
    public Result getSendEmail(String token) {
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(consumerDO.getEmailAccounts()));
        UserSendEmail one = mongoTemplate.findOne(query, UserSendEmail.class);
        if(null == one){
            UserSendEmail sendEmail = new UserSendEmail();
            sendEmail.setEmailAccounts(consumerDO.getEmailAccounts());
            sendEmail.setWaitApproval(0);
            sendEmail.setApproved(0);
            sendEmail.setApprovalRejected(0);
            sendEmail.setApprovalRevocation(0);
            sendEmail.setVersionUp(0);
            UserSendEmail save = mongoTemplate.save(sendEmail);
            return ResultUtils.success(save);
        }
        return ResultUtils.success(one);
    }

    @Override
    public void setRead(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("is_read", "0");
        mongoTemplate.updateFirst(query, update, Notice.class);
    }

    @Override
    public Result setSendEmail(String token, int waitApproval,int approved, int approvalRejected, int approvalRevocation, int versionUp) {
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        String emailAccounts = consumerDO.getEmailAccounts();
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(emailAccounts));
        UserSendEmail one = mongoTemplate.findOne(query, UserSendEmail.class);
        UserSendEmail sendEmail = new UserSendEmail();
        sendEmail.setEmailAccounts(emailAccounts);
        sendEmail.setWaitApproval(waitApproval);
        sendEmail.setApproved(approved);
        sendEmail.setApprovalRejected(approvalRejected);
        sendEmail.setApprovalRevocation(approvalRevocation);
        sendEmail.setVersionUp(versionUp);
        if (null != one) {
            sendEmail.setId(one.getId());
        }
        mongoTemplate.save(sendEmail);
        return ResultUtils.success();
    }

    @Override
    public void setAllRead(String token) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        Query query = new Query();
        if(roles.contains(Constant.ADMIN)){
            List<String> list = new ArrayList<>();
            list.add(Constant.ADMIN);
            list.add(consumerDO.getEmailAccounts());
            query.addCriteria(Criteria.where("username").in(list));
        }else {
            //Otherwise, only check your own information
            query.addCriteria(Criteria.where("username").is(consumerDO.getEmailAccounts()));
        }
        Update update = new Update();
        update.set("is_read", "0");
        mongoTemplate.updateMulti(query, update, Notice.class);
    }

    @Override
    public void addNotice(String username, String type, String content,String resourcesId,String title) {
        Notice notice = new Notice();
        notice.setUsername(username);
        notice.setType(type);
        notice.setContent(content);
        notice.setTitle(title);
        notice.setIs_read("1");
        notice.setCreateTime(LocalDateTime.now());
        if(StringUtils.isNotBlank(resourcesId)) {
            notice.setResourcesId(resourcesId);
        }
        mongoTemplate.insert(notice);
    }


    @Override
    public PageHelper findAllComment(String resourcesId,Integer pageOffset, Integer pageSize) {
        List<CommentVo> list = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        long count = mongoTemplate.count(query, Comment.class);
        mongoUtil.start(pageOffset, pageSize, query);
        List<Comment> comments = mongoTemplate.find(query, Comment.class);
        if (null != comments && comments.size() > 0) {
            for (Comment comment : comments) {
                CommentVo vo = new CommentVo();
                BeanUtils.copyProperties(comment, vo);
                Query queryF = new Query();
                queryF.addCriteria(Criteria.where("parentId").is(comment.getId()));
                List<CommentVo> children = mongoTemplate.find(queryF, CommentVo.class, COLLECTION_NAME);
                vo.setChildren(children);
                list.add(vo);
            }
        }

        return mongoUtil.pageHelper(count, list);

    }

    @Override
    public void addComment(String token,String resourcesId,String content) {
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        Comment comment = new Comment();
        comment.setAvatar(consumerDO.getAvatar());
        comment.setContent(content);
        comment.setResourcesId(resourcesId);
        comment.setEmailAccounts(consumerDO.getEmailAccounts());
        comment.setUsername(consumerDO.getName());
        //Define the relationship between comments and replies as parent-child level Define the relationship between comments and replies as parent-child level，Define the relationship between comments and replies as parent-child level，Define the relationship between comments and replies as parent-child level【-1】，Define the relationship between comments and replies as parent-child level
        comment.setParentId("-1");
        comment.setCreateTime(LocalDateTime.now());
        mongoTemplate.insert(comment);

        Criteria criteria = Criteria.where("_id").is(resourcesId);
        Query query = new Query();
        query.addCriteria(criteria);
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);

        //Generate notifications
        if(null != resourcesManage && StringUtils.isNotBlank(resourcesManage.getPublish().getEmail())) {
           // addNotice(resourcesManage.getPublish().getEmail(), Constant.Comment.COMMENT,consumerDO.getName() +"Commented on your data resource《"+resourcesManage.getName()+"》",resourcesManage.getId(),"Commented on your data resource");
        }
    }

    @Override
    public void replyToComments(String token,String commentId, String content) {
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        Comment comment = new Comment();
        comment.setAvatar(consumerDO.getAvatar());
        comment.setContent(content);
        comment.setEmailAccounts(consumerDO.getEmailAccounts());
        comment.setUsername(consumerDO.getName());
        //Define the relationship between comments and replies as parent-child level Define the relationship between comments and replies as parent-child level，Define the relationship between comments and replies as parent-child level，Define the relationship between comments and replies as parent-child level【-1】，Define the relationship between comments and replies as parent-child level
        comment.setParentId(commentId);
        comment.setCreateTime(LocalDateTime.now());
        mongoTemplate.insert(comment);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(commentId));
        Comment comments = mongoTemplate.findOne(query, Comment.class);
        if (null != comments) {
            //Obtain relevant resource information
            Query queryResources = new Query();
            Criteria criteria = Criteria.where("_id").is(comments.getResourcesId());
            queryResources.addCriteria(criteria);
            ResourcesManage resourcesManage = mongoTemplate.findOne(queryResources, ResourcesManage.class);
            //Generate notifications
            if (null != resourcesManage && StringUtils.isNotBlank(comments.getEmailAccounts())) {
              //  addNotice(comments.getEmailAccounts(), Constant.Comment.REPLY, consumerDO.getName() + "I replied to you on《" + resourcesManage.getName() + "》I replied to you on【" + comments.getContent() + "】", resourcesManage.getId(),"I replied to you on");
            }
        }
    }

//    @Override
//    public List<Reply> findReply(String commentId) {
//        Query query = new Query();
//        query.addCriteria(Criteria.where("commentId").is(commentId));
//        List<Reply> reply = mongoTemplate.find(query, Reply.class);
//        return reply;
//    }


}
