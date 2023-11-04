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
@Document(collection = "basic_configuration")
public class BasicConfiguration {

    private String id;

    private String name;
    private String name_en;
    private String email;
    private String phone;
    //mechanismid mechanism
    private String orgName;
    private String banaerLogo;

    private String logo;
    private String icoLogo;
    private String icoEndLogo;

    private String resourcesTopLogo;
    private String resourcesEndLogo;

    //Do I need to log in to download
    private String downloadPower;

    //Theme Colors
    private String themeColor;

    private List<Links> links;

    //Filing number Filing number  Filing number
    private List<Links> recordNumber;

    //Filing number
    private String recordNo;
    //Public security registration number
    private String publicSecurityRecordNo;


    //Copyright Information
    private String copyright;
    private String copyright_en;

    private String update;

    //Is the main center account configured
    private boolean isCenterAccount;

    //Update time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //Modified by
    private String updateByUser;


    @Data
    public static class Links {
        private String name;
        private String name_en;
        private String url;
    }
}
