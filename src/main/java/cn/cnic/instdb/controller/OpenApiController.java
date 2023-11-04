package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.openApi.Apis;
import cn.cnic.instdb.model.openApi.SecretKeyDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ApproveService;
import cn.cnic.instdb.service.AuthService;
import cn.cnic.instdb.service.OpenApiService;
import cn.cnic.instdb.service.ResourcesService;
import cn.cnic.instdb.utils.JwtTokenUtils;
import cn.cnic.instdb.utils.PageHelper;
import cn.cnic.instdb.utils.RSAEncrypt;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @Auther wdd
 * @Date 2021/5/26 10:58
 * @Desc External services provided
 */
@RestController
@Api(tags = "OpenApi")
@Slf4j
@RequestMapping(value = "/open")
public class OpenApiController extends ResultUtils {

    @Resource
    private OpenApiService openApiService;

    @Resource
    private ResourcesService resourcesService;


    @Resource
    private ApproveService approveService;


    @Resource
    private AuthService authService;

    @Resource
    private JwtTokenUtils jwtTokenUtils;


    @PostMapping("/set/secretKey")
    @ApiOperation("Authorization code-Authorization code")
    public Result setSecretKey(@RequestHeader("Authorization") String token, @RequestBody SecretKeyDTO SecretKey) {
        return openApiService.setSecretKey(token, SecretKey);
    }

    @PostMapping("/delete/secretKey")
    @ApiOperation("Authorization code-Authorization code")
    public Result deletSecretKey(@RequestHeader("Authorization") String token, String id) {
        return openApiService.deletSecretKey(token, id);
    }

    @ApiOperation(value = "Disabled/Disabled")
    @PostMapping("/disable")
    public Result disable(@RequestHeader("Authorization") String token,
                          @RequestParam(value = "secretKeyId") String secretKeyId,
                          @RequestParam(value = "status") String status) {
        openApiService.disable(token, secretKeyId, status);
        return success();
    }

    @ApiOperation(value = "Modify authorization code")
    @PostMapping("/updateSecretKey")
    public Result updateSecretKey(@RequestHeader("Authorization") String token,
                                  @RequestParam(value = "id") String id,
                                  @RequestParam(value = "applicationName") String applicationName,
                                  @RequestParam(value = "organId") String organId,
                                  @RequestParam(value = "organName") String organName) {
        return openApiService.updateSecretKey(token, id, applicationName, organId, organName);
    }


    @RequestMapping(value = "/getALLSecretKey", method = RequestMethod.GET)
    @ApiOperation(value = "Authorization code-Authorization code", notes = "Authorization code", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "organName", dataType = "String", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true, dataTypeClass = Integer.class)})
    public Result getALLSecretKey(@RequestHeader("Authorization") String token, String organName, Integer pageOffset, Integer pageSize) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("token", token);
        condition.put("organName", organName);
        condition.put("pageOffset", pageOffset);
        condition.put("pageSize", pageSize);
        PageHelper allSecretKey = openApiService.getALLSecretKey(condition);
        return success(allSecretKey);
    }


    @ApiOperation(value = "apilist")
    @GetMapping("/getApiList")
    public Result getApiList(String name, Integer pageOffset, Integer pageSize, String status) {
        return success(openApiService.getApiList(name, pageOffset, pageSize, status));
    }

    @ApiOperation(value = "ResetapiReset")
    @GetMapping("/resetapiList")
    public Result resetapiList(@RequestHeader("Authorization") String token) {
        return openApiService.resetapiList(token);
    }

    @ApiOperation(value = "apilist-list")
    @GetMapping("/getApiAuth")
    public Result getApiAuth(String apiId) {
        return success(openApiService.getApiAuth(apiId));
    }

    @ApiOperation(value = "apilist-list/listapi")
    @PostMapping("/disableApi")
    public Result disableApi(@RequestHeader("Authorization") String token,
                             @RequestParam(value = "apiId") String apiId,
                             @RequestParam(value = "status") String status) {
        return openApiService.disableApi(token, apiId, status);
    }

    @ApiOperation(value = "authorization-authorization/authorizationapi")
    @PostMapping("/updateApiAuth")
    public Result updateApi(@RequestHeader("Authorization") String token,
                            @RequestParam(value = "apiId") String apiId,
                            @RequestBody Apis.Authorization authorization) {
        openApiService.updateApiAuth(token, apiId, authorization);
        return success();
    }


    @ApiOperation(value = "apilist-list")
    @PostMapping("/deleteApiAuth")
    public Result deleteApiAuth(@RequestHeader("Authorization") String token,
                                @RequestParam(value = "apiId") String apiId,
                                @RequestParam(value = "orgId") String orgId) {
        return openApiService.deleteApiAuth(token, apiId, orgId);
    }


    @RequestMapping(value = "/approveSubmit", method = RequestMethod.POST)
    @ApiOperation(value = "Approval", notes = "Approval,Approvaldataspace", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String", value = "resourcesId Unique identification of resources", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "approvalStatus", dataType = "String", value = "Approval status", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "reason", dataType = "String", value = "Approval opinions", required = false, dataTypeClass = String.class)})
    public Result approveSubmit(@RequestHeader("Authorization") String token,
                                String resourcesId,
                                String approvalStatus, @RequestParam(value = "reason", required = false) String reason, @RequestParam(value = "rejectApproval", required = false) String rejectApproval,
                                @RequestParam(value = "file", required = false) MultipartFile file) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null == roles) {
            return ResultUtils.error("LOGIN_EXCEPTION");
        }
        if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
            return ResultUtils.error("PERMISSION_DENIED");
        }

        return approveService.approveSubmit(token, resourcesId, approvalStatus, reason, rejectApproval, file);
    }


