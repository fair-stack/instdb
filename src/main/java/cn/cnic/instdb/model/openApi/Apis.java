package cn.cnic.instdb.model.openApi;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author  wdd
 * @describe open_apilist
 * @date  2022/8/8 16:22
 */

@Data
@Document(collection = "open_api")
public class Apis {

    @Id
    private String id ;
    private String name ;
    private String desc;
    private String version;
    private String url;

    //state state /0state1state
    private String status;
    private List<Authorization> authorizationList;


    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;


    @lombok.Data
    public static class Authorization {
        private String id;
        private String organId;
        private String name;
        private String applicationName;
        private String type;
        //Protection period time
        private String openDate;
        //Authorized person
        private String operator;
    }
}
