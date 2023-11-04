package cn.cnic.instdb.service;

import cn.cnic.instdb.model.system.SearchConfigDTO;
import cn.cnic.instdb.result.Result;

/**
*@Author：wdd
*@describe：Retrieve Configuration Service Interface
*@Date：2023/2/13 17:51
*/
public interface SearchConfigService {
    Result setSearchConfigs(String token,SearchConfigDTO searchConfigDTO);
    Result getSearchConfigs(String token,String type);
    Result deleteSearchConfigs(String token,String id);
    Result resetSearchConfigs(String token);
    Result updateSearchConfigs(String token, String id, String name);
}
