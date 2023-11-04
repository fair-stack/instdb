package cn.cnic.instdb.service;

import cn.cnic.instdb.model.openApi.Apis;
import cn.cnic.instdb.model.openApi.SecretKeyDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface OpenApiService {

    /**
     * Authorization code generation
     *
     * @param token
     * @param secretKey
     */
    Result setSecretKey(String token, SecretKeyDTO secretKey);

    /**
     * Delete authorization code
     * @param token
     * @param id
     * @return
     */
    Result deletSecretKey(String token, String id);

    /**
     * Modify authorization code
     * @param token
     * @param id
     * @param applicationName
     * @param organName
     * @return
     */
    Result updateSecretKey(String token, String id, String organId, String applicationName, String organName);

    /**
     * Resetapi
     * @return
     */
    Result resetapiList(String token);

    /**
     * Authorization Code List
     *
     * @param condition
     * @return
     */
    PageHelper getALLSecretKey(Map<String, Object> condition);

    /**
     * Disabled/Disabled
     * @param token
     * @param id
     * @param status
     */
    void disable(String token,String id, String status);

    /**
     * Disabled/Disabledapi
     * @param token
     * @param id
     * @param status
     */
    Result disableApi(String token,String id, String status);

    /**
     * apilist
     * @return
     */
    PageHelper getApiList(String name, Integer pageOffset, Integer pageSize,String status);


    /**
     * api Authorization List
     * @param apiId
     * @return
     */
    Result getApiAuth(String apiId);

    /**
     * api authorization authorization/authorization
     * @param token
     * @param apiId
     * @param authorization
     * @return
     */
    Result updateApiAuth(String token, String apiId, Apis.Authorization authorization);


    /**
     * Delete Authorization
     * @param token
     * @param apiId
     * @param orgId
     * @return
     */
    Result deleteApiAuth(String token,String apiId, String orgId);

    /**
     * Search for dataset details based on dataset number
     * @param relatedDataset
     * @return
     */
    Result getDetailsByRelatedDataset(String relatedDataset);

    /**
     * Set file size and redirect download address  Set file size and redirect download address  Set file size and redirect download addresssdkSet file size and redirect download address
     *
     * @param storageNum
     * @param datasetRemoteUrl
     * @return
     */
    Result setFilestorageInfo(String datasetId, long storageNum, String datasetRemoteUrl,int visits);

    /**
     * Details display page
     *
     * @param id
     */
    Result getResourcesDetails(String id);

    /**
     * ftp information acquisition 
     * @param id
     * @return
     */
    Result resourcesFtpDownloadFile(String id);

    /**
     * Dataset File Information
     * @param resourcesId
     * @param pid
     * @param fileName
     * @param pageOffset
     * @param pageSize
     * @param sort
     * @return
     */
    PageHelper getResourceFileTree(String resourcesId,int pid,String fileName,Integer pageOffset, Integer pageSize,String sort);

    /**
     * File Download
     * @param resourcesId
     * @param fileId
     * @param response
     */
     void resourcesDownloadFile(String resourcesId, String fileId, HttpServletResponse response);


    /**
     * interfaceapiinterface-interface
     * @param token
     * @param response
     * @return
     */
    void downloadApiFile(String token, HttpServletResponse response);


}
