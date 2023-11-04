package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.sf.json.JSONArray;
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
@Document(collection = "resources_manage")
public class ResourcesManage {

    @Id
    private String id;
    private String resourcesId;
    private String es_id;

    private String name;
    private String name_en;

    //Resource Type
    private String resourceType;

    //version
    private String version;
    //Markings for the latest version Markings for the latest versiontrue
    private String versionFlag;
    private String doi;
    private String cstr;

    //Privacy Policy
    private PrivacyPolicy privacyPolicy;

    //license agreement
    private String license;

    private String description;
    private String description_en;

    private List<String> keywords;
    private List<String> keywords_en;

    //subject
    private List<String> subject;
    private List<String> subjectEn;
    private List<String> ipc;

    private JSONArray author;

    //Identification of whether the resource file has been downloaded and completed
    private String downloadFileFlag;

    //Approval callback and address modification
    private Resources.CallbackUrl callbackUrl;

    //Release institutional information
    private Resources.Organization organization;

    //Publisher Information
    private Resources.Publish publish;

    //Metadata template name
    private String templateName;

    //0  File Publishing  1 File Publishing   2 File Publishing+File Publishing   -1File Publishing
    private Map<String,Object> dataType;

    //Yes No zip   yes /no
    private String fileIsZip;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Visits
    private long visitNum;
    //Number of downloads
    private long downloadNum;
    //Number of followers
    private long followNum;
    //Physical file size
    private long storageNum;

    //Number of files
    private long fileCount;

    private boolean showFile;

    //state  state
    private String status;
    //Approval time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;
    //Approved by
    private String approvalAuthor;

    @lombok.Data
    public static class PrivacyPolicy {
        //Privacy Policy ： Privacy Policy：notOpen ,Privacy Policy：open ,Privacy Policy：protect Privacy Policy：condition
        private String type;
        //Protection period time
        private String openDate;
        private String condition;
    }


    @lombok.Data
    public static class Value {
        private String id;
        private String name;
    }



}
