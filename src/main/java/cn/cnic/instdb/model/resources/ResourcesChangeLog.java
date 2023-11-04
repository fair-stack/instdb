package cn.cnic.instdb.model.resources;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:Resource Change Log
 */

@Data
@Document(collection = "Resources_change_log")
public class ResourcesChangeLog {

    @Id
    private String id;

    //resourceid
    private String resourcesId;

    //Name of operator
    private String operatorName;
    //Operator email
    private String operatorEmail;
    //Operation time
    private LocalDateTime operatorTime;

    //Operation content
    private String content;

    //type type
    private String type;

}
