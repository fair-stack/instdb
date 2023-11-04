package cn.cnic.instdb.model.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/04/10/01:37
 * @Description:
 */

@Data
@Document(collection = "about_config")
public class AboutConfiguration {

    private String id;
    private String height;
    private String banaerLogo;
    //content
    private String content;
    private String content_en;

    //contact us
    private String contact;
    private String contact_en;

    //Terms of Use
    private String termsOfUse;
    private String termsOfUse_en;

    //Modified by
    private String updateByUser;

}
