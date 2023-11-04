package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.Approve;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ApproveService;
import cn.cnic.instdb.utils.FileUtils;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/19:48
 * @Description: special
 */
@RestController
@Slf4j
@RequestMapping(value = "/approve")
@Api(value = "Approval control class", tags = "Approval control class")
public class ApproveController extends ResultUtils {

    @Resource
    private ApproveService approveService;

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

        if (StringUtils.isBlank(approvalStatus) || StringUtils.isBlank(resourcesId)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        if (Constant.Approval.NO.equals(approvalStatus)) {
            if (null != file && !file.isEmpty()) {
                if (!FileUtils.checkFileSizeIsLimit(file.getSize(), 10, "M")) {
                    return ResultUtils.error("FILE_TOO_BIG10");
                }
            }
            if (StringUtils.isBlank(rejectApproval) || StringUtils.isBlank(reason)) {
                return ResultUtils.error("APPROVE_NO");
            }
        }
        return approveService.approveSubmit(token, resourcesId, approvalStatus, reason, rejectApproval, file);
    }


    @ApiOperation("Export Approval List")
    @RequestMapping(value = "/exportApprovalData", method = RequestMethod.GET)
    public void exportApprovalData(HttpServletResponse response, String token, String name,
                                   String applyAuthor, String applyEmail, String claimAuthor, String claimStatus, String resourceType, String approvalAuthor, String approvalStatus, String identifier,
                                   String startDate, String endDate) {
        approveService.exportApprovalData(response, token, name, applyAuthor, applyEmail, claimAuthor, claimStatus, resourceType, approvalAuthor, approvalStatus, identifier, startDate, endDate);
    }

    @RequestMapping(value = "/toApprove", method = RequestMethod.GET)
    @ApiOperation(value = "Obtain approval records under resources", notes = "Obtain approval records under resources", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String", required = true, value = "resourcesId Unique identification of resources", dataTypeClass = String.class),
            @ApiImplicitParam(name = "version", dataType = "String", required = true, value = "Current approved version", dataTypeClass = String.class)})
    public Result toApprove(String resourcesId, String version) {
        List<Approve> approves = approveService.toApprove(resourcesId, version);
        return success(approves);
    }


    @ApiOperation("Approval Rejection Attachment-Approval Rejection Attachment")
    @RequestMapping(value = "/downloadRejectFile", method = RequestMethod.GET)
    public void downloadRejectFile(String token, String id, HttpServletResponse response) {
        approveService.downloadRejectFile(token, id, response);
    }



    @RequestMapping(value = "/findAllApproval", method = RequestMethod.GET)
    @ApiOperation(value = "Approval List", notes = "Approval List", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tag", dataType = "String", value = "tagpassallpass,pass", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", dataType = "String", value = "content", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", dataType = "String", dataTypeClass = String.class),

            @ApiImplicitParam(name = "applyAuthor", dataType = "String", value = "Applicant's Name,Applicant's Name,Applicant's Name,Applicant's Name", dataTypeClass = String.class),
            @ApiImplicitParam(name = "applyEmail", dataType = "String", value = "Applicant email", dataTypeClass = String.class),
            @ApiImplicitParam(name = "claimAuthor", dataType = "String", value = "Claimant's Name", dataTypeClass = String.class),
            @ApiImplicitParam(name = "claimStatus", dataType = "String", value = "Claim status", dataTypeClass = String.class),
            @ApiImplicitParam(name = "resourceType", dataType = "String", value = "Dataset Resource Type", dataTypeClass = String.class),
            @ApiImplicitParam(name = "approvalStatus", dataType = "String", value = "Approval status", dataTypeClass = String.class),
            @ApiImplicitParam(name = "approvalAuthor", dataType = "String", value = "Approved by", dataTypeClass = String.class),
            @ApiImplicitParam(name = "identifier", dataType = "String", value = "Project number", dataTypeClass = String.class),
            @ApiImplicitParam(name = "sort", dataType = "String", value = "sort field", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startDate", dataType = "String", value = "start time", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endDate", dataType = "String", value = "End time", dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true, dataTypeClass = Integer.class)})
    public Result findAllApproval(@RequestHeader("Authorization") String token, String tag, String name, String type,
                                  String applyAuthor, String applyEmail, String claimAuthor, String claimStatus, String resourceType, String approvalAuthor,String approvalStatus,String identifier,
                                  String startDate, String endDate, Integer pageOffset, Integer pageSize,String sort) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("token", token);
        condition.put("tag", tag);
        condition.put("name", name);
        condition.put("resourcesId", "");
        condition.put("type", type);
        condition.put("sort", StringUtils.isNotBlank(sort) ? sort : "");

        condition.put("approvalAuthor", approvalAuthor);
        condition.put("applyAuthor", applyAuthor);
        condition.put("applyEmail", applyEmail);
        condition.put("claimAuthor", claimAuthor);
        condition.put("claimStatus", claimStatus);
        condition.put("resourceType", resourceType);
        condition.put("approvalStatus", approvalStatus);
        condition.put("identifier",  StringUtils.isNotBlank(identifier) ? identifier : "");

        condition.put("startDate", startDate);
        condition.put("endDate", endDate);
        condition.put("pageOffset", pageOffset);
        condition.put("pageSize", pageSize);
        PageHelper allApprove = approveService.findApproveList(condition);
        return success(Constant.StatusMsg.SUCCESS, allApprove);
    }


    @ApiOperation(value = "Approval of claim")
    @PostMapping("/claim")
    public Result claim(@RequestHeader("Authorization") String token,
                        @RequestParam(value = "id") String id,
                        @RequestParam(value = "status") String status) {
        return approveService.claim(token, id, status);
    }

    @ApiOperation(value = "Administrator Assign Approval Claim")
    @PostMapping("/admin/claim")
    public Result adminClaim(@RequestHeader("Authorization") String token,
                             @RequestParam(value = "id") String id,
                             @RequestParam(value = "userEmail") String userEmail) {
        return approveService.adminClaim(token, id, userEmail);
    }


}




