package cn.cnic.instdb.elasticsearch;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2020/09/16/19:26
 * @Description:
 */


@Data
@Document(collection = "es_search_history")
public class EsServiceParamsDb {

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Number of results
    private long resultNum;

    //user name
    private String username;

    /**
     * Filter conditions
     */
    private List<EsParameter> esParameter;

    @Data
    public static class EsParameter {
        private String fieldName;//Parameter Name
        private String field;//Parameter Name
        private String value;//Parameter value
        private String operator;// Query operation keywords
        private String connector;// Connector
        private String fieldType; //Type of field
    }


}



