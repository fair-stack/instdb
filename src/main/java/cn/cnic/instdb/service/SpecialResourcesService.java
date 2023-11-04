package cn.cnic.instdb.service;

import cn.cnic.instdb.model.special.SpecialResourcesDTO;
import cn.cnic.instdb.utils.PageHelper;

import java.util.List;
import java.util.Map;

public interface SpecialResourcesService {

    /**
     * Resource addition topic
     */
    void resourcesAddSpecial(String token,SpecialResourcesDTO specialResourcesDTO);


    /**
     * Adding Resources to a Topic
     */
    void specialAddResources(String token,SpecialResourcesDTO specialResourcesDTO);


    /**
     * Obtain topics related to resources
     *
     * @param resourcesId
     */
    List<Map<String, Object>> getSpecialByResourcesId(String resourcesId);


    /**
     * Obtain resource information related to the topic
     *
     * @param specialId
     */
    PageHelper getResourcesBySpecialId(String token,String specialId,String resourcesName,String resourceType, Integer pageOffset, Integer pageSize,String sort);

    Map<String, Object> getResourcesListBySpecialId(String specialId);

    /**
     * Obtaining resources not added under the theme
     */
    PageHelper getResourcesByNoSpecial(String specialId, String resourcesName,Integer pageOffset, Integer pageSize);

    /**
     * Obtain topics not added under resources
     */
    PageHelper getSpecialByNoResources(String resourcesId,String specialName, Integer pageOffset, Integer pageSize);


    void deleteResourcesInSpecial(SpecialResourcesDTO specialResourcesDTO);


}
