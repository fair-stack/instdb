package cn.cnic.instdb.model.special;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:special
 */

@Data
@Document(collection = "special")
public class Special {

    @Id
    private String id;

    //Topic Name
    private String specialName;
    private String specialNameEn;

    //Topic Description
    private String specialDesc;
    private String specialDescEn;

    //speciallogo
    private String logo;

    //Topic administrator
    private List<User> user;

    //Theme tags
    private List<String> specialTag;
    private List<String> specialTagEn;

    //Include resourcesid
    private int resourcesNum;
    //Number of downloads
    private int downloadNum;
    //Visits
    private int visitNum;
    //Physical file size
    private long storageNum;

    private Set<AuthorizationPerson> authorizationList;



    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Update time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //Creator
    private String createByUser;


    /**
     * @Auther: wdd
     * @Date: 2021/03/23/17:09
     * @Description:special
     */
    @Data
    public static class User {
        private String id;
        private String name;
    }

    @Data
    public static class SpecialDTO {

        private String id;

        //Topic Name
        @NotBlank(message = "Topic name cannot be empty")
        private String specialName;
        private String specialNameEn;

        //Topic Description
        private String specialDesc;
        private String specialDescEn;

        //speciallogo
        private String logo;

        //Topic administrator
        private List<String> userId;

        //Theme tags
        private List<String> specialTag;
        private List<String> specialTagEn;

    }
}
