package cn.cnic.instdb.controller;

import cn.cnic.instdb.model.resources.Expert;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ExpertReviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/19:48
 * @Description: expert review 
 */
@RestController
@Slf4j
@RequestMapping(value = "/review")
@Api(value = "expert review ", tags = "expert review ")
public class ExpertReviewController extends ResultUtils {

    @Resource
    private ExpertReviewService expertReviewService;


    @RequestMapping(value = "/saveExpert", method = RequestMethod.POST)
    @ApiOperation(value = "Add Expert", notes = "Add Expert", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", dataType = "String", required = true,dataTypeClass = String.class),
            @ApiImplicitParam(name = "email", dataType = "String", required = true,dataTypeClass = String.class),
            @ApiImplicitParam(name = "org", dataType = "String", required = true,dataTypeClass = String.class)})
    public Result saveExpert(String username, String email, String org) {
        return expertReviewService.saveExpert(username, email, org);
    }

    @RequestMapping(value = "/updateExpert", method = RequestMethod.POST)
    @ApiOperation(value = "Modify Expert", notes = "Modify Expert", response = Result.class)
    public Result updateExpert(@RequestBody Expert expert) {
        return expertReviewService.updateExpert(expert);
    }

    @RequestMapping(value = "/getExpert", method = RequestMethod.GET)
    @ApiOperation(value = "Obtaining expert information", notes = "Obtaining expert information", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", dataType = "String", required = true,dataTypeClass = String.class)})
    public Result getExpert(String username) {
        return expertReviewService.getExpert(username);
    }


    @RequestMapping(value = "/create/review", method = RequestMethod.POST)
    @ApiOperation(value = "Generate data resource evaluation", notes = "Generate data resource evaluation", response = Result.class)
    public Result createPrivacyLink(String resourcesId,@RequestBody List<String> ids) {
        return expertReviewService.createPrivacyLink(resourcesId, ids);
    }

    @RequestMapping(value = "/resourcesReview", method = RequestMethod.POST)
    @ApiOperation(value = "Data resource evaluation", notes = "Data resource evaluation", response = Result.class)
    public Result resourcesReview(String status, String reason,String resourcesId, HttpServletRequest request, HttpServletResponse response) {
        return expertReviewService.resourcesReview(status, reason,resourcesId, request,response);
    }

    @RequestMapping(value = "/getResourcesReview", method = RequestMethod.GET)
    @ApiOperation(value = "Obtain Dataset Review List", notes = "Obtain Dataset Review List", response = Result.class)
    public Result getResourcesReview(String resourcesId) {
        return expertReviewService.getResourcesReview(resourcesId);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ApiOperation(value = "Delete evaluation", notes = "Delete evaluation", response = Result.class)
    public Result deleteResourcesReview(String id) {
        return expertReviewService.deleteResourcesReview(id);
    }


    @RequestMapping(value = "/resendEmail/link", method = RequestMethod.POST)
    @ApiOperation(value = "Resend evaluation link to email", notes = "Resend evaluation link to email", response = Result.class)
    public Result resendEmailPrivacyLink(String resourcesId, String id) {
        return expertReviewService.reCreatePrivacyLink(resourcesId, id);
    }

    @RequestMapping(value = "/create/ExpertReview", method = RequestMethod.POST)
    @ApiOperation(value = "Invite experts again for evaluation", notes = "Invite experts again for evaluation", response = Result.class)
    public Result createExpertReview(String resourcesId, String id) {
        List<String> ids = new ArrayList<>();
        ids.add(id);
        return expertReviewService.createPrivacyLink(resourcesId, ids);
    }

}




