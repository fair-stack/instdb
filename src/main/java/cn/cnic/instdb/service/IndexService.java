package cn.cnic.instdb.service;

import cn.cnic.instdb.elasticsearch.EsServiceParams;
import cn.cnic.instdb.model.resources.ResourcesIndexQuery;
import cn.cnic.instdb.model.resources.ResourcesListManage;
import cn.cnic.instdb.model.system.SearchConfig;
import cn.cnic.instdb.result.EsDataPage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;

import java.util.List;
import java.util.Map;

public interface IndexService {


    /**
     * Hot search terms on the homepage
     * @return
     */
    List<String> getIndexHotSearch();


    /**
     * Homepage boutique topic
     * @return
     */
    Map<String, Object> getIndexBoutiqueSpecial();


    /**
     * Data Resource Ranking Data Resource Ranking、Data Resource Ranking、Data Resource Ranking
     * @return
     */
    Map<String, Object> getIndexResourceRank();


    /**
     * Five statistics on the homepage
     * @return
     */
    Map<String,Object> getIndexStatisticsNum();


    /**
     * Obtain all data resource types
     * @return
     */
    Map<String,String> getIndexResourceType();


    /**
     * Search data based on the type of data resource
     * @param resourceType
     * @return
     */
    List<ResourcesListManage> getResourceByType(String resourceType);


    /**
     * Homepage Credit Field Classification
     * @return
     */
    Map<String,Object> getIndexSubjectArea(Integer num);

    /**
     * resource list
     *
     * @param resourcesIndexQuery
     * @return
     */
    PageHelper getIndexAllResource(ResourcesIndexQuery resourcesIndexQuery);

    /**
     * Homepage Latest Resource Fixed
     *
     * @return
     */
    List<ResourcesListManage> getIndexNewResource();


    /**
     * resource listEs
     *
     * @param EsServiceParams
     * @return
     */
    EsDataPage getIndexAllResourceByES(String token,EsServiceParams EsServiceParams);


    /**
     * Retrieve search items on the homepage
     * @return
     */
    List<SearchConfig>  getIndexSearchitems(String type);


    /**
     * Resource Retrieval History
     * @return
     */
    Result getHistorySearch();



}
