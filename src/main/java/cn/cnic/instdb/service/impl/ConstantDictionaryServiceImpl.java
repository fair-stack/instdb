package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.model.system.ConstantDictionary;
import cn.cnic.instdb.model.system.ConstantDictionaryDTO;
import cn.cnic.instdb.service.ConstantDictionaryService;
import cn.cnic.instdb.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
* @Auther  wdd
* @Date  2021/4/22 18:25
* @Desc  Constant Dictionary Table Maintenance
*/
@Service
@Slf4j
public class ConstantDictionaryServiceImpl implements ConstantDictionaryService {

    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public List<ConstantDictionary> getConstantDictionaryByType(String type) {
        Query query = new Query();
        Criteria criteria = Criteria.where("type").is(type);
        query.addCriteria(criteria);
        List<ConstantDictionary> constantDictionary = mongoTemplate.find(query, ConstantDictionary.class);
        return constantDictionary;
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query();
        Criteria criteria = Criteria.where("_id").is(id);
        query.addCriteria(criteria);
        mongoTemplate.remove(query, ConstantDictionary.class);
    }

    @Override
    public void save(ConstantDictionaryDTO dto) {
        ConstantDictionary constantDictionary = new ConstantDictionary();
        BeanUtils.copyProperties(dto, constantDictionary);
        constantDictionary.setCreateTime(LocalDateTime.now());
        mongoTemplate.save(constantDictionary);
    }

}
