package cn.cnic.instdb.service;

import cn.cnic.instdb.model.system.ConstantDictionary;
import cn.cnic.instdb.model.system.ConstantDictionaryDTO;

import java.util.List;

public interface ConstantDictionaryService {

   List<ConstantDictionary> getConstantDictionaryByType(String type);

   void deleteById(String id);

   void save(ConstantDictionaryDTO constantDictionary);
}
