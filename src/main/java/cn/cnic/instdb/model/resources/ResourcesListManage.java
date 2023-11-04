package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.sf.json.JSONArray;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:Dedicated to displaying resource information in a list
 */

@Data
public class ResourcesListManage {
    private String id;
    private String resourcesId;
    private String name;
    private String name_en;
    //Resource Type
    private String resourceType;

    //version
    private String version;
    private String doi;
    private String cstr;

    //Privacy Policy
    private ResourcesManage.PrivacyPolicy privacyPolicy;

    private String description_en;
    private String description;

    private JSONArray subjectOf;

    private JSONArray author;
    private String license;
    private boolean image;

    //subject
    private List<String> subject;
    private List<String> subjectEn;

    private List<String> keywords;
    private List<String> keywords_en;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createTime;

    //Approval time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime approveTime;

    //Visits
    private int visitNum;
    //Number of downloads
    private int downloadNum;
    //Number of followers
    private int followNum;

    //Physical file size
    private long storageNum;

    private String status;
    //Is it being followed
    private String follow;

    private String mode;
    private String operator;
    private String operatorEn;
    private String operatorEmail;

}
