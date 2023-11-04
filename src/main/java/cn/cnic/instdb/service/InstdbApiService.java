package cn.cnic.instdb.service;

import cn.cnic.instdb.result.EsDataPage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.elasticsearch.client.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/05/28/15:32
 * @Description:
 */
public interface InstdbApiService {

    /**
     * Authentication interface
     * @param secretKey
     * @return
     */
    Map entry(String secretKey);

    /**
     * Get a list of all datasets
     * @return
     */
    List<Map<String, Object>> datasetList(String publishDate);

    List<Map<String, String>> getServiceList();

    /**
     * Obtain a list of metadata standards
     * @return
     */
    List<Map<String, String>> getDataTemplate();


    /**
     * fairdMetadata acquisition interface
     * @param identifier
     * @return
     */
    Map getMetaData(String identifier);



    /**
     * Data resource query service
     *
     * @param param
     * @param filters
     * @param startDate
     * @param endDate
     * @param page
     * @param pageSize
     * @return
     */
    EsDataPage datasetSearch(String param,
                             String filters,
                             String startDate,
                             String endDate,
                             Integer page,
                             Integer pageSize);

    /**
     * according to id according to
     * @param id
     * @return
     */
    Map getDetails(String id,String version);

    Map getDetailsOld(String id);


    /**
     * Obtain dataset update time
     * @param page
     * @param pageSize
     */
    PageHelper getDatasetStatus(Integer page, Integer pageSize);


    /**
     * Obtain Dataset Service Information
     * @return
     */
    Map<String,Object> getDatasetInfo(String id);

    /**
     * checktoken
     * @param request
     * @return
     */
    int checkToken(HttpServletRequest request);


    /**
     * Data push completion notification interface
     * @param resourceId
     */
    Map<String ,Object> uploadCompleted(String resourceId);


}
