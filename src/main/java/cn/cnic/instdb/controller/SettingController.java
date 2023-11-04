package cn.cnic.instdb.controller;

import cn.cnic.instdb.model.config.EmailConfig;
import cn.cnic.instdb.model.system.CenterAccount;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@Api(tags = "Basic Settings")
@RequestMapping("/setting")
public class SettingController extends ResultUtils {

    @Resource
    private SettingService settingService;

    @ApiOperation(value = "Technology Cloud Configuration-Technology Cloud Configuration")
    @GetMapping("/umt.get")
    public Result umtGet() {
        return settingService.umtGet();
    }

    @ApiOperation(value = "Technology Cloud Configuration-Technology Cloud Configuration/Technology Cloud Configuration")
    @PostMapping("/umt.update")
    public Result umtUpdate(@RequestParam(name = "id", required = false) String id,
                            @RequestParam(name = "appKey") String appKey,
                            @RequestParam(name = "appSecret") String appSecret,
                            @RequestParam(name = "page") String page,
                            @RequestParam(name = "callback") String callback,
                            @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.umtUpdate(id, appKey, appSecret, page, callback, isOpen);
    }


    @ApiOperation(value = "CASallocation-allocation")
    @GetMapping("/cas.get")
    public Result casGet() {
        return settingService.casGet();
    }


    @ApiOperation(value = "CASallocation-allocation/allocation")
    @PostMapping("/cas.update")
    public Result casUpdate(@RequestParam(name = "id", required = false) String id,
                            @RequestParam(name = "casServerUrl") String casServerUrl,
                            @RequestParam(name = "casServerUrlLogin") String casServerUrlLogin,
                            @RequestParam(name = "casServerUrlLogoutUrl") String casServerUrlLogoutUrl,
                            @RequestParam(name = "homePage") String homePage,
                            @RequestParam(name = "username") String username,
                            @RequestParam(name = "name") String name,
                            @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.casUpdate(id, casServerUrl, casServerUrlLogin, casServerUrlLogoutUrl, homePage, username, name, isOpen);
    }


    @ApiOperation(value = "WeChat configuration-WeChat configuration")
    @GetMapping("/weChat.get")
    public Result weChatGet() {
        return settingService.weChatGet();
    }


    @ApiOperation(value = "WeChat configuration-WeChat configuration/WeChat configuration")
    @PostMapping("/weChat.update")
    public Result weChatUpdate(@RequestParam(name = "id", required = false) String id,
                               @RequestParam(name = "appId") String appId,
                               @RequestParam(name = "secretKey") String secretKey,
                               @RequestParam(name = "page") String page,
                            @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.weChatUpdate(id, appId, secretKey, page, isOpen);
    }

    @ApiOperation(value = "Shared Network Configuration-Shared Network Configuration")
    @GetMapping("/escience.get")
    public Result escienceGet() {
        return settingService.escienceGet();
    }


    @ApiOperation(value = "Shared Network Configuration-Shared Network Configuration/Shared Network Configuration")
    @PostMapping("/escience.update")
    public Result umtUpdate(@RequestParam(name = "id", required = false) String id,
                            @RequestParam(name = "clientId") String clientId,
                            @RequestParam(name = "clientSecret") String clientSecret,
                            @RequestParam(name = "page") String page,
                            @RequestParam(name = "isOpen", defaultValue = "false") boolean isOpen) {
        return settingService.escienceUpdate(id, clientId, clientSecret, page, isOpen);
    }

    @ApiOperation("Basic configuration-banaer favicon logo")
    @PostMapping("/banaerlLogo")
    public Result banaerlLogo(@RequestParam String id, String type, @RequestParam("file") MultipartFile file) {
        String path = settingService.uploadLogo(id, type, file);
        return success("FILE_UPLOAD", path);
    }

    @ApiOperation("Metadata template configuration file-Metadata template configuration file")
    @PostMapping("/uploadTemplateConfig")
    public Result uploadTemplateConfig(@RequestHeader("Authorization") String token, String constantId, @RequestParam("file") MultipartFile file) {
        return  settingService.uploadDataTemplate(token, constantId, file);
    }

    @ApiOperation("Metadata template configuration file-Metadata template configuration file")
    @RequestMapping(value = "/downloadDataTemplate", method = RequestMethod.GET)
    public Result downloadDataTemplate(String token, String id, HttpServletResponse response) {
        settingService.downloadDataTemplate(token, id, response);
        return success();
    }


