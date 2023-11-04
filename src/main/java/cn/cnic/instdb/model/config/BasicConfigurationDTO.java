package cn.cnic.instdb.model.config;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/04/10/01:37
 * @Description:
 */

@Data
public class BasicConfigurationDTO {

    private String name;
    private String name_en;
    private String email;
    private String phone;

    private String logo;
    private String icoLogo;
    private String icoEndLogo;

    private String banaerLogo;
    private String themeColor;

    private List<Links> links = new ArrayList<>();

    //Void  Void Void
    private List<BasicConfiguration.Links> recordNumber;

    //Filing number
    private String recordNo;
    //Public security registration number
    private String publicSecurityRecordNo;


    private String copyright;
    private String copyright_en;
    @Data
    public static class Links {
        private String name;
        private String name_en;
        private String url;
    }
}
