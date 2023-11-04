package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:
 */

@Data
@Document(collection = "applyAccess_template")
public class ApplyAccessTemplate {

    @Id
    private String id;

    //Application content
    private String name;
    private String nameEn;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Approval status
    private String approvalStatus;

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
    //Approval opinions
    private String reason;

    //Resource Type
    private String resourceType;

    private String privacyPolicy;

}
