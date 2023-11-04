package cn.cnic.instdb.model.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/04/10/01:37
 * @Description:
 */

@Data
public class AboutConfigurationDTO {

    private String banaerLogo;

    private String height;
    //content
    private String content;
    private String content_en;

    //contact us
    private String contact;
    private String contact_en;

    //Terms of Use
    private String termsOfUse;
    private String termsOfUse_en;

}
