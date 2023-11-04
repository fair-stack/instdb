package cn.cnic.instdb.service;

import cn.cnic.instdb.model.resources.ApplyAccessApproval;
import cn.cnic.instdb.model.resources.ApplyAccessSubmit;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface ApplyAccessService {

    /**
     * Resource Restricted Access Request
     *
     * @param token
     * @param resourceId
     * @param startTime
     * @param endTime
     * @param listMap
     * @param org
     * @param phone
     * @return
     */
    Result resourceAccessRequest(String token, ApplyAccessSubmit applyAccessSubmit);


    /**
     * Template creation
     *
     * @param token
     * @param file
     * @return
     */
    Result uploadDataTemplate(String token, @RequestParam("file") MultipartFile file);

    /**
     * File confirmation
     *
     * @param token
     * @param id
     * @return
     */
    Result submitFile(String token, String id);

    /**
     * Apply for access template deletion
     *
     * @param token
     * @param id
     * @return
     */
    Result deleteTemplateById(String token, String id);


    /**
     * Obtain Request Access Template  Obtain Request Access Template
     *
     * @return
     */
    Result getMyApplyTemplate();


    /**
     * Application access template list display
     *
     * @return
     */
    Result getMyApplyTemplateList();

    /**
     * My application
     *
     * @param token
     * @param status
     * @return
     */
    Map getMyApply(String token, String status);


    /**
     * Request Access List
     *
     * @param token
     * @param tag
     * @param name
     * @param resourceType
     * @param approvalAuthor
     * @param applyAuthor
     * @param startDate
     * @param endDate
     * @return
     */
    PageHelper getResourceAccess(String token, String tag, String name, String resourceType, String approvalAuthor, String applyAuthor, String approvalStatus, String startDate, String endDate, Integer pageOffset, Integer pageSize,String sort);


    /**
     * Resource limited application access approval
     *
     * @param applyAccessApproval
     */
    Result resourceAccessApproval(String token, ApplyAccessApproval applyAccessApproval);

    /**
     * Application for access details
     *
     * @param id
     * @return
     */
    Result getResourceAccess(String id);

    /**
     * Example of applying for access-Example of applying for access
     * @param token
     * @param response
     * @return
     */
    void downloadAccessTemplate(String token, HttpServletResponse response);

}
