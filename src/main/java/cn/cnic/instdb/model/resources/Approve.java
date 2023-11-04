package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:
 */

@Data
@Document(collection = "approve")
public class Approve {

    @Id
    private String id;

    //Application content
    private String name;
    private String nameEn;

    //type
    private String type;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Approval status
    private String approvalStatus;

    //Approval opinions
    private String reason;
    //Reason for rejection
    private String rejectApproval;

    //Approval time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalTime;

    //Applicant's Name
    private String applyAuthor;
    private String applyAuthorEn;
    //Applicant email
    private String applyEmail;

    //Approved by
    private String approvalAuthor;
    private String approvalAuthorEn;
    //Approver email
    private String approvalEmail;

    //Unique identification of the same resource
    private String resourcesId;

    //Usage Description Usage Description
    private String describe;

    //Claimant's Name
    private String claimAuthor;
    private String claimAuthorEn;
    //Claimant's email
    private String claimEmail;
    //Claim status
    private String claimStatus;

    //Resource Type
    private String resourceType;
    //Project number
    private String identifier;

    //Identification of whether the resource file has been downloaded and completed
    private String downloadFileFlag;


    private long storageNum;
    private String privacyPolicy;


    //Expert evaluation information  2/3   Expert evaluation information/Expert evaluation information
    private String resourcesReview;


    //Review information
    private List<ResourcesReview> resourcesReviews;

    private String rejectFilePath;
    private String rejectFileName;
}
