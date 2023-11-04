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
public class BasicConfigurationVo {

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

    private String themeColor;

    private List<Links> links = new ArrayList<>();

    private List<BasicConfiguration.Links> recordNumber;


    //Do I need to log in to download   yesDo I need to log in to download   noDo I need to log in to download Do I need to log in to download
    private String downloadPower;
    //Is it enabledftpIs it enabled
    private String ftpSwitch;
    //Can I access this system without logging in
    private String noLoginAccess;
    //Is the email suffix configured for download
    private String emailDownloadPower;
    private String emailSuffixDownload;

    //Filing number
    private String recordNo;
    //Public security registration number
    private String publicSecurityRecordNo;

    private String copyright;
    private String copyright_en;

    //Whether to enable technology cloud login
    private Boolean isCloudLogin;
    private Boolean isCasLogin;
    private Boolean isWechatLogin;
    private Boolean isEscienceLogin;

    //Is the main center account configured
    private boolean isCenterAccount;

    @Data
    public static class Links {
        private String name;
        private String name_en;
        private String url;
    }
}
