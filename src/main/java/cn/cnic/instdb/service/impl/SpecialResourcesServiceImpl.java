package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.FollowResources;
import cn.cnic.instdb.model.resources.ResourcesListManage;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.special.Special;
import cn.cnic.instdb.model.special.SpecialResources;
import cn.cnic.instdb.model.special.SpecialResourcesDTO;
import cn.cnic.instdb.model.special.SpecialVo;
import cn.cnic.instdb.repository.ResourcesManageRepository;
import cn.cnic.instdb.repository.SpecialRepository;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.service.SpecialResourcesService;
import cn.cnic.instdb.service.SpecialService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther wdd
 * @Date 2021/3/23 20:23
 * @Desc Topic and resource correlation processing
 */
@Service
@Slf4j
public class SpecialResourcesServiceImpl implements SpecialResourcesService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpecialRepository specialRepository;

    @Autowired
    private ResourcesManageRepository resourcesManageRepository;

    @Resource
    private MongoUtil mongoUtil;

    @Resource
    private JwtTokenUtils jwtTokenUtils;
    @Resource
    private UserRepository userRepository;
    @Resource
    private SpecialService specialService;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();




    @Override
    public void resourcesAddSpecial(String token,SpecialResourcesDTO specialResourcesDTO) {
        if(StringUtils.isBlank(specialResourcesDTO.getResourcesId()) ||  0 == specialResourcesDTO.getSpecialIds().size()  ){
            throw new RuntimeException(I18nUtil.get("PARAMETER_ERROR"));
        }
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        List<String> roles = consumerDO.getRoles();
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        List<SpecialResources> specialResourcesList = new ArrayList<>();
        for (String specialId:specialResourcesDTO.getSpecialIds()) {
            SpecialResources specialResources = new SpecialResources();
            specialResources.setCreateTime(LocalDateTime.now());
            //Querying resourcesname
            Optional<ResourcesManage> byId = resourcesManageRepository.findById(specialResourcesDTO.getResourcesId());
            if (byId.isPresent()){
                ResourcesManage manage = byId.get();
                specialResources.setResourcesId(specialResourcesDTO.getResourcesId());
                specialResources.setResourcesName(manage.getName());
                specialResources.setResourceType(manage.getResourceType());
                //Search for special topicsname
                Optional<Special> byId1 = specialRepository.findById(specialId);
                if (byId1.isPresent()){
                    Special special = byId1.get();
                    specialResources.setSpecialId(specialId);
                    specialResources.setSpecialName(special.getSpecialName());
                    specialResources.setSpecialNameEn(special.getSpecialNameEn());
                    specialResources.setMode("Manual addition");
                    specialResources.setOperator(consumerDO.getName());
                    specialResources.setOperatorEn(consumerDO.getEnglishName());
                    specialResources.setOperatorEmail(consumerDO.getEmailAccounts());
                }
            }
            specialResourcesList.add(specialResources);
        }
        mongoTemplate.insertAll(specialResourcesList);
        log.info("Successfully added a topic to the resource：Successfully added a topic to the resource："+specialResourcesDTO.getResourcesId()+" Successfully added a topic to the resource："+specialResourcesDTO.getSpecialIds().size());

    }

    @Override
    public void specialAddResources(String token,SpecialResourcesDTO specialResourcesDTO) {
        if(StringUtils.isBlank(specialResourcesDTO.getSpecialId()) ||  0 == specialResourcesDTO.getResourcesIds().size()  ){
            throw new RuntimeException(I18nUtil.get("PARAMETER_ERROR"));
        }
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        List<String> roles = consumerDO.getRoles();
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));
        List<SpecialResources> specialResourcesList = new ArrayList<>();
        for (String resourcesId:specialResourcesDTO.getResourcesIds()) {
            SpecialResources specialResources = new SpecialResources();
            specialResources.setCreateTime(LocalDateTime.now());
            //Querying resourcesname
            Optional<ResourcesManage> byId = resourcesManageRepository.findById(resourcesId);
            if(byId.isPresent()){
                ResourcesManage resourcesManage = byId.get();
                specialResources.setResourcesName(resourcesManage.getName());
                specialResources.setResourcesId(resourcesId);
                specialResources.setResourceType(resourcesManage.getResourceType());
                //Search for special topicsname
                Optional<Special> byId1 = specialRepository.findById(specialResourcesDTO.getSpecialId());
                if (byId1.isPresent()){
                    Special special = byId1.get();
                    specialResources.setSpecialId(specialResourcesDTO.getSpecialId());
                    specialResources.setSpecialName(special.getSpecialName());
                    specialResources.setSpecialNameEn(special.getSpecialNameEn());
                    specialResources.setMode("Manual addition");
                    specialResources.setOperator(consumerDO.getName());
                    specialResources.setOperatorEn(consumerDO.getEnglishName());
                    specialResources.setOperatorEmail(consumerDO.getEmailAccounts());
                }
            }
            specialResourcesList.add(specialResources);
        }
        mongoTemplate.insertAll(specialResourcesList);
        log.info("Successfully added resources for the theme：Successfully added resources for the theme："+specialResourcesDTO.getSpecialId()+" Successfully added resources for the theme："+specialResourcesDTO.getResourcesIds().size());
    }

    @Override
    public List<Map<String, Object>> getSpecialByResourcesId(String resourcesId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        List<SpecialResources> specialResources = mongoTemplate.find(query, SpecialResources.class);
        if (null != specialResources && !specialResources.isEmpty()) {
            for (SpecialResources data : specialResources) {
                Optional<Special> byId = specialRepository.findById(data.getSpecialId());
                if (byId.isPresent()) {
                    Map<String, Object> map = new HashMap<>();
                    Special special = byId.get();
                    map.put("id", special.getId());
                    map.put("name", special.getSpecialName());
                    map.put("name_en", special.getSpecialNameEn());
                    map.put("resourcesNum", special.getResourcesNum());
                    map.put("logo", special.getLogo());
                    list.add(map);
                }
            }
        }
        return list;
    }


    @Override
    public Map<String, Object> getResourcesListBySpecialId(String specialId) {
        Map<String, Object> map = new HashMap<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("specialId").is(specialId));
        List<SpecialResources> specialResources = mongoTemplate.find(query, SpecialResources.class);
        if(null != specialResources && !specialResources.isEmpty()){
            for (SpecialResources data:specialResources) {
                Optional<ResourcesManage> byId = resourcesManageRepository.findById(data.getResourcesId());
                if(byId.isPresent()){
                    ResourcesManage resourcesManage = byId.get();
                    map.put(resourcesManage.getId(),resourcesManage.getName());
                }
            }
        }
        return map;
    }

    @Override
    public PageHelper getResourcesByNoSpecial(String specialId,String resourcesName, Integer pageOffset, Integer pageSize) {

        Query query = new Query();
        query.addCriteria(Criteria.where("specialId").is(specialId));

        List<SpecialResources> list = mongoTemplate.find(query, SpecialResources.class);

        List<String> strList = new ArrayList<>();
        for (SpecialResources specialResources:list) {
            strList.add(specialResources.getResourcesId());
        }
        String[] obj =new String[list.size()];

        for(int i=0;i<list.size();i++) {
            obj[i]=strList.get(i);
        }

        Query query1 = new Query();
        query1.addCriteria(Criteria.where("_id").nin(obj));
        query1.addCriteria(Criteria.where("status").is(Constant.Approval.ADOPT));
        if (StringUtils.isNotBlank(resourcesName)) {
            query1.addCriteria(Criteria.where("name").regex(resourcesName));
        }

        List<ResourcesListManage> listManages = new ArrayList<>();
        long count = mongoTemplate.count(query1, ResourcesManage.class);
        mongoUtil.start(pageOffset, pageSize, query1);
        List<ResourcesManage> resourcesManage = mongoTemplate.find(query1, ResourcesManage.class);
        if (null != resourcesManage && resourcesManage.size() > 0) {
            for (ResourcesManage data : resourcesManage) {
                ResourcesListManage manage = new ResourcesListManage();
                BeanUtils.copyProperties(data, manage);
                listManages.add(manage);
            }
        }


        return mongoUtil.pageHelper(count, listManages);


    }

    @Override
    public PageHelper getSpecialByNoResources(String resourcesId, String specialName,Integer pageOffset, Integer pageSize) {

        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        List<SpecialResources> list = mongoTemplate.find(query, SpecialResources.class);

        List<String> strList = new ArrayList<>();
        for (SpecialResources specialResources:list) {
            strList.add(specialResources.getSpecialId());
        }
//        String[] obj =new String[list.size()];
//
//        for(int i=0;i<list.size();i++) {
//            obj[i]=strList.get(i);
//        }

        Query query1 = new Query();
//        query1.addCriteria(Criteria.where("_id").nin(obj));
//        if (StringUtils.isNotBlank(specialName)) {
//            query1.addCriteria(Criteria.where("specialName").regex(specialName));
//        }

        List<SpecialVo> specialVo = new ArrayList<>();
        long count = mongoTemplate.count(query1, Special.class);

        mongoUtil.start(pageOffset, pageSize, query1);
        List<Special> specialList = mongoTemplate.find(query1, Special.class);
        if (null != specialList && specialList.size() > 0) {
            for (Special data : specialList) {
                SpecialVo vo = new SpecialVo();
                BeanUtils.copyProperties(data, vo);
                specialVo.add(vo);
                for (String str:strList) {
                    if(str.equals(data.getId())){
                       vo.setResources(true);
                    }
                }
            }
        }


        specialService.updateNumSpecial(specialVo);

        return mongoUtil.pageHelper(count, specialVo);

    }


    @Override
    public void deleteResourcesInSpecial(SpecialResourcesDTO specialResourcesDTO) {

        if(StringUtils.isBlank(specialResourcesDTO.getSpecialId()) ||  0 == specialResourcesDTO.getResourcesIds().size()  ){
            throw new RuntimeException(I18nUtil.get("PARAMETER_ERROR"));
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("specialId").is(specialResourcesDTO.getSpecialId()));
        query.addCriteria(Criteria.where("resourcesId").in(specialResourcesDTO.getResourcesIds()));
        mongoTemplate.remove(query, SpecialResources.class);
    }


    @Override
    public PageHelper getResourcesBySpecialId(String token,String specialId,String resourcesName,String resourceType, Integer pageOffset, Integer pageSize,String sort) {
        Map<String, String> map = new HashMap<>();
        Assert.isTrue(StringUtils.isNotBlank(specialId), I18nUtil.get("PARAMETER_ERROR"));
        Query query = new Query();
        query.addCriteria(Criteria.where("specialId").is(specialId));
        List<SpecialResources> specialResources = mongoTemplate.find(query, SpecialResources.class);
        List<ResourcesListManage> listManages = new ArrayList<>();
        if (null != specialResources && !specialResources.isEmpty()) {
            for (SpecialResources data : specialResources) {
                Query resourcesQuery = new Query();
                resourcesQuery.addCriteria(Criteria.where("_id").is(data.getResourcesId()));
                //according toidaccording to
                ResourcesListManage resourcesManage = mongoTemplate.findOne(resourcesQuery, ResourcesListManage.class, "resources_manage");
                //Complete the special resource table Complete the special resource table
                if (null != resourcesManage) {
                    if (StringUtils.isBlank(data.getResourceType())) {
                        Update update = new Update();
                        Query queryUpdate = new Query();
                        queryUpdate.addCriteria(Criteria.where("_id").is(data.getId()));
                        update.set("resourceType", resourcesManage.getResourceType());
                        mongoTemplate.upsert(queryUpdate, update, SpecialResources.class);
                    }
                    listManages.add(resourcesManage);
                }
            }
        }
        //Resource Type Classification
        if (null != listManages && listManages.size() > 0) {
            Map<String, List<ResourcesListManage>> collect = listManages.stream().collect(Collectors.groupingBy(ResourcesListManage::getResourceType));
            if (null != collect && collect.size() > 0) {
                for (Map.Entry<String, List<ResourcesListManage>> entry : collect.entrySet()) {
                    map.put(entry.getKey() + "-" + entry.getValue().size(), "");
                }
                CommonUtils.addResourceType(map, Constant.LanguageStatus.RESOURCE_TYPES);
            }
        }
        if (StringUtils.isNotBlank(resourcesName)) {
            query.addCriteria(Criteria.where("resourcesName").regex(resourcesName));
        }
        long count = mongoTemplate.count(query, SpecialResources.class);
        if (StringUtils.isNotBlank(resourceType)) {
            query.addCriteria(Criteria.where("resourceType").is(resourceType));
        }
        mongoUtil.start(pageOffset, pageSize, query);
        List<SpecialResources> specialResourcesData = mongoTemplate.find(query, SpecialResources.class);
        List<ResourcesListManage> listManagesData = new ArrayList<>();
        if (null != specialResourcesData && !specialResourcesData.isEmpty()) {
            for (SpecialResources data : specialResourcesData) {
                Query resourcesQuery = new Query();
                resourcesQuery.addCriteria(Criteria.where("_id").is(data.getResourcesId()));
                //according toidaccording to
                ResourcesListManage resourcesManage = mongoTemplate.findOne(resourcesQuery, ResourcesListManage.class, "resources_manage");

                //Is it being followed
                if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(jwtTokenUtils.getUserIdFromToken(token))) {
                    Optional<ConsumerDO> byId = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token));
                    if (byId.isPresent()) {
                        ConsumerDO user = byId.get();
                        Query queryF = new Query();
                        queryF.addCriteria(Criteria.where("username").is(user.getEmailAccounts()));
                        queryF.addCriteria(Criteria.where("resourcesId").is(resourcesManage.getId()));
                        FollowResources followResources = mongoTemplate.findOne(queryF, FollowResources.class);
                        resourcesManage.setFollow(null == followResources ? "no" : "yes");
                    }
                }

                String lang = tokenCache.getIfPresent("lang");
                if (Constant.Language.english.equals(lang)) {
                    if (StringUtils.isNotBlank(resourcesManage.getName_en())) {
                        resourcesManage.setName(resourcesManage.getName_en());
                    }
                    if (null != resourcesManage.getKeywords_en()) {
                        resourcesManage.setKeywords(resourcesManage.getKeywords_en());
                    }
                    if (null != resourcesManage.getAuthor() && resourcesManage.getAuthor().size() > 0) {
                        JSONArray author = resourcesManage.getAuthor();
                        for (int i = 0; i < author.size(); i++) {
                            net.sf.json.JSONObject o = (net.sf.json.JSONObject) author.get(i);
                            if (null != o.get("en_name") && StringUtils.isNotBlank(o.getString("en_name"))) {
                                o.put("name", o.getString("en_name"));
                            }
                        }
                    }
                }
                resourcesManage.setMode(data.getMode());
                resourcesManage.setOperator(data.getOperator());
                resourcesManage.setOperatorEn(data.getOperatorEn());
                resourcesManage.setOperatorEmail(data.getOperatorEmail());
                listManagesData.add(resourcesManage);
            }
        }
        //sort
        if (StringUtils.isNotBlank(sort) && sort.contains("_")) {
            String[] s = sort.split("_");
            if (s[1].equals("asc")) {
                Collections.sort(listManagesData, new Comparator<ResourcesListManage>() {
                    @Override
                    public int compare(ResourcesListManage o1, ResourcesListManage o2) {
                        //Ascending order
                        return o1.getCreateTime().compareTo(o2.getCreateTime());
                    }
                });
            } else if (s[1].equals("desc")) {
                Collections.sort(listManagesData, new Comparator<ResourcesListManage>() {
                    @Override
                    public int compare(ResourcesListManage o1, ResourcesListManage o2) {
                        //Descending order
                        return o2.getCreateTime().compareTo(o1.getCreateTime());
                    }
                });
            }
        }
        return mongoUtil.pageHelper(count, listManagesData, map);

    }


}
