package cn.cnic.instdb.utils;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.service.SubjectAreaService;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Data
public class InstdbUrl {

    private MongoTemplate mongoTemplate;

    private SubjectAreaService subjectAreaService;

    private SettingService settingService;

    @Value("${data.activation.resetPassword}")
    private String resetPassword;

    @Value("${data.activation.setUpPsd}")
    private String setUpPsd;

    @Value("${data.activation.review}")
    private String review;

    //Current environment variables
    @Value("${spring.profiles.active}")
    private String profilesActive;

    /* ftpAddress and Port */
//    @Value("${ftp.host}")
//    private String host;

    @Value("${ftp.port}")
    private String port;

    @Value("${ftp.port1}")
    private String port1;

    //Default timeout time
    @Value("${ftp.timeOut}")
    private String timeOut;

    //Default buffer size
    @Value("${ftp.bufferSize}")
    private String bufferSize;

    //Resource Details Page Path
    @Value("${data.resources.address}")
    private String resourcesAddress;

    //Official storage location for resource files
    @Value("${data.resources.file}")
    private String resourcesFilePath;

    //resource fileftpresource file
    @Value("${data.resources.ftpFile}")
    private String resourcesFtpFilePath;

    //Temporary storage location for resource files
    @Value("${data.resources.temp}")
    private String resourcesTempFilePath;

    //Official storage location for resource images
    @Value("${data.resources.picture}")
    private String resourcesPicturePath;

    //Approval rejection attachment storage location
    @Value("${data.resources.rejectFile}")
    private String resourcesRejectFilePath;



    //Installation component address
    @Value("${data.install_component_source}")
    private String installComSource;


    //Installation component address
    @Value("${data.install_component_web}")
    private String installComWeb;


    @Value("${data.special.logo}")
    private String specialLogo;

    @Value("${data.user.logo}")
    private String userLogo;

    @Value("${data.subject.logo}")
    private String subjectLogo;

    @Value("${data.banaer_favicon.logo}")
    private String banaer_icoLogo;

    @Value("${data.template.url}")
    private String templateUrl;

    //allocationesallocation
    @Value("${config.es.highlight}")
    private String esHighlight;

    //allocationesallocation
    @Value("${config.es.searchField}")
    private String esSearchField;


    //allocationesallocation
    @Value("${config.es.highlight_color}")
    private String esHighlightColor;


    /* Technology Cloud */
    @Value("${call.casLoginUrl}")
    private volatile String casLoginUrl;
    @Value("${call.casLogoutUrl}")
    private volatile String casLogoutUrl;
    @Value("${call.authUrl}")
    private volatile String authUrl;
    @Value("${call.authParam}")
    private volatile String authParam;


    /* WeChat */
    @Value("${call.wechatCallbackUrl}")
    private String wechatCallbackUrl;

    @Value("${call.wechatBindingUrl}")
    private String wechatBindingUrl;

    @Value("${call.wechatConfUrl}")
    private String wechatConfUrl;

    /* Shared Network */
    @Value("${call.escienceCallbackUrl}")
    private String escienceCallbackUrl;

    /* cas */
    @Value("${call.casCallbackUrl}")
    private String casCallbackUrl;

    @Value("${config.dataciteDoiUrl}")
    private String dataciteDoiUrl;

    @Value("${config.findataAPIUrl}")
    private String findataAPIUrl;
    @Value("${config.findataUrl}")
    private String findataUrl;

    @Value("${config.cstrUrl}")
    private String cstrUrl;

    @Value("${config.marketUrl}")
    private String marketUrl;

    @Value("${config.fairman_market_url}")
    private String fairmanMarketUrl;


    //set upipset up set upfriman
    @Value("${acc.host}")
    private String accHost;


    //Specially designedftpSpecially designed
    @Value("${acc.ftpHost}")
    private String accftpHost;

    @Value("${acc.port}")
    private String accPort;

    private String callHost;
    //ftpaddress
    private String ftpHost;

    //handleftphandle
    public String getFtpHost() {
        if (StringUtils.isBlank(accftpHost)) {
            String ftpIp = accHost;
            if (StringUtils.isBlank(ftpIp)) {
                ftpHost = "ftp://localhost:" + port1;
            } else {
                String substring = ftpIp.substring(ftpIp.length() - 1);
                if (substring.equals("/")) {
                    ftpIp = ftpIp.substring(0, ftpIp.length() - 1);
                }
                if (ftpIp.contains("https://")) {
                    ftpIp = ftpIp.replaceAll("https://", "");
                } else if (ftpIp.contains("http://")) {
                    ftpIp = ftpIp.replaceAll("http://", "");
                }
                ftpHost = "ftp://" + ftpIp + ":" + port1;
            }
        } else {
            return "ftp://" + accftpHost + ":" + port1;
        }
        return ftpHost;
    }

    //Processing front-end unified addresses
//    public String getCallHost() {
//        String url = "";
//        if (accHost == null) {
//            url = "localhost";
//        }else {
//            url = accHost;
//        }
//        if (StringUtils.isNotEmpty(accPort)) {
//            int port = Integer.parseInt(accPort);
//            if (port != 80) {
//                callHost = Constant.HTTP + url + ":" + port;
//            } else {
//                callHost = Constant.HTTP + url;
//            }
//        } else {
//            callHost = Constant.HTTP + url;
//        }
//        return callHost;
//    }

    public String getCallHost() {
        String url = "";
        if (accHost == null) {
            url = "localhost";
        } else {
            url = accHost;
        }

        if (!url.contains("https") && !url.contains("http")) {
            url = Constant.HTTP + url;
        }

        String substring = url.substring(url.length() - 1);
        if (substring.equals("/")) {
            url = url.substring(0, url.length() - 1);
        }

        if (StringUtils.isNotEmpty(accPort)) {
            int port = 0;
            try {
                port = Integer.parseInt(accPort);
            } catch (Exception e) {
            }

            if (url.contains("https")) {
                if (port != 443 && port != 80 && port != 0) {
                    url = url + ":" + port;
                }
            } else {
                if (port != 80 && port != 0) {
                    url = url + ":" + port;
                }
            }
        }

        return url;
    }

}
