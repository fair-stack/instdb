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
@Document(collection = "push_findatas_config")
public class PushFinDatasParam {

    @Id
    private String id;
    //Recommended cycle executeOnce Recommended cycle day Recommended cycle  week Recommended cycle  monthRecommended cycle closeRecommended cycle
    private String type;

    //Resource Type
    private List<String>   resourceType;

    //Privacy Policy
    private  List<String>  privacyPolicy;

    //license agreement
    private List<String>  license;

    //specialid
    private   List<String> special;

    private   List<String> keywords;

    //subject
    private List<String> subject;

    private List<String> year;

    private String author;

    private String status;  //yesopen

    //Timed expression
    private String cron;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;


}
