package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.system.Subject;
import cn.cnic.instdb.model.system.SubjectArea;
import cn.cnic.instdb.model.system.SubjectAreaDTO;
import cn.cnic.instdb.model.system.SubjectData;
import cn.cnic.instdb.repository.SubjectAreaRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SubjectAreaService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Auther wdd
 * @Date 2021/3/23 20:23
 * @Desc Discipline field configuration
 */
@Service
public class SubjectAreaServiceImpl implements SubjectAreaService {

    public static final String COLLECTION_NAME = "subject";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private MongoUtil mongoUtil;

    @Resource
    private SubjectAreaRepository subjectAreaRepository;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    @Resource
    private InstdbUrl instdbUrl;


    /**
     * Parameter verification
     *
     * @param subjectAreaDTO
     */
    private void verificationParam(SubjectAreaDTO subjectAreaDTO) {
        Assert.isTrue(StringUtils.isNotBlank(subjectAreaDTO.getName()), I18nUtil.get("PARAMETER_ERROR"));

        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(subjectAreaDTO.getName()));
        SubjectArea subjectArea = mongoTemplate.findOne(query, SubjectArea.class);
        Assert.isTrue(null == subjectArea, I18nUtil.get("SPECIAL_NAME_ALREADY_EXISTS"));
        Assert.isTrue(null != subjectAreaDTO.getSubject(), I18nUtil.get("PARAMETER_ERROR"));
        Assert.isTrue(StringUtils.isNotBlank(subjectAreaDTO.getDesc()), I18nUtil.get("PARAMETER_ERROR"));


//        Assert.isTrue(null != subjectAreaDTO.getIconColor(), "Subject colored icon cannot be empty");
//        Assert.isTrue(null != subjectAreaDTO.getIcon(), "Subject icon cannot be empty");

    }


    @Override
    public Result save(SubjectAreaDTO subjectAreaDTO) {

        if (StringUtils.isBlank(subjectAreaDTO.getName())) {
            return ResultUtils.error("NAME_IS_NULL");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(subjectAreaDTO.getName()));
        SubjectArea subjectArea1 = mongoTemplate.findOne(query, SubjectArea.class);
        if (null != subjectArea1) {
            return ResultUtils.error("SPECIAL_NAME_ALREADY_EXISTS");
        }
        if (null == subjectAreaDTO.getSubject() || subjectAreaDTO.getSubject().size() == 0) {
            return ResultUtils.error("SUBJECT_NOTNULL");
        }
        if (StringUtils.isBlank(subjectAreaDTO.getDesc())) {
            return ResultUtils.error("DESC_IS_NULL");
        }
        if (StringUtils.isBlank(subjectAreaDTO.getLogo())) {
            return ResultUtils.error("picture_IS_NULL");
        }
        if (StringUtils.isBlank(subjectAreaDTO.getIcon())) {
            return ResultUtils.error("picture_IS_NULL");
        }
        if (StringUtils.isBlank(subjectAreaDTO.getIconColor())) {
            return ResultUtils.error("picture_IS_NULL");
        }

        SubjectArea subjectArea = new SubjectArea();
        BeanUtils.copyProperties(subjectAreaDTO, subjectArea);

        if (subjectAreaDTO.getSubject().size() > 0) {
            if (!CommonUtils.chineseVerify(subjectAreaDTO.getSubject().get(0))) {
                subjectArea.setSubject(getSubjectByName(subjectAreaDTO.getSubject(), Constant.Language.english));
                subjectArea.setSubjectEn(subjectAreaDTO.getSubject());
            } else {
                subjectArea.setSubjectEn(getSubjectByName(subjectAreaDTO.getSubject(), Constant.Language.chinese));
            }
        }

        //Image processing
        if (StringUtils.isNotBlank(subjectArea.getLogo())) {
            if (subjectArea.getLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), subjectArea.getLogo(), uuid);
                subjectArea.setLogo(uuid + Constant.PNG);
            }
        }

        //Image processing
        if (StringUtils.isNotBlank(subjectArea.getIcon())) {
            if (subjectArea.getIcon().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), subjectArea.getIcon(), uuid + "icon");
                subjectArea.setIcon(uuid + "icon" + Constant.PNG);
            }
        }
        if (StringUtils.isNotBlank(subjectArea.getIconColor())) {
            if (subjectArea.getIconColor().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), subjectArea.getIconColor(), uuid + "iconColor");
                subjectArea.setIconColor(uuid + "iconColor" + Constant.PNG);
            }
        }

        subjectArea.setId(CommonUtils.generateUUID());
        subjectArea.setCreateTime(LocalDateTime.now());
        subjectArea.setUpdateTime(LocalDateTime.now());
        subjectAreaRepository.save(subjectArea);
        return ResultUtils.success("CREATE_SUCCESS");
    }

    @Override
    public Result update(SubjectAreaDTO subjectAreaDTO) {

        if (StringUtils.isBlank(subjectAreaDTO.getName())) {
            return ResultUtils.error("NAME_IS_NULL");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(subjectAreaDTO.getName()));
        SubjectArea subjectArea1 = mongoTemplate.findOne(query, SubjectArea.class);
        if (null != subjectArea1 && !subjectArea1.getId().equals(subjectAreaDTO.getId())) {
            return ResultUtils.error("SPECIAL_NAME_ALREADY_EXISTS");
        }
        if (null == subjectAreaDTO.getSubject() || subjectAreaDTO.getSubject().size() == 0) {
            return ResultUtils.error("SUBJECT_NOTNULL");
        }
        if (StringUtils.isBlank(subjectAreaDTO.getDesc()) && !subjectArea1.getId().equals(subjectAreaDTO.getId())) {
            return ResultUtils.error("DESC_IS_NULL");
        }

        if (StringUtils.isBlank(subjectAreaDTO.getLogo())) {
            return ResultUtils.error("picture_IS_NULL");
        }
        if (StringUtils.isBlank(subjectAreaDTO.getIcon())) {
            return ResultUtils.error("picture_IS_NULL");
        }
        if (StringUtils.isBlank(subjectAreaDTO.getIconColor())) {
            return ResultUtils.error("picture_IS_NULL");
        }

        Optional<SubjectArea> byId = subjectAreaRepository.findById(subjectAreaDTO.getId());
        if (!byId.isPresent()) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

        SubjectArea subjectArea = new SubjectArea();
        BeanUtils.copyProperties(subjectAreaDTO, subjectArea);


        if (subjectAreaDTO.getSubject().size() > 0) {
            if (!CommonUtils.chineseVerify(subjectAreaDTO.getSubject().get(0))) {
                subjectArea.setSubject(getSubjectByName(subjectAreaDTO.getSubject(), Constant.Language.english));
                subjectArea.setSubjectEn(subjectAreaDTO.getSubject());
            } else {
                subjectArea.setSubjectEn(getSubjectByName(subjectAreaDTO.getSubject(), Constant.Language.chinese));
            }
        }
        //Image processing
        if (StringUtils.isNotBlank(subjectArea.getLogo())) {
            if (subjectArea.getLogo().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), subjectArea.getLogo(), uuid);
                subjectArea.setLogo(uuid + Constant.PNG);
                if (StringUtils.isNotBlank(byId.get().getLogo())) {
                    FileUtils.deleteFile(instdbUrl.getSubjectLogo() + byId.get().getLogo());
                }
            }
        }


        //Image processing
        if (StringUtils.isNotBlank(subjectArea.getIcon())) {
            if (subjectArea.getIcon().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), subjectArea.getIcon(), uuid + "icon");
                subjectArea.setIcon(uuid + "icon" + Constant.PNG);
                if (StringUtils.isNotBlank(byId.get().getIcon())) {
                    FileUtils.deleteFile(instdbUrl.getSubjectLogo() + byId.get().getIcon());
                }
            }
        }


        //Image processing
        if (StringUtils.isNotBlank(subjectArea.getIconColor())) {
            if (subjectArea.getIconColor().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), subjectArea.getIconColor(), uuid + "iconColor");
                subjectArea.setIconColor(uuid + "iconColor" + Constant.PNG);
                if (StringUtils.isNotBlank(byId.get().getIconColor())) {
                    FileUtils.deleteFile(instdbUrl.getSubjectLogo() + byId.get().getIconColor());
                }
            }
        }

        subjectArea.setUpdateTime(LocalDateTime.now());
        subjectArea.setCreateTime(byId.get().getCreateTime());
        subjectAreaRepository.save(subjectArea);

        return ResultUtils.success("UPDATE_SUCCESS");
    }

    @Override
    public void deleteById(String id) {
        Assert.isTrue(StringUtils.isNotBlank(id), I18nUtil.get("PARAMETER_ERROR"));
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, SubjectArea.class);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        if (null != ids && ids.size() > 0) {
            for (String id : ids) {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(id));
                mongoTemplate.remove(query, SubjectArea.class);
            }
        }
    }

    @Override
    public SubjectArea getSubjectAreaById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        SubjectArea one = mongoTemplate.findOne(query, SubjectArea.class);
        return one;
    }

    @Override
    public PageHelper getSubjectAreaAll(Map<String, Object> condition) {
        String subject = condition.get("subject").toString();
        String name = condition.get("name").toString();
        Query query = new Query();

        query.with(Sort.by(Sort.Direction.ASC, "sort"));
        if (StringUtils.isNotBlank(name)) {
            query.addCriteria(Criteria.where("name").regex((".*?\\" + name + ".*")));
        }
        if (StringUtils.isNotBlank(subject)) {
            query.addCriteria(Criteria.where("subject").in(subject));
        }

        long count = mongoTemplate.count(query, SubjectArea.class);
        mongoUtil.start(Integer.parseInt(condition.get("pageOffset").toString()), Integer.parseInt(condition.get("pageSize").toString()), query);
        List<SubjectArea> subjectArea = mongoTemplate.find(query, SubjectArea.class);

        if (null != subjectArea && subjectArea.size() > 0) {

            for (SubjectArea sub : subjectArea) {
                Query query1 = new Query();
                query1.addCriteria(Criteria.where("_id").is(sub.getId()));
                if (null == sub.getSubjectEn() || sub.getSubjectEn().size() == 0 && sub.getSubject().size() > 0) {
                    Update update = new Update();
                    update.set("subjectEn", this.getSubjectByName(sub.getSubject(), Constant.Language.chinese));
                    mongoTemplate.updateFirst(query1, update, SubjectArea.class);
                }
            }
        }

        return mongoUtil.pageHelper(count, subjectArea);

    }

    @Override
    public List<String> getSubjectByName(List<String> subject, String lang) {
        if (subject.size() > 0) {
            List<String> list = new ArrayList<>();
            for (String data : subject) {
                Query query1 = new Query();
                query1.addCriteria(Criteria.where(Constant.Language.chinese.equals(lang) ? "one_rank_name" : "one_rank_name_en").is(data));
                SubjectData subjectData = mongoTemplate.findOne(query1, SubjectData.class);
                if (null != subjectData) {
                    list.add(Constant.Language.chinese.equals(lang) ? subjectData.getOne_rank_name_en() : subjectData.getOne_rank_name());
                    continue;
                }
                Query query2 = new Query();
                query2.addCriteria(Criteria.where(Constant.Language.chinese.equals(lang) ? "two_rank_name" : "two_rank_name_en").is(data));
                SubjectData subjectData2 = mongoTemplate.findOne(query2, SubjectData.class);
                if (null != subjectData2) {
                    list.add(Constant.Language.chinese.equals(lang) ? subjectData2.getTwo_rank_name_en() : subjectData2.getTwo_rank_name());
                    continue;
                }
                Query query3 = new Query();
                query3.addCriteria(Criteria.where(Constant.Language.chinese.equals(lang) ? "three_rank_name" : "three_rank_name_en").is(data));
                SubjectData subjectData3 = mongoTemplate.findOne(query3, SubjectData.class);
                if (null != subjectData3) {
                    list.add(Constant.Language.chinese.equals(lang) ? subjectData3.getThree_rank_name_en() : subjectData3.getThree_rank_name());
                    continue;
                }
            }
            return list;

        } else {
            return subject;
        }
    }

    @Override
    public List<Subject> getSubjectAreaInfo() {
        List<Subject> subjects = new ArrayList<>();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "one_rank_name")),//sort
                Aggregation.group("one_rank_id", "one_rank_no", "one_rank_name", "one_rank_name_en").count().as("f")//Group Fields
        );
        return getSubject(agg, subjects, "1");
    }

    @Override
    public List<Subject> getSubjectAreaInfoTwo(String id, String num) {
        List<Subject> subjects = new ArrayList<>();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("one_rank_id").is(id)),//condition
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "two_rank_name")),//sort
                Aggregation.group("two_rank_id", "two_rank_no", "two_rank_name", "two_rank_name_en").count().as("f")//Group Fields
        );
        return getSubject(agg, subjects, num);
    }

    private List<Subject> getSubject(Aggregation agg, List<Subject> subjects, String num) {
        AggregationResults<SubjectData> outputType = mongoTemplate.aggregate(agg, COLLECTION_NAME, SubjectData.class);
        List<SubjectData> list = outputType.getMappedResults();
        if (null != list && list.size() > 0) {
            for (SubjectData data : list) {
                Subject subject = new Subject();
                if ("1".equals(num)) {
                    subject.setId(data.getOne_rank_id());
                    subject.setNo(data.getOne_rank_no());
                    subject.setName(data.getOne_rank_name());
                    subject.setNameEn(data.getOne_rank_name_en());
                } else if ("2".equals(num)) {
                    subject.setId(data.getTwo_rank_id());
                    subject.setNo(data.getTwo_rank_no());
                    subject.setName(data.getTwo_rank_name());
                    subject.setNameEn(data.getTwo_rank_name_en());
                } else if ("3".equals(num)) {
                    subject.setId(data.getThree_rank_id());
                    subject.setNo(data.getThree_rank_no());
                    subject.setName(data.getThree_rank_name());
                    subject.setNameEn(data.getThree_rank_name_en());
                }
                subjects.add(subject);
            }
            return subjects;
        }
        return subjects;


    }

    @Override
    public List<Subject> getSubjectAreaInfoThree(String id, String num) {
        List<Subject> subjects = new ArrayList<>();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("two_rank_id").is(id)),//condition
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "three_rank_name")),//sort
                Aggregation.group("three_rank_id", "three_rank_no", "three_rank_name", "three_rank_name_en").count().as("f")//Group Fields
        );
        return getSubject(agg, subjects, num);
    }

    @Override
    public Result getSubjectList() {
        String subjectList = tokenCache.getIfPresent("subject");
        if (StringUtils.isEmpty(subjectList)) {

            Map<String, Object> map = new HashMap<>();
            List<SubjectData> all = mongoTemplate.findAll(SubjectData.class);
            all.stream().forEachOrdered(subject -> {
                String oneName = subject.getOne_rank_name();
                if (map.containsKey(oneName)) {
                    Map twoMap = (Map<String, Object>) map.get(oneName);
                    String two_rank_name = subject.getTwo_rank_name();
                    if (twoMap.containsKey(two_rank_name)) {
                        ((List) twoMap.get(two_rank_name)).add(subject.getThree_rank_name());
                    } else {
                        twoMap.put(two_rank_name, new ArrayList<String>() {{
                            add(subject.getThree_rank_name());
                        }});
                    }
                } else {
                    Map<String, List<String>> twoMap = new HashMap<>();
                    twoMap.put(subject.getTwo_rank_name(), new ArrayList<String>() {{
                        add(subject.getThree_rank_name());
                    }});
                    map.put(oneName, twoMap);
                }
            });
            List<Map<String, Object>> resultList = new LinkedList<>();
            map.entrySet().stream().forEachOrdered(sub -> {
                String key = sub.getKey();
                List<Map<String, Object>> twoList = new LinkedList<>();
                Map<String, Object> twoMap = (Map<String, Object>) sub.getValue();
                twoMap.entrySet().stream().forEachOrdered(subTwo -> {
                    String key1 = subTwo.getKey();
                    Map<String, Object> threeMap = new HashMap<>();
                    threeMap.put("value", key1);
                    threeMap.put("label", key1);
                    List<String> list = (List<String>) subTwo.getValue();
                    List<Map<String, Object>> threeList = new LinkedList<>();
                    Iterator<String> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        String next = iterator.next();
                        Map<String, Object> mm = new HashMap<>();
                        mm.put("value", next);
                        mm.put("label", next);
                        threeList.add(mm);
                    }
                    threeMap.put("children", threeList);
                    twoList.add(threeMap);
                });
                Map<String, Object> oneMap = new HashMap<>();
                oneMap.put("label", key);
                oneMap.put("value", key);
                oneMap.put("children", twoList);
                resultList.add(oneMap);
            });

            String s = JSON.toJSONString(resultList);
            tokenCache.put("subject", s);
            return ResultUtils.success(resultList);
        }
        List list = JSONObject.parseObject(subjectList, List.class);
        return ResultUtils.success(list);
    }

    @Override
    public Result getSubjectArea() {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.ASC, "sort"));
        query.addCriteria(Criteria.where("icon").is(null));
        query.addCriteria(Criteria.where("iconColor").is(null));
        List<SubjectArea> subjectArea = mongoTemplate.find(query, SubjectArea.class);
        return ResultUtils.success(subjectArea);
    }

    @Override
    public Result uploadIco(String id, String icon, String iconColor) {
        if (StringUtils.isBlank(icon) || StringUtils.isBlank(iconColor)) {
            return ResultUtils.error("picture_IS_NULL");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        SubjectArea subjectArea = mongoTemplate.findOne(query, SubjectArea.class);
        if (null == subjectArea) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        Update update = new Update();
        //Image processing
        if (StringUtils.isNotBlank(icon)) {
            if (icon.contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), icon, uuid + "icon");
                update.set("icon", uuid + "icon" + Constant.PNG);
                if (StringUtils.isNotBlank(subjectArea.getIcon())) {
                    FileUtils.deleteFile(instdbUrl.getSubjectLogo() + subjectArea.getIcon());
                }
            }
        }
        if (StringUtils.isNotBlank(iconColor)) {
            if (iconColor.contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getSubjectLogo(), iconColor, uuid + "iconColor");
                update.set("iconColor", uuid + "iconColor" + Constant.PNG);
                if (StringUtils.isNotBlank(subjectArea.getIconColor())) {
                    FileUtils.deleteFile(instdbUrl.getSubjectLogo() + subjectArea.getIconColor());
                }
            }
        }
        mongoTemplate.updateFirst(query, update, SubjectArea.class);
        return ResultUtils.success();
    }

}
