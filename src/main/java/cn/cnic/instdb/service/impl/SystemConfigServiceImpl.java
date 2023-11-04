package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.cacheLoading.CacheLoading;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.config.*;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.system.SubjectArea;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SystemConfigService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class SystemConfigServiceImpl implements SystemConfigService {

    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private JwtTokenUtils jwtTokenUtils;
    @Resource
    private UserRepository userRepository;

    @Resource
    private InstdbUrl instdbUrl;

    @Override
    public Result setBasicConfig(String token, BasicConfigurationDTO basicConfigurationDTO) {

        String name = check(token);

        Query query = new Query();
        BasicConfiguration basicConfiguration = mongoTemplate.findOne(query, BasicConfiguration.class);
        BasicConfiguration basic = new BasicConfiguration();
        basic.setId(basicConfiguration.getId());
        BeanUtils.copyProperties(basicConfigurationDTO, basic);

        basic.setUpdateByUser(name);
        basic.setUpdateTime(LocalDateTime.now());

        //Image processing
        if (StringUtils.isNotBlank(basic.getLogo())) {
            if (basic.getLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), basic.getLogo(), uuid);
                basic.setLogo(uuid + Constant.PNG);
                if (StringUtils.isNotBlank(basicConfiguration.getLogo())) {
                    FileUtils.deleteFile(instdbUrl.getBanaer_icoLogo() + basicConfiguration.getLogo());
                }
            }
        }
        if (StringUtils.isNotBlank(basic.getIcoLogo())) {
            if (basic.getIcoLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), basic.getIcoLogo(), uuid + "icoLogo");
                basic.setIcoLogo(uuid + "icoLogo" + Constant.PNG);
                if (StringUtils.isNotBlank(basicConfiguration.getIcoLogo())) {
                    FileUtils.deleteFile(instdbUrl.getBanaer_icoLogo() + basicConfiguration.getIcoLogo());
                }
            }
        }
        if (StringUtils.isNotBlank(basic.getIcoEndLogo())) {
            if (basic.getIcoEndLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), basic.getIcoEndLogo(), uuid + "icoEndLogo");
                basic.setIcoEndLogo(uuid + "icoEndLogo" + Constant.PNG);
                if (StringUtils.isNotBlank(basicConfiguration.getIcoEndLogo())) {
                    FileUtils.deleteFile(instdbUrl.getBanaer_icoLogo() + basicConfiguration.getIcoEndLogo());
                }
            }
        }
        basic.setResourcesEndLogo(basicConfiguration.getResourcesEndLogo());
        basic.setResourcesTopLogo(basicConfiguration.getResourcesTopLogo());
        mongoTemplate.save(basic);
        config.invalidate("basis");
        return ResultUtils.success();
    }

    @Override
    public Result setResourcesConfig(String resourcesTopLogo, String resourcesEndLogo) {
        Query query = new Query();
        Update update = new Update();
        BasicConfiguration basicConfiguration = mongoTemplate.findOne(query, BasicConfiguration.class);

        if (StringUtils.isNotBlank(resourcesTopLogo)) {
            if (resourcesTopLogo.contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), resourcesTopLogo, uuid);
                update.set("resourcesTopLogo", uuid + Constant.PNG);
                if (StringUtils.isNotBlank(basicConfiguration.getResourcesTopLogo())) {
                    FileUtils.deleteFile(instdbUrl.getBanaer_icoLogo() + basicConfiguration.getResourcesTopLogo());
                }
            }
        }
        if (StringUtils.isNotBlank(resourcesEndLogo)) {
            if (resourcesEndLogo.contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), resourcesEndLogo, uuid);
                update.set("resourcesEndLogo", uuid + Constant.PNG);
                if (StringUtils.isNotBlank(basicConfiguration.getResourcesEndLogo())) {
                    FileUtils.deleteFile(instdbUrl.getBanaer_icoLogo() + basicConfiguration.getResourcesEndLogo());
                }
            }
        }

        query.addCriteria(Criteria.where("_id").is(basicConfiguration.getId()));
        mongoTemplate.updateFirst(query, update, BasicConfiguration.class);
        config.invalidate("basis");
        return ResultUtils.success();
    }


    @Override
    public BasicConfigurationVo getBasicConfig() {
        //Obtain basic information from cache
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        BasicConfigurationVo basic = (BasicConfigurationVo) cacheLoading.loadingConfig();
        return basic;
    }

    @Override
    public Result setAboutConfig(String token, AboutConfigurationDTO aboutConfiguration) {
        String name = check(token);

        Query query  = new Query();
        AboutConfiguration data = mongoTemplate.findOne(query, AboutConfiguration.class);
        AboutConfiguration about = new AboutConfiguration();
        BeanUtils.copyProperties(aboutConfiguration, about);
        about.setId(data.getId());

        //Image processing
        if (StringUtils.isNotBlank(about.getBanaerLogo())) {
            if (about.getBanaerLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), about.getBanaerLogo(), uuid);
                about.setBanaerLogo(uuid + Constant.PNG);
                if (StringUtils.isNotBlank(data.getBanaerLogo())) {
                    FileUtils.deleteFile(instdbUrl.getBanaer_icoLogo() + data.getBanaerLogo());
                }
            }
        }

        //Empty indicates creation operation
        about.setUpdateByUser(name);
        mongoTemplate.save(about);
        config.invalidate("aboutBasis");
        return ResultUtils.success();

    }

    @Override
    public AboutConfiguration getAboutConfig() {
        //Obtain basic information from cache
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        AboutConfiguration basic = (AboutConfiguration) cacheLoading.loadingAboutConfig();
        return basic;
    }

    private String check(String token){
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        List<String> roles = consumerDO.getRoles();
        Assert.isTrue(null != roles, I18nUtil.get("LOGIN_EXCEPTION"));
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));
        return consumerDO.getName();
    }


    @Override
    public Result setIndexConfig(String token, IndexConfigurationDTO indexConfig) {
        String name = check(token);
        Query query = new Query();
        IndexConfiguration data = mongoTemplate.findOne(query, IndexConfiguration.class);
        IndexConfiguration configConfig = new IndexConfiguration();
        configConfig.setId(data.getId());
        BeanUtils.copyProperties(indexConfig, configConfig);

        if (null != indexConfig.getBanaerLogo() && indexConfig.getBanaerLogo().size() > 0) {
            List<IndexConfigurationDTO.PathInfo> banaerLogoDTO = indexConfig.getBanaerLogo();
            List<IndexConfiguration.PathInfo> banaerLogo = new ArrayList<>();
            for (IndexConfigurationDTO.PathInfo pathInfo : banaerLogoDTO) {
                //Image processing
                if (StringUtils.isNotBlank(pathInfo.getPath())) {
                    if (pathInfo.getPath().contains("data:image")) {
                        String uuid = CommonUtils.generateUUID();
                        CommonUtils.base64ToFile(instdbUrl.getBanaer_icoLogo(), pathInfo.getPath(), uuid);
                        pathInfo.setPath(uuid + Constant.PNG);
                    }
                }
                IndexConfiguration.PathInfo pathInfoD = new IndexConfiguration.PathInfo();
                pathInfoD.setPath(pathInfo.getPath());
                pathInfoD.setSort(pathInfo.getSort());
                banaerLogo.add(pathInfoD);
            }
            configConfig.setBanaerLogo(banaerLogo);
        }

        configConfig.setUpdateByUser(name);
        configConfig.setUpdateTime(LocalDateTime.now());
        mongoTemplate.save(configConfig);
        config.invalidate("indexBasis");
        return ResultUtils.success();
    }

    @Override
    public IndexConfiguration getIndexConfig() {
        //Obtain basic information from cache
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        IndexConfiguration basic = (IndexConfiguration) cacheLoading.loadingIndexConfig();
        return basic;
    }

    @Override
    public Result setSubjectAreaConfig(SubjectAreaIndex subjectAreaIndex) {
        if(null == subjectAreaIndex.getInfoSort() || subjectAreaIndex.getInfoSort().size() == 0){
            return ResultUtils.error("PARAMETER_ERROR");
        }
//        Query queryData = new Query();
//        List<SubjectArea> lists = mongoTemplate.find(queryData, SubjectArea.class);
//        if (null != lists && lists.size() > 0) {
//            for (SubjectArea list : lists) {
//                Query query = new Query();
//                query.addCriteria(Criteria.where("_id").is(list.getId()));
//                Update update = new Update();
//                update.set("sort", "");
//                mongoTemplate.updateFirst(query, update, SubjectArea.class);
//            }
//        }

        for (SubjectAreaIndex.Info map:subjectAreaIndex.getInfoSort()) {
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(map.getId()));
            SubjectArea subjectArea = mongoTemplate.findOne(query, SubjectArea.class);
            if(null == subjectArea){
                return ResultUtils.error("DATA_QUERY_EXCEPTION");
            }
            Update update = new Update();
            update.set("sort", map.getSort());
            mongoTemplate.updateFirst(query, update, SubjectArea.class);
        }

        return Result.success();
    }


    @Override
    public Result setDsUrl(String dsUrl) {

        if (StringUtils.isBlank(dsUrl) || !CommonUtils.urlVerify(dsUrl)) {
            return ResultUtils.error("URL_INCORRECT");
        }
        Query query = new Query();
        IndexConfiguration indexConfig = mongoTemplate.findOne(query, IndexConfiguration.class);
        Update update = new Update();
        query.addCriteria(Criteria.where("_id").is(indexConfig.getId()));
        update.set("dsUrl", dsUrl);
        mongoTemplate.updateFirst(query, update, IndexConfiguration.class);
        config.invalidate("indexBasis");
        return ResultUtils.success();
    }


}