    @RequestMapping(value = "/getTemplateConfigAll", method = RequestMethod.GET)
    @ApiOperation(value = "Metadata template configuration file-Metadata template configuration file", notes = "Metadata template configuration file", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "sort", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result getTemplateConfigAll(String name, Integer pageOffset, Integer pageSize,String sort) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("name", name);
        condition.put("sort", StringUtils.isNotBlank(sort) ? sort : "");
        condition.put("pageOffset", pageOffset);
        condition.put("pageSize", pageSize);
        PageHelper allTemplateConfig = settingService.getTemplateConfigAll(condition);
        return success(allTemplateConfig);
    }


    @ApiOperation("Metadata template configuration file-Metadata template configuration file")
    @RequestMapping(value = "/deleteTemplateConfigById", method = RequestMethod.POST)
    public Result deleteTemplateConfigById(String id) {
        return settingService.deleteTemplateConfigById(id);
    }

    @ApiOperation("Metadata template configuration file-Metadata template configuration file")
    @RequestMapping(value = "/deleteTemplateConfigByIds", method = RequestMethod.POST)
    public Result deleteTemplateConfigByIds(@RequestBody List<String> ids) {
        settingService.deleteTemplateConfigByIds(ids);
        return success("DELETE_SUCCESS_BATCH");
    }



    @PostMapping("/set/network/password")
    @ApiOperation(value = "Scientific research network configuration account password", notes = "Scientific research network configuration account password", response = Result.class)
    public Result setNetworkPassword(@RequestHeader("Authorization") String token,boolean isNetwork, String username, String password) {
        return settingService.setNetworkPassword(token,isNetwork, username,password);
    }

    @PostMapping("/set/org")
    @ApiOperation(value = "Binding institution information", notes = "Binding institution information", response = Result.class)
    public Result setOrg(@RequestHeader("Authorization") String token,String cstr, String host) {
        return settingService.setOrg(token,cstr ,host);
    }

    @PostMapping("/set/cstr")
    @ApiOperation(value = "bindingcstr", notes = "bindingcstr", response = Result.class)
    public Result setCstr(@RequestHeader("Authorization") String token,String cstr, String clientId, String secret, String cstrCode, int cstrLength) {
        return settingService.setCstr(token,cstr ,clientId,secret,cstrCode,cstrLength);
    }

    @PostMapping("/set/doi")
    @ApiOperation(value = "bindingdoi", notes = "bindingdoi", response = Result.class)
    public Result setDoi(@RequestHeader("Authorization") String token, String doiType,String doiPrefiex, String repositoryID, String doiPassword, String doiCode, int doiLength) {
        return settingService.setDoi(token, doiType, doiPrefiex,repositoryID, doiPassword, doiCode, doiLength);
    }

    @ApiOperation(value = "General Center Configuration-General Center Configuration")
    @GetMapping("/getCenterConf")
    public Result getCenterConf() {
        CenterAccount centerConf = settingService.getCenterConf();
        return success(centerConf);
    }

    @ApiOperation(value = "mailbox system -mailbox system ")
    @GetMapping("/getEmailConfig")
    public Result getEmailConfig() {
        EmailConfig emailConfig = settingService.getEmailConfig();
        return success(emailConfig);
    }


    @ApiOperation(value = "mailbox system -mailbox system ")
    @PostMapping("/setEmailConfig")
    public Result setEmailConfig(@RequestBody EmailConfig emailConfig) {
        return settingService.setEmailConfig(emailConfig);
    }

    @ApiOperation(value = "mailbox system -mailbox system ")
    @GetMapping("/testSendEmail")
    public Result testSendEmail(@RequestHeader("Authorization") String token, String email) {
        return settingService.testSendEmail(token, email);
    }

    @PostMapping("/set/download/NoLogin")
    @ApiOperation(value = "Do I need to log in to download", notes = "Do I need to log in to download", response = Result.class)
    public Result downloadNoLogin(@RequestHeader("Authorization") String token, String downloadPower, String ftpSwitch, String noLoginAccess, String emailDownloadPower, String emailSuffix) {
        return settingService.downloadNoLogin(token, downloadPower, ftpSwitch, noLoginAccess, emailDownloadPower, emailSuffix);
    }

}
