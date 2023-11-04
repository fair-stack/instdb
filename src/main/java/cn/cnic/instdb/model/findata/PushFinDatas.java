package cn.cnic.instdb.model.findata;

import cn.cnic.instdb.model.resources.ResourcesManage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.sf.json.JSONArray;
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
@Document(collection = "push_findatas")
public class PushFinDatas {

    @Id
    private String id;
    private String resourcesId;

    private String name;
    private String name_en;

    private String type;

    //Resource Type
    private String resourceType;

    //version
    private String version;
    private String doi;
    private String cstr;

    //Privacy Policy
    private ResourcesManage.PrivacyPolicy privacyPolicy;

    //license agreement
    private String license;

    private String description;
    private String description_en;

    private List<String> keywords;
    private List<String> keywords_en;

    //subject
    private List<String> subject;
    private List<String> subjectEn;

    private JSONArray author;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Physical file size
    private long storageNum;

    //Number of files
    private long fileCount;

    @Data
    public static class PrivacyPolicy {
        //Privacy Policy ： Privacy Policy：notOpen ,Privacy Policy：open ,Privacy Policy：protect Privacy Policy：condition
        private String type;
        //Protection period time
        private String openDate;
        private String condition;
    }

    @Data
    public static class Value {
        private String id;
        private String name;
    }


}
