package cn.cnic.instdb.cacheLoading;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.config.*;
import cn.cnic.instdb.model.login.EscConf;
import cn.cnic.instdb.model.login.WechatConf;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.model.login.LoginConfig;
import cn.cnic.instdb.model.login.LoginConfigCas;
import cn.cnic.instdb.utils.CaffeineUtil;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cached data -  Cached data
 */
@Component
public class CacheLoading {

    private MongoTemplate mongoTemplate;

    public static final String BASIC_CONFIGURATION_COLLECTION_NAME = "basic_configuration";

    public CacheLoading(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private final Cache<String, Object> config = CaffeineUtil.getConfig();


    /**
     * Load technology cloud configuration
     */
    public Object loadingUmt() {
        Object umt = config.getIfPresent(Constant.LoginWay.UMP);
        if (null == umt) {//Load Cache
            List<LoginConfig> all = mongoTemplate.findAll(LoginConfig.class);
            if (all.size() > 0) {
                LoginConfig umtConf = all.get(0);
                if (umtConf.getIsOpen()) {
                    config.put(Constant.LoginWay.UMP, umtConf);
                    umt = umtConf;
                }
            }
        }
        return umt;
    }


    /**
     * loadingcasloading
     */
    public Object loadingCasLoginConf() {
        Object umt = config.getIfPresent(Constant.LoginWay.CAS);
        if (null == umt) {//Load Cache
            List<LoginConfigCas> all = mongoTemplate.findAll(LoginConfigCas.class);
            if (all.size() > 0) {
                LoginConfigCas umtConf = all.get(0);
                if (umtConf.getIsOpen()) {
                    config.put(Constant.LoginWay.CAS, umtConf);
                    umt = umtConf;
                }
            }
        }
        return umt;
    }


    //WeChat configuration
    public Object loadingWechat() {
        Object wechat = config.getIfPresent(Constant.LoginWay.WECHAT);
        if (wechat == null) {
            List<WechatConf> all = mongoTemplate.findAll(WechatConf.class);
            if (all.size() > 0) {
                WechatConf wechatConf = all.get(0);
                if (wechatConf.getIsOpen()) {
                    config.put(Constant.LoginWay.WECHAT, wechatConf);
                    wechat = wechatConf;
                }
            }
        }
        return wechat;
    }


    //Shared Network
    public Object loadingEsc() {
        Object esc = config.getIfPresent(Constant.LoginWay.ESCIENCE);
        if (esc == null) {
            List<EscConf> all = mongoTemplate.findAll(EscConf.class);
            if (all.size() > 0) {
                EscConf escConf = all.get(0);
                if (escConf.getIsOpen()) {
                    config.put(Constant.LoginWay.ESCIENCE, escConf);
                    esc = escConf;
                }
            }
        }
        return esc;
    }


    //Central account
    public Object loadingCenter() {
        Object acc = config.getIfPresent("acc");
        if (acc == null) {
            Query query = new Query();
            CenterAccount centerAccount = mongoTemplate.findOne(query, CenterAccount.class);
            if (null != centerAccount) {
                if (centerAccount.isNetwork()) {
                    config.put("acc", centerAccount);
                }
                acc = centerAccount;
            }
        }
        return acc;
    }

    //Loading System Configuration
    public Object loadingConfig() {
        Object basis = config.getIfPresent("basis");
        if (null == basis) {
            Query query = new Query();
            BasicConfigurationVo one = mongoTemplate.findOne(query, BasicConfigurationVo.class, BASIC_CONFIGURATION_COLLECTION_NAME);
            if (null != one) {
                List<LoginConfig> all = mongoTemplate.findAll(LoginConfig.class);
                LoginConfig loginConfig = 0 == all.size() ? null : all.get(0);
                one.setIsCloudLogin(null == loginConfig ? false : loginConfig.getIsOpen());

                List<LoginConfigCas> cas = mongoTemplate.findAll(LoginConfigCas.class);
                LoginConfigCas loginConfigCas = 0 == cas.size() ? null : cas.get(0);
                one.setIsCasLogin(null == loginConfigCas ? false : loginConfigCas.getIsOpen());

                List<WechatConf> wechatConf = mongoTemplate.findAll(WechatConf.class);
                WechatConf wechat = 0 == wechatConf.size() ? null : wechatConf.get(0);
                one.setIsWechatLogin(null == wechat ? false : wechat.getIsOpen());

                List<EscConf> escConfs = mongoTemplate.findAll(EscConf.class);
                EscConf escConf = 0 == escConfs.size() ? null : escConfs.get(0);
                one.setIsEscienceLogin(null == escConf ? false : escConf.getIsOpen());

                if (StringUtils.isBlank(one.getName())) {
                    one.setName("Institutional data repository");
                }
                if (StringUtils.isBlank(one.getOrgName())) {
                    CenterAccount centerConf = (CenterAccount) loadingCenter();
                    if (null != centerConf && StringUtils.isNotBlank(centerConf.getOrgName())) {
                        one.setOrgName(centerConf.getOrgName());
                        Update update  = new Update();
                        update.set("orgName", centerConf.getOrgName());
                        Query query1 = new Query();
                        query1.addCriteria(Criteria.where("_id").is(one.getId()));
                        mongoTemplate.updateFirst(query1, update, BasicConfiguration.class);
                    }

                }

                //Filing number processing
                if (StringUtils.isBlank(one.getRecordNo()) && null != one.getRecordNumber() && one.getRecordNumber().size() > 0) {
                    one.setRecordNo(one.getRecordNumber().get(0).getName());
                    if (StringUtils.isBlank(one.getPublicSecurityRecordNo()) && null != one.getRecordNumber() && one.getRecordNumber().size() > 1) {
                        one.setPublicSecurityRecordNo(one.getRecordNumber().get(1).getName());
                    }
                }

                config.put("basis", one);
                basis = one;
            }
        }
        return basis;

    }


    //Loading About Configuration
    public Object loadingAboutConfig() {
        Object basis = config.getIfPresent("aboutBasis");
        if (null == basis) {
            Query query  = new Query();
            AboutConfiguration one = mongoTemplate.findOne(query, AboutConfiguration.class);
            config.put("aboutBasis", one);
            basis = one;
        }
        return basis;
    }

    //Load homepage configuration
    public Object loadingIndexConfig() {
        Object basis = config.getIfPresent("indexBasis");
        if (null == basis) {
            Query query  = new Query();
            IndexConfiguration one = mongoTemplate.findOne(query, IndexConfiguration.class);
            config.put("indexBasis", one);
            basis = one;
        }
        return basis;
    }

    // Load system mailbox
    public EmailConfig getEmailConfig(){
        Object emailConfig = config.getIfPresent("emailConfig");

        if(null == emailConfig){
            EmailConfig systemConf = mongoTemplate.findOne(new Query(), EmailConfig.class);
            if(null != systemConf){
                config.put("emailConfig",systemConf);
                return systemConf;
            }
        }
        return (EmailConfig) emailConfig;
    }

}
