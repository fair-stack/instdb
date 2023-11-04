package cn.cnic.instdb.config;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.config.AboutConfiguration;
import cn.cnic.instdb.model.config.BasicConfiguration;
import cn.cnic.instdb.model.config.EmailConfig;
import cn.cnic.instdb.model.config.IndexConfiguration;
import cn.cnic.instdb.model.findata.PushFinDatasParam;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.rbac.Role;
import cn.cnic.instdb.model.resources.ApplyAccessTemplate;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.*;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.EsDataService;
import cn.cnic.instdb.service.IndexService;
import cn.cnic.instdb.service.SearchConfigService;
import cn.cnic.instdb.service.UserService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.jsonldjava.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Auther: wdd
 * @Date: 2021/09/10/19:41
 * @Description: Take action at project startup
 */

@Component
@Slf4j
public class InitConfig {

    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    private static final String subjectURL = "/data/subject.json";
    private static final String aboutURL = "/data/about_config.json";
    private static final String base64 = "/data/base64.json";
    private static final String license = "/data/license.json";

    @Resource
    private UserService userService;

    @Resource
    private EsDataService esDataService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private InstdbUrl instdbUrl;

    Map<String,String> mapsBase64 = new HashMap<>();

    @Autowired
    private CreateMongoIndex createMongoIndex;

    @Resource
    private ScheduledTask scheduledTask;


    @Resource
    private IndexService indexService;


    @Resource
    private SearchConfigService searchConfigService;



    //Adding this annotation indicates that this method is run when the project starts
    @PostConstruct
    public void init() {
        //Initialize Hypertube
        setAllRobotToUnConn();
    }

    private void setAllRobotToUnConn() {
        initializeBase64();
        initializeBasic();
        initializeUser();
        initializeRole();
        initializeConstant();
        initializeSubject();
        initializeEmail();
        createMongoIndex.createIndex();
        createMongoIndex.createApproveIndex();
        createMongoIndex.createResourceFileTreeIndex();
        base64TurnPicture();
        cleanJsonIdStr();
        addTask();
        initializeSearchconfig();

        log.info("-----------------------------------Initialize Run---------------------------------");
    }

//    public static void main(String[] args) {
//        Map<String,String> mapsBase64 = new HashMap<>();
//        File resourceFile = FileUtils.getResourceFile(base64);
//        //Reading file content
//        String json = FileUtils.readJsonFile(resourceFile.getPath());
//        try {
//            mapsBase64 = (Map<String, String>) JsonUtils.fromString(json);
//        } catch (IOException e) {
//            log.error("context",e);
//        }
//        System.out.println(mapsBase64.size());
//    }

    //Initialize some images by default
    private void initializeBase64() {
        File resourceFile = FileUtils.getResourceFile(base64);
        //Reading file content
        String json = FileUtils.readJsonFile(resourceFile.getPath());
        try {
            mapsBase64 = (Map<String, String>) JsonUtils.fromString(json);
        } catch (IOException e) {
            log.error("context",e);
        }
        if (resourceFile.exists()) {
            FileUtils.deleteFile(resourceFile.getPath());
        }
    }

    private void initializeBasic() {
        Query query = new Query();
        List<BasicConfiguration> all = mongoTemplate.findAll(BasicConfiguration.class);

        if (null == all || all.size() == 0) {
            BasicConfiguration basicConfiguration = new BasicConfiguration();
            basicConfiguration.setId(CommonUtils.generateUUID());

            String uuidResource = CommonUtils.generateUUID();
            CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("resource"), uuidResource);
            String resourcesTopLogo = uuidResource + Constant.PNG;
            String uuidResource1 = CommonUtils.generateUUID();
            CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("resource1"), uuidResource1);
            String resourcesEndLogo = uuidResource1 + Constant.PNG;

