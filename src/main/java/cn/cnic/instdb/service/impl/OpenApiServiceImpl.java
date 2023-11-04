package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.openApi.Apis;
import cn.cnic.instdb.model.openApi.SecretKey;
import cn.cnic.instdb.model.openApi.SecretKeyDTO;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.FtpUser;
import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.model.system.Template;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.*;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@EnableScheduling
public class OpenApiServiceImpl implements OpenApiService {

    @Resource
    private ResourcesService resourcesService;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private InstdbApiService instdbApiService;

    @Resource
    private MongoUtil mongoUtil;

    @Resource
    private AuthService authService;

    @Resource
    private SettingService settingService;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    @Override
    public Result setSecretKey(String token, SecretKeyDTO secretKey) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        String username = jwtTokenUtils.getUsernameFromToken(token);

        if (StringUtils.isBlank(secretKey.getApplicationName()) || StringUtils.isBlank(secretKey.getOrganId()) || StringUtils.isBlank(secretKey.getOrganName())  ) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("applicationName").is(secretKey.getApplicationName()));
        SecretKey one = mongoTemplate.findOne(query, SecretKey.class);
        if (null != one) {
            return ResultUtils.errorBefore(secretKey.getApplicationName(), "ALREADY_GENERATED_CODE");
        }

        SecretKey obj = new SecretKey();
        BeanUtils.copyProperties(secretKey, obj);
        //generate18generate
        String code = CommonUtils.getCode(18);
        obj.setId(CommonUtils.generateUUID());
        obj.setValue(code);
        obj.setCreateTime(LocalDateTime.now());
        obj.setStatus("0");
        obj.setUsername(username);
        mongoTemplate.save(obj);
        getApiList("", 0, 10, "");
        return ResultUtils.success();
    }

    @Override
    public Result deletSecretKey(String token, String id) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        SecretKey one = mongoTemplate.findOne(query, SecretKey.class);
        if (null == one) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

        Query query1 = new Query();
        query1.addCriteria(Criteria.where("authorizationList").elemMatch(Criteria.where("id").is(id)));
        List<Apis> apis = mongoTemplate.find(query1, Apis.class);
        if (null != apis && apis.size() > 0) {
            return ResultUtils.error("SECRETKEY_API_IS");
        }

        mongoTemplate.remove(query, SecretKey.class);
        return ResultUtils.success("DELETE_SUCCESS");
    }

    @Override
    public Result updateSecretKey(String token, String id, String applicationName, String organId, String organName) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        if (StringUtils.isBlank(applicationName) || StringUtils.isBlank(organId) || StringUtils.isBlank(organName)  ) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        SecretKey one = mongoTemplate.findOne(query, SecretKey.class);
        if (null == one) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("applicationName").is(applicationName));
        SecretKey one1 = mongoTemplate.findOne(query2, SecretKey.class);
        if (null != one1) {
            if(!one.getId().equals(one1.getId())){
                return ResultUtils.errorBefore(applicationName, "ALREADY_GENERATED_CODE");
            }
        }


        Update update = new Update();
        if (!applicationName.equals(one.getApplicationName())) {
            update.set("applicationName", applicationName);
        }
        if (!organId.equals(one.getOrganId())) {
            update.set("organId", organId);
            update.set("organName", organName);
        }
        if (update.getUpdateObject().size() > 0) {
            mongoTemplate.updateFirst(query, update, SecretKey.class);
        }
        return ResultUtils.success("UPDATE_SUCCESS");
    }

    @Override
    public Result resetapiList(String token) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        mongoTemplate.remove(new Query(), Apis.class);
        return ResultUtils.success();
    }

    @Override
    public PageHelper getALLSecretKey(Map<String, Object> condition) {
        String token = condition.get("token").toString();
        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("NEED_TOKEN"));
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));
        String organName = condition.get("organName").toString();
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));

        if (StringUtils.isNotBlank(organName)) {
            query.addCriteria(Criteria.where("applicationName").regex(organName));
        }

        long count = mongoTemplate.count(query, SecretKey.class);
        mongoUtil.start(Integer.parseInt(condition.get("pageOffset").toString()), Integer.parseInt(condition.get("pageSize").toString()), query);
        List<SecretKey> secretKey = mongoTemplate.find(query, SecretKey.class);

        return mongoUtil.pageHelper(count, secretKey);
    }

    @Override
    public void disable(String token, String id, String status) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("LOGIN_EXCEPTION"));
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        SecretKey one = mongoTemplate.findOne(query, SecretKey.class);
        Assert.isTrue(null != one, I18nUtil.get("DATA_QUERY_EXCEPTION"));

        Update update = new Update();
        update.set("status", status);
        mongoTemplate.updateFirst(query, update, SecretKey.class);
    }

    @Override
    public Result disableApi(String token, String id, String status) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        Apis one = mongoTemplate.findOne(query, Apis.class);
        Assert.isTrue(null != one, I18nUtil.get("DATA_QUERY_EXCEPTION"));

        Update update = new Update();
        update.set("status", status);
        mongoTemplate.updateFirst(query, update, Apis.class);
        return ResultUtils.success();
    }


    @Override
    public PageHelper getApiList(String name, Integer pageOffset, Integer pageSize,String status) {
        Query queryApis = new Query();

        if (StringUtils.isNotBlank(status)) {
            queryApis.addCriteria(Criteria.where("status").is(status));
        }
        if (StringUtils.isNotBlank(name)) {
            queryApis.addCriteria(Criteria.where("name").regex(name));
        }

        long count = mongoTemplate.count(queryApis, Apis.class);
        mongoUtil.start(pageOffset, pageSize, queryApis);

        List<Apis> ApisList = mongoTemplate.find(queryApis, Apis.class);

        List<Apis> lists = mongoTemplate.find(new Query(), Apis.class);
        if (null == lists || 18 != lists.size()) {


            //Start processing the addition again
            List<Map<String, String>> serviceList = instdbApiService.getServiceList();
            List<Apis> apisList = new ArrayList<>();
            List<Apis> apisListAll = new ArrayList<>();
            if (null != serviceList && serviceList.size() > 0) {
                Query query = new Query();
                query.addCriteria(Criteria.where("status").is("0"));
                // Less than or equal to 2022-08-08"
               // query.addCriteria(Criteria.where("createTime").lte(DateUtils.getLocalDateTimeByString2("2022-08-08")));
                List<SecretKey> secretKey = mongoTemplate.find(query, SecretKey.class);
                for (Map<String, String> map : serviceList) {
                    Apis apiList = lists.stream().filter(s -> Objects.equals(s.getName(), map.get("name"))).findFirst().orElse(null);
                    if (null != apiList) {
                        apisListAll.add(apiList);
                        continue;
                    }
                    Apis apis = new Apis();
                    apis.setName(map.get("name"));
                    apis.setDesc("Interoperability interface");
                    apis.setUrl(map.get("url"));
                    apis.setVersion(map.get("version"));
                    apis.setStatus("0");
                    apis.setCreateTime(LocalDateTime.now());
                    if (null != secretKey && secretKey.size() > 0) {
                        List<Apis.Authorization> authorizationList = new ArrayList<>();
                        for (SecretKey key : secretKey) {
                            Apis.Authorization authorization = new Apis.Authorization();
                            authorization.setId(key.getId());
                            authorization.setOrganId(key.getOrganId());
                            authorization.setName(key.getOrganName());
                            authorization.setApplicationName(key.getApplicationName());
                            authorization.setType(Constant.LONG_TERM);
                            authorization.setOperator("admin");
                            authorizationList.add(authorization);
                        }
                        apis.setAuthorizationList(authorizationList);
                    }
                    apisListAll.add(apis);
                    apisList.add(apis);
                }
            }

            CenterAccount centerConf = settingService.getCenterConf();
            if (null == centerConf || StringUtils.isBlank(centerConf.getHost())) {
                return mongoUtil.pageHelper(apisListAll.size(), apisListAll);
            }

            String ip = "";
            if (StringUtils.isNotBlank(centerConf.getHost()) && centerConf.getHost().length() > 0) {
                ip = "/".equals(centerConf.getHost().substring(centerConf.getHost().length() - 1)) ? centerConf.getHost() + "api" : centerConf.getHost() + "/api";
            }

            Apis apiList = lists.stream().filter(s -> Objects.equals(s.getName(), "Data resource approval")).findFirst().orElse(null);
            if (null == apiList) {
                Apis apis = new Apis();
                apis.setName("Data resource approval");
                apis.setDesc("Data resource approval function");
                apis.setUrl(ip + "/open/approveSubmit");
                apis.setVersion("1.0");
                apis.setStatus("0");
                apis.setCreateTime(LocalDateTime.now());
                apisList.add(apis);
            } else {
                apisListAll.add(apiList);
            }

            Apis apiList1 = lists.stream().filter(s -> Objects.equals(s.getName(), "Obtain Dataset Details")).findFirst().orElse(null);
            if (null == apiList1) {
                Apis apis1 = new Apis();
                apis1.setName("Obtain Dataset Details");
                apis1.setDesc("according to id according to");
                apis1.setUrl(ip + "/open/dataset/details");
                apis1.setVersion("1.0");
                apis1.setStatus("0");
                apis1.setCreateTime(LocalDateTime.now());
                apisList.add(apis1);
            } else {
                apisListAll.add(apiList1);
            }

            Apis apiList2 = lists.stream().filter(s -> Objects.equals(s.getName(), "Data Resource Approval List")).findFirst().orElse(null);
            if (null == apiList2) {
                Apis apis2 = new Apis();
                apis2.setName("Data Resource Approval List");
                apis2.setDesc("Query all approval lists");
                apis2.setUrl(ip + "/open/findAllApproval");
                apis2.setVersion("1.0");
                apis2.setStatus("0");
                apis2.setCreateTime(LocalDateTime.now());
                apisList.add(apis2);
            } else {
                apisListAll.add(apiList2);
            }

            Apis apiList3 = lists.stream().filter(s -> Objects.equals(s.getName(), "Approval of claim")).findFirst().orElse(null);
            if (null == apiList3) {
                Apis apis3 = new Apis();
                apis3.setName("Approval of claim");
                apis3.setDesc("Approval of claim");
                apis3.setUrl(ip + "/open/claim");
                apis3.setVersion("1.0");
                apis3.setStatus("0");
                apis3.setCreateTime(LocalDateTime.now());
                apisList.add(apis3);
            } else {
                apisListAll.add(apiList3);
            }


            Apis apiList4 = lists.stream().filter(s -> Objects.equals(s.getName(), "List of data resource files")).findFirst().orElse(null);
            if (null == apiList4) {
                Apis apis4 = new Apis();
                apis4.setName("List of data resource files");
                apis4.setDesc("Query Data Resource File List");
                apis4.setUrl(ip + "/open/getResourceFileTree");
                apis4.setVersion("1.0");
                apis4.setStatus("0");
                apis4.setCreateTime(LocalDateTime.now());
                apisList.add(apis4);
            } else {
                apisListAll.add(apiList4);
            }


            Apis apiList5 = lists.stream().filter(s -> Objects.equals(s.getName(), "Single file download of data resources")).findFirst().orElse(null);
            if (null == apiList5) {
                Apis apis5 = new Apis();
                apis5.setName("Single file download of data resources");
                apis5.setDesc("Single file download of data resources");
                apis5.setUrl(ip + "/open/resourcesDownloadFile");
                apis5.setVersion("1.0");
                apis5.setStatus("0");
                apis5.setCreateTime(LocalDateTime.now());
                apisList.add(apis5);
            } else {
                apisListAll.add(apiList5);
            }

            Apis apiList6 = lists.stream().filter(s -> Objects.equals(s.getName(), "Data resourcesFTPData resources")).findFirst().orElse(null);
            if (null == apiList6) {
                Apis apis6 = new Apis();
                apis6.setName("Data resourcesFTPData resources");
                apis6.setDesc("Data resourcesFTPData resources");
                apis6.setUrl(ip + "/open/resourcesFtpDownloadFile");
                apis6.setVersion("1.0");
                apis6.setStatus("0");
                apis6.setCreateTime(LocalDateTime.now());
                apisList.add(apis6);
            } else {
                apisListAll.add(apiList6);
            }


            Apis apiList7 = lists.stream().filter(s -> Objects.equals(s.getName(), "Obtain dataset details based on dataset number")).findFirst().orElse(null);
            if (null == apiList7) {
                Apis apis7 = new Apis();
                apis7.setName("Obtain dataset details based on dataset number");
                apis7.setDesc("Obtain dataset details based on dataset number");
                apis7.setUrl(ip + "/open/dataset/details/relatedDataset");
                apis7.setVersion("1.0");
                apis7.setStatus("0");
                apis7.setCreateTime(LocalDateTime.now());
                apisList.add(apis7);
            } else {
                apisListAll.add(apiList7);
            }


            Apis apiList8 = lists.stream().filter(s -> Objects.equals(s.getName(), "Identity information acquisition")).findFirst().orElse(null);
            if (null == apiList8) {
                Apis apis8 = new Apis();
                apis8.setName("Identity information acquisition");
                apis8.setDesc("Identity information acquisitiontoken");
                apis8.setUrl(ip + "/open/getToken");
                apis8.setVersion("1.0");
                apis8.setStatus("0");
                apis8.setCreateTime(LocalDateTime.now());
                apisList.add(apis8);
            } else {
                apisListAll.add(apiList8);
            }

            Apis apiList9 = lists.stream().filter(s -> Objects.equals(s.getName(), "Set file size and redirect download address")).findFirst().orElse(null);
            if (null == apiList9) {
                Apis apis8 = new Apis();
                apis8.setName("Set file size and redirect download address");
                apis8.setDesc("Set file size and redirect download address,Set file size and redirect download address");
                apis8.setUrl(ip + "/open/setFilestorageInfo");
                apis8.setVersion("1.0");
                apis8.setStatus("0");
                apis8.setCreateTime(LocalDateTime.now());
                apisList.add(apis8);
            } else {
                apisListAll.add(apiList9);
            }

            mongoTemplate.insertAll(apisList);
            return mongoUtil.pageHelper(apisListAll.size(), apisListAll);
        }
        return mongoUtil.pageHelper(count, ApisList);
    }

    @Override
    public Result getApiAuth(String apiId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(apiId));
        Apis apis = mongoTemplate.findOne(query, Apis.class);
        List<Apis.Authorization> authorizationList = apis.getAuthorizationList();
        return ResultUtils.success(authorizationList);
    }

    @Override
    public Result updateApiAuth(String token, String apiId, Apis.Authorization authorization) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        ConsumerDO consumerDO = authService.getUserBytoken(token);
        if (null == consumerDO) {
            return ResultUtils.error("ENVIRONMENT_EXCEPTION");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(apiId));
        Apis apis = mongoTemplate.findOne(query, Apis.class);
        if (null != apis) {
            List<Apis.Authorization> authorizationList = apis.getAuthorizationList();
            if (null != authorizationList && authorizationList.size() > 0) {
                Iterator<Apis.Authorization> iterator = authorizationList.iterator();
                boolean exist = false;
                while (iterator.hasNext()) {
                    Apis.Authorization next = iterator.next();
                    //Already exists -- Already exists
                    if (next.getId().equals(authorization.getId())) {
                        iterator.remove();
                        authorizationList.add(authorization);
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    authorizationList.add(authorization);
                }
            } else {
                authorizationList = new ArrayList<>();
                authorizationList.add(authorization);
            }
            Update update = new Update();
            update.set("authorizationList", authorizationList);
            mongoTemplate.updateFirst(query, update, Apis.class);
        }
        return ResultUtils.success();
    }

    @Override
    public Result deleteApiAuth(String token, String apiId, String orgId) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(apiId));
        Apis apis = mongoTemplate.findOne(query, Apis.class);
        if (null != apis) {
            List<Apis.Authorization> authorizationList = apis.getAuthorizationList();
            if (null != authorizationList && authorizationList.size() > 0) {
                Iterator<Apis.Authorization> iterator = authorizationList.iterator();
                while (iterator.hasNext()) {
                    Apis.Authorization next = iterator.next();
                    if (next.getId().equals(orgId)) {
                        iterator.remove();
                        break;
                    }
                }
                Update update = new Update();
                update.set("authorizationList", authorizationList);
                mongoTemplate.updateFirst(query, update, Apis.class);
            }
        }
        return ResultUtils.success();
    }

    @Override
    public Result getDetailsByRelatedDataset(String relatedDataset) {
        return ResultUtils.success();
    }

    @Override
    public Result setFilestorageInfo(String datasetId, long storageNum, String datasetRemoteUrl, int visits) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(datasetId));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null == resourcesManage) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
