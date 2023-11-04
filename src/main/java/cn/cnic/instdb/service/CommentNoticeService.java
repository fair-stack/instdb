package cn.cnic.instdb.service;

import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

public interface CommentNoticeService {


    /**
     *  Message recording
     * @param condition
     * @return
     */
    PageHelper findAllNotice(Map<String, Object> condition);

    Result getNoticesNum(String token);
    Result getSendEmail(String token);

    /**
     * Set Unread Read
     * @param id
     */
    void setRead(String id);

    /**
     * Set personal email recipient type
     *
     * @param token
     * @param waitApproval
     */
    Result setSendEmail(String token, int waitApproval,int approved, int approvalRejected, int approvalRevocation, int versionUp);

    /**
     * Set all read
     */
    void setAllRead(String token);

    /**
     * Create a notification
     * @param type
     * @param content
     */
    void addNotice(String username, String type, String content, String resourcesId,String title);


    /**
     * Get comments from resources
     * @param resourcesId
     * @return
     */
    PageHelper findAllComment(String resourcesId,Integer pageOffset,Integer pageSize);

    /**
     * Add a comment under Resources
     * @param resourcesId
     * @param content
     */
    void addComment(String token,String resourcesId,String content);

    /**
     * Reply to comments
     * @param commentId
     * @param content
     */
    void replyToComments(String token,String commentId,String content);


    /**
     * Obtain reply information under comments
     * @param commentId
     * @return
     */
//    List<Reply> findReply(String commentId);

}