//    /**
//     * Single data resource query service
//     * Provide access to single data resources shared by the central service platform at the station level for the central center and other platform systems。
//     *
//     * @return
//     */
//    @ApiOperation("according to id according to")
//    @GetMapping("/dataset/details")
//    public Result getInfo(@RequestHeader("Authorization") String token, String resourcesId, Integer examine, HttpServletRequest request) {
//        return resourcesService.getResourcesDetails(token, resourcesId, examine, request);
//    }

    /**
     * Single data resource query service
     * Provide access to single data resources shared by the central service platform at the station level for the central center and other platform systems。
     *
     * @return
     */
    @ApiOperation("according to id according to")
    @GetMapping("/dataset/details")
    public Result getResourcesDetails(String resourcesId) {
        return openApiService.getResourcesDetails(resourcesId);
    }

    @ApiOperation("Obtain dataset details based on dataset number")
    @GetMapping("/dataset/details/relatedDataset")
    public Result getDetailsByRelatedDataset(String relatedDataset) {
        return openApiService.getResourcesDetails(relatedDataset);
    }


    @RequestMapping(value = "/getResourceFileTree", method = RequestMethod.GET)
    @ApiOperation(value = "Query File List", notes = "Query File ListidQuery File List", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String", required = true, dataTypeClass = String.class)})
    public Result getResourceFileTree(String resourcesId, int pid, String fileName, Integer pageOffset, Integer pageSize, String sort) {
        return success(openApiService.getResourceFileTree(resourcesId, pid, fileName, pageOffset, pageSize, sort));
    }

    @RequestMapping(value = "/resourcesDownloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "Resource Data File-Resource Data File", notes = "Resource Data File", response = Result.class)
    public Result resourcesDownloadFile(String resourcesId, String fileId, HttpServletResponse response) {
        openApiService.resourcesDownloadFile(resourcesId, fileId, response);
        return success();
    }

    @RequestMapping(value = "/resourcesFtpDownloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "Resource Data File-ftpResource Data File", notes = "Resource Data File", response = Result.class)
    public Result resourcesFtpDownloadFile(String id) {
        return openApiService.resourcesFtpDownloadFile(id);
    }


    /**
     * Query all approval lists
     *
     * @return
     */
    @RequestMapping(value = "/findAllApproval", method = RequestMethod.GET)
    @ApiOperation(value = "Approval List", notes = "Approval List", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "String", value = "content", dataTypeClass = String.class),
            @ApiImplicitParam(name = "claimStatus", dataType = "String", value = "Claim status", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true, dataTypeClass = Integer.class)})
    public Result findAllApproval(@RequestHeader("Authorization") String token, String name, String claimStatus,String identifier, Integer pageOffset, Integer pageSize,String resourcesId,String sort) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (null == roles) {
            return ResultUtils.error("LOGIN_EXCEPTION");
        }
        if (!roles.contains(Constant.ADMIN) && !roles.contains(Constant.ROLE_APPROVE)) {
            return ResultUtils.error("PERMISSION_DENIED");
        }
        Map<String, Object> condition = new HashMap<>();
        condition.put("token", token);
        condition.put("name", StringUtils.isNotBlank(name) ? name : "");
        condition.put("resourcesId", StringUtils.isNotBlank(resourcesId) ? resourcesId : "");
        condition.put("tag", "openApi");
        condition.put("pageOffset", pageOffset);
        condition.put("pageSize", pageSize);
        condition.put("approvalStatus", "");
        condition.put("type", Constant.Comment.RESOURCE_PUBLISHING);
        condition.put("approvalAuthor", "");
        condition.put("applyAuthor", "");
        condition.put("applyEmail", "");
        condition.put("claimAuthor", "");
        condition.put("sort", StringUtils.isNotBlank(sort) ? sort : "");
        condition.put("claimStatus", StringUtils.isNotBlank(claimStatus) ? claimStatus : "");
        condition.put("resourceType", "");
        condition.put("identifier",  StringUtils.isNotBlank(identifier) ? identifier : "");
        condition.put("startDate", "");
        condition.put("endDate", "");

        PageHelper datasetStatus = approveService.findApproveList(condition);
        return success(Constant.StatusMsg.SUCCESS, datasetStatus);
    }

    @ApiOperation(value = "Approval of claim")
    @PostMapping("/claim")
    public Result claim(@RequestHeader("Authorization") String token,
                        @RequestParam(value = "id") String id,
                        @RequestParam(value = "status") String status) {
        return approveService.claim(token, id, status);
    }


    @RequestMapping(value = "/getToken", method = RequestMethod.GET)
    public Result getToken(String username, String password, HttpServletResponse response, HttpServletRequest request,String unionId) {
        if (StringUtils.isNotBlank(username)) {
            username = RSAEncrypt.encrypt(username);
        }
        if (StringUtils.isNotBlank(password)) {
            password = RSAEncrypt.encrypt(password);
        }
        return authService.login(username, password, response, request,unionId);
    }

    @ApiOperation(value = "Set file size and redirect download address")
    @PostMapping("/setFilestorageInfo")
    public Result setFilestorageInfo(String datasetId, long storageNum, String datasetRemoteUrl, int visits) {
        return openApiService.setFilestorageInfo(datasetId, storageNum, datasetRemoteUrl, visits);
    }


    @ApiOperation("interfaceapiinterface-interface")
    @RequestMapping(value = "/downloadApiFile", method = RequestMethod.GET)
    public void downloadApiFile(String token, HttpServletResponse response) {
        openApiService.downloadApiFile(token, response);
        return;
    }

}




