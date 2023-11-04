package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.ResourcesManageUpdate;
import cn.cnic.instdb.model.special.SpecialResourcesDTO;
import cn.cnic.instdb.model.system.Template;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.InstdbApiService;
import cn.cnic.instdb.service.ResourcesService;
import cn.cnic.instdb.service.SpecialResourcesService;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/19:48
 * @Description: Resource Publishing Processing
 */
@RestController
@Slf4j
@RequestMapping(value = "/resources")
@Api(value = "Resource Publishing", tags = "Resource Publishing")
public class ResourcesController extends ResultUtils {

    @Resource
    private ResourcesService resourcesService;

    @Resource
    private InstdbApiService instdbApiService;

    @Resource
    private SpecialResourcesService specialResourcesService;

    @RequestMapping(value = "/structuredDownloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "Structured files-Structured files", notes = "Structured files", response = Result.class)
    public Result structuredDownloadFile(String token, String resourcesId, String tableName, HttpServletRequest request, HttpServletResponse response) {
        resourcesService.structuredDownloadFile(token,resourcesId,tableName,request,response);
        return success();
    }


    @RequestMapping(value = "/resourcesDownloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "Resource Data File-Resource Data File", notes = "Resource Data File", response = Result.class)
    public Result resourcesDownloadFile(String token,String resourcesId,String fileId,HttpServletRequest request, HttpServletResponse response) {
        resourcesService.resourcesDownloadFile(token,resourcesId,fileId,request,response);
        return success();
    }

    @RequestMapping(value = "/resourcesDownloadFileAll", method = RequestMethod.GET)
    @ApiOperation(value = "Resource Data File-Resource Data File", notes = "Resource Data File", response = Result.class)
    public Result resourcesDownloadFileAll(String token,String id,HttpServletRequest request, HttpServletResponse response) {
        resourcesService.resourcesDownloadFileAll(token,id,request,response);
        return success();
    }

    @RequestMapping(value = "/resourcesFtpDownloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "Resource Data File-ftpResource Data File", notes = "Resource Data File", response = Result.class)
    public Result resourcesFtpDownloadFile(@RequestHeader("Authorization") String token,String id,HttpServletRequest request) {
        return resourcesService.resourcesFtpDownloadFile(token,id,request);
    }

    @RequestMapping(value = "/resourcesDownloadJsonLd", method = RequestMethod.GET)
    @ApiOperation(value = "Resource datajson-ldResource data", notes = "Resource datajson-ldResource data", response = Result.class)
    public Result resourcesDownloadJsonLd(String id, HttpServletResponse response) {
        resourcesService.resourcesDownloadJsonLd(id,response);
        return success();
    }


    @RequestMapping(value = "/getResourcesDetails", method = RequestMethod.GET)
    @ApiOperation(value = "Resource Details Page", notes = "Resource Details Page", response = Result.class)
    public Result getResourcesDetails(@RequestHeader("Authorization") String token, String id, Integer examine,HttpServletRequest request) {
        return resourcesService.getResourcesDetails(token, id, examine,request);
    }


    @ApiOperation("Resource Collection Topic")
    @RequestMapping(value = "/resourcesAddSpecial", method = RequestMethod.POST)
    public Result resourcesAddSpecial(@RequestHeader("Authorization") String token,@RequestBody SpecialResourcesDTO specialResourcesDTO) {
        specialResourcesService.resourcesAddSpecial(token,specialResourcesDTO);
        return success("RESOURCE_SPECIAL_ADD");
    }

    @ApiOperation("Special Topic on Obtaining Resources")
    @RequestMapping(value = "/getSpecialByResourcesId", method = RequestMethod.GET)
    public Result getSpecialByResourcesId(String resourcesId) {
        return success(Constant.StatusMsg.SUCCESS, specialResourcesService.getSpecialByResourcesId(resourcesId));
    }


    @ApiOperation("Obtain topics not added under resources")
    @RequestMapping(value = "/getSpecialByNoResources", method = RequestMethod.GET)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "specialName", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result getSpecialByNoResources(String resourcesId,String specialName, Integer pageOffset, Integer pageSize) {
        PageHelper special = specialResourcesService.getSpecialByNoResources(resourcesId, specialName,pageOffset, pageSize);
        return success(special);
    }


    @RequestMapping(value = "/emailShare", method = RequestMethod.GET)
    @ApiOperation(value = "Resource email sharing", notes = "Resource email sharing", response = Result.class)
    public Result emailShare(@RequestHeader("Authorization") String token,String resourcesId, String email) {
        return resourcesService.emailShare(token,resourcesId, email);
    }


    @RequestMapping(value = "/setFollow", method = RequestMethod.POST)
    @ApiOperation(value = "Resource attention", notes = "Resource attention", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String",required = false,dataTypeClass = String.class),
            @ApiImplicitParam(name = "value", dataType = "String",value = "Follow Biographyyes  Follow Biographyno",dataTypeClass = String.class)})
    public Result setFollow(@RequestHeader("Authorization") String token, String resourcesId, String value) {
        return resourcesService.setFollow(token, resourcesId, value);
    }


    @RequestMapping(value = "/getMyFollow", method = RequestMethod.GET)
    @ApiOperation(value = "My attention", notes = "My attention", response = Result.class)
    public Result getMyFollow(@RequestHeader("Authorization") String token,String resourcesType, Integer pageOffset, Integer pageSize,String sort) {
        PageHelper myFollow = resourcesService.getMyFollow(token, resourcesType, pageOffset, pageSize,sort);
        return success(myFollow);
    }

    @RequestMapping(value = "/getMyRelease", method = RequestMethod.GET)
    @ApiOperation(value = "My Publishing", notes = "My Publishing", response = Result.class)
    public Result getMyRelease(@RequestHeader("Authorization") String token,String status,String resourcesType, Integer pageOffset, Integer pageSize,String sort) {
        PageHelper myRelease = resourcesService.getMyRelease(token, status,resourcesType, pageOffset, pageSize,sort);
        return success(myRelease);
    }



    @RequestMapping(value = "/resourceUpdateByAdmin", method = RequestMethod.POST)
    @ApiOperation(value = "Approval and modification by data resource administrator", notes = "Approval and modification by data resource administrator,Approval and modification by data resource administrator", response = Result.class)
    public Result resourceUpdateByAdmin(@RequestHeader("Authorization") String token,@RequestBody ResourcesManageUpdate resourcesManageUpdate) {
        resourcesService.resourceUpdateByAdmin(token,resourcesManageUpdate);
        return success("UPDATE_SUCCESS");
    }



    @RequestMapping(value = "/getTemplateByResourcesId", method = RequestMethod.GET)
    @ApiOperation(value = "Based on data resourcesidBased on data resources", notes = "Based on data resourcesidBased on data resources", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String", required = true,dataTypeClass = String.class) })
    public Result getTemplateByResourcesId(@RequestHeader("Authorization") String token,String resourcesId) {
        List<Template.Group> templateInfo = resourcesService.getTemplateByResourcesId(token, resourcesId);
        return success(templateInfo);
    }


    @RequestMapping(value = "/getResourceFileTree", method = RequestMethod.GET)
    @ApiOperation(value = "Query File List", notes = "Query File ListidQuery File List", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String", required = true,dataTypeClass = String.class) })
    public Result getResourceFileTree(@RequestHeader("Authorization") String token,String resourcesId,int pid,String fileName,Integer pageOffset, Integer pageSize,String sort, HttpServletRequest request) {
        return success(resourcesService.getResourceFileTree(token,resourcesId,pid, fileName,pageOffset,pageSize,sort,request));
    }

    @RequestMapping(value = "/getResourceFiles", method = RequestMethod.GET)
    @ApiOperation(value = "Apply for access to directly query the dataset file list", notes = "Apply for access to directly query the dataset file list", response = Result.class)
    public Result getResourceFiles(String resourcesId, int pid, String fileName, Integer pageOffset, Integer pageSize) {
        return success(resourcesService.getResourceFiles(resourcesId, pid, fileName, pageOffset, pageSize));
    }

    @RequestMapping(value = "/getApproveLog", method = RequestMethod.GET)
    @ApiOperation(value = "Dataset Approval Record", notes = "Dataset Approval Record", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String", required = true,dataTypeClass = String.class) })
    public Result getApproveLog(String resourcesId) {
        return success(resourcesService.getApproveLog(resourcesId));
    }


    @RequestMapping(value = "/getResourceRecommend", method = RequestMethod.GET)
    @ApiOperation(value = "Dataset recommendation", notes = "Dataset recommendationidDataset recommendation", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "String", required = true, dataTypeClass = String.class)})
    public Result getResourceRecommend(String id) {
        return success(resourcesService.getResourceRecommend(id));
    }

    @RequestMapping(value = "/resourcesListManage", method = RequestMethod.GET)
    @ApiOperation(value = "Data resource management", notes = "Data resource management", response = Result.class)
    public Result resourcesListManage(String displayStatus,String name, String resourceType, String privacyPolicy, Integer pageOffset, Integer pageSize, String startDate,String endDate,String publishName,String version,String identifier,String sort,String templateName) {
        return success(resourcesService.resourcesListManage(displayStatus,name,resourceType, privacyPolicy,pageOffset,pageSize,startDate,endDate,publishName,version,identifier,sort,templateName));
    }

    @RequestMapping(value = "/upAndDown", method = RequestMethod.GET)
    @ApiOperation(value = "Data resource up and down shelves", notes = "Data resource up and down shelves", response = Result.class)
    public Result upAndDown(@RequestHeader("Authorization") String token, String id, String type) {
        return resourcesService.upAndDown(token, id, type);
    }

    @RequestMapping(value = "/upAndDownRun", method = RequestMethod.GET)
    @ApiOperation(value = "Execution on and off shelves", notes = "Execution on and off shelves", response = Result.class)
    public Result upAndDownRun(@RequestHeader("Authorization") String token, String id, String type) {
        return resourcesService.upAndDownBefore(token, id, type);
    }

    @RequestMapping(value = "/getStructured", method = RequestMethod.GET)
    @ApiOperation(value = "Based on data resourcesidBased on data resources", notes = "Based on data resourcesidBased on data resources", response = Result.class)
    public Result getStructured(String id) {
        return resourcesService.getStructured(id);
    }

    @RequestMapping(value = "/getStructuredData", method = RequestMethod.GET)
    @ApiOperation(value = "Query structured data", notes = "Query structured data", response = Result.class)
    public Result getStructuredData(String id,String name,String content, Integer pageOffset, Integer pageSize) {
        return success(resourcesService.getStructuredData(id,name,content, pageOffset, pageSize));
    }

    @ApiOperation("Dataset Column Chart-Dataset Column Chart")
    @RequestMapping(value = "/getStatisticsResourcesMonth", method = RequestMethod.GET)
    public Result getStatisticsResourcesMonth(String resourcesId) {
        return resourcesService.getStatisticsResourcesMonth(resourcesId);
    }

    @ApiOperation("Dataset Column Chart-Dataset Column Chart")
    @RequestMapping(value = "/getStatisticsResourcesDay", method = RequestMethod.GET)
    public Result getStatisticsResourcesDay(String resourcesId) {
        return resourcesService.getStatisticsResourcesDay(resourcesId);
    }


    @ApiOperation("Dataset Map Display")
    @RequestMapping(value = "/getResourcesMap", method = RequestMethod.GET)
    public Result getResourcesMap(String resourcesId) {
        return resourcesService.getResourcesMap(resourcesId);
    }

    @ApiOperation("Reference citation detailed information query")
    @RequestMapping(value = "/getCitationDetail", method = RequestMethod.GET)
    public Result getCitationDetail(String cstr) {
        return resourcesService.getCitationDetail(cstr);
    }

    @ApiOperation("jsonldStructured embedding")
    @RequestMapping(value = "/getJsonld", method = RequestMethod.GET)
    public Result getJsonld(String id) {
        return resourcesService.getJsonld(id);
    }



    @ApiOperation("Dataset List")
    @RequestMapping(value = "/getResourceList", method = RequestMethod.GET)
    public Result getResourceList(String name) {
        return resourcesService.getResourceList(name);
    }


    @ApiOperation("export")
    @RequestMapping(value = "/exportResourceData", method = RequestMethod.GET)
    public void exportResourceData(HttpServletResponse response,String displayStatus,String name, String resourceType, String privacyPolicy, String startDate,String endDate,String publishName,String version,String token,String identifier,String templateName) {
        resourcesService.exportResourceData(response,displayStatus,name,resourceType, privacyPolicy,startDate,endDate,publishName,version,token, identifier,templateName);
    }

    @ApiOperation("Obtain all types of the dataset")
    @RequestMapping(value = "/getResourceGroupList", method = RequestMethod.GET)
    public Result getResourceGroupList() {
        return resourcesService.getResourceGroupList();
    }


    /**
     * Obtain a list of metadata standards
     *
     * @return
     */
    @ApiOperation("Obtain a list of metadata standards")
    @GetMapping("/getTemplates")
    public Result getTemplates() {
        return success(instdbApiService.getDataTemplate());
    }

}




