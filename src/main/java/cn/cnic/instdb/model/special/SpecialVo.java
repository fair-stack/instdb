package cn.cnic.instdb.model.special;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:special
 */

@Data
public class SpecialVo {

    private String id;

    //Topic Name
    private String specialName;
    private String specialNameEn;

    //Topic Description
    private String specialDesc;
    private String specialDescEn;

    //speciallogo
    private String logo;

    private boolean isResources;

    //Theme tags
    private List<String> specialTag;
    private List<String> specialTagEn;

    //Number of included resources
    private int resourcesNum;
    //Number of downloads
    private int downloadNum;
    //Visits
    private int visitNum;

    //Physical file size
    private long storageNum;

    //Topic administrator
    private Set<AuthorizationPerson> authorizationList;


    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createTime;


}
