package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.Approve;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.resources.ResourcesReview;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.Restrict;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.model.system.ValuesResult;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.CommentNoticeService;
import cn.cnic.instdb.service.MySettingService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class MySettingServiceImpl implements MySettingService {

    private final Cache<String, String> check = CaffeineUtil.getCHECK();
    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();
    private final Cache<String, List<String>> emailToken = CaffeineUtil.getEmailToken();
    private final Cache<String, String> captchaCache = CaffeineUtil.getCaptcha();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private JwtTokenUtils jwtTokenUtils;
    @Resource
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private AsyncDeal asyncDeal;

    @Resource
    private CommentNoticeService commentNoticeService;



    @Override
    public String checkPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return CommonUtils.WEAK;
        }
        return CommonUtils.checkPassword(password);
    }

    @Override
    public String verification(String token) {
        Optional<ConsumerDO> byId = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token));
        if (byId.isPresent()) {
            if (null != byId.get() && StringUtils.isNotBlank(byId.get().getPassword())) {
                return checkPassword(RSAEncrypt.decrypt(byId.get().getPassword()));
            } else if (null != byId.get() && StringUtils.isBlank(byId.get().getPassword()) && "Technology Cloud".equals(byId.get().getAddWay())) {
                return "Password not set";
            }
        }
        return CommonUtils.WEAK;
    }

    @Override
    public Result pwdEmail(String email, String name, String randomStr,String captcha,HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        String token = jwtTokenUtils.getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            String username = jwtTokenUtils.getUsernameFromToken(token);
            if (StringUtils.isNotBlank(username)) {
                email = jwtTokenUtils.getUsernameFromToken(token);
            }
        }
        String ipAddr = IPUtil.getIpAddr(request);
        //Email verification
        if (!CommonUtils.isEmail(email)) {
            return ResultUtils.error("EMAIL_INCORRECT");
        }

        if(StringUtils.isNotBlank(captcha) &&StringUtils.isNotBlank(randomStr)){
            int captchaCode = ImgCodeCheck.checkCode(captcha, randomStr);
            if (400 == captchaCode) {
                return ResultUtils.error("CAPTCHA_NULL");
            } else if (403 == captchaCode) {
                return ResultUtils.error(302,"CAPTCHA_NO");
            }
        }


        Query emailAccounts = new Query().addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(emailAccounts, ConsumerDO.class);
        if (user == null) {
            return ResultUtils.error("EMAIL_ERROR");
        }

        String ifPresent = captchaCache.getIfPresent("pwdEmail" + email);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(ifPresent)) {
            return ResultUtils.error("EMAILRESTRICT_MAILBOXES");
        }

        int type = 0;
        if (StringUtils.isNotEmpty(name)) {
            if (!user.getName().equals(name)) {  //Forgot password
                return ResultUtils.error("EMAIL_ERROR");
            }
            type = 1;
        }
        if (user.getState() == 0 || user.getState() == 3) {
            return ResultUtils.error("EMAIL_FORBIDDEN");
        }
        String current = DateUtils.getCurrentDateTimeString();
        Restrict re = judgePawCount(ipAddr, current, type);
        if (re.getResult()) {
            return ResultUtils.error("RETRIEVE_LIMIT");
        } else {
            Restrict result = judgePawCount(email, current, type);
            if (result.getResult()) {
                return ResultUtils.error("RETRIEVE_LIMIT");
            } else {
                // IP + email Limit the number of times+1
                mongoTemplate.save(re);
                mongoTemplate.save(result);
            }
        }
        long stringTime = System.currentTimeMillis();
        String code = SMS4.Encryption("pwdBack&" + email + "&" + stringTime);
        Map<String, Object> param = new HashMap<>();
        param.put("name", user.getName());
        param.put("url", instdbUrl.getCallHost() + instdbUrl.getResetPassword() + code);
        ToEmail toEmail = new ToEmail();
        toEmail.setTos(new String[]{user.getEmailAccounts()});
        asyncDeal.send(toEmail, param, EmailModel.EMAIL_PASS());
        captchaCache.put("pwdEmail"+email,email);
        return ResultUtils.success("SEND_EMAIL");
    }

    private Restrict judgePawCount(String main, String date, int type) {
        boolean result = false;
        Restrict restrict = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("main")
                .is(main).and("date").is(date).and("type").is(type)), Restrict.class);
        if (restrict != null) {
            long count = restrict.getCount();
            if (count >= 3) {
                result = true;
            } else {
                restrict.setCount(restrict.getCount() + 1);
            }
        } else {
            restrict = new Restrict();
            restrict.setCount(1);
            restrict.setCreateTime(new Date());
            restrict.setMain(main);
            restrict.setType(type);
            restrict.setDate(date);
        }
        restrict.setResult(result);
        return restrict;
    }

    @Override
    public Result updatePwd(String password, String confirmPassword, HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        String id = request.getSession().getId();
        String ifPresent = check.getIfPresent(id);
        if (ifPresent == null) {
            return ResultUtils.error("LINK_ERROR");
        }
        if (StringUtils.isBlank(password) || StringUtils.isBlank(confirmPassword)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        String decrypt_password = RSAEncrypt.decrypt(password);
        String decrypt_confirmPassword = RSAEncrypt.decrypt(confirmPassword);

        if (!decrypt_password.equals(decrypt_confirmPassword)) {
            return ResultUtils.error("PWD_NOT_MATCH");
        }
        if (!CommonUtils.passVerify(decrypt_password)) {
            return ResultUtils.error("PWD_STRENGTH");
        }

        String[] split = ifPresent.split("&");
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(split[0]));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null == user) {
            return ResultUtils.error("USER_UNREGISTERED");
        }
        Update update = new Update();
        update.set("password", password);
        mongoTemplate.upsert(query, update, UserServiceImpl.COLLECTION_NAME);
        //Clear account online users
        List<String> tokenList = emailToken.getIfPresent(user.getEmailAccounts());
        if (null != tokenList && tokenList.size() > 0) {
            for (String t : tokenList) {
                tokenCache.invalidate(t);
            }
            emailToken.invalidate(user.getEmailAccounts());
        }
        //Clear Activation
        check.put(split[0] + split[1], split[1]);
        check.invalidate(id);
        return ResultUtils.success("PWD_UPDATE");

    }

    @Override
    public Result passActivation(String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(code)) {
            return ResultUtils.error("CODE_ERROR");
        }
        String decrypt = SMS4.Decrypt(code);
        if (StringUtils.isBlank(decrypt)) {
            return ResultUtils.error("CODE_FORMAT_ERROR");
        }
        if(!decrypt.contains("&")){
            return ResultUtils.error("CODE_ERROR");
        }
        String[] split = decrypt.split("&");
        String email = split[1];
        String time = split[2];
        if(time.trim().equals("")){
            return ResultUtils.error("CODE_TIME_ERROR");
        }
        Long activationTime = Long.valueOf(time);
        if(activationTime.longValue() < DateUtils.yesterday(24)){
            return ResultUtils.error("LINK_INVALID");
        }
        String chick = check.getIfPresent(email + time);
        if (chick != null) {
            return ResultUtils.error("LINK_USAGE");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (user == null || user.getState() == 0) {
            return ResultUtils.error("EMAIL_FORBIDDEN");
        }
        String id = request.getSession().getId();
        String ifPresent = check.getIfPresent(id);
        if (ifPresent == null) {
            check.put(id, email + "&" + time);
        }
        return ResultUtils.success("MAILBOX_VERIFICATION");
    }

    @Override
    public Result expertReview(String code, HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (StringUtils.isBlank(code)) {
            return ResultUtils.error("CODE_ERROR");
        }
        String decrypt = SMS4.Decrypt(code);
        if (StringUtils.isBlank(decrypt)) {
            return ResultUtils.error("CODE_FORMAT_ERROR");
        }
        if (!decrypt.contains("&")) {
            return ResultUtils.error("CODE_ERROR");
        }
        String[] split = decrypt.split("&");
        if(split.length!=3){
            return ResultUtils.error("CODE_ERROR");
        }
        String time = split[2];
        if (time.trim().equals("")) {
            return ResultUtils.error("CODE_TIME_ERROR");
        }
        Long activationTime = Long.valueOf(time);
        if(activationTime.longValue() < DateUtils.yesterday(7*24)){
            return ResultUtils.error("RESOURCE_URL_REVIEW");
        }


        String resourcesId = split[1];
        String reviewId = split[0];

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(split[1]));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (resourcesManage == null) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if (!Constant.Approval.PENDING_APPROVAL.equals(resourcesManage.getStatus())) {
            return ResultUtils.error("RESOURCE_APPROVED_REVIEW");
        }

        Query query1 = new Query();
        query1.addCriteria(Criteria.where("_id").is(split[0]));
        ResourcesReview resourcesReview = mongoTemplate.findOne(query1, ResourcesReview.class);
        if (null == resourcesReview) {
            return ResultUtils.error("RESOURCE_CLOSE_REVIEW");
        }
        if (null != resourcesReview && !resourcesReview.getStatus().equals(Constant.Approval.PENDING_APPROVAL)) {
            return ResultUtils.error("RESOURCE_CLOSE_REVIEW");
        }

        String ifPresent = check.getIfPresent(resourcesId + Constant.REVIEW);
        if (StringUtils.isBlank(ifPresent)) {
            check.put(resourcesId + Constant.REVIEW, reviewId + "&" + resourcesId);
        }

        String token = CommonUtils.getCookie(request, resourcesId);
        if (StringUtils.isBlank(token)) {
            //SavecookSave Save
            Cookie cookie = CommonUtils.setCookie(resourcesId, RSAEncrypt.encrypt(resourcesId), 24 * 3600, 1);
            response.addCookie(cookie);
        }
        return ResultUtils.success(resourcesId);

