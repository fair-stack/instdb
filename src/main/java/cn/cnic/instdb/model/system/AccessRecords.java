package cn.cnic.instdb.model.system;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Technology Cloud Account Configuration
 */
@Data
@Document(collection = "access_records")
public class AccessRecords {

    @Id
    private String id;

    private String resourcesId;

    private String ip;
    private String name;
    //user name
    private String username;
    private int visitNum;
    private int downloadNum;
    //Download storage capacity
    private long downloadStorage ;
    private String createTime;



}
