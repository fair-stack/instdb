package cn.cnic.instdb.service;

import cn.cnic.instdb.model.resources.Approve;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ApproveService {

    /**
     * Approval Record Generation
     *
     * @param manage
     * @return
     */
    void save(ResourcesManage manage);

    /**
     * Approval
     *
     * @param resourcesId
     * @param approvalStatus
     * @param reason
     */
    Result approveSubmit(String token, String resourcesId, String approvalStatus, String reason, String rejectApproval, @RequestParam("file") MultipartFile file);

    /**
     * Go for approval
     *
     * @param resourcesOnlyId
     * @param version
     */
    List<Approve> toApprove(String resourcesOnlyId, String version);


    /**
     * Approval Record
     *
     * @param condition
     * @return
     */
    PageHelper findApproveList(Map<String, Object> condition);


    /**
     * Revoke Approval
     *
     * @param resourceId
     */
    Map<String, Object> revokeApprove(String resourceId);


    /**
     * Claim function
     *
     * @param token
     * @param id
     */
    Result claim(String token, String id, String status);

    /**
     * Administrator Assign Approval Claim
     *
     * @param token
     * @param id
     * @param userEmail
     * @return
     */
    Result adminClaim(String token, String id, String userEmail);

    /**
     * Approval rejection attachment download
     * @param token
     * @param id
     * @param response
     * @return
     */
    void downloadRejectFile(String token, String id, HttpServletResponse response);


    /**
     * Export Approval List
     *
     * @param response
     * @param token
     * @param name
     * @param applyAuthor
     * @param applyEmail
     * @param claimAuthor
     * @param claimStatus
     * @param resourceType
     * @param approvalAuthor
     * @param approvalStatus
     * @param identifier
     * @param startDate
     * @param endDate
     */
    void exportApprovalData(HttpServletResponse response, String token, String name,
                            String applyAuthor, String applyEmail, String claimAuthor, String claimStatus, String resourceType, String approvalAuthor, String approvalStatus, String identifier,
                            String startDate, String endDate);
}
