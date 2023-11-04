package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.ApplyAccessApproval;
import cn.cnic.instdb.model.resources.ApplyAccessSubmit;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ApplyAccessService;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/19:48
 * @Description: Apply for access
 */
@RestController
@Slf4j
@RequestMapping(value = "/apply/access")
@Api(value = "Apply for access", tags = "Apply for access")
public class ApplyAccessController extends ResultUtils {

    @Resource
    private ApplyAccessService applyAccessService;

    @ApiOperation("Template-Template")
    @PostMapping("/uploadDataTemplate")
    public Result uploadDataTemplate(@RequestHeader("Authorization") String token, @RequestParam("file") MultipartFile file) {
        return applyAccessService.uploadDataTemplate(token, file);
    }

    @ApiOperation("Template-Template")
    @PostMapping("/submitFile")
    public Result submitFile(@RequestHeader("Authorization") String token, String id) {
        return applyAccessService.submitFile(token, id);
    }

    @ApiOperation("Template-Template")
    @RequestMapping(value = "/deleteTemplateById", method = RequestMethod.POST)
    public Result deleteTemplateById(@RequestHeader("Authorization") String token, String id) {
        return applyAccessService.deleteTemplateById(token, id);
    }


    @RequestMapping(value = "/getMyApplyTemplateList", method = RequestMethod.GET)
    @ApiOperation(value = "Template-Template", notes = "Template", response = Result.class)
    public Result getMyApplyTemplateList() {
        return applyAccessService.getMyApplyTemplateList();
    }

    @RequestMapping(value = "/getMyApplyTemplate", method = RequestMethod.GET)
    @ApiOperation(value = "Template-Template", notes = "Template  Template", response = Result.class)
    public Result getMyApplyTemplate() {
        return applyAccessService.getMyApplyTemplate();
    }


    @RequestMapping(value = "/getMyApply", method = RequestMethod.GET)
    @ApiOperation(value = "My application", notes = "My application", response = Result.class)
    public Result getMyApply(@RequestHeader("Authorization") String token, String status) {
        Map getMyApply = applyAccessService.getMyApply(token, status);
        return success(getMyApply);
    }

    @RequestMapping(value = "/resourceAccessRequest", method = RequestMethod.POST)
    @ApiOperation(value = "Resource Restricted Access Request", notes = "Resource Restricted Access Request", response = Result.class)
    public Result resourceAccessRequest(@RequestHeader("Authorization") String token, @RequestBody ApplyAccessSubmit applyAccessSubmit) {
        return applyAccessService.resourceAccessRequest(token, applyAccessSubmit);
    }


    @RequestMapping(value = "/getResourceAccess", method = RequestMethod.GET)
    @ApiOperation(value = "Request Access List", notes = "Request Access List", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tag", dataType = "String", value = "tagpassallpass,pass", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", dataType = "String", value = "content", dataTypeClass = String.class),
            @ApiImplicitParam(name = "applyAuthor", dataType = "String", value = "Applicant's Name,Applicant's Name,Applicant's Name,Applicant's Name", dataTypeClass = String.class),
            @ApiImplicitParam(name = "resourceType", dataType = "String", value = "Dataset Resource Type", dataTypeClass = String.class),
            @ApiImplicitParam(name = "approvalAuthor", dataType = "String", value = "Approved by", dataTypeClass = String.class),
            @ApiImplicitParam(name = "sort", dataType = "String", value = "sort field", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startDate", dataType = "String", value = "start time", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endDate", dataType = "String", value = "End time", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true, dataTypeClass = Integer.class)})
    public Result findAllApproval(@RequestHeader("Authorization") String token, String tag, String name,
                                  String applyAuthor, String resourceType, String approvalAuthor, String approvalStatus,
                                  String startDate, String endDate, Integer pageOffset, Integer pageSize,String sort) {
        PageHelper allResourceAccess = applyAccessService.getResourceAccess(token, tag, name, resourceType, approvalAuthor, applyAuthor, approvalStatus, startDate, endDate, pageOffset, pageSize,sort);
        return success(Constant.StatusMsg.SUCCESS, allResourceAccess);
    }

    @RequestMapping(value = "/resourceAccessApproval", method = RequestMethod.POST)
    @ApiOperation(value = "Resource limited application access approval", notes = "Resource limited application access approval", response = Result.class)
    public Result resourceAccessApproval(@RequestHeader("Authorization") String token, @RequestBody ApplyAccessApproval applyAccessApproval) {
        return applyAccessService.resourceAccessApproval(token, applyAccessApproval);
    }


    @RequestMapping(value = "/getData", method = RequestMethod.GET)
    @ApiOperation(value = "Application for access details", notes = "Application for access details", response = Result.class)
    public Result resourceAccessApproval(String id) {
        return applyAccessService.getResourceAccess(id);
    }

    @ApiOperation("Example of applying for access-Example of applying for access")
    @RequestMapping(value = "/downloadAccessTemplate", method = RequestMethod.GET)
    public void downloadAccessTemplate(String token, HttpServletResponse response) {
        applyAccessService.downloadAccessTemplate(token, response);
        return;
    }
}




