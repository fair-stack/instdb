package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.sf.json.JSONArray;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:Dedicated to displaying resource information in a list
 */

@Data
public class ResourcesManageIndex {
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
    private String description;
    private boolean image;

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

}
