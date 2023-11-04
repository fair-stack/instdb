package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.system.SearchConfig;
import cn.cnic.instdb.model.system.SearchConfigDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ResourcesService;
import cn.cnic.instdb.service.SearchConfigService;
import cn.cnic.instdb.utils.CaffeineUtil;
import cn.cnic.instdb.utils.JwtTokenUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @Author: wdd
 * @Date: 2023/02/13/17:52
 * @Description:
 */
@Service
@Slf4j
public class SearchConfigServiceImpl implements SearchConfigService {

    private final Cache<String, Object> searchConfigCache = CaffeineUtil.getSearchConfig();


    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ResourcesService resourcesService;

    @Override
    public Result setSearchConfigs(String token, SearchConfigDTO searchConfig) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null != roles && !roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        if (0 == searchConfig.getInfoSort().size()) {
            return ResultUtils.success();
        }

        String type = "";
        List<String> ids = new ArrayList<>();
        for (SearchConfigDTO.Info info : searchConfig.getInfoSort()) {
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(info.getId()));
            SearchConfig config = mongoTemplate.findOne(query, SearchConfig.class);
            if (null == config) {
                return ResultUtils.error("DATA_QUERY_EXCEPTION");
            }
            type = config.getType();
            ids.add(config.getId());
            Update update = new Update();
            update.set("sort", info.getSort());
            update.set("status", "1");
            mongoTemplate.updateFirst(query, update, SearchConfig.class);
        }

        if (ids.size() > 0) {
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").nin(ids));
            query.addCriteria(Criteria.where("type").is(type));
            Update update = new Update();
            update.set("sort", "");
            update.set("status", "-1");
            mongoTemplate.updateMulti(query, update, SearchConfig.class);
            searchConfigCache.invalidate("searchConfig" + Constant.STATISTICS);
            searchConfigCache.invalidate("searchConfig" + Constant.SEARCH);
        }
        return ResultUtils.success();
    }



    @Override
    public Result getSearchConfigs(String token, String type) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null != roles && !roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Map<String,Object> map = new HashMap<>();
        String username = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query();
        query.addCriteria(Criteria.where("type").is(type));
        List<SearchConfig> searchConfigs = mongoTemplate.find(query, SearchConfig.class);
        if (null == searchConfigs || 0 == searchConfigs.size()) {
            resetSearchConfigs(token);
        }
        map.put("all",searchConfigs);
        //Query checked items
        query.addCriteria(Criteria.where("status").is("1"));
        query.with(Sort.by(Sort.Direction.ASC, "sort"));
        List<SearchConfig> select = mongoTemplate.find(query, SearchConfig.class);
        map.put("select",select);
        Object searchConfig = searchConfigCache.getIfPresent("searchConfig"+type);
        if(null == searchConfig){
            searchConfigCache.put("searchConfig"+type,select);
        }

        return ResultUtils.success(map);
    }

    @Override
    public Result deleteSearchConfigs(String token, String id) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null != roles && !roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Update update = new Update();
        update.set("status", "-1");
        update.set("sort", "");
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, SearchConfig.class);
        return ResultUtils.success();
    }

    @Override
    public Result resetSearchConfigs(String token) {
        String usernameFromToken = "admin";
        if ("init".equals(token)) {
        } else {
            List<String> roles = jwtTokenUtils.getRoles(token);
            usernameFromToken = jwtTokenUtils.getUsernameFromToken(token);
            if (null != roles && !roles.contains(Constant.ADMIN)) {
                return ResultUtils.error("PERMISSION_FORBIDDEN");
            }
        }

        mongoTemplate.remove(new Query(),SearchConfig.class);
        List<String>list = new ArrayList<>();
        list.add(Constant.STATISTICS);
        list.add(Constant.SEARCH);

        Map<String, String> templateInfo = resourcesService.getTemplateInfo();
        templateInfo.put("approveTime&year", "Year");
        templateInfo.put("templateName&text", "Dataset Source Template");
        if (null != templateInfo && templateInfo.size() > 0) {
            for (String str:list) {
                List<SearchConfig> searchConfigList = new ArrayList<>();
                for (Map.Entry<String, String> entry : templateInfo.entrySet()) {
                    SearchConfig config = new SearchConfig(entry.getValue(), entry.getValue(), entry.getKey().split("&")[0], entry.getKey().split("&")[1], "", "-1", str, usernameFromToken);
                    searchConfigList.add(config);
                }
                mongoTemplate.insertAll(searchConfigList);
            }
            searchConfigCache.invalidate("searchConfig" + Constant.STATISTICS);
            searchConfigCache.invalidate("searchConfig" + Constant.SEARCH);
            return ResultUtils.success();
        }
        return ResultUtils.error("Template information is empty");
    }

    @Override
    public Result updateSearchConfigs(String token, String id, String name) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null != roles && !roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Update update = new Update();
        update.set("name", name);
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, SearchConfig.class);
        searchConfigCache.invalidate("searchConfig" + Constant.STATISTICS);
        searchConfigCache.invalidate("searchConfig" + Constant.SEARCH);
        return ResultUtils.success();
    }

}
