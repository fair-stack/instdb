package cn.cnic.instdb.model.system;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/04/22/18:12
 * @Description:Constant Dictionary Class
 */

@Data
@Document(collection = "constant_dictionary")
public class ConstantDictionary {

    @Id
    private String id;

    private String name;
    private String nameEn;

    private String code;

    private String type;

    //describe
    private String value;

    private String explain;
    private String explainEn;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
