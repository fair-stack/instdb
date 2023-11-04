package cn.cnic.instdb.model.openApi;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
* @Auther  wdd
* @Date  2021/9/9 18:58
* @Desc  Authorization Code Management
*/
@Data
@Document(collection = "secret_key")
public class SecretKey {

    @Id
    private String id ;
    //Authorization code
    private String value ;
    //Creator
    private String username;

    //state state /0state1state
    private String status;
    private String organId;
    private String organName;
    private String applicationName;
    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
