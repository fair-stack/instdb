package cn.cnic.instdb.service;

import cn.cnic.instdb.model.config.EmailConfig;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.model.system.ConstantDictionary;
import cn.cnic.instdb.model.system.Template;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/04/07/19:03
 * @Description:
 */
public interface SettingService {

    /**
     * Version notification update
     * @param version
     * @param details
     * @return
     */
    void versionPush(String version,String details);


    /**
     * uploadlogo
     *
     * @param id
     * @param type type  special / subject
     * @param file
     * @return
     */
    String uploadLogo(String id, String type, MultipartFile file);

    /**
     * Upload metadata template
     *
     * @param token
     * @param file
     */
    Result uploadDataTemplate(String token, String constantId, @RequestParam("file") MultipartFile file);


    /**
     * Download metadata template
     *
     * @param
     */
    void downloadDataTemplate(String token, String id, HttpServletResponse response);

    /**
     * Query metadata template configuration list
     *
     * @param condition
     * @return
     */
    PageHelper getTemplateConfigAll(Map<String, Object> condition);

    Result deleteTemplateConfigById(String id);

    void deleteTemplateConfigByIds(List<String> ids);

    /**
     * Template interface provided externally
     *
     * @return
     */
    String getTemplatesByName(String name);

    Template getTemplate(String name);

    /**
     * License Agreement Interface Provided Externally
     *
     * @return
     */
    List<ConstantDictionary> getlicenseAgreement();


    /**
     * Obtaining License Agreement Content
     * @param name
     * @return
     */
    Result getlicenseData(String name);


    /**
     * Technology Cloud Information Configuration
     */
    Result umtUpdate(String id, String appKey, String appSecret, String page, String callback, boolean isOpen);


    /**
     * casLogin Configuration
     *
     * @param id
     * @param casServerUrl
     * @param casServerUrlLogin
     * @param casServerUrlLogoutUrl
     * @param username
     * @param name
     * @param isOpen
     * @return
     */
    Result casUpdate(String id, String casServerUrl, String casServerUrlLogin, String casServerUrlLogoutUrl, String homePage,String username, String name, boolean isOpen);

    /**
     * WeChat login configuration
     *
     * @param id
     * @param appId
     * @param secretKey
     * @param page
     * @param isOpen
     * @return
     */
    Result weChatUpdate(String id, String appId, String secretKey, String page, boolean isOpen);

    /**
     * Shared Network Configuration Login
     *
     * @param id
     * @param clientId
     * @param clientSecret
     * @param page
     * @param isOpen
     * @return
     */
    Result escienceUpdate(String id, String clientId, String clientSecret, String page, boolean isOpen);


    /**
     * Binding institution information
     * @return
     */
    Result setOrg(String token,String cstr,  String host);

    /**
     * bindingcstr
     * @param token
     * @param cstr
     * @param clientId
     * @param secret
     * @return
     */
    Result setCstr( String token,String cstr, String clientId, String secret, String cstrCode, int cstrLength);


    /**
     * bindingdoi
     * @param token
     * @param doiType
     * @param repositoryID
     * @param doiPassword
     * @param doiCode
     * @param doiLength
     * @return
     */
    Result setDoi( String token,String doiType,String doiPrefiex, String repositoryID, String doiPassword, String doiCode, int doiLength);

    /**
     * Set the account password for interaction with the main center
     * @param token
     * @param username
     * @param password
     * @return
     */
    Result setNetworkPassword(String token, boolean isNetwork,String username,String password);

    /**
     * Obtaining Technology Cloud Configuration Information
     * @return
     */
    Result umtGet();
    /**
     * CASConfiguration information acquisition
     * @return
     */
    Result casGet();
    /**
     * WeChat configuration information acquisition
     * @return
     */
    Result weChatGet();
    /**
     * Obtaining Shared Network Configuration Information
     * @return
     */
    Result escienceGet();

    CenterAccount getCenterConf();

    /**
     * System email configuration acquisition
     * @return
     */
    EmailConfig getEmailConfig();
    /**
     * System email configuration settings
     * @return
     */
    Result setEmailConfig(EmailConfig emailConfig);

    /**
     * Email test sending
     * @param email
     * @return
     */
    Result testSendEmail(String token ,String email);

    /**
     * Does the file download require login configuration
     * @param token
     * @param downloadPower
     * @return
     */
    Result downloadNoLogin(String token, String downloadPower,String ftpSwitch,String noLoginAccess,String emailDownloadPower,String emailSuffix);

}
