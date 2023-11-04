package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.cacheLoading.CacheLoading;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.exception.CommonException;
import cn.cnic.instdb.exception.ExceptionType;
import cn.cnic.instdb.model.login.*;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.rbac.ConsumerDTO;
import cn.cnic.instdb.model.rbac.Role;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.AuthService;
import cn.cnic.instdb.service.UserService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    public static final String COLLECTION_NAME = "db_user";

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, Integer> errorPwdCache = CaffeineUtil.getErrorPwd();

    private final Cache<String, String> errorPwdCheck = CaffeineUtil.getErrorPwdCheck();

    private final Cache<String, List<String>> emailToken = CaffeineUtil.getEmailToken();
    private final Cache<String, String> captchaCache = CaffeineUtil.getCaptcha();

    private final Cache<String, String> thirdParty = CaffeineUtil.getThirdParty();

    @Autowired
    private CacheLoading cacheLoading;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;


    @Autowired
    private UserService userService;

    @Autowired
    private InstdbUrl instdbUrl;

    @Autowired
    @Lazy
    private AsyncDeal asyncDeal;

    @Value("${data.activation.address}")
    private String activationAddress;
    @Resource
    private UserRepository userRepository;


//    public static void main(String[] args) {
////  System.out.println(RSAEncrypt.decrypt("Sn0JiO2ju0ZO515d2OXiybS1y0EyYZuszL0IXk+IWQX+cQtSsBSb++s78Snu0nF2bGWEdXpJQVYNyinfMpLcyijTmwLXZF0Rbsdj7qAZY9JCN8UOw1cA/Y8S/gfjYpSJuzKPpKU6DN5Rm/LSG8MwdISMEcFejRNN1MsLwYJhmty0="));
//    }

    @Override
    public Result login(String emailAccounts, String password, HttpServletResponse response, HttpServletRequest request, String unionId) {
        CommonUtils.setLangToReq(request);
        if (org.apache.commons.lang3.StringUtils.isEmpty(emailAccounts)
                || org.apache.commons.lang3.StringUtils.isEmpty(password)) {
            return ResultUtils.error("LOGIN_NOT_PARAMETER");
        }

        String email = RSAEncrypt.decrypt(emailAccounts);
        String pwd = RSAEncrypt.decrypt(password);

        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        String errorPwd = errorPwdCheck.getIfPresent(email);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(errorPwd)) {
            return ResultUtils.errorAfter(errorPwd, "LOGIN_LOCK");
        }
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null != user && 2 == user.getState() && null != user.getNextLoginTime()) {
            //Determine if the login time has exceeded24Determine if the login time has exceeded
            if (!DateUtils.judgmentDate(DateUtils.getDateTimeString(user.getNextLoginTime()), 24)) {
                Update update = new Update();
                update.unset("nextLoginTime");
                //Unlock Status
                update.set("state", 1);
                mongoTemplate.updateFirst(query, update, ConsumerDO.class);
                user.setState(1);
            }
        }

        if (user == null) {
            return ResultUtils.error("USER_UNREGISTERED");
        } else if (user.getState() == 0) {
            return ResultUtils.error(202, "USER_NOT_ACTIVE");
        } else if (user.getState() == 2) {
            return ResultUtils.errorAfter(DateUtils.getDateTimeString(user.getNextLoginTime()), "LOGIN_LOCK");
        } else if (user.getState() == 3) {
            return ResultUtils.error("EMAIL_FORBIDDEN");
        } else if ("Technology Cloud".equals(user.getAddWay()) && org.apache.commons.lang3.StringUtils.isBlank(user.getPassword())) {
            return ResultUtils.error("USER_IS_UMT");
        } else if ("CAS".equals(user.getAddWay()) && org.apache.commons.lang3.StringUtils.isBlank(user.getPassword())) {
            return ResultUtils.error("USER_IS_CAS");
        } else if ("WeChat".equals(user.getAddWay()) && org.apache.commons.lang3.StringUtils.isBlank(user.getPassword())) {
            return ResultUtils.error("USER_IS_WECHAT");
        } else if ("Shared Network".equals(user.getAddWay()) && org.apache.commons.lang3.StringUtils.isBlank(user.getPassword())) {
            return ResultUtils.error("USER_IS_ESCIENCE");
        } else {
            String userPassword = user.getPassword();
            if (StringUtils.isBlank(userPassword)) {
                return ResultUtils.error("USER_NOT_PASSWORD");
            }
            String decrypt = RSAEncrypt.decrypt(userPassword);
            if (decrypt.equals(pwd)) {
                Token token = generateToken(user);
                if (null == token) {
                    return ResultUtils.error("USER_NO_ROLE");
                }
                // cache
                tokenCache.put(token.getAccessToken(), email);
                List<String> ifPresent = emailToken.getIfPresent(email);
                if (null == ifPresent || ifPresent.size() == 0) {
                    emailToken.put(email, new ArrayList<String>() {{
                        add(token.getAccessToken());
                    }});
                } else {
                    ifPresent.add(token.getAccessToken());
                    emailToken.put(email, ifPresent);
                }
                String loginWay = Constant.LoginWay.SYS;
                if (StringUtils.isNotBlank(unionId)) {
                    String wechatId = user.getWechatId();
                    if (StringUtils.isNotEmpty(wechatId)) {
                        return ResultUtils.error("AU_WECHAT_ACC");
                    } else {
                        user.setWechatId(unionId);
                        mongoTemplate.save(user);
                    }
                    loginWay = Constant.LoginWay.WECHAT;
                }
                Cookie cookie = CommonUtils.setCookie("token", token.getAccessToken(), -1, 1);
                Cookie cookieTo = CommonUtils.setCookie("way", loginWay, -1, 1);
                response.addCookie(cookie);
                response.addCookie(cookieTo);
                return ResultUtils.success(token);
            } else {
                //The account password will be locked due to continuous input errors
                Integer ifPresent = errorPwdCache.getIfPresent(email + "_login");
                if (null != ifPresent) {
                    if (ifPresent >= 4) {

                        Date tomorrowday = DateUtils.tomorrowday(24);
                        errorPwdCache.put(email + "_login", ifPresent + 1);
                        //Save first24Save first
                        errorPwdCheck.put(email, DateUtils.getDateTimeString(tomorrowday));
                        Update update = new Update();
                        update.set("nextLoginTime", DateUtils.DateasLocalDateTime(tomorrowday));
                        //Locked state
                        update.set("state", 2);
                        mongoTemplate.updateFirst(query, update, ConsumerDO.class);
                        return ResultUtils.errorAfter(DateUtils.getDateTimeString(tomorrowday), "LOGIN_5_LOCK");
                    } else {
                        errorPwdCache.put(email + "_login", ifPresent + 1);
                    }
                } else {
                    errorPwdCache.put(email + "_login", 1);
                }
            }
        }
        return ResultUtils.error("LOGIN_USERNAME_ERROR");
    }


    private Token generateToken(ConsumerDO user) {
        //Obtain unauthorized paths
        Query query = new Query().addCriteria(Criteria.where("logo").in(user.getRoles()));
        List<Role> instdbRoleDOS = mongoTemplate.find(query, Role.class);
        Set<String> unAuthPath = new HashSet<>();
        for (Role instdbRoleDO : instdbRoleDOS) {
            unAuthPath.addAll(instdbRoleDO.getPathList());
        }
        String accessToken = jwtTokenUtils.generateToken(user, "accessToken", unAuthPath);
        String refreshToken = jwtTokenUtils.generateRefreshToken(user, "refreshToken", unAuthPath);
        Token token = new Token();
        token.setUserId(user.getId());
        token.setName(user.getName());
        token.setEmailAccounts(user.getEmailAccounts());
        token.setRoles(user.getRoles());
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        return token;
    }


    @Override
    public Result emailActivation(String code) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(code)) {
            return ResultUtils.error("CODE_IS_NULL");
        }
        String decrypt = SMS4.Decrypt(code);
        if (decrypt == null) {
            return ResultUtils.error("CODE_FORMAT_ERROR");
        }
        if (!decrypt.contains("&")) {
            return ResultUtils.error("CODE_ERROR");
        }
        String[] split = decrypt.split("&");
        String email = split[0];
        String time = split[1];
        if (time.trim().equals("")) {
            return ResultUtils.error("CODE_TIME_ERROR");
        }
        Long activationTime = Long.valueOf(time);
        if (activationTime.longValue() < DateUtils.yesterday(24)) {
            return ResultUtils.error("LINK_INVALID");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null == user) {
            return ResultUtils.error("USER_UNREGISTERED");
        }
        if (null != user && user.getState() == 1) {
            return ResultUtils.error("USER_IS_ACTIVE");
        }
        Update update = new Update();
        if (!StringUtils.isEmpty(user.getName())) {
            update.set("state", 1);
        }
        mongoTemplate.upsert(query, update, UserServiceImpl.COLLECTION_NAME);
        return ResultUtils.success("USER_ACTIVE_SUCCESS");
    }

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        String token = CommonUtils.getCookie(request, "token");
        String way = CommonUtils.getCookie(request, "way");
        String loginUrl = "";
        if (token == null) {
            loginUrl = instdbUrl.getCallHost();
        } else {
            request.getSession().invalidate();
            if (tokenCache != null) {
                tokenCache.invalidate(token);
            }
            Cookie cookie = CommonUtils.setCookie("token", "", -1, 1);
            Cookie cookieTo = CommonUtils.setCookie("way", "", -1, 1);
            response.addCookie(cookie);
            response.addCookie(cookieTo);
            if (way.equals(Constant.LoginWay.UMP)) {
                loginUrl = instdbUrl.getCasLogoutUrl();
                Object umt = judgeUmt(response);
                if (null == umt) {
                    return;
                }
                LoginConfig umtConf = (LoginConfig) umt;
                loginUrl = loginUrl + umtConf.getIndexPage();
            } else if (way.equals(Constant.LoginWay.CAS)) {
                Object umt = checkCas(response);
                if (null == umt) {
                    return;
                }
                LoginConfigCas umtConf = (LoginConfigCas) umt;
                loginUrl = umtConf.getCasServerUrlLogoutUrl() + "?service=" + instdbUrl.getCallHost();
                log.info("logouturl" + loginUrl);
            } else {
                loginUrl = instdbUrl.getCallHost();
            }
        }
        response.sendRedirect(loginUrl);
    }

    @Override
    public Result register(ConsumerDTO consumerDTO, HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        //Decryption verification
        String name = RSAEncrypt.decrypt(consumerDTO.getName());
        String emailAccounts = RSAEncrypt.decrypt(consumerDTO.getEmailAccounts());
        String password = RSAEncrypt.decrypt(consumerDTO.getPassword());
        String confirmPassword = RSAEncrypt.decrypt(consumerDTO.getConfirmPassword());

        if (!CommonUtils.isEmail(emailAccounts)) {
            return ResultUtils.error("EMAIL_INCORRECT");
        }
        if (!password.equals(confirmPassword)) {
            return ResultUtils.error("PWD_NOT_MATCH");
        }
        if (password.length() < 8) {
            return ResultUtils.error("PWD_LENGTH");
        }
        if (!CommonUtils.passVerify(password)) {
            return ResultUtils.error("PWD_STRENGTH");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(consumerDTO.getOrgChineseName())) {
            return ResultUtils.error("UNIT_IS_NULL");
        }

        int code = ImgCodeCheck.checkCode(consumerDTO.getCaptcha(), consumerDTO.getRandomStr());
        if (400 == code) {
            return ResultUtils.error("CAPTCHA_NULL");
        } else if (403 == code) {
            return ResultUtils.error(302, "CAPTCHA_NO");
        }

        ConsumerDO user = userService.getUserInfoByName(emailAccounts);
        if (user == null) {

            //Did you save the encrypted password
            consumerDTO.setPassword(consumerDTO.getPassword());
            consumerDTO.setName(name);
            consumerDTO.setEmailAccounts(emailAccounts);
            ConsumerDO consumerDO = new ConsumerDO();
            BeanUtils.copyProperties(consumerDTO, consumerDO);
            consumerDO.setCreateTime(LocalDateTime.now());
            consumerDO.setState(0);
            consumerDO.setAddWay("register");
            consumerDO.setOrgChineseName(consumerDTO.getOrgChineseName());

            List<String> listRoles = new ArrayList();
            listRoles.add(Constant.GENERAL);
            consumerDO.setRoles(listRoles);
            addUser(consumerDO, name);
        } else if (user.getState() == 0) {
            String dateTimeString = DateUtils.getDateTimeString(user.getCreateTime());
            if (DateUtils.judgmentDate(dateTimeString, 24)) {
                return ResultUtils.error("USER_NOT_ACTIVE");
            } else {
                user.setPassword(consumerDTO.getPassword());
                user.setCreateTime(LocalDateTime.now());
                addUser(user, name);
            }
        } else {
            return ResultUtils.error("USER_ALREADY_EXISTS");
        }
        return ResultUtils.successAfter("REGISTER_SUCCESS", emailAccounts);
    }


    @Override
    public Result registerSendEmail(String accounts, String name, String type) {
        String emailAccounts;
        try {
            emailAccounts = RSAEncrypt.decrypt(accounts);
            name = RSAEncrypt.decrypt(name);
        } catch (Exception e) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        String ifPresent = captchaCache.getIfPresent("registerSendEmail" + emailAccounts);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(ifPresent)) {
            return ResultUtils.error("EMAILRESTRICT_MAILBOXES");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(emailAccounts));
        if ("register".equals(type)) {
            query.addCriteria(Criteria.where("name").is(name));
        }

        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null == user) {
            return ResultUtils.error("USER_UNREGISTERED");
        } else if (null != user && 0 != user.getState()) {
            return ResultUtils.error("USER_IS_ACTIVE");
        } else {
            if ("login".equals(type)) {
                String decrypt = RSAEncrypt.decrypt(user.getPassword());
                if (!name.equals(decrypt)) {
                    return ResultUtils.error("LOGIN_USERNAME_ERROR");
                }
                name = user.getName();
            }
        }

        long stringTime = System.currentTimeMillis();
        String code = SMS4.Encryption(emailAccounts + "&" + stringTime);
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("name", name);
        attachment.put("email", emailAccounts);
        attachment.put("url", instdbUrl.getCallHost() + activationAddress + code);
        ToEmail email = new ToEmail();
        email.setTos(new String[]{emailAccounts});
        asyncDeal.send(email, attachment, EmailModel.EMAIL_REGISTER());
        captchaCache.put("registerSendEmail" + emailAccounts, emailAccounts);
        return ResultUtils.success();
    }


    private void addUser(ConsumerDO consumerDO, String name) {

        long stringTime = System.currentTimeMillis();
        String code = SMS4.Encryption(consumerDO.getEmailAccounts() + "&" + stringTime);
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("name", name);
        attachment.put("email", consumerDO.getEmailAccounts());
        attachment.put("url", instdbUrl.getCallHost() + activationAddress + code);
        ToEmail email = new ToEmail();
        email.setTos(new String[]{consumerDO.getEmailAccounts()});
        asyncDeal.send(email, attachment, EmailModel.EMAIL_REGISTER());
        mongoTemplate.save(consumerDO, COLLECTION_NAME);
    }

    @Override
    public Result getUserInfo(String token) {
        if (!jwtTokenUtils.validateToken(token)) {
            return ResultUtils.error("LOGIN_EXCEPTION");
        } else {
            String ifPresent = tokenCache.getIfPresent(token);
            if (ifPresent == null) {
                return ResultUtils.error("LOGIN_EXCEPTION");
            }
        }
        Token user = jwtTokenUtils.getToken(token);
        if (user == null) {
            return ResultUtils.error("LOGIN_EXCEPTION");
        }
        user.setAccessToken(token);
        return ResultUtils.success(user);
    }


    private Object judgeUmt(HttpServletResponse response) {
        Object umt = cacheLoading.loadingUmt();
        if (umt == null) { //Not added to technology cloud configuration
            Map<String, Object> param = new HashMap<>();
            param.put("code", 500);
            param.put("message", I18nUtil.get("USER_NO_UMT"));
            CommonUtils.errorMsg(response, param);
        }
        return umt;
    }

    private Object checkCas(HttpServletResponse response) {
        Object umt = cacheLoading.loadingCasLoginConf();
        if (umt == null) { //Not added to technology cloud configuration
            Map<String, Object> param = new HashMap<>();
            param.put("code", 500);
            param.put("message", I18nUtil.get("USER_NO_CAS"));
            CommonUtils.errorMsg(response, param);
        }
        return umt;
    }


    @Override
    public void umtLogin(HttpServletResponse response) {
        String loginUrl = instdbUrl.getCasLoginUrl();
        Object umt = judgeUmt(response);
        if (null == umt) {
            return;
        }
        LoginConfig umtConf = (LoginConfig) umt;
        try {
            response.sendRedirect(loginUrl + "redirect_uri=" + umtConf.getCallback() + "&client_id=" + umtConf.getAppKey());
            return;
        } catch (Exception e) {
            log.error("context", e);
        }
        return;
    }

    private Token umpUserDispose(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(username));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (user == null) {
            return null;
        }
        return generateToken(user);
    }

    private Token umpAdd(String realName, String email, String type) {
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO judge = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (judge != null) {
            throw new CommonException(ExceptionType.SYSTEM_ERROR);
        }
        ConsumerDO user = new ConsumerDO();
        user.setEmailAccounts(email);
        user.setCreateTime(LocalDateTime.now());
        user.setState(1);
        user.setAddWay(type);
        user.setName(realName);

        List<String> listRoles = new ArrayList();
        listRoles.add(Constant.GENERAL);
        user.setRoles(listRoles);
        ConsumerDO save = mongoTemplate.save(user, UserServiceImpl.COLLECTION_NAME);
        Token token = generateToken(save);
        return token;
    }

    @Override
    public void umtCallback(String code, HttpServletResponse response, HttpServletRequest request) throws IOException {
        //Technology Cloud Certification
        HttpClient httpClient = new HttpClient();
        Object umt = judgeUmt(response);
        if (null == umt) {
            return;
        }
        LoginConfig umtConf = (LoginConfig) umt;
        String param = "redirect_uri=" + umtConf.getCallback() + "&client_id=" + umtConf.getAppKey() + "&client_secret=" + umtConf.getAppSecret() + "&" + instdbUrl.getAuthParam() + code;

        String reData = httpClient.sendPost(instdbUrl.getAuthUrl(), param);
        if (org.apache.commons.lang3.StringUtils.isEmpty(reData)) {
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
        }
        Gson gson = new Gson();
        Map map = gson.fromJson(reData, Map.class);
        String userInfo = (String) map.get("userInfo");
        Map userMap = gson.fromJson(userInfo, Map.class);
        String username = (String) userMap.get("cstnetId");
        String realName = (String) userMap.get("truename");
        Token token = umpUserDispose(username);
        if (token == null) {
            token = umpAdd(realName, username, "Technology Cloud");
        }

        tokenCache.put(token.getAccessToken(), username);
        Cookie cookie = CommonUtils.setCookie("token", token.getAccessToken(), -1, 1);
        Cookie cookieTo = CommonUtils.setCookie("way", Constant.LoginWay.UMP, -1, 1);
        Cookie cookieTo1 = CommonUtils.setCookie("userId", token.getUserId(), -1, 1);
        response.addCookie(cookie);
        response.addCookie(cookieTo);
        response.addCookie(cookieTo1);

        HttpSession session = request.getSession();
        Object umtCallbackPreUrl = session.getAttribute("umtCallbackPreUrl");
        String url = umtConf.getIndexPage();
        if (null != umtCallbackPreUrl && StringUtils.isNotBlank(umtCallbackPreUrl.toString())) {
            url = umtCallbackPreUrl.toString();

        }
        String html = "<script type='text/javascript'>location.href='" + url + "';</script>";
        response.getWriter().print(html);
        return;
    }

    @Override
    public void casLogin(HttpServletResponse response) {

        Object umt = checkCas(response);
        if (null == umt) {
            return;
        }
        LoginConfigCas configCas = (LoginConfigCas) umt;
        try {
       response.sendRedirect(configCas.getCasServerUrlLogin() + "?service=" + configCas.getHomePage() + instdbUrl.getCasCallbackUrl());
         //     response.sendRedirect(configCas.getCasServerUrlLogin() + "?service=" + "http://127.0.0.1:8787" + instdbUrl.getCasCallbackUrl());
            return;
        } catch (Exception e) {
            log.error("context", e);
        }
        return;
    }

    @Override
    public void casCallback(AttributePrincipal principal, HttpServletResponse response, HttpServletRequest request) throws IOException {
        Object o = checkCas(response);
        if (null == o) {
            return;
        }
        LoginConfigCas configCas = (LoginConfigCas) o;


        Map<String, Object> attributes = principal.getAttributes();
        if (null == attributes || 0 == attributes.size()) {
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
            return;
        }
        Map<String, Object> attributes1 = principal.getAttributes();
        if (null == principal.getAttributes().get(configCas.getUsername())) {
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
        }


        String username = principal.getAttributes().get(configCas.getUsername()).toString();
        String truename = "";
        if (null != principal.getAttributes().get(configCas.getName())) {
            truename = principal.getAttributes().get(configCas.getName()).toString();
        } else {
            truename = username;
        }

        Token token = umpUserDispose(username);
        if (token == null) {
            token = umpAdd(truename, username, "CAS");
        }

        tokenCache.put(token.getAccessToken(), username);
        Cookie cookie = CommonUtils.setCookie("token", token.getAccessToken(), -1, 1);
        Cookie cookieTo = CommonUtils.setCookie("way", Constant.LoginWay.CAS, -1, 1);
        Cookie cookieTo1 = CommonUtils.setCookie("userId", token.getUserId(), -1, 1);
        response.addCookie(cookie);
        response.addCookie(cookieTo);
        response.addCookie(cookieTo1);

        HttpSession session = request.getSession();
        Object umtCallbackPreUrl = session.getAttribute("umtCallbackPreUrl");
        String url = instdbUrl.getCallHost();
        if (null != umtCallbackPreUrl && StringUtils.isNotBlank(umtCallbackPreUrl.toString())) {
            url = umtCallbackPreUrl.toString();
        }
        String html = "<script type='text/javascript'>location.href='" + url + "';</script>";
        response.getWriter().print(html);
        return;
    }

    @Override
    public ConsumerDO getUserBytoken(String token) {
        ConsumerDO consumerDO = null;
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(userIdFromToken)) {
            Optional<ConsumerDO> byId = userRepository.findById(userIdFromToken);
            if (byId.isPresent()) {
                consumerDO = byId.get();
            }
        }
        return consumerDO;
    }

    private Object judgeWechat(HttpServletResponse response) {
        Object wechat = cacheLoading.loadingWechat();
        if (wechat == null) { //Not added to WeChat configuration
            Map<String, Object> param = new HashMap<>();
            param.put("code", 500);
            param.put("message", I18nUtil.get("USER_NO_WECHAT"));
            CommonUtils.errorMsg(response, param);
        }
        return wechat;
    }

    private Object judgeEsc(HttpServletResponse response) {
        Object esc = cacheLoading.loadingEsc();
        if (esc == null) { //Not joined the shared network configuration
            Map<String, Object> param = new HashMap<>();
            param.put("code", 500);
            param.put("message", I18nUtil.get("USER_NO_ESCIENCE"));
            CommonUtils.errorMsg(response, param);
        }
        return esc;
    }


    @Override
    public void wechatLogin(String type, HttpServletResponse response) {
        Object wechat = judgeWechat(response);
        if (null == wechat) {
            return;
        }
        WechatConf wechatConf = (WechatConf) wechat;
        String state = "login";
        try {
            if (StringUtils.isNotEmpty(type) && type.equals("1")) {
                state = "binding";
            }
            String authUrl = WechatUrl.getAuthUrl(wechatConf.getAppId(), wechatConf.getHongPage() + instdbUrl.getWechatCallbackUrl(), state);
            response.sendRedirect(authUrl);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * WeChat login callback
     *
     * @param code
     * @param state
     * @param response
     */
    @Override
    public void wechatCallback(String code, String state, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpClient httpClient = new HttpClient();
        Object wechat = judgeWechat(response);
        if (null == wechat) {
            return;
        }
        Map<String, Object> param = new HashMap<>();
        WechatConf wechatConf = (WechatConf) wechat;
        String tokenParam = WechatUrl.getTokenUrl(wechatConf.getAppId(), code, wechatConf.getSecretKey());
        String reData = httpClient.sendPost(WechatUrl.tokenUrl, tokenParam);
        if (StringUtils.isEmpty(reData)) {
            param.put("code", 500);
            param.put("message", "param code errorInternational");
            CommonUtils.errorMsg(response, param);
            return;
        }
        log.info("wechat {} obtaintokenobtain:" + reData);
        Gson gson = new Gson();
        Map dataMap = gson.fromJson(reData, Map.class);

        if (null == dataMap || !dataMap.containsKey("access_token")) {
            log.info("wechat-callback-getToken: error {} " + reData);
            param.put("code", 500);
            param.put("message", "Login error!");
            CommonUtils.errorMsg(response, param);
            return;
        }
        String access_token = (String) dataMap.get("access_token");
        String openid = (String) dataMap.get("openid");
        //Obtain WeChat user information
        String result = null;
        try {
            result = httpClient.doGetWayTwo(WechatUrl.getUserInfoUrl(openid, access_token), new HashMap<>());
        } catch (Exception e) {
            e.printStackTrace();
            log.info("wechat-callback-getToken: error {} " + reData);
            param.put("code", 500);
            param.put("message", "Login error!");
            CommonUtils.errorMsg(response, param);
            return;
        }
        log.info("wechat {} Obtain user information data:" + reData);
        String info = new String(result.getBytes("iso-8859-1"), "utf-8");

        Map userInfo = gson.fromJson(info, Map.class);
        if (null == userInfo || !userInfo.containsKey("unionid")) {
            log.info("wechat-callback-getUserinfo: error {} " + result);
            param.put("code", 500);
            param.put("message", "Login error!");
            CommonUtils.errorMsg(response, param);
            return;
        }
        String unionId = (String) userInfo.get("unionid");
        String nickname = (String) userInfo.get("nickname");
        String headImgUrl = (String) userInfo.get("headimgurl");
        ConsumerDO wechatId = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("wechatId").is(unionId)), ConsumerDO.class);
        String webUrl;
        if (state.equals("binding")) { //User binding
            if (null != wechatId) {
                param.put("code", 500);
                param.put("message", "The wechat account has been bound to another system account!");
                CommonUtils.errorMsg(response, param);
                return;
            }
            String token = CommonUtils.getCookie(request, "token");
            if (StringUtils.isEmpty(token)) {
                param.put("code", 500);
                param.put("message", "Login error!");
                CommonUtils.errorMsg(response, param);
                return;
            }
            String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
            ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(userIdFromToken)), ConsumerDO.class);
            if (null == consumerDO) {
                param.put("code", 500);
                param.put("message", "User does not exist!");
                CommonUtils.errorMsg(response, param);
                return;
            }
            if (StringUtils.isNotEmpty(consumerDO.getWechatId())) {
                param.put("code", 500);
                param.put("message", "The wechat account has been bound to another system account!");
                CommonUtils.errorMsg(response, param);
                return;
            }
            consumerDO.setWechatId(unionId);
            mongoTemplate.save(consumerDO);
            WechatUser wechatUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("unionId").is(unionId)), WechatUser.class);
            wechatUpdate(userInfo, wechatUser);
            webUrl = instdbUrl.getWechatConfUrl();
        } else {
            if (null == wechatId) { //new user - new user
                WechatUser wechatUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("unionId").is(unionId)), WechatUser.class);
                wechatUpdate(userInfo, wechatUser);
                String sessionId = request.getSession().getId();
                thirdParty.put(Constant.LoginWay.WECHAT + sessionId, unionId);
                thirdParty.put(Constant.LoginWay.WECHAT + unionId, headImgUrl + "~" + nickname);
                webUrl = instdbUrl.getWechatBindingUrl();
            } else { //Found bound user login

                Token token = umpUserDispose(wechatId.getEmailAccounts());
                if (token == null) {
                    param.put("code", 500);
                    param.put("message", "User does not exist!");
                    CommonUtils.errorMsg(response, param);
                    return;
                }
                webUrl = instdbUrl.getCallHost();
                Cookie cookie = CommonUtils.setCookie("token", token.getAccessToken(), -1, 1);
                Cookie cookieTo = CommonUtils.setCookie("way", Constant.LoginWay.WECHAT, -1, 1);
                response.addCookie(cookie);
                response.addCookie(cookieTo);
            }
        }
        String html = "<script type='text/javascript'>location.href='" + webUrl + "';</script>";
        response.getWriter().print(html);
        return;
    }

    /**
     * WeChat user synchronization+WeChat user synchronization
     *
     * @param wechatMap
     * @param wechatUser
     */
    private void wechatUpdate(Map<String, Object> wechatMap, WechatUser wechatUser) {
        String unionId = (String) wechatMap.get("unionid");
        String nickname = (String) wechatMap.get("nickname");
        String headImgUrl = (String) wechatMap.get("headimgurl");
        String openid = (String) wechatMap.get("openid");
        int sex = (int) (double) wechatMap.get("sex");
        String province = (String) wechatMap.get("province");
        String city = (String) wechatMap.get("city");
        String country = (String) wechatMap.get("country");
        // 1For men 2For men
        String gen = sex == 1 ? "male" : "male";
        if (null == wechatUser) {
            WechatUser chatUser = new WechatUser();
            chatUser.setUnionId(unionId);
            chatUser.setRealName(nickname);
            chatUser.setHeadImgUrl(headImgUrl);
            chatUser.setOpenId(openid);
            chatUser.setSex(gen);
            chatUser.setProvince(province);
            chatUser.setCity(city);
            chatUser.setCountry(country);
            chatUser.setCreateTime(new Date());
            mongoTemplate.insert(chatUser);
        } else { // update
            boolean judge = false;
            if (StringUtils.isEmpty(wechatUser.getRealName()) || !wechatUser.getRealName().equals(nickname)) {
                wechatUser.setRealName(nickname);
                judge = true;
            }

            if (StringUtils.isEmpty(wechatUser.getHeadImgUrl()) || !wechatUser.getHeadImgUrl().equals(headImgUrl)) {
                wechatUser.setHeadImgUrl(headImgUrl);
                judge = true;
            }

            if (StringUtils.isEmpty(wechatUser.getSex()) || !wechatUser.getSex().equals(gen)) {
                wechatUser.setSex(gen);
                judge = true;
            }

            if (StringUtils.isEmpty(wechatUser.getProvince()) || !wechatUser.getProvince().equals(province)) {
                wechatUser.setProvince(province);
                judge = true;
            }

            if (StringUtils.isEmpty(wechatUser.getCity()) || !wechatUser.getCity().equals(city)) {
                wechatUser.setCity(city);
                judge = true;
            }

            if (StringUtils.isEmpty(wechatUser.getCountry()) || !wechatUser.getCountry().equals(country)) {
                wechatUser.setCountry(country);
                judge = true;
            }

            if (judge) {
                wechatUser.setLastUpdateTime(new Date());
                mongoTemplate.save(wechatUser);
            }
        }
        return;
    }


    @Override
    public Result wechatAcc(String emailAccounts, String password, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = request.getSession().getId();
        String unionId = thirdParty.getIfPresent(Constant.LoginWay.WECHAT + sessionId);
        if (null == unionId) {
            return ResultUtils.error("AU_WECHAT_LOGIN");
        }
        Result login = login(emailAccounts, password, response, request, unionId);
        if (login.getCode() == 200) {
            thirdParty.invalidate(Constant.LoginWay.WECHAT + sessionId);
            thirdParty.invalidate(Constant.LoginWay.WECHAT + unionId);
        }
        return login;
    }

    @Override
    public Result wechatRegister(String emailAccounts, String name, String org, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = request.getSession().getId();
        String unionId = thirdParty.getIfPresent(Constant.LoginWay.WECHAT + sessionId);
        if (null == unionId) {
            return ResultUtils.error("AU_WECHAT_LOGIN");
        }
        //Decryption
        String username = RSAEncrypt.decrypt(emailAccounts);
        String realName = RSAEncrypt.decrypt(name);
        String work = RSAEncrypt.decrypt(org);

        if (StringUtils.isEmpty(username.trim()) || StringUtils.isEmpty(realName.trim()) || StringUtils.isEmpty(work.trim())) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        if (!CommonUtils.isEmail(username.trim())) {
            return ResultUtils.error("USER_EMAIL");
        }

        Query query = new Query().addCriteria(Criteria.where("emailAccounts").is(username.trim()));
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class);
        if (null != consumerDO) {
            return ResultUtils.error("USER_UNREGISTERED");
        }
        consumerDO = new ConsumerDO();
        consumerDO.setWechatId(unionId);
        consumerDO.setEmailAccounts(username);
        consumerDO.setState(1);
        consumerDO.setOrgChineseName(work);
        consumerDO.setAddWay("WeChat");
        consumerDO.setCreateTime(LocalDateTime.now());
        consumerDO.setName(realName);
        List<String> listRoles = new ArrayList();
        listRoles.add(Constant.GENERAL);
        consumerDO.setRoles(listRoles);
        ConsumerDO insert = mongoTemplate.insert(consumerDO);


        Token token = umpUserDispose(username);

        Cookie cookie = CommonUtils.setCookie("token", token.getAccessToken(), -1, 1);
        Cookie cookieTo = CommonUtils.setCookie("way", Constant.LoginWay.WECHAT, -1, 1);
        response.addCookie(cookie);
        response.addCookie(cookieTo);

        thirdParty.invalidate(Constant.LoginWay.WECHAT + sessionId);
        thirdParty.invalidate(Constant.LoginWay.WECHAT + unionId);
        return ResultUtils.success();
    }

    @Override
    public void escLogin(HttpServletResponse response) {
        Object esc = judgeEsc(response);
        if (null == esc) {
            return;
        }
        EscConf escConf = (EscConf) esc;
        try {
            String authUrl = EscUrl.getAuthUrl(escConf.getClientId(), escConf.getHongPage() + instdbUrl.getEscienceCallbackUrl());
            response.sendRedirect(authUrl);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    @Override
    public Result wechatUserinfo(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        String unionId = thirdParty.getIfPresent(Constant.LoginWay.WECHAT + sessionId);
        if (null == unionId) {
            return ResultUtils.success();
        }
        String ifPresent = thirdParty.getIfPresent(Constant.LoginWay.WECHAT + unionId);
        if (ifPresent == null) {
            return ResultUtils.error("AU_WECHAT_LOGIN");
        }
        String[] split = ifPresent.split("~");
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("name", split[1]);
        resultMap.put("image", split[0]);
        return ResultUtils.success(resultMap);
    }

    @Override
    public void escCallback(String code, HttpServletResponse response) throws IOException {
        HttpClient httpClient = new HttpClient();
        Object escObj = judgeEsc(response);
        if (null == escObj) {
            return;
        }
        Map<String, Object> param = new HashMap<>();
        EscConf escConf = (EscConf) escObj;
        String tokenParam = EscUrl.getTokenUrl(escConf.getClientId(), code, escConf.getClientSecret(), escConf.getHongPage() + instdbUrl.getEscienceCallbackUrl());
        String reData = httpClient.sendPost(EscUrl.tokenUrl, tokenParam);
        if (StringUtils.isEmpty(reData)) {
            param.put("code", 500);
            param.put("message", "param code errorInternational");
            CommonUtils.errorMsg(response, param);
            return;
        }
        log.info("esc(Shared Network) {} Shared NetworktokenShared Network:" + reData);
        Gson gson = new Gson();
        Map dataMap = gson.fromJson(reData, Map.class);

        if (null == dataMap || !dataMap.containsKey("access_token")) {
            log.info("esc-callback-getToken: error {} " + reData);
            param.put("code", 500);
            param.put("message", "Login error");
            CommonUtils.errorMsg(response, param);
            return;
        }
        String access_token = (String) dataMap.get("access_token");
        String token_type = (String) dataMap.get("token_type");

        //Obtain shared network user information
        Map<String, String> hearMap = new HashMap<>();
        hearMap.put("Authorization", token_type + " " + access_token);
        String result = httpClient.doGetWayTwo(EscUrl.userUrl, hearMap);
        log.info("esc(Shared Network) {} Shared Network:" + result);
        Map userInfo = gson.fromJson(result, Map.class);

        if (null == userInfo || !userInfo.containsKey("code") || (int) (double) userInfo.get("code") != 200) {
            log.info("esc-callback-getUserinfo: error {} " + result);
            param.put("code", 500);
            param.put("message", "Login error");
            CommonUtils.errorMsg(response, param);
            return;
        }
        Map userMap = (Map) ((Map) userInfo.get("data")).get("user");
        if (null == userMap) {
            log.info("esc-callback-getUserinfo: error {} " + result);
            param.put("code", 500);
            param.put("message", "Login error");
            CommonUtils.errorMsg(response, param);
            return;
        }

        String email = (String) userMap.get("email");
        String username = (String) userMap.get("username");

        Token token = umpUserDispose(username);
        if (token == null) {
            token = umpAdd(username, email, "Shared Network");
        }

        tokenCache.put(token.getAccessToken(), username);
        Cookie cookie = CommonUtils.setCookie("token", token.getAccessToken(), -1, 1);
        Cookie cookieTo = CommonUtils.setCookie("way", Constant.LoginWay.ESCIENCE, -1, 1);
        response.addCookie(cookie);
        response.addCookie(cookieTo);
        String html = "<script type='text/javascript'>location.href='" + escConf.getHongPage() + "';</script>";
        response.getWriter().print(html);
        return;
    }


}
