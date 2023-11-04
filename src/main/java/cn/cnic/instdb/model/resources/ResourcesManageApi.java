package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:
 */

@Data
public class ResourcesManageApi {

    private String id;
    private String titleZh;
    private String titleEn;

    private String descZh;
    private String descEn;

    private List<String> keywordZh;
    private List<String> keywordEn;

    //subject
    private List<String> subject;

    //Author's thesis project
    //  projectproject paperproject authorproject contributorproject
    private List<ResourcesManage.Value> author;
    private List<ResourcesManage.Value> paper;
    private List<ResourcesManage.Value> project;
    private List<ResourcesManage.Value> contributor;

    //Publishing agencyid
    private String organizationId;

    private String organizationName;

    //version
    private String version;
    private String resourcesId;

    //Resource Type
    private String resourceType;

    private String doi;

    private String cstr;

    //Reference Link
    private String reference_link;


    //Name of publisher
    private String publisherName;

    //Publisher's email
    private String publisherEmail;


    //license agreement
    private String licenseAgreement;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime approveTime;

    //Privacy Policy
    private ResourcesManage.PrivacyPolicy privacyPolicy;

    //Visits
    private int visitNum;
    //Number of downloads
    private int downloadNum;
    //Number of followers
    private int followNum;


}
