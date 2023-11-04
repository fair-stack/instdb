package cn.cnic.instdb.model.special;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/03/29/14:37
 * @Description: Topic and resource correlation
 */
@Data
@Document(collection = "special_resources")
public class SpecialResources {

    private String id;

    private String specialId;

    private String specialName;

    private String specialNameEn;

    private String resourcesId;

    private String resourcesName;
    private String resourceType;

    private String mode;
    private String operator;
    private String operatorEn;
    private String operatorEmail;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}

