package cn.cnic.instdb.model.resources;

import cn.cnic.instdb.model.system.Template;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:
 */

@Data
@Document(collection = "resource_access")
public class ResourceAccess {

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

    private  Map applyAuthorInfo;

    //Approved by
    private String approvalAuthor;
    private String approvalAuthorEn;
    //Approver email
    private String approvalEmail;

    //Unique identification of the same resource
    private String resourcesId;

    //Visit duration  Visit duration unlimited  Visit duration range
    private String accessPeriod;

    private String startTime;
    private String endTime;


    //access0access  access unlimited  access range
    private String accessData;
    //Data access restrictions  Data access restrictions unlimited  Data access restrictions range
    private  List<String> filesId;

    //Is it a template used
    private String template;

    //Template data
    private  List<Template.Group> templateData;

    //Usage Description Usage Description
    private String describe;
    //Approval opinions
    private String reason;

    //Resource Type
    private String resourceType;

    private String privacyPolicy;

}
