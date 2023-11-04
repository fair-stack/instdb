package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.cacheLoading.CacheLoading;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.commentNotice.Notice;
import cn.cnic.instdb.model.config.BasicConfiguration;
import cn.cnic.instdb.model.config.BasicConfigurationVo;
import cn.cnic.instdb.model.config.EmailConfig;
import cn.cnic.instdb.model.login.EscConf;
import cn.cnic.instdb.model.login.LoginConfig;
import cn.cnic.instdb.model.login.LoginConfigCas;
import cn.cnic.instdb.model.login.WechatConf;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.special.Special;
import cn.cnic.instdb.model.system.*;
import cn.cnic.instdb.repository.SpecialRepository;
import cn.cnic.instdb.repository.SubjectAreaRepository;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ExternalInterService;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.service.SystemConfigService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class SettingServiceImpl implements SettingService {

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();
    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private MongoUtil mongoUtil;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private JwtTokenUtils jwtTokenUtils;
    @Resource
    private UserRepository userRepository;

    @Autowired
    private SpecialRepository specialRepository;

    @Autowired
    private SubjectAreaRepository subjectAreaRepository;

    @Autowired
    private ExternalInterService externalInterService;

    @Resource
    private EmailUtils emailUtils;

    @Resource
    private SystemConfigService systemConfigService;

    @Override
    public void versionPush(String version, String details) {
        //Push to each administrator  Push to each administratorinstdbPush to each administrator
        StringBuffer sb = new StringBuffer();
        sb.append("Hello administrator InstDBHello administrator" + version + "，Hello administrator,");
        if (StringUtils.isNotBlank(details)) {
            sb.append("The main contents of this update include：" + details);
        }
        sb.append("。If updated, please ignore。");

        StringBuffer sbEn = new StringBuffer();
        sbEn.append("Hello administrator, instdb has released a new version " + version + "，Please update it in time,");
        if (StringUtils.isNotBlank(details)) {
            sbEn.append("The main contents of this update are：" + details);
        }
        sbEn.append("。Ignore if updated。");

        Query query = new Query();
        query.addCriteria(Criteria.where("roles").in(Constant.ADMIN));
        List<ConsumerDO> userList = mongoTemplate.find(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null != userList && userList.size() > 0) {
            for (ConsumerDO user : userList) {
                //Send message notifications to administrators
                Notice notice = new Notice();
                notice.setUsername((user.getEmailAccounts()));
                notice.setType(Constant.Comment.VERSION);
                notice.setContent(sb.toString());
                notice.setContentEn(sbEn.toString());
                notice.setTitle("System version upgrade reminder");
                notice.setTitleEn("System version upgrade reminder");
                notice.setIs_read("1");
                notice.setCreateTime(LocalDateTime.now());
                notice.setResourcesId(null);
                mongoTemplate.insert(notice);
                //commentNoticeService.addNotice(user.getEmailAccounts(), Constant.Comment.VERSION, sb.toString(), null, "System version upgrade reminder");
            }
        }
    }

    @Override
    public String uploadLogo(String id, String type, MultipartFile file) {

        //judgetypejudge  judge
        String path = "special".equals(type) ? instdbUrl.getSpecialLogo() : "user".equals(type) ? instdbUrl.getUserLogo() :
                "subject".equals(type) ? instdbUrl.getSubjectLogo() : "banaer".equals(type) || "favicon".equals(type) ? instdbUrl.getBanaer_icoLogo() : "";

        Assert.isTrue(StringUtils.isNotBlank(path), I18nUtil.get("SYSTEM_ERROR"));

        String fileName = file.getOriginalFilename();
        if (!CommonUtils.isPic(fileName.substring(fileName.lastIndexOf(".") + 1))) {
            throw new RuntimeException(I18nUtil.get("PICTURE_FORMAT_ERROR"));
        }

        File filePath = new File(path, CommonUtils.generateUUID() + "_" + fileName);

        //Determine if the file size exceeds2M
        if (FileUtils.judgeSize(filePath)) {
            throw new RuntimeException(file.getOriginalFilename() + I18nUtil.get("PICTURE_TOO_BIG"));
        }

        try {
            if (!filePath.getParentFile().exists()) {
                filePath.getParentFile().mkdirs();
            }
            file.transferTo(filePath);
        } catch (IOException e) {
            log.error("context", e);
            throw new RuntimeException(I18nUtil.get("SYSTEM_ERROR"));
        }

        String logoPath = filePath.getPath().replace(path.substring(0, path.length() - 1), "");

        if (StringUtils.isNotBlank(id) && !"0".equals(id)) {
            switch (type) {
                case "special":
                    Optional<Special> special = specialRepository.findById(id);
                    Assert.isTrue(special.isPresent(), I18nUtil.get("DATA_QUERY_EXCEPTION"));
                    if (StringUtils.isNotBlank(special.get().getLogo())) {
                        FileUtils.deleteFile(special.get().getLogo());
                    }
                    break;
                case "subject":
                    Optional<SubjectArea> subjectArea = subjectAreaRepository.findById(id);
                    Assert.isTrue(subjectArea.isPresent(), I18nUtil.get("DATA_QUERY_EXCEPTION"));
                    if (StringUtils.isNotBlank(subjectArea.get().getLogo())) {
                        FileUtils.deleteFile(subjectArea.get().getLogo());
                    }
                    break;
                case "user":
                    Query query = new Query();
                    query.addCriteria(Criteria.where("_id").is(id));
                    ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class);
                    Assert.isTrue(null != consumerDO, I18nUtil.get("DATA_QUERY_EXCEPTION"));
                    if (StringUtils.isNotBlank(consumerDO.getAvatar())) {
                        FileUtils.deleteFile(consumerDO.getAvatar());
                    }
                    //Users can directly modify their avatars
                    Update update = new Update();
                    update.set("avatar", logoPath);
                    mongoTemplate.upsert(query, update, ConsumerDO.class);
                    break;
                case "banaer":
                    Query query1 = new Query();
                    query1.addCriteria(Criteria.where("_id").is(id));
                    BasicConfiguration basicConfiguration = mongoTemplate.findOne(query1, BasicConfiguration.class);
                    Assert.isTrue(null != basicConfiguration, I18nUtil.get("DATA_QUERY_EXCEPTION"));
                    if (StringUtils.isNotBlank(basicConfiguration.getBanaerLogo())) {
                        FileUtils.deleteFile(basicConfiguration.getBanaerLogo());
                    }
                    break;
                case "favicon":
                    Query query2 = new Query();
                    query2.addCriteria(Criteria.where("_id").is(id));
                    BasicConfiguration basicConfiguration1 = mongoTemplate.findOne(query2, BasicConfiguration.class);
                    Assert.isTrue(null != basicConfiguration1, I18nUtil.get("DATA_QUERY_EXCEPTION"));
                    if (StringUtils.isNotBlank(basicConfiguration1.getIcoLogo())) {
                        FileUtils.deleteFile(basicConfiguration1.getIcoLogo());
                    }
                    break;
                default:
                    throw new RuntimeException(I18nUtil.get("PARAMETER_ERROR"));

            }
        }
        return logoPath;
    }


    @Override
    public Result uploadDataTemplate(String token, String constantId, MultipartFile file) {

        if (StringUtils.isBlank(constantId)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        if (null == file || file.isEmpty()) {
            return ResultUtils.error("FILE_IS_NULL");
        }

        if (!FileUtils.checkFileSizeIsLimit(file.getSize(), 5, "M")) {
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

        //Verify if the name exists
        Criteria criteria = Criteria.where("state").is("0");
        criteria.and("name").is(template.getTemplateName());
        Query query = new Query();
        query.addCriteria(criteria);
        TemplateConfig TemplateConfig = mongoTemplate.findOne(query, TemplateConfig.class);

        if (null != TemplateConfig) {
            return ResultUtils.errorBefore(template.getTemplateName() ,"CONTENT_ALREADY_EXISTS");
        }

//        CenterAccount centerConf = this.getCenterConf();
//        if(null == centerConf){
//            return ResultUtils.error("Currently does not support uploading metadata standards，Currently does not support uploading metadata standards");
//        }

        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        //Upload files
        String upload = FileUtils.upload(file, instdbUrl.getTemplateUrl(), CommonUtils.generateUUID());

        //Verify the upload status based on the returned value uploaded
        Map<String, Object> mapUploadFile = JSON.parseObject(upload);
        String codes = (String) mapUploadFile.get("code");
        if ("500".equals(codes)) {
            return ResultUtils.error("SYSTEM_ERROR");
        }
        String path = (String) mapUploadFile.get("url");
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setName(template.getTemplateName());
        templateConfig.setTemplateDesc(template.getTemplateDesc());
        templateConfig.setTemplateAuthor(template.getTemplateAuthor());
        templateConfig.setCode(CommonUtils.getCode(15));
        Query queryConstant = new Query();
        Criteria criteriaConstant = Criteria.where("_id").is(constantId);
        queryConstant.addCriteria(criteriaConstant);
        ConstantDictionary one = mongoTemplate.findOne(queryConstant, ConstantDictionary.class);
        if (null != one) {
            templateConfig.setType(one.getCode());
            templateConfig.setTypeName(one.getName());
        }
        templateConfig.setId(CommonUtils.generateUUID());
        templateConfig.setPath(path);
        templateConfig.setUsername(consumerDO.getName());
        templateConfig.setUsernameEn(consumerDO.getEnglishName());
        templateConfig.setUserEmail(consumerDO.getEmailAccounts());
        templateConfig.setCreateTime(LocalDateTime.now());
        templateConfig.setState("0");
        mongoTemplate.save(templateConfig);

        //Synchronize to the main center
        // externalInterService.syncTemplate(centerConf.getPublisherId(),id,name,one.getCode(),one.getName(),FileUtils.multipartFileToFile(file));
        return ResultUtils.success("FILE_UPLOAD");
    }


    @Override
    public void downloadDataTemplate(String token, String id, HttpServletResponse response) {
        String username = tokenCache.getIfPresent(token);
        Assert.isTrue(StringUtils.isNotBlank(username), I18nUtil.get("ENVIRONMENT_EXCEPTION"));

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        Criteria criteria = Criteria.where("state").is("0");
        criteria.and("_id").is(id);
        Query query = new Query();
        query.addCriteria(criteria);
        TemplateConfig TemplateConfig = mongoTemplate.findOne(query, TemplateConfig.class);
        FileUtils.downloadFile(TemplateConfig.getPath(), response,TemplateConfig.getName());
    }

    @Override
    public PageHelper getTemplateConfigAll(Map<String, Object> condition) {
        String name = condition.get("name").toString();
        String sort = condition.get("sort").toString();
        Query query = new Query();
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

        query.addCriteria(Criteria.where("state").is("0"));
        if (StringUtils.isNotBlank(name)) {
            query.addCriteria(Criteria.where("name").regex(name));
        }
        long count = mongoTemplate.count(query, TemplateConfig.class);
        mongoUtil.start(Integer.parseInt(condition.get("pageOffset").toString()), Integer.parseInt(condition.get("pageSize").toString()), query);
        List<TemplateConfig> templateConfig = mongoTemplate.find(query, TemplateConfig.class);
        if (null != templateConfig && templateConfig.size() > 0) {
            for (TemplateConfig template : templateConfig) {
                //Type conversion between Chinese and English

                template.setType(CommonUtils.getValueByType(template.getType(),Constant.LanguageStatus.RESOURCE_TYPES));
                template.setTypeName(template.getType());
                template.setPath("");
                //The English name of the completion operator
                if(StringUtils.isBlank(template.getUsernameEn())){
                    Query queryConstant = new Query();
                    Criteria criteriaConstant = Criteria.where("emailAccounts").is(template.getUserEmail());
                    queryConstant.addCriteria(criteriaConstant);
                    ConsumerDO one = mongoTemplate.findOne(queryConstant, ConsumerDO.class);
                    if (null != one && StringUtils.isNotBlank(one.getEnglishName())) {
                        template.setUsernameEn(one.getEnglishName());
                        Query query1 = new Query();
                        Criteria criteria = Criteria.where("_id").is(template.getId());
                        query1.addCriteria(criteria);
                        Update update = new Update();
                        update.set("usernameEn", one.getEnglishName());
                        mongoTemplate.updateFirst(query1, update, TemplateConfig.class);
                    }
                }
            }
        }
        return mongoUtil.pageHelper(count, templateConfig);
    }

    @Override
    public Result deleteTemplateConfigById(String id) {
        if(StringUtils.isBlank(id)){
            return ResultUtils.error("PARAMETER_ERROR");
        }
        Query query = new Query();
        Criteria criteria = Criteria.where("_id").is(id);
        query.addCriteria(criteria);
        TemplateConfig templateConfig = mongoTemplate.findOne(query, TemplateConfig.class);
        if (null != templateConfig) {
            Query queryResources = new Query();
            Criteria criteriaResources = Criteria.where("templateName").is(templateConfig.getName());
            queryResources.addCriteria(criteriaResources);
            ResourcesManage one = mongoTemplate.findOne(queryResources, ResourcesManage.class);
            if (null != one) {
                Update update = new Update();
                update.set("state", 1);
                mongoTemplate.updateFirst(query, update, TemplateConfig.class);
                return ResultUtils.success("DELETE_SUCCESS");
            } else {
                mongoTemplate.remove(query, TemplateConfig.class);
                FileUtils.deleteFile(templateConfig.getPath());
            }
        }
        //Call the main center and delete it as well
        //  externalInterService.deleteTemplate(id);
        return ResultUtils.success("DELETE_SUCCESS");
    }

    @Override
    public void deleteTemplateConfigByIds(List<String> ids) {
        if (null != ids && ids.size() > 0) {
            for (String id : ids) {
                deleteTemplateConfigById(id);
            }
        }
    }


    @Override
    public String getTemplatesByName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        TemplateConfig templateConfig = mongoTemplate.findOne(new Query().addCriteria(new Criteria().orOperator(
                Criteria.where("code").is(name), Criteria.where("name").is(name))).addCriteria(Criteria.where("state").is("0")).with(Sort.by(Sort.Direction.DESC, "createTime")), TemplateConfig.class);
        if (null != templateConfig) {
            String templateInfo = XmlTemplateUtil.getTemplateStr(templateConfig.getPath());
            if (StringUtils.isNotBlank(templateInfo)) {
                return templateInfo;
            }
        }
        return "";
    }

    @Override
    public Template getTemplate(String name) {
        TemplateConfig templateConfig = mongoTemplate.findOne(new Query().addCriteria(new Criteria().orOperator(
                Criteria.where("code").is(name), Criteria.where("name").is(name))).with(Sort.by(Sort.Direction.DESC, "createTime")), TemplateConfig.class);
        if (null != templateConfig) {
            return XmlTemplateUtil.getTemplateInfo(templateConfig.getPath());
        }
        return null;
    }

    @Override
    public List<ConstantDictionary> getlicenseAgreement() {
        Query query = new Query();
        query.addCriteria(Criteria.where("type").is("license"));
        List<ConstantDictionary> list = mongoTemplate.find(query, ConstantDictionary.class);
        return list;
    }

    @Override
    public Result getlicenseData(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where("type").is("license"));
        query.addCriteria(Criteria.where("name").is(name));
        ConstantDictionary one = mongoTemplate.findOne(query, ConstantDictionary.class);
        return ResultUtils.success(one);
    }


    @Override
    public Result umtUpdate(String id, String appKey, String appSecret, String page, String callback, boolean isOpen) {

        if (StringUtils.isBlank(appKey) || StringUtils.isBlank(appSecret)
                || StringUtils.isBlank(page) || StringUtils.isBlank(callback)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        LoginConfig loginConfig = null;
        if (StringUtils.isNotEmpty(id)) {
            loginConfig = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), LoginConfig.class);
            if (loginConfig == null) {
                return ResultUtils.error("DATA_QUERY_EXCEPTION");
            }
            loginConfig.setId(id);
            loginConfig.setLastUpdateTime(new Date());
        } else {
            loginConfig = new LoginConfig();
            loginConfig.setCreateTime(new Date());
        }
        loginConfig.setAppKey(appKey);
        loginConfig.setAppSecret(appSecret);
        loginConfig.setIndexPage(page);
        loginConfig.setIsOpen(isOpen);
        loginConfig.setCallback(callback);
        mongoTemplate.save(loginConfig);
        config.invalidate("basis");
        config.invalidate(Constant.LoginWay.UMP);
        return ResultUtils.success();
    }

    @Override
    public Result casUpdate(String id, String casServerUrl, String casServerUrlLogin, String casServerUrlLogoutUrl,String homePage ,String username, String name, boolean isOpen) {
        if (StringUtils.isBlank(casServerUrl) || StringUtils.isBlank(casServerUrlLogin)
                || StringUtils.isBlank(casServerUrlLogoutUrl) || StringUtils.isBlank(username)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        LoginConfigCas loginConfig = null;
        if (StringUtils.isNotEmpty(id)) {
            loginConfig = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), LoginConfigCas.class);
            if (loginConfig == null) {
                return ResultUtils.error("DATA_QUERY_EXCEPTION");
            }
            loginConfig.setId(id);
            loginConfig.setLastUpdateTime(new Date());
        } else {
            loginConfig = new LoginConfigCas();
            loginConfig.setCreateTime(new Date());
        }
        loginConfig.setCasServerUrl(casServerUrl);
        loginConfig.setCasServerUrlLogin(casServerUrlLogin);
        loginConfig.setCasServerUrlLogoutUrl(casServerUrlLogoutUrl);
        loginConfig.setHomePage(homePage);
        loginConfig.setIsOpen(isOpen);
        loginConfig.setUsername(username);
        loginConfig.setName(name);
        mongoTemplate.save(loginConfig);
        config.invalidate("basis");
        config.invalidate(Constant.LoginWay.CAS);
        return ResultUtils.success();
    }

    @Override
    public Result weChatUpdate(String id, String appId, String secretKey, String page, boolean isOpen) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(secretKey)
                || StringUtils.isBlank(page) ) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        WechatConf loginConfig = null;
        if (StringUtils.isNotEmpty(id)) {
            loginConfig = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), WechatConf.class);
            if (loginConfig == null) {
                return ResultUtils.error("DATA_QUERY_EXCEPTION");
            }
            loginConfig.setId(id);
            loginConfig.setLastUpdateTime(new Date());
        } else {
            loginConfig = new WechatConf();
            loginConfig.setCreateTime(new Date());
        }
        loginConfig.setAppId(appId);
        loginConfig.setSecretKey(secretKey);
        loginConfig.setHongPage(page);
        loginConfig.setIsOpen(isOpen);
        mongoTemplate.save(loginConfig);
        config.invalidate("basis");
        config.invalidate(Constant.LoginWay.WECHAT);
        return ResultUtils.success();
    }

    @Override
    public Result escienceUpdate(String id, String clientId, String clientSecret, String page, boolean isOpen) {
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(clientSecret)
                || StringUtils.isBlank(page) ) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        EscConf loginConfig = null;
        if (StringUtils.isNotEmpty(id)) {
            loginConfig = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), EscConf.class);
            if (loginConfig == null) {
                return ResultUtils.error("DATA_QUERY_EXCEPTION");
            }
            loginConfig.setId(id);
            loginConfig.setLastUpdateTime(new Date());
        } else {
            loginConfig = new EscConf();
            loginConfig.setCreateTime(new Date());
        }
        loginConfig.setClientId(clientId);
        loginConfig.setClientSecret(clientSecret);
        loginConfig.setHongPage(page);
        loginConfig.setIsOpen(isOpen);
        mongoTemplate.save(loginConfig);
        config.invalidate("basis");
        config.invalidate(Constant.LoginWay.ESCIENCE);
        return ResultUtils.success();
    }

    @Override
    public Result setOrg(String token, String cstr, String host) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));
        if (StringUtils.isBlank(host)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        CenterAccount centerConf = this.getCenterConf();

        if (null == centerConf || StringUtils.isBlank(centerConf.getOrgId()) || StringUtils.isBlank(centerConf.getOrgName())) {
            return ResultUtils.error("CENTER_SET");
        }
        Update update = new Update();
        update.set("host", host);

//        if (StringUtils.isNotBlank(cstr)) {
//            update.set("cstr", cstr);
//        }

        Query query = new Query(Criteria.where("_id").is(centerConf.getId()));
        //Synchronize the main center Synchronize the main center
        Result result = externalInterService.bandOrg(host, "cstr", centerConf.getOrgId());
        if (200 == result.getCode() && null != result.getData()) {
            JSONObject data = (JSONObject) result.getData();
            //Update institutional information
            update.set("publisherId", data.getString("id"));
            mongoTemplate.upsert(query, update, CenterAccount.class);
        } else {
            return ResultUtils.error("REMOTE_INTERFACE_ERROR");
        }
        config.invalidate("acc");
        return ResultUtils.success(result.getMessage());
    }

    @Override
    public Result setCstr(String token, String cstr, String clientId, String secret, String cstrCode, int cstrLength) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null == roles || !roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        if (StringUtils.isBlank(cstr) || StringUtils.isBlank(clientId) || StringUtils.isBlank(secret)
                || StringUtils.isBlank(cstrCode) || 0 >= cstrLength) {
            return ResultUtils.error("CSTR_IS_NULL");
        }

        HttpClient httpClient = new HttpClient();

        String url = instdbUrl.getCstrUrl()+"/openapi/v2/pid-user-service/user/checkPrefix?prefix=" + cstr + "&checkType=cstr";
        try {
            String result = httpClient.doGetCstr(url, clientId, secret);
            Map resultMap = JSONObject.parseObject(result, Map.class);
            int code = (int) resultMap.get("code");
            if (200 != code) {
                log.info("cstrVerification failed");
                return ResultUtils.error("CSTR_CONFIG_ERROR");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("cstrAbnormal verification result returned，Abnormal verification result returned");
            return ResultUtils.error("CSTR_CONFIG_ERROR");
        }

        CenterAccount centerConf = this.getCenterConf();
        Query query = new Query(Criteria.where("_id").is(centerConf.getId()));
        Update update = new Update();
        update.set("cstr", cstr);
        update.set("clientId", clientId);
        update.set("secret", secret);
        update.set("cstrCode", cstrCode);
        update.set("cstrLength", cstrLength);
        mongoTemplate.upsert(query, update, CenterAccount.class);
        config.invalidate("acc");
        return ResultUtils.success();
    }

    @Override
    public Result setDoi(String token, String doiType, String doiPrefiex,String repositoryID, String doiPassword, String doiCode, int doiLength) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null == roles || !roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        //Without the main centerdoi  Without the main center
        if (!Constant.CASDC.equals(doiType)) {
            if (StringUtils.isBlank(doiType) || StringUtils.isBlank(repositoryID) || StringUtils.isBlank(doiPassword)|| StringUtils.isBlank(doiPrefiex)
                    || StringUtils.isBlank(doiCode) || 0 >= doiLength) {
                return ResultUtils.error("DOI_IS_NULL");
            }
        }
        CenterAccount centerConf = this.getCenterConf();
        Query query = new Query(Criteria.where("_id").is(centerConf.getId()));
        Update update = new Update();
        update.set("doiType", doiType);
        //Without the main centerdoi Without the main center
        if (!Constant.CASDC.equals(doiType)) {
            update.set("repositoryID", repositoryID);
            update.set("doiPassword", doiPassword);
            update.set("doiPrefiex", doiPrefiex);
            update.set("doiCode", doiCode);
            update.set("doiLength", doiLength);
        }
        mongoTemplate.upsert(query, update, CenterAccount.class);
        config.invalidate("acc");
        return ResultUtils.success();
    }

    @Override
    public Result setNetworkPassword(String token, boolean isNetwork, String username, String password) {
        Update update = new Update();
        Query query = new Query();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        CenterAccount centerAccount = this.getCenterConf();

        String doitype = Constant.CASDC;
        if (null != centerAccount) {
            query.addCriteria(Criteria.where("_id").is(centerAccount.getId()));
            if(StringUtils.isNotBlank(centerAccount.getDoiType())){
                doitype = centerAccount.getDoiType();
            }
        }

        //Go to the main center for verification Go to the main center for verificationidGo to the main center for verification
        Result result = externalInterService.findOrgByAccount(username, password);
        if (200 == result.getCode() && null != result.getData()) {
            JSONObject data = (JSONObject) result.getData();
            //Update institutional information
            update.set("orgId", data.getString("id"));
            update.set("orgName", data.getString("zh_Name"));

            //Modify System Name
            BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
            if(null!= basicConfig){
                Update update1  = new Update();
                update1.set("orgName", data.getString("zh_Name"));
                Query query1 = new Query();
                query1.addCriteria(Criteria.where("_id").is(basicConfig.getId()));
                mongoTemplate.updateFirst(query1, update1, BasicConfiguration.class);
            }

        } else {
            return ResultUtils.error("REMOTE_INTERFACE_ERROR");
        }

        update.set("isNetwork", isNetwork);
        update.set("username", username);
        update.set("password", password);
        update.set("doiType", doitype);
        mongoTemplate.upsert(query, update, CenterAccount.class);

        //Configure this Configure this
        BasicConfigurationVo indexCopyrightLinks = systemConfigService.getBasicConfig();
        Query queryF = new Query(Criteria.where("_id").is(indexCopyrightLinks.getId()));
        Update updateF = new Update();
        updateF.set("isCenterAccount", true);
        mongoTemplate.upsert(queryF, updateF, BasicConfiguration.class);
        config.invalidate("acc");
        return ResultUtils.success();
    }

    @Override
    public Result umtGet() {
        List<LoginConfig> all = mongoTemplate.findAll(LoginConfig.class);
        if (all.size() > 0) {
            return ResultUtils.success(all.get(0));
        } else {
            return ResultUtils.success(null);
        }
    }

    @Override
    public Result casGet() {
        List<LoginConfigCas> all = mongoTemplate.findAll(LoginConfigCas.class);
        if (all.size() > 0) {
            return ResultUtils.success(all.get(0));
        } else {
            return ResultUtils.success(null);
        }
    }

    @Override
    public Result weChatGet() {
        List<WechatConf> all = mongoTemplate.findAll(WechatConf.class);
        if (all.size() > 0) {
            return ResultUtils.success(all.get(0));
        } else {
            return ResultUtils.success(null);
        }
    }

    @Override
    public Result escienceGet() {
        List<EscConf> all = mongoTemplate.findAll(EscConf.class);
        if (all.size() > 0) {
            return ResultUtils.success(all.get(0));
        } else {
            return ResultUtils.success(null);
        }
    }

    @Override
    public CenterAccount getCenterConf() {
        //Obtain basic information from cache
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        CenterAccount centerAccount = (CenterAccount) cacheLoading.loadingCenter();
        return centerAccount;
    }

    @Override
    public EmailConfig getEmailConfig() {
        //Obtain basic information from cache
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        EmailConfig emailConfig = (EmailConfig) cacheLoading.getEmailConfig();
        return emailConfig;
    }

    @Override
    public Result setEmailConfig(EmailConfig emailConfig) {
        if (StringUtils.isBlank(emailConfig.getType())) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        EmailConfig emailConfig1 = getEmailConfig();
        Update update = new Update();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(emailConfig1.getId()));
        if ("other".equals(emailConfig.getType())) {
            if (StringUtils.isBlank(emailConfig.getUsername()) || StringUtils.isBlank(emailConfig.getPasswordOther())
                    || StringUtils.isBlank(emailConfig.getHost()) || StringUtils.isBlank(emailConfig.getProtocol()) || 0 >= emailConfig.getPort()) {
                return ResultUtils.error("PARAMETER_ERROR");
            }
            update.set("host", emailConfig.getHost());
            update.set("port", emailConfig.getPort());
            update.set("protocol", emailConfig.getProtocol());
            update.set("username", emailConfig.getUsername());
            update.set("from", emailConfig.getUsername());
            update.set("passwordOther", RSAEncrypt.encrypt(emailConfig.getPassword()));
        } else if (Constant.CASDC.equals(emailConfig.getType())) {
            update.set("host", "mail.cstnet.cn");
            update.set("port", 465);
            update.set("protocol", "smtp");
            update.set("username", Constant.CASDC_EMAIL);
            update.set("from", Constant.CASDC_EMAIL);
            update.set("password", RSAEncrypt.encrypt(Constant.CASDC_EMAIL_PASSWORD));
        }
        update.set("type", emailConfig.getType());
        mongoTemplate.upsert(query, update, EmailConfig.class);
        config.invalidate("emailConfig");
        return ResultUtils.success();
    }

    @Override
    public Result testSendEmail(String token ,String email) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if(!roles.contains(Constant.ADMIN)){
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        EmailConfig emailConfig = getEmailConfig();
        if(null == email){
            return ResultUtils.error("EMAIL_SET_NO");
        }
        if (null != emailConfig && StringUtils.isNotBlank(emailConfig.getProtocol()) && StringUtils.isNotBlank(emailConfig.getFrom())
                && StringUtils.isNotBlank(emailConfig.getUsername()) && StringUtils.isNotBlank(emailConfig.getPassword())
                && StringUtils.isNotBlank(emailConfig.getHost()) && emailConfig.getPort() > 0) {

            Map<String, Object> attachment = new HashMap<>();
            ToEmail toEmail = new ToEmail();
            toEmail.setTos(new String[]{email});
            boolean b = emailUtils.sendSimpleMail(toEmail, attachment, EmailModel.EMAIL_TEST());
            if (b) {
                return ResultUtils.success();
            }

        }else {
            return ResultUtils.error("EMAIL_SET_ERROR");
        }
        return ResultUtils.error("FAILED");
    }

    @Override
    public Result downloadNoLogin(String token, String downloadPower,String ftpSwitch,String noLoginAccess,String emailDownloadPower, String emailSuffix) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if(!roles.contains(Constant.ADMIN)){
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Update update = new Update();
        Query query = new Query();
        if (StringUtils.isBlank(downloadPower) || StringUtils.isBlank(ftpSwitch) || StringUtils.isBlank(noLoginAccess) || StringUtils.isBlank(emailDownloadPower)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        if (Constant.Approval.YES.equals(emailDownloadPower)) {
            if (Constant.Approval.YES.equals(downloadPower) &&  StringUtils.isBlank(emailSuffix)) {
                return ResultUtils.error("PARAMETER_ERROR");
            }
            update.set("emailSuffixDownload", emailSuffix);
        }
        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
        if (null != basicConfig) {
            query.addCriteria(Criteria.where("_id").is(basicConfig.getId()));
            update.set("downloadPower", downloadPower);
            update.set("ftpSwitch", ftpSwitch);
            update.set("noLoginAccess", noLoginAccess);
            update.set("emailDownloadPower", emailDownloadPower);
            mongoTemplate.upsert(query, update, BasicConfiguration.class);
            config.invalidate("basis");
            return ResultUtils.success();
        }
        return ResultUtils.error("FAILED");
    }


}
