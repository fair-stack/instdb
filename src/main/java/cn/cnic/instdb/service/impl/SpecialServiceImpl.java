package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.special.AuthorizationPerson;
import cn.cnic.instdb.model.special.Special;
import cn.cnic.instdb.model.special.SpecialVo;
import cn.cnic.instdb.repository.ResourcesManageRepository;
import cn.cnic.instdb.repository.SpecialRepository;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SpecialResourcesService;
import cn.cnic.instdb.service.SpecialService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @Auther wdd
 * @Date 2021/3/23 20:23
 * @Desc special
 */
@Service
public class SpecialServiceImpl implements SpecialService {

    public static final String COLLECTION_NAME = "special";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private MongoUtil mongoUtil;

    @Resource
    private UserRepository userRepository;

    @Autowired
    private SpecialRepository specialRepository;
    @Autowired
    private SpecialResourcesService specialResourcesService;

    @Autowired
    private ResourcesManageRepository resourcesManageRepository;

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    @Resource
    private InstdbUrl instdbUrl;

    @Override
    public Result save(String token,Special.SpecialDTO specialDTO) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        List<String> roles = consumerDO.getRoles();
        if(!roles.contains(Constant.ADMIN)){
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        if(StringUtils.isBlank(specialDTO.getSpecialName())){
            return ResultUtils.error("SPECIAL_NAME_NOTNULL");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("specialName").is(specialDTO.getSpecialName()));
        Special specialData = mongoTemplate.findOne(query, Special.class);
        if(null != specialData){
            return ResultUtils.error("SPECIAL_NAME_ALREADY_EXISTS");
        }

        Special special = new Special();
        BeanUtils.copyProperties(specialDTO, special);
        special.setId(CommonUtils.generateUUID());
        special.setCreateTime(LocalDateTime.now());
        special.setUpdateTime(LocalDateTime.now());
        special.setCreateByUser(consumerDO.getName());
        //Image processing
        if (StringUtils.isNotBlank(special.getLogo())) {
            if (special.getLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSpecialLogo(), special.getLogo(), uuid);
                special.setLogo(uuid + Constant.PNG);
            }
        }
        //Set up administrator
       // setAuthorizationList(specialDTO.getUserId(),special);
        specialRepository.save(special);
        return ResultUtils.success("SPECIAL_ADD");
    }

    /**
     * Special administrator handling
     * @param listUserId
     * @param special
     */
    private void setAuthorizationList(List<String> listUserId , Special special){
        if (null != listUserId && listUserId.size() > 0) {
            Set<AuthorizationPerson> authorizationList = new HashSet<>();
            for (String userId : listUserId) {
                Optional<ConsumerDO> user = userRepository.findById(userId);
                if (user.isPresent() && user.get().getState() == 1) {
                    ConsumerDO consumerDO = user.get();
                    AuthorizationPerson authorizationPerson = new AuthorizationPerson(userId, consumerDO.getName(), consumerDO.getEmailAccounts());
                    authorizationList.add(authorizationPerson);
                }
            }
            special.setAuthorizationList(authorizationList);
        }
    }


    private String check(String token){
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        List<String> roles = consumerDO.getRoles();
        Assert.isTrue(null != roles, I18nUtil.get("LOGIN_EXCEPTION"));
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));
        return consumerDO.getName();
    }


    @Override
    public Result update(String token,Special.SpecialDTO specialDTO) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        List<String> roles = consumerDO.getRoles();
        if(!roles.contains(Constant.ADMIN)){
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("specialName").is(specialDTO.getSpecialName()));
        Special specialData = mongoTemplate.findOne(query, Special.class);
        if (null != specialData && !specialData.getId().equals(specialDTO.getId())) {
            return ResultUtils.error("SPECIAL_NAME_ALREADY_EXISTS");
        }

        Optional<Special> byId = specialRepository.findById(specialDTO.getId());
        if(!byId.isPresent()){
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        Special special = new Special();
        BeanUtils.copyProperties(specialDTO, special);
        special.setUpdateTime(LocalDateTime.now());
        special.setCreateByUser(byId.get().getCreateByUser());
        special.setCreateTime(byId.get().getCreateTime());
        //Image processing
        if (StringUtils.isNotBlank(special.getLogo())) {
            if (special.getLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSpecialLogo(), special.getLogo(), uuid);
                special.setLogo(uuid + Constant.PNG);
                if (StringUtils.isNotBlank( byId.get().getLogo())) {
                    FileUtils.deleteFile(instdbUrl.getSpecialLogo() + byId.get().getLogo());
                }
            }
        }
        //Set up administrator
        //setAuthorizationList(specialDTO.getUserId(),special);
        specialRepository.save(special);
        return ResultUtils.success("SPECIAL_UPDATE");
    }

    @Override
    public Result delete(String token, String id) {

        Optional<Special> byId = specialRepository.findById(id);
        if (!byId.isPresent()) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        Map<String, Object> resourcesMap = specialResourcesService.getResourcesListBySpecialId(id);
        if (resourcesMap.size() > 0) {
            return ResultUtils.error("RESOURCE_SPECIAL_YES");
        }
        specialRepository.deleteById(id);
        return ResultUtils.success("SPECIAL_DELETE");
    }

    @Override
    public PageHelper findAllSpecial(String specialName, Integer pageOffset, Integer pageSize,String sort) {
        String lang = tokenCache.getIfPresent("lang");

        Query query = new Query();
        if (StringUtils.isNotBlank(specialName)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(specialName) + ".*$", Pattern.CASE_INSENSITIVE);
            if (StringUtils.isBlank(lang) || Constant.Language.chinese.equals(lang)) {
                query.addCriteria(Criteria.where("specialName").regex(pattern));
            } else {
                query.addCriteria(Criteria.where("specialNameEn").regex(pattern));
            }
        }
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

        long count = mongoTemplate.count(query, COLLECTION_NAME);
        mongoUtil.start(pageOffset, pageSize, query);
        List<SpecialVo> list = mongoTemplate.find(query, SpecialVo.class, COLLECTION_NAME);


        //Obtain and update information on three topics when querying the list
        if (null != list && list.size() > 0) {
            updateNumSpecial(list);
        }

        return mongoUtil.pageHelper(count, list);
    }


    /**
     * Set the resource directory under the theme，Set the resource directory under the theme
     *
     * @param special
     */
    private void getSpecialNum(SpecialVo special) {

        //Obtain the number of included resources under the topic
        Map<String, Object> resourcesMap = specialResourcesService.getResourcesListBySpecialId(special.getId());
        special.setResourcesNum(resourcesMap.size());
        //Get Visits
        //Get downloads
        int downNum = 0;
        int fangwenNum = 0;
        long storageNum = 0;
        for (String resourcesId : resourcesMap.keySet()) {
            Optional<ResourcesManage> byId = resourcesManageRepository.findById(resourcesId);
            ResourcesManage manage = byId.get();
            downNum += manage.getDownloadNum();
            fangwenNum += manage.getVisitNum();
            storageNum += manage.getStorageNum();
        }
        special.setVisitNum(fangwenNum);
        special.setDownloadNum(downNum);
        special.setStorageNum(storageNum);

    }

    @Override
    public SpecialVo getSpecialById(String id) {
        Assert.isTrue(StringUtils.isNotBlank(id), I18nUtil.get("PARAMETER_ERROR"));
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        SpecialVo one = mongoTemplate.findOne(query, SpecialVo.class, COLLECTION_NAME);
        if(null == one.getAuthorizationList()){
            one.setAuthorizationList(new HashSet<>());
        }
        //Obtain the resource directory under the topic，Obtain the resource directory under the topic
        getSpecialNum(one);
        return one;
    }

    @Override
    public void updateNumSpecial(List<SpecialVo> specialVo) {
        for (SpecialVo special : specialVo) {
            //Obtain the resource directory under the topic，Obtain the resource directory under the topic
            getSpecialNum(special);
            Special obj = new Special();
            BeanUtils.copyProperties(special, obj);

//            if(StringUtils.isNotBlank(special.getLogo()) && !special.getLogo().contains("data:image")){
//                String base64 = CommonUtils.imageToBase64Str(instdbUrl.getSpecialLogo() + special.getLogo());
//                obj.setLogo(base64);
//            }
//            //Image processing
//            if (StringUtils.isNotBlank(special.getLogo())) {
//                if (special.getLogo().contains("data:image")) {
//                    CommonUtils.base64ToFile(instdbUrl.getSpecialLogo(), special.getLogo(), special.getId());
//                    obj.setLogo(special.getId() + Constant.PNG);
//                }
//            }
            obj.setSpecialName(special.getSpecialName());
            obj.setSpecialNameEn(special.getSpecialNameEn());
            obj.setSpecialDesc(special.getSpecialDesc());
            obj.setSpecialDescEn(special.getSpecialDescEn());
            obj.setSpecialTag(special.getSpecialTag());
            obj.setSpecialTagEn(special.getSpecialTagEn());
            //Update three numbers
            specialRepository.save(obj);
        }
    }


    @Override
    public List<Map<String, String>> userList(String specialId,String username,Integer pageOffset, Integer pageSize) {

        Special special = specialRepository.findById(specialId).get();

        Query query = new Query();
        if (StringUtils.isNotBlank(username)) {
            query.addCriteria(Criteria.where("name").regex(".*?\\" + username + ".*"));
        }
        mongoUtil.start(pageOffset, pageSize, query);
        List<ConsumerDO> userPage = mongoTemplate.find(query, ConsumerDO.class);

        //The administrator already exists
        Set<AuthorizationPerson> authorizationList = special.getAuthorizationList();
        List<Map<String, String>> invited = new ArrayList<>();

        Set<String> set = new HashSet<>();
        if(null!= authorizationList &&!authorizationList.isEmpty()){
            for (AuthorizationPerson authorizationPerson : authorizationList) {
                set.add(authorizationPerson.getUserId());
            }
        }
        for (ConsumerDO consumerDO : userPage) {
            Map<String, String> map = new HashMap<>(8);
            String userId = consumerDO.getId();
            map.put("userId", userId);
            map.put("email", consumerDO.getEmailAccounts());
            map.put("userName", consumerDO.getName());
            map.put("states", set.contains(userId) ? "Added" : "Added");
            invited.add(map);
        }
        return invited;
    }

    @Override
    public List<Map<String, String>> addSpecialUserList(String username, Integer pageOffset, Integer pageSize) {
        Query query = new Query();
        if (StringUtils.isNotBlank(username)) {
            query.addCriteria(Criteria.where("name").regex(".*?\\" + username + ".*"));
        }
        mongoUtil.start(pageOffset, pageSize, query);
        List<ConsumerDO> userPage = mongoTemplate.find(query, ConsumerDO.class);
        List<Map<String, String>> invited = new ArrayList<>();
        for (ConsumerDO consumerDO : userPage) {
            Map<String, String> map = new HashMap<>(8);
            String userId = consumerDO.getId();
            map.put("userId", userId);
            map.put("email", consumerDO.getEmailAccounts());
            map.put("userName", consumerDO.getName());
            invited.add(map);
        }
        return invited;
    }

    @Override
    public void addAdministrators(String specialId, String userId) {
        Optional<ConsumerDO> user = userRepository.findById(userId);
        if (user.isPresent() && user.get().getState() == 1) {
            ConsumerDO consumerDO = user.get();
            AuthorizationPerson authorizationPerson = new AuthorizationPerson(userId, consumerDO.getName(), consumerDO.getEmailAccounts());
            Special special = specialRepository.findById(specialId).get();

            Set<AuthorizationPerson> authorizationList = special.getAuthorizationList();
            if(null == authorizationList){
                authorizationList = new HashSet();
            }
            if (authorizationList.add(authorizationPerson)) {
                special.setAuthorizationList(authorizationList);
                specialRepository.save(special);
            } else {
                throw new RuntimeException("The user has been added");
            }
        }
    }

    @Override
    public void deleteAdministrators(String specialId, String userId) {
        ConsumerDO consumerDO = userRepository.findById(userId).get();
        for (Special special : mongoTemplate.find(new Query().addCriteria(Criteria.where("_id").is(specialId)
                .and("authorizationList.userId").is(userId)), Special.class)) {
            Set<AuthorizationPerson> authorizationList = special.getAuthorizationList();
            authorizationList.remove(new AuthorizationPerson(consumerDO.getId(), consumerDO.getName(), consumerDO.getEmailAccounts()));
            special.setAuthorizationList(authorizationList);
            specialRepository.save(special);
        }
    }


    /**
     * Special parameter verification
     *
     * @param specialDTO
     */
    private void verificationParam(Special.SpecialDTO specialDTO) {
        Assert.isTrue(StringUtils.isNotBlank(specialDTO.getSpecialName()), I18nUtil.get("SPECIAL_NAME_NOTNULL"));
        Query query = new Query();
        query.addCriteria(Criteria.where("specialName").is(specialDTO.getSpecialName()));
        Special special = mongoTemplate.findOne(query, Special.class);
        Assert.isTrue(null == special, I18nUtil.get("SPECIAL_NAME_ALREADY_EXISTS"));
    }


}
