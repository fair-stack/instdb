package cn.cnic.instdb.service;

import cn.cnic.instdb.model.resources.Approve;
import cn.cnic.instdb.model.resources.Resources;
import cn.cnic.instdb.model.resources.ResourcesManageUpdate;
import cn.cnic.instdb.model.system.Template;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ResourcesService {

    /**
     * Data publishing
     *
     * @param resources
     */
    Map<String, String> dataRelease(Resources resources);


    /**
     * Resource datajson-ldResource data
     *
     * @param id
     * @param response
     */
    void resourcesDownloadJsonLd(String id, HttpServletResponse response);


    /**
     * Download resource data files
     *
     * @param resourcesId data setid
     * @param fileId Documentid
     */
    void resourcesDownloadFile(String token,String resourcesId, String fileId,HttpServletRequest request, HttpServletResponse response);

    /**
     * Download all resource data files
     *
     * @param id
     */
    void resourcesDownloadFileAll(String token,String id,HttpServletRequest request, HttpServletResponse response);


    /**
     * ftpFile Download
     * @param id
     * @return
     */
    Result resourcesFtpDownloadFile(String token,String id,HttpServletRequest request);

    /**
     * Details display page
     *
     * @param resourcesOnlyId
     * examine Normal access 0  Normal access 1  Normal access 2  3Normal access
     */
    Result getResourcesDetails(String token, String resourcesOnlyId,Integer examine,HttpServletRequest request);


    /**
     * Resource email sharing
     *
     * @param resourcesId
     */
    Result emailShare(String token, String resourcesId, String email);


    /**
     * Set resource attention
     *
     * @param resourcesId
     * @param value
     */
    Result setFollow(String token, String resourcesId, String value);


    /**
     * My attention
     *
     * @return
     */
    PageHelper getMyFollow(String token, String resourcesType, Integer pageOffset, Integer pageSize,String sort);


    /**
     * My Publishing
     * @param token
     * @param status
     * @param pageOffset
     * @param pageSize
     * @param sort
     * @return
     */
    PageHelper getMyRelease(String token,String status,String resourcesType, Integer pageOffset, Integer pageSize,String sort);


    /**
     * Approval and modification by data resource administrator
     * @param token
     * @param resourcesManageUpdate
     */
    void resourceUpdateByAdmin(@RequestHeader("Authorization") String token, ResourcesManageUpdate resourcesManageUpdate);



    /**
     * Return formatted template data based on data resources
     * @param resourcesId
     * @return
     */
    List<Template.Group> getTemplateByResourcesId(@RequestHeader("Authorization") String token, String resourcesId);


    /**
     * Generate DatasetftpGenerate Dataset
     * @param resourcesId
     * @param auth
     * @param map
     */
    void createFtpUser(String ip,String resourcesId, String auth, Map<String,String> map);



    PageHelper getResourceFileTree(String token,String resourcesId,int pid,String fileName,Integer pageOffset, Integer pageSize,String sort, HttpServletRequest request);


    PageHelper getResourceFiles(String resourcesId,int pid,String fileName,Integer pageOffset, Integer pageSize);

    List<Approve> getApproveLog(String resourcesId);

    /**
     * Dataset recommendation
     * @param resourcesId
     * @return
     */
    Result getResourceRecommend(String resourcesId);


    /**
     * resource management
     * @param name
     * @param resourceType
     * @param privacyPolicy
     * @param pageOffset
     * @param pageSize
     * @return
     */
    PageHelper resourcesListManage(String displayStatus,String name,String resourceType,String privacyPolicy,Integer pageOffset, Integer pageSize,String startDate,String endDate,String publishName,String version,String identifier,String sort,String templateName);

    /**
     * Dataset online and offline
     * @param id
     * @param type
     * @return
     */
    Result upAndDown(String token,String id, String type);

    PageHelper getResourcesHistory(String token,Integer pageOffset, Integer pageSize);


    /**
     * Obtaining structured information
     *
     * @param id
     * @return
     */
    Result getStructured(String id);

    /**
     * Structured Content Query Structured Content Query
     *
     * @param content
     * @param pageOffset
     * @param pageSize
     * @return
     */
    PageHelper getStructuredData(String id,String name,String content, Integer pageOffset, Integer pageSize);


    /**
     * Structured File Download
     *
     * @param resourcesId data setid
     * @param fileId Structured Recordedid
     */
    void structuredDownloadFile(String token,String resourcesId, String fileId,HttpServletRequest request, HttpServletResponse response);



    /**
     * Dataset Column Chart-Dataset Column Chart
     * @param resourcesId
     * @return
     */
    Result getStatisticsResourcesMonth(String resourcesId);

    /**
     * Dataset Column Chart-Dataset Column Chart
     * @param resourcesId
     * @return
     */
    Result getStatisticsResourcesDay(String resourcesId);

    /**
     * Dataset Map Display
     * @param resourcesId
     * @return
     */
    Result getResourcesMap(String resourcesId);

    /**
     * Reference citation detailed information query
     * @param cstr
     * @return
     */
    Result getCitationDetail(String cstr);


    /**
     * Up and down shelf operation
     * @param token
     * @param id
     * @return
     */
    Result upAndDownBefore(String token,String id,String type);

    /**
     * Dataset List
     * @param name
     * @return
     */
    Result getResourceList(String name);

    /**
     * resource management
     * @param name
     * @param resourceType
     * @param privacyPolicy
     * @return
     */
    void exportResourceData(HttpServletResponse response,String displayStatus,String name,String resourceType,String privacyPolicy,String startDate,String endDate,String publishName,String version,String token,String identifier,String templateName);


    /**
     * Data resource classification
     * @return
     */
    Result getResourceGroupList();

    Map<String, String> getTemplateInfo();

    /**
     * jsonldStructured embedding
     * @param id
     * @return
     */
    Result getJsonld(String id);

}
