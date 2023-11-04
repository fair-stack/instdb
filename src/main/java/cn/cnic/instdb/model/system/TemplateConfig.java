package cn.cnic.instdb.model.system;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/04/09/23:27
 * @Description: Template Configuration
 */

@Data
@Document(collection = "template_config")
public class TemplateConfig {

    @Id
    private String id;

    private String name;
    //Template Description
    private String templateDesc;
    //Templateauthor
    private String templateAuthor;

    private String code;

    private String type;
    private String typeName;


    private String path;

    private String username;
    private String usernameEn;
    private String userEmail;

    //0Effective 1Effective
    private String state;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
