package cn.cnic.instdb.service;

import cn.cnic.instdb.model.resources.Expert;
import cn.cnic.instdb.result.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface ExpertReviewService {


    /**
     * Add Expert
     * @param username
     * @param email
     * @param org
     * @return
     */
    Result saveExpert(String username, String email, String org);


    Result updateExpert(Expert expert);

    /**
     * Expert acquisition
     * @param username
     * @return
     */
    Result getExpert(String username);


    /**
     * Generate expert reviews
     * @param resourcesId
     * @param ids
     * @return
     */
    Result createPrivacyLink(String resourcesId, List<String> ids);


    /**
     * Resend evaluation link to email
     * @param resourcesId
     * @param id
     * @return
     */
    Result reCreatePrivacyLink(String resourcesId, String id);

    /**
     * Evaluation function
     * @param status
     * @param reason
     * @return
     */
    Result resourcesReview(String status, String reason,String resourcesId, HttpServletRequest request, HttpServletResponse response);


    /**
     * Obtain the evaluation list of the dataset
     * @param resourcesId
     * @return
     */
    Result getResourcesReview(String resourcesId);

    /**
     * delete
     * @param id
     * @return
     */
    Result deleteResourcesReview(String id);
}
