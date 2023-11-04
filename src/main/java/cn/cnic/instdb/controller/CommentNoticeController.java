package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.CommentNoticeService;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/19:48
 * @Description: Comment notification
 */
@RestController
@Slf4j
@RequestMapping(value = "/commentNotice")
@Api(value = "Comment notification", tags = "Comment notification")
public class CommentNoticeController extends ResultUtils {

    @Autowired
    private CommentNoticeService commentNoticeService;


    @RequestMapping(value = "/findAllNotice", method = RequestMethod.GET)
    @ApiOperation(value = "My message", notes = "My message", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "content", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "startDate", dataType = "String", value = "start time",dataTypeClass = String.class),
            @ApiImplicitParam(name = "endDate", dataType = "String", value = "End time",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result findAllNotice(@RequestHeader("Authorization") String token ,String content, String type,String startDate,String endDate,Integer pageOffset, Integer pageSize) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("token", token);
        condition.put("content", content);
        condition.put("type", type);
        condition.put("startDate", startDate);
        condition.put("endDate", endDate);
        condition.put("pageOffset", pageOffset);
        condition.put("pageSize", pageSize);
        PageHelper allApprove = commentNoticeService.findAllNotice(condition);
        return success(Constant.StatusMsg.SUCCESS, allApprove);
    }

    @RequestMapping(value = "/setSendEmail", method = RequestMethod.POST)
    @ApiOperation(value = "Set personal email recipient type", notes = "Set personal email recipient type", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "waitApproval", dataType = "Integer", required = true, dataTypeClass = Integer.class)})
    public Result setSendEmail(@RequestHeader("Authorization") String token, int waitApproval, int approved, int approvalRejected, int approvalRevocation, int versionUp) {
        return commentNoticeService.setSendEmail(token, waitApproval, approved, approvalRejected, approvalRevocation, versionUp);
    }

    @RequestMapping(value = "/setRead", method = RequestMethod.POST)
    @ApiOperation(value = "Set Read", notes = "Set Read", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "String",dataTypeClass = String.class)})
    public Result setRead(String id) {
       commentNoticeService.setRead(id);
       return success("SETTINGS_READ");
    }

    @RequestMapping(value = "/getSendEmail", method = RequestMethod.GET)
    public Result getSendEmail(@RequestHeader("Authorization") String token) {
        return commentNoticeService.getSendEmail(token);
    }

    @RequestMapping(value = "/getNoticesNum", method = RequestMethod.GET)
    public Result getNoticesNum(@RequestHeader("Authorization") String token) {
        return commentNoticeService.getNoticesNum(token);
    }

    @RequestMapping(value = "/setAllRead", method = RequestMethod.POST)
    @ApiOperation(value = "Set all read", notes = "Set all read", response = Result.class)
    public Result setAllRead(@RequestHeader("Authorization") String token) {
        commentNoticeService.setAllRead(token);
        return success("SETTINGS_ALL_READ");
    }


//    @RequestMapping(value = "/addNotice", method = RequestMethod.POST)
//    @ApiOperation(value = "Create Notification", notes = "Create Notification", response = Result.class)
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "name", dataType = "String"),
//            @ApiImplicitParam(name = "type", dataType = "String"),
//            @ApiImplicitParam(name = "content", dataType = "String")})
//    public Result addNotice(String name,String type,String content) {
//        commentNoticeService.addNotice(name,type,content);
//        return success("Successfully created notification...");
//    }

    @RequestMapping(value = "/findAllComment", method = RequestMethod.GET)
    @ApiOperation(value = "Get resource comments", notes = "Get resource comments", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result findAllComment(String resourcesId,Integer pageOffset, Integer pageSize) {
        PageHelper allComment = commentNoticeService.findAllComment(resourcesId,pageOffset,pageSize);
        return success(Constant.StatusMsg.SUCCESS,allComment);
    }

    @RequestMapping(value = "/addComment", method = RequestMethod.POST)
    @ApiOperation(value = "Comment on it", notes = "Comment on it", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "resourcesId", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "content", dataType = "String",dataTypeClass = String.class)})
    public Result addComment(@RequestHeader("Authorization") String token,String resourcesId,String content) {
        commentNoticeService.addComment(token,resourcesId,content);
        return success("COMMENT_SUCCESS");
    }


//    @RequestMapping(value = "/findReply", method = RequestMethod.GET)
//    @ApiOperation(value = "Get a comment reply", notes = "Get a comment reply", response = Result.class)
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "commentId", dataType = "String",required = true)})
//    public Result findReply(String commentId) {
//        List<Reply> reply = commentNoticeService.findReply(commentId);
//        return success("Successfully obtained a comment reply...",reply);
//    }


    @RequestMapping(value = "/replyToComments", method = RequestMethod.POST)
    @ApiOperation(value = "Reply to comments", notes = "Reply to comments", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "commentId", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "content", dataType = "String",dataTypeClass = String.class)})
    public Result replyToComments(@RequestHeader("Authorization") String token, String commentId,String content) {
        commentNoticeService.replyToComments(token,commentId,content);
        return success("REPLY");
    }

}