//        if (Constant.Approval.ADOPT.equals(resourcesManage.getStatus())) {
//            log.error("Approved and cannot be modified again");
//            return ResultUtils.error("FAILED");
//        }
        Update update = new Update();
        update.set("storageNum", storageNum);
        update.set("datasetRemoteUrl", datasetRemoteUrl);
        update.set("visitNum", visits);
        mongoTemplate.updateFirst(query, update, ResourcesManage.class);
        return ResultUtils.success();
    }

    @Override
    public Result getResourcesDetails(String id) {
        if (StringUtils.isBlank(id)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("relatedDataset").is(id),Criteria.where("name").is(id), Criteria.where("_id").is(id)));
        Map map = mongoTemplate.findOne(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null == map) {
            return ResultUtils.error(504, "DATA_QUERY_EXCEPTION");
        }
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

        map.put("templateData", groups);
        removeField(map);
        return ResultUtils.success(map);
    }

    @Override
    public Result resourcesFtpDownloadFile(String id) {

        if (StringUtils.isBlank(id)) {
            return ResultUtils.error("idCannot be empty");
        }
        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("cstr").is(id), Criteria.where("doi").is(id), Criteria.where("_id").is(id)));
        query.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        Map map = mongoTemplate.findOne(query, Map.class, Constant.RESOURCE_COLLECTION_NAME);
        if (null == map) {
            return ResultUtils.error(504, "DATA_QUERY_EXCEPTION");
        }
        Query queryFtp = new Query();
        queryFtp.addCriteria(Criteria.where("resourcesId").is(map.get("_id").toString())).addCriteria(Criteria.where("authType").ne("part")).addCriteria(Criteria.where("auth").is(Constant.GENERAL));
        FtpUser ftpUser = mongoTemplate.findOne(queryFtp, FtpUser.class);
        Map<String, String> ftpInfo = new HashMap<>();
        if (null != ftpUser) {
            ftpInfo.put("username", ftpUser.getUsername());
            ftpInfo.put("password", ftpUser.getPassword());
            ftpInfo.put("ftpUrl", instdbUrl.getFtpHost());
        } else {
            resourcesService.createFtpUser(instdbUrl.getFtpHost(), map.get("_id").toString(), Constant.GENERAL, ftpInfo);
        }
        return ResultUtils.success(ftpInfo);
    }

    @Override
    public PageHelper getResourceFileTree(String resourcesId, int pid, String fileName, Integer pageOffset, Integer pageSize, String sort) {

        Query query2 = new Query();
        query2.addCriteria(Criteria.where("_id").is(resourcesId));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query2, ResourcesManage.class);
        if (null == resourcesManage) {
            List<ResourceFileTree> list = new ArrayList<>();
            return mongoUtil.pageHelper(0L, list);
        }

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

        if (StringUtils.isNotBlank(sort) && sort.contains("_")) {
            String[] s = sort.split("_");
            if (s[1].equals("asc")) {
                query.with(Sort.by(Sort.Direction.ASC, s[0]));
            } else if (s[1].equals("desc")) {
                query.with(Sort.by(Sort.Direction.DESC, s[0]));
            }
        }

        long count = mongoTemplate.count(query, ResourceFileTree.class);
        mongoUtil.start(pageOffset, pageSize, query);
        List<ResourceFileTree> list = mongoTemplate.find(query, ResourceFileTree.class);

        return mongoUtil.pageHelper(count, list);
    }

    @Override
    public void resourcesDownloadFile(String resourcesId, String fileId, HttpServletResponse response) {
        Query query2 = new Query(Criteria.where("_id").is(resourcesId));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query2, ResourcesManage.class);
        if (null == resourcesManage) {
            throw new RuntimeException(I18nUtil.get("PERMISSION_DENIED"));
        }
        Query queryFile = new Query(Criteria.where("_id").is(fileId));
        ResourceFileTree resourceFileTree = mongoTemplate.findOne(queryFile, ResourceFileTree.class);
        if (null != resourceFileTree && StringUtils.isNotBlank(resourceFileTree.getFilePath())) {
            FileUtils.downloadFile(resourceFileTree.getFilePath(), response,"");
        }
    }

    @Override
    public void downloadApiFile(String token, HttpServletResponse response) {
        String username = tokenCache.getIfPresent(token);
        Assert.isTrue(StringUtils.isNotBlank(username), I18nUtil.get("ENVIRONMENT_EXCEPTION"));

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        final String url = "/data/InstDBOpen Interface Document.pdf";
        File file = FileUtils.getResourceFile(url);
        if (!file.exists()) {
            log.error(url + "file cannot be found，file cannot be found！");
        } else {
            FileUtils.downloadFile(file.getPath(), response,"InstDBOpen Interface Document");
        }
    }

    private void removeField(Map map){
        if (!map.containsKey("url")) {
            map.put("url", instdbUrl.getCallHost() + instdbUrl.getResourcesAddress() + map.get("_id"));
        }
        if (map.containsKey("image")) {
            boolean image = (boolean) map.get("image");
            if (true == image) {
                map.put("image", instdbUrl.getCallHost() + "/api/datasetLogo/" + map.get("_id") + ".png");
            }
        }
        map.remove("callbackUrl");
        map.remove("sendFileList");
        map.remove("downloadFileFlag");
        map.remove("json_id_content");
        map.remove("images");
        map.remove("@type");
        map.remove("@context");
        map.remove("dataType");
        map.remove("versionFlag");
        map.remove("fileIsZip");
        map.remove("showFile");
        map.remove("esSync");
        map.remove("es_id");
    }

}
