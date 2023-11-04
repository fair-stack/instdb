package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:
 */

@Data
public class ResourcesManageVo {

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

    //File viewing interface
//    private String fileUrl;

    //Publishing agencyid
    private String organizationId;

    private String organizationName;

    //Metadata template
    private String metadataTemplateId;

    //Metadata template name
    private String metadataTemplateName;

    //version
    private String version;
    private String resourcesId;

    //Resource Type
    private String resourceType;

    private String doi;

    private String cstr;

    //Reference Link
    private String reference_link;

    //Whether to follow Whether to followis no
    private String follow;

    //Name of publisher
    private String publisherName;

    //Publisher's email
    private String publisherEmail;

    //Approval time
    private LocalDateTime approveTime;

    //license agreement
    private String licenseAgreement;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createTime;

    //Privacy Policy
    private ResourcesManage.PrivacyPolicy privacyPolicy;


    //Show files or not  trueShow files or not  falseShow files or not
    private boolean showFile;


    //Historical versions of resources
    private List<ResourcesManageVo.ResourcesVersion> resourcesVersion;

    //Visits
    private int visitNum;
    //Number of downloads
    private int downloadNum;
    //Number of followers
    private int followNum;

    @lombok.Data
    public static class ResourcesVersion {
        private String name;
        private String name_en;
        private String doi;
        private String cstr;
        private String version;
        private String id;
        private String author;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime createTime;
    }

}