//        //Frontend password modification page
//        String html = "<script type='text/javascript'>location.href='" + instdbUrl.getCallHost()+instdbUrl.getResourcesAddress() + resourcesManage.getId()+ "-3';</script>";
//        //SavecookSave Save
//        Cookie cookie = CommonUtils.setCookie(split[1], split[1], 24*3600, 1);
//        response.addCookie(cookie);
//        response.getWriter().print(html);
//        return;
    }


    private void returnRes(HttpServletResponse response, String message) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("code", 500);
        paramMap.put("message", message);
        errorMsg(response, paramMap);
    }


    private void errorMsg(HttpServletResponse response, Map param) {
        OutputStream out = null;
        try {
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.setCharacterEncoding("utf-8");
            response.setContentType("text/json");
            out = response.getOutputStream();
            out.write(JSON.toJSONString(param).getBytes(StandardCharsets.UTF_8));
            out.flush();
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    private void checkReviewCode(String code, HttpServletResponse response) {
        if (StringUtils.isBlank(code)) {
            returnRes(response, "code errorInternational!");
            return;
        }

        String decrypt = SMS4.Decrypt(code);
        if (decrypt == null) {
            returnRes(response, "code errorInternational!");
            return;
        }
        if (!decrypt.contains("&")) {
            returnRes(response, "code errorInternational!");
            return;
        }
        String[] split = decrypt.split("&");
        if(split.length!=3){
            returnRes(response, "errorInternational");
            return;
        }
        String time = split[2];
        if (time.trim().equals("")) {
            returnRes(response, "code errorInternational!");
            return;
        }
        Long activationTime = Long.valueOf(time);
        if(activationTime.longValue() < DateUtils.yesterday(7*24)){
            returnRes(response, "Link broken!");
        }
    }

    @Override
    public String spareEmail(String token) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        if (Objects.isNull(consumerDO.getSpareEmail())) {
            return "Unbound";
        } else {
            return consumerDO.getSpareEmail();
        }

    }

    @Override
    public Result setEmail(String token, String spareEmail) {
        if (StringUtils.isBlank(spareEmail) || !CommonUtils.isEmail(spareEmail)) {
            return ResultUtils.error("EMAIL_INCORRECT");
        }
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        if(consumerDO.getEmailAccounts().equals(spareEmail)){
            return ResultUtils.error("EMAIL_NOT_MAIN");
        }
        consumerDO.setSpareEmail(spareEmail);
        userRepository.save(consumerDO);
        return ResultUtils.success("SET");

    }

    @Override
    public Result getOverview(String token) {
        Map<String, Object> map = new HashMap<>();
        List<String> roles = jwtTokenUtils.getRoles(token);
        String username = jwtTokenUtils.getUsernameFromToken(token);
        //If it's not for the administrator, check the records that need to be approved by oneself
        //Administrators can see all the data
        Query query = new Query();
        if (!roles.contains(Constant.ADMIN)) {
            query.addCriteria(Criteria.where("approvalEmail").is(username));
        }
        query.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.PENDING_APPROVAL));
        long count = mongoTemplate.count(query, Approve.class);
        map.put("ApproveNum",count);
        //Get the number of my messages
        Result noticesNum = commentNoticeService.getNoticesNum(token);
        map.put("noticesNum",noticesNum.getData());


        LocalDateTime localDateTimeByString2 = DateUtils.getLocalDateTimeByString2(DateUtils.getDateToString(DateUtils.yesterday(24)));
        LocalDateTime localDateTime = localDateTimeByString2.plusDays(1);

        //New data resources added yesterday
        Query queryResources = new Query();
        queryResources.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        Criteria criteria = new Criteria();
        queryResources.addCriteria(criteria.andOperator(Criteria.where("approveTime").gte(localDateTimeByString2),
                Criteria.where("approveTime").lt(localDateTime)));
        map.put("ResourcesNum",mongoTemplate.count(queryResources,ResourcesManage.class));


        //New users added yesterday
        Query queryUser = new Query();
        queryUser.addCriteria(Criteria.where("state").is(1));
        Criteria criteria1 = new Criteria();
        queryUser.addCriteria(criteria1.andOperator(Criteria.where("createTime").gte(localDateTimeByString2),
                Criteria.where("createTime").lt(localDateTime)));
        map.put("UserNum",mongoTemplate.count(queryUser, ConsumerDO.class));

        return ResultUtils.success(map);
    }

    @Override
    public Result getStatisticsResources(String token) {
        Map<String, Object> map = new HashMap<>();
        List<String> roles = jwtTokenUtils.getRoles(token);
        //If it's not the administrator, it won't be returned
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.success(map);
        }
        List<AggregationOperation> aggList = new ArrayList<>();
        aggList.add(Aggregation.match(Criteria.where("status").is(Constant.Approval.ADOPT)));
        aggList.add(Aggregation.match(Criteria.where("approveTime").lte(DateUtils.getLocalDateTimeByString2(DateUtils.getDateString(new Date())))));
        aggList.add(Aggregation.project().and("approveTime").dateAsFormattedString("%Y-%m-%d").as("time"));
        aggList.add(Aggregation.group("time")  .max("time").as("name")
                .count().as("value"));
        aggList.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "name")));
        aggList.add(Aggregation.skip(0));
        aggList.add(Aggregation.limit(30));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<ValuesResult> document = mongoTemplate.aggregate(aggregation, "resources_manage", ValuesResult.class);

        List<Map<String, Object>> statistics7 = getStatistics(document, 7);
        map.put("statistics7",statistics7);
        List<Map<String, Object>> statistics30 = getStatistics(document, 30);
        map.put("statistics30",statistics30);
        return ResultUtils.success(map);
    }
    @Override
    public Result getStatisticsUser(String token) {
        Map<String, Object> map = new HashMap<>();
        List<String> roles = jwtTokenUtils.getRoles(token);
        //If it's not the administrator, it won't be returned
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.success(map);
        }
        List<AggregationOperation> aggList = new ArrayList<>();
        aggList.add(Aggregation.match(Criteria.where("state").is(1)));
        aggList.add(Aggregation.match(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2(DateUtils.getDateString(new Date())))));
        aggList.add(Aggregation.project().and("createTime").dateAsFormattedString("%Y-%m-%d").as("time"));
        aggList.add(Aggregation.group("time")  .max("time").as("name")
                .count().as("value"));
        aggList.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "name")));
        aggList.add(Aggregation.skip(0));
        aggList.add(Aggregation.limit(30));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<ValuesResult> document = mongoTemplate.aggregate(aggregation, "db_user", ValuesResult.class);

        List<Map<String, Object>> statistics7 = getStatistics(document, 7);
        map.put("statistics7",statistics7);
        List<Map<String, Object>> statistics30 = getStatistics(document, 30);
        map.put("statistics30",statistics30);
        return ResultUtils.success(map);
    }


    private List<Map<String,Object>>  getStatistics(AggregationResults<ValuesResult> valuesResults,int num ){
        //format date
        List<Date> lDate = DateUtils.getTimeInterval(new Date(), num);//Get all this weekdate
        List<String> list = new ArrayList<>();
        for (Date date : lDate) {
            list.add(DateUtils.getDateString(date));
        }
        String[] strs = list.toArray(new String[list.size()]);
        List<Map<String, Object>> objMapList = new ArrayList();
        for (String t : strs) {
            Map objMap = new HashMap();
            objMap.put("date", t);
            if (null != valuesResults.getMappedResults() && valuesResults.getMappedResults().size() > 0) {
                for (ValuesResult valuesResult : valuesResults) {
                    if (valuesResult.getName().equals(t)) {
                        objMap.put("data", valuesResult.getValue());
                    }
                }
            }
            if (!objMap.containsKey("data")) {
                objMap.put("data", 0);
            }
            objMapList.add(objMap);
        }
        return objMapList;
    }


}