            //Initialize small icons
            String icoLogUUID = CommonUtils.generateUUID();
            CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("icoLogo"), icoLogUUID);
            String icologo = icoLogUUID + Constant.PNG;
            //Transparent small icons
            String icoLog2 = CommonUtils.generateUUID();
            CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("logo"), icoLog2);
            String icologo2 = icoLog2 + Constant.PNG;

            basicConfiguration.setLogo(icologo);
            basicConfiguration.setIcoLogo(icologo2);
            basicConfiguration.setIcoEndLogo(icologo);

            //Two images of resources
            basicConfiguration.setResourcesTopLogo(resourcesTopLogo);
            basicConfiguration.setResourcesEndLogo(resourcesEndLogo);

            basicConfiguration.setThemeColor("");
            basicConfiguration.setName("Institutional data repository");
            basicConfiguration.setLinks(new ArrayList<>());

            mongoTemplate.save(basicConfiguration);
        }else {
            BasicConfiguration basicConfiguration = all.get(0);
            Query queryF = new Query();
            Update update = new Update();
            queryF.addCriteria(Criteria.where("_id").is(basicConfiguration.getId()));
            if (StringUtils.isBlank(basicConfiguration.getResourcesTopLogo())) {
                String uuidResource = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("resource"), uuidResource);
                String resourcesTopLogo = uuidResource + Constant.PNG;
                update.set("resourcesTopLogo", resourcesTopLogo);
            }
            if (StringUtils.isBlank(basicConfiguration.getResourcesEndLogo())) {
                String uuidResource1 = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("resource1"), uuidResource1);
                String resourcesEndLogo = uuidResource1 + Constant.PNG;
                update.set("resourcesEndLogo", resourcesEndLogo);
            }
            //small icons
            if(StringUtils.isBlank(basicConfiguration.getLogo())){
                //Initialize small icons
                String icoLogUUID = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("icoLogo"), icoLogUUID);
                String icologo = icoLogUUID + Constant.PNG;
                update.set("logo", icologo);
            }
            if(StringUtils.isBlank(basicConfiguration.getIcoLogo())){
                //Transparent small icons
                String icoLog2 = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("logo"), icoLog2);
                String icologo2 = icoLog2 + Constant.PNG;
                update.set("icoLogo", icologo2);
            }
            if(StringUtils.isBlank(basicConfiguration.getIcoEndLogo())){
                //Initialize small icons
                String icoLogUUID = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("icoLogo"), icoLogUUID);
                String icologo = icoLogUUID + Constant.PNG;
                update.set("icoEndLogo", icologo);
            }

            if (0 < update.getUpdateObject().size()) {
                mongoTemplate.updateFirst(queryF, update, BasicConfiguration.class);
            }

        }

        //Initialize About Configuration
        List<AboutConfiguration> aboutConfig = mongoTemplate.findAll(AboutConfiguration.class);
        if (null == aboutConfig || aboutConfig.size() == 0) {
            //Start withresourceStart with
            File resourceFile = FileUtils.getResourceFile(aboutURL);
            //Reading file content
            String json = FileUtils.readJsonFile(resourceFile.getPath());
            List<SubjectData> maps = null;
            try {
                maps = (List<SubjectData>) JsonUtils.fromString(json);
            } catch (IOException e) {
                log.error("context",e);
            }
            mongoTemplate.insert(maps, "about_config");
            if (resourceFile.exists()) {
                FileUtils.deleteFile(resourceFile.getPath());
            }
        }
        List<AboutConfiguration> aboutConfig2 = mongoTemplate.findAll(AboutConfiguration.class);
        if(null != aboutConfig2){
            AboutConfiguration aboutConfiguration = aboutConfig2.get(0);
            if(StringUtils.isBlank(aboutConfiguration.getBanaerLogo())){
                Query queryF = new Query();
                queryF.addCriteria(Criteria.where("_id").is(aboutConfiguration.getId()));
                Update update = new Update();
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("about"), uuid);
                update.set("banaerLogo", uuid + Constant.PNG);
                mongoTemplate.updateFirst(queryF, update, AboutConfiguration.class);
            }
        }

        //Initialize homepage configuration
        IndexConfiguration indexConfig = mongoTemplate.findOne(query, IndexConfiguration.class);
        if (null == indexConfig ) {

            String name = "Institutional data repository";

            List<BasicConfiguration> basci = mongoTemplate.findAll(BasicConfiguration.class);
            if (null != basci && null != basci.get(0)) {
                BasicConfiguration basicConfiguration = basci.get(0);
                if (StringUtils.isNotBlank(basicConfiguration.getName())) {
                    name = basicConfiguration.getName();
                }
            }

            IndexConfiguration index = new IndexConfiguration();
            //Initialize image
            List<IndexConfiguration.PathInfo> banaerLogos = new ArrayList<>();

            //Image processing1
            IndexConfiguration.PathInfo pathInfo = new IndexConfiguration.PathInfo();
            String uuid = CommonUtils.generateUUID();
            CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("banaerLogo1"), uuid);
            pathInfo.setPath(uuid + Constant.PNG);
            pathInfo.setSort("1");
            banaerLogos.add(pathInfo);
            //Image processing2
            IndexConfiguration.PathInfo pathInfo2 = new IndexConfiguration.PathInfo();
            String uuid2 = CommonUtils.generateUUID();
            CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), mapsBase64.get("banaerLogo2"), uuid2);
            pathInfo2.setPath(uuid2 + Constant.PNG);
            pathInfo2.setSort("2");
            banaerLogos.add(pathInfo2);

            index.setBanaerLogo(banaerLogos);

            index.setId(CommonUtils.generateUUID());
            index.setName(name);
            index.setName_en("Institution DataBase");
            index.setContact("<p class=\"ql-align-center\"><span style=\"font-size: 20px;\">Findability&nbsp;&nbsp;|&nbsp;&nbsp;Accessibility&nbsp;&nbsp;|&nbsp;&nbsp;Interoperability&nbsp;&nbsp;|&nbsp;&nbsp;Reusability</span></p><p class=\"ql-align-center\"><span style=\"font-size: 20px;\">Data is the cornerstone and source of scientific discovery，Data is the cornerstone and source of scientific discovery，Data is the cornerstone and source of scientific discovery，Data is the cornerstone and source of scientific discovery。</span></p>");
            index.setContact_en("<p class=\"ql-align-center\"><span style=\"font-size: 20px;\">Findability&nbsp;&nbsp;|&nbsp;&nbsp;Accessibility&nbsp;&nbsp;|&nbsp;&nbsp;Interoperability&nbsp;&nbsp;|&nbsp;&nbsp;Reusability</span></p><p class=\"ql-align-center\"><br></p><p class=\"ql-align-center\"><span style=\"font-size: 20px;\">Data is the cornerstone and source of scientific discovery. </span></p><p class=\"ql-align-center\"><span style=\"font-size: 20px;\">We are committed to the scientific data mining and transmission to encourage more people to appreciate and explore the value.</span></p>");
            index.setSpecialName("What is a special topic？\n" +
                    "In published data resources，InstDBIn published data resources，In published data resources！");
            index.setSpecialName_en("A one sentence introduction to the whole special column, which can be configured through the background. Example text: a data management platform that is simple and easy to operate, safe enough and convenient for team cooperation. You can complete the collection and submission of project data through an independent data space.");
            mongoTemplate.save(index);
            mapsBase64.clear();
        }
    }

    //Initialize mailbox
    private void initializeEmail() {
        String newPassword = Constant.CASDC_EMAIL_PASSWORD;
        EmailConfig systemConf = mongoTemplate.findOne(new Query(), EmailConfig.class);
        EmailConfig sysEmail = null;
        if (null == systemConf) {
            sysEmail = new EmailConfig();
            sysEmail.setType(Constant.CASDC);
            sysEmail.setHost("mail.cstnet.cn");
            sysEmail.setPort(465);
            sysEmail.setFrom(Constant.CASDC_EMAIL);
            sysEmail.setUsername(Constant.CASDC_EMAIL);
            //sysEmail.setPassword(RSAEncrypt.encrypt("Ruanjianzhan0802!@#"));
            sysEmail.setPassword(RSAEncrypt.encrypt(newPassword));
            sysEmail.setProtocol("smtp");
            mongoTemplate.save(sysEmail);
        } else {
            if ("casdc@cnic.cn".equals(systemConf.getUsername())) {
                String password = systemConf.getPassword();
                if (StringUtils.isNotBlank(password)) {
                    String decrypt = RSAEncrypt.decrypt(password);
                    if (!"7&gUIwx3MTSw?&qD".equals(decrypt)) {
                        systemConf.setPassword(RSAEncrypt.encrypt(newPassword));
                        Update update = new Update();
                        update.set("password", RSAEncrypt.encrypt(newPassword));
                        update.set("type",Constant.CASDC);
                        mongoTemplate.updateFirst(new Query(), update, EmailConfig.class);
                    }
                }
            }
            sysEmail = systemConf;
        }
        config.put("emailConfig", sysEmail);
    }

    private void initializeUser() {

        long count = mongoTemplate.count(new Query(), ConsumerDO.class);
        if (0 == count) {
            List<String> listRoles = new ArrayList();
            listRoles.add(Constant.ADMIN);
            //Password encryption save
            String password = RSAEncrypt.encrypt(Constant.PASSWORD);
            ConsumerDO consumerDO = new ConsumerDO();
            consumerDO.setName("Super administrator");
            consumerDO.setEnglishName("admin");
            consumerDO.setPassword(password);
            consumerDO.setEmailAccounts(Constant.USERNAME);
            consumerDO.setState(1);
            consumerDO.setAddWay("Initialize Add");
            consumerDO.setCreateTime(LocalDateTime.now());
            consumerDO.setRoles(listRoles);
            mongoTemplate.save(consumerDO);
        }

    }

    private void initializeSubject() {
        Query query = new Query();
        mongoTemplate.remove(query,SubjectData.class);
        List<SubjectData> all = mongoTemplate.findAll(SubjectData.class);
        if (null == all || all.size() == 0) {
            //Start withresourceStart with
            File resourceFile = FileUtils.getResourceFile(subjectURL);
            //Reading file content
            String json = FileUtils.readJsonFile(resourceFile.getPath());
            List<SubjectData> maps = null;
            try {
                maps = (List<SubjectData>) JsonUtils.fromString(json);
            } catch (IOException e) {
                log.error("context",e);
            }
            mongoTemplate.insert(maps, "subject");
            maps.clear();
            if (resourceFile.exists()) {
                FileUtils.deleteFile(resourceFile.getPath());
            }
        }
    }

    private void initializeRole() {
        Query query = new Query();
        mongoTemplate.remove(query,Role.class);
        List<Role> all = mongoTemplate.findAll(Role.class);
        if (null == all || all.size() == 0) {
            /*   administrators  */
            String[] adminPath = {};
            Role roleAdmin = new Role("administrators","administrators", Constant.ADMIN, "administrators", Arrays.asList(adminPath), LocalDateTime.now());
            mongoTemplate.save(roleAdmin);

            String[] approvePath = {
                    "/constant/deleteById",
                    "/constant/save",
                    "/es/resetES",
                    "/es/save",
                    "/es/delete",
                    "/resources/resourcesListManage",
                    "/resources/upAndDown",
                    "/resources/resourcesAddSpecial",
                    "/resources/getSpecialByNoResources",
                    "/setting/umt.update",
                    "/setting/uploadTemplateConfig",
                    "/setting/downloadDataTemplate",
                    "/setting/getTemplateConfigAll",
                    "/setting/deleteTemplateConfigById",
                    "/setting/deleteTemplateConfigByIds",
                    "/setting/set/secretKey",
                    "/setting/getALLSecretKey",
                    "/setting/set/network/password",
                    "/setting/set/org",
                    "/setting/getCenterConf",
                    "/special/save",
                    "/special/update",
                    "/special/delete",
                    "/special/specialAddResources",
                    "/special/getResourcesByNoSpecial",
                    "/special/specialLogo",
                    "/special/deleteResourcesInSpecial",
                    "/special/addSpecialUserList",
                    "/subject/save",
                    "/subject/update",
                    "/subject/deleteById",
                    "/subject/deleteByIds",
                    "/subject/uploadIco",
                    "/subject/getSubjectAreaAll",
                    "/system/basic_config",
                    "/system/setResourcesConfig",
                    "/system/about_config",
                    "/system/index_config",
                    "/system/subjectArea__config",
                    "/user/add",
                    "/user/adminUserUpdate",
                    "/user/userList",
                    "/user/import.user",
                    "/user/addUserList",
                    "/user/deleteUserById"

            };
            Role roleApprove = new Role("Auditor", "auditor",Constant.ROLE_APPROVE, "Auditor", Arrays.asList(approvePath), LocalDateTime.now());
            mongoTemplate.save(roleApprove);

            /*   General users  */
            String[] generalPath = {
                    "/approve/approveSubmit",
                    "/approve/toApprove",
                    "/constant/deleteById",
                    "/constant/save",
                    "/es/resetES",
                    "/es/save",
                    "/es/delete",
                    "/review/saveExpert",
                    "/review/getExpert",
                    "/review/create/review",
                    "/review/getResourcesReview",
                    "/review/delete",
                    "/resources/resourcesAddSpecial",
                    "/resources/getSpecialByNoResources",
                    "/resources/resourceUpdateByAdmin",
                    "/resources/getApproveLog",
                    "/resources/resourcesListManage",
                    "/resources/upAndDown",
                    "/setting/umt.update",
                    "/setting/uploadTemplateConfig",
                    "/setting/downloadDataTemplate",
                    "/setting/getTemplateConfigAll",
                    "/setting/deleteTemplateConfigById",
                    "/setting/deleteTemplateConfigByIds",
                    "/setting/set/secretKey",
                    "/setting/getALLSecretKey",
                    "/setting/set/network/password",
                    "/setting/set/org",
                    "/setting/getCenterConf",
                    "/special/save",
                    "/special/update",
                    "/special/delete",
                    "/special/specialAddResources",
                    "/special/getResourcesByNoSpecial",
                    "/special/specialLogo",
                    "/special/deleteResourcesInSpecial",
                    "/special/addSpecialUserList",
                    "/subject/save",
                    "/subject/update",
                    "/subject/deleteById",
                    "/subject/deleteByIds",
                    "/subject/uploadIco",
                    "/subject/getSubjectAreaAll",
                    "/system/basic_config",
                    "/system/setResourcesConfig",
                    "/system/about_config",
                    "/system/index_config",
                    "/system/subjectArea__config",
                    "/user/add",
                    "/user/adminUserUpdate",
                    "/user/userList",
                    "/user/import.user",
                    "/user/addUserList",
                    "/user/deleteUserById"
                    };
            Role roleGeneral = new Role("Ordinary users","General user", Constant.GENERAL, "Ordinary users", Arrays.asList(generalPath), LocalDateTime.now());
            mongoTemplate.save(roleGeneral);
        }
    }

    private void setResourceData() {
        List<ConstantDictionary> resourceTypeList = new ArrayList<>();
        for (String resourceType : Constant.RESOURCE_TYPES) {
            ConstantDictionary con = new ConstantDictionary();
            con.setId(CommonUtils.generateUUID());
            con.setCreateTime(LocalDateTime.now());
            con.setName(resourceType.split("&")[1]);
            con.setCode(resourceType.split("&")[0]);
            con.setNameEn(resourceType.split("&")[2]);
            con.setType("resourceType");
            resourceTypeList.add(con);
        }
        mongoTemplate.insertAll(resourceTypeList);
        resourceTypeList.clear();
    }

    private void initializeConstant() {
        //Dataset Resource Type
        Query query = new Query();
        query.addCriteria(Criteria.where("type").is("resourceType"));
        List<ConstantDictionary> all = mongoTemplate.find(query, ConstantDictionary.class);

        if (null == all || all.size() == 0) {
            setResourceData();
        } else if (null != all && null != all.get(0) && all.size() != 9) {
            mongoTemplate.remove(query, ConstantDictionary.class);
            setResourceData();
        }

        //license agreement
        Query queryLicense = new Query();
        queryLicense.addCriteria(Criteria.where("type").is("license"));
        List<ConstantDictionary> licenses = mongoTemplate.find(queryLicense, ConstantDictionary.class);
        if (null == licenses || licenses.size() == 0) {
            addLicense();
        } else {
            boolean flag = false;
            for (ConstantDictionary license : licenses) {
                if (StringUtils.isBlank(license.getExplain())) {
                    flag = true;
                    break;
                }
            }
            if (licenses.size() < 16 || flag) {
                mongoTemplate.remove(queryLicense, ConstantDictionary.class);
                log.info("licenseCleared and needs to be reset");
                addLicense();
            }
        }

        //Reset Project
        esDataService.updateProjectAll();
    }

    private void addLicense() {
        //Start withresourceStart with
        File resourceFile = FileUtils.getResourceFile(license);
        //Reading file content
        String json = FileUtils.readJsonFile(resourceFile.getPath());
        List<SubjectData> maps = null;
        try {
            maps = (List<SubjectData>) JsonUtils.fromString(json);
        } catch (IOException e) {
            log.error("context", e);
            maps.clear();
        }
        mongoTemplate.insert(maps, "constant_dictionary");
        log.info("licenseReset completed，Reset completed" + maps.size() + "Reset completed");
        if (resourceFile.exists()) {
            FileUtils.deleteFile(resourceFile.getPath());
        }
        maps.clear();

    }


    /**
     * base64Convert Picture
     */
    private void base64TurnPicture() {
        Query query = new Query();
        query.addCriteria(Criteria.where("image").ne(true));
        List<Map> resourcesManage = mongoTemplate.find(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        log.info("Found the number of datasets that need to process images：" + resourcesManage.size());
        if (null != resourcesManage && resourcesManage.size() > 0) {
            for (Map resource : resourcesManage) {
                if (resource.containsKey("image") && null != resource.get("image")) {
                    try {
                        List image = (List) resource.get("image");
                        if (null != image && image.size() > 0) {
                            Query queryF = new Query(Criteria.where("_id").is(resource.get("_id").toString()));
                            Update update = new Update();
                            CommonUtils.base64ToFile(instdbUrl.getResourcesPicturePath(), image.get(0).toString(), resource.get("_id").toString());
                            update.set("image", true);
                            mongoTemplate.updateFirst(queryF, update, ResourcesManage.class);
                            log.info("Image conversion in progress...");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("Image conversion error,id:" + resource.get("_id").toString());
                    }
                }
            }
            log.info("Image conversion completed。");
        } else {
            log.info("No need to convert dataset images to work。");
        }
    }

    /**
     * clean upjson_id_content
     */
    private void cleanJsonIdStr() {
        List<Map> resourcesManage = mongoTemplate.find(new Query(Criteria.where("json_id_content").ne(null)), Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null != resourcesManage && resourcesManage.size() > 0) {
            for (Map resource : resourcesManage) {
                if (resource.containsKey("json_id_content") && null != resource.get("json_id_content")) {
                    Query queryF = new Query(Criteria.where("_id").is(resource.get("_id").toString()));
                    Update update = new Update();
                    update.set("json_id_content", null);
                    mongoTemplate.updateFirst(queryF, update, ResourcesManage.class);
                    log.info("json_id_contentCleaning up...");
                }
            }
            log.info("Clean up the datasetjson_id_contentClean up the dataset。");
        } else {
            log.info("No need to clean up the datasetjson_id_contentNo need to clean up the dataset。");
        }
    }

    /**
     * findataTimed task activation
     */
    private void addTask() {
        List<PushFinDatasParam> pushFinDatasParams = mongoTemplate.find(new Query(), PushFinDatasParam.class);
        if (null != pushFinDatasParams & pushFinDatasParams.size() > 0) {
            PushFinDatasParam pushFinDatasParam = pushFinDatasParams.get(0);
            if (!"close".equals(pushFinDatasParam.getType()) && !"executeOnce".equals(pushFinDatasParam.getType())) {
                if (StringUtils.isNotBlank(pushFinDatasParam.getCron()) && Constant.Approval.YES.equals(pushFinDatasParam.getStatus())) {
                    scheduledTask.refresh(pushFinDatasParams.get(0).getId(), pushFinDatasParam.getCron());
                    log.info("findataTimed task activation--->" + pushFinDatasParam.getType() + ",cron:" + pushFinDatasParam.getCron());
                }
            }
            log.info("findataTimed tasks do not need to be activated");
        }
        //General template initialization
        List<TemplateConfig> templateConfigList = mongoTemplate.find(new Query(), TemplateConfig.class);
        if (null == templateConfigList || 0 == templateConfigList.size()) {

            final String subjectURL = "/data/DbMetadata.xml";
            File resourceFile = FileUtils.getResourceFile(subjectURL);
            if (!resourceFile.exists()) {
                log.error(subjectURL + "file cannot be found，file cannot be found");
            } else {

                MultipartFile multipartFile = FileUtils.getMultipartFile(resourceFile);
                //Upload files
                String upload = FileUtils.upload(multipartFile, instdbUrl.getTemplateUrl(), CommonUtils.generateUUID());

                //Verify the upload status based on the returned value uploaded
                Map<String, Object> mapUploadFile = JSON.parseObject(upload);
                String codes = (String) mapUploadFile.get("code");
                if ("500".equals(codes)) {
                    log.error("Universal template built-in attachment uploadidbUniversal template built-in attachment upload");
                    return;
                }
                String path = (String) mapUploadFile.get("url");
                TemplateConfig templateConfig = new TemplateConfig();
                templateConfig.setName("Generic Dataset Template");
                templateConfig.setTemplateDesc("Dataset metadata standards");
                templateConfig.setTemplateAuthor("admin");
                templateConfig.setCode(CommonUtils.getCode(15));
                templateConfig.setType("11");
                templateConfig.setTypeName("data set");
                templateConfig.setId(CommonUtils.generateUUID());
                templateConfig.setPath(path);
                templateConfig.setUsername("admin");
                templateConfig.setUsernameEn("admin");
                templateConfig.setUserEmail("admin");
                templateConfig.setCreateTime(LocalDateTime.now());
                templateConfig.setState("0");
                mongoTemplate.save(templateConfig);
                log.error(subjectURL + "Built in template completion");
            }
        } else {
            log.info("Dataset template already exists Dataset template already exists");
        }

        //Dataset Request Access Template Initialization
        List<ApplyAccessTemplate> applyAccessTemplate = mongoTemplate.find(new Query(), ApplyAccessTemplate.class);
        if (null == applyAccessTemplate || 0 == applyAccessTemplate.size()) {

            final String subjectURL = "/data/accessTemplate.xml";
            File resourceFile = FileUtils.getResourceFile(subjectURL);
            if (!resourceFile.exists()) {
                log.error(subjectURL + "file cannot be found，file cannot be found");
            } else {
                MultipartFile multipartFile = FileUtils.getMultipartFile(resourceFile);
                Template template = XmlTemplateUtil.getTemplate(FileUtils.multipartFileToFile(multipartFile));
                if (null == template || null == template.getGroup() || 0 == template.getGroup().size()) {
                    log.error("The content format of the dataset application access template file is not qualified");
                    return;
                }
                //Upload files
                String upload = FileUtils.upload(multipartFile, instdbUrl.getTemplateUrl(),  CommonUtils.getCode(8));

                //Verify the upload status based on the returned value uploaded
                Map<String, Object> mapUploadFile = JSON.parseObject(upload);
                String codes = (String) mapUploadFile.get("code");
                if ("500".equals(codes)) {
                    log.error("Dataset application access template built-in upload failed");
                    return;
                }
                String path = (String) mapUploadFile.get("url");
                Map map = new HashMap();

                String UUID = CommonUtils.generateUUID();
                map.put("name", template.getTemplateName());
                map.put("desc", template.getTemplateDesc());
                map.put("author", template.getTemplateAuthor());
                map.put("_id", UUID);
                map.put("path", path);
                map.put("state", Constant.Approval.YES);
                map.put("username", "admin");
                map.put("userEmail", Constant.USERNAME);
                map.put("createTime", LocalDateTime.now());
                mongoTemplate.insert(map, "applyAccess_template");
                log.error(subjectURL + "Dataset application access built-in template completed");
            }
        } else {
            log.info("The dataset request access already has a template The dataset request access already has a template");
        }
    }

    /**
     * Advanced Retrieval Initialization Default Configuration
     */
    private void initializeSearchconfig() {
        Query queryF = new Query();
        List<SearchConfig> searchConfigList = mongoTemplate.find(queryF, SearchConfig.class);
        if (null == searchConfigList || 0 == searchConfigList.size()) {
            searchConfigService.resetSearchConfigs("init");
            log.info("Advanced retrieval data initialization completed");
        }


        List<SearchConfig> indexSearchitems = indexService.getIndexSearchitems(Constant.SEARCH);
        if (null == indexSearchitems || 0 == indexSearchitems.size()) {
            List<SearchConfig> searchList = mongoTemplate.find(new Query(Criteria.where("type").is(Constant.SEARCH)), SearchConfig.class);
            if (null != searchList && searchList.size() > 0) {
                String name = "name";
                for (SearchConfig search : searchList) {
                    if (search.getField().equals(name)) {
                        Update update = new Update();
                        update.set("status", "1");
                        update.set("sort", "0");
                        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(search.getId())), update, SearchConfig.class);
                        log.info("The advanced search homepage search item data has been initialized");
                        break;
                    }
                }
            }
        }
        List<SearchConfig> indexSearchitems1 = indexService.getIndexSearchitems(Constant.STATISTICS);
        if (null == indexSearchitems1 || 0 == indexSearchitems1.size()) {
            List<SearchConfig> searchList = mongoTemplate.find(new Query(Criteria.where("type").is(Constant.STATISTICS)), SearchConfig.class);
            if (null != searchList && searchList.size() > 0) {
                List<String> list = new ArrayList<>();
                list.add("privacyPolicy");
                list.add("keywords");
                list.add("subject");
                list.add("approveTime");
                int num = 0;
                for (SearchConfig search : searchList) {
                    for (String xx : list) {
                        if (search.getField().equals(xx)) {
                            Update update = new Update();
                            update.set("status", "1");
                            update.set("sort", num+"");
                            mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(search.getId())), update, SearchConfig.class);
                            log.info("Advanced search left aggregation item data has been initialized"+xx+" --> "+num);
                            num++;
                            break;
                        }
                    }

                }
            }
        }
        log.info("End of advanced retrieval data initialization！");

    }



}
