package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.system.ConstantDictionary;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.AuthService;
import cn.cnic.instdb.service.InstdbApiService;
import cn.cnic.instdb.service.MySettingService;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.utils.CaffeineUtil;
import cn.cnic.instdb.utils.CommonUtils;
import cn.hutool.core.codec.Base64;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.code.kaptcha.Producer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
* @Auther  wdd
* @Date  2021/4/22 15:02
* @Desc  External callback interface
*/
@RestController
@Api(tags = "Callback interface")
@Slf4j
public class CallbackController extends ResultUtils {

    private final Cache<String, String> captchaCache = CaffeineUtil.getCaptcha();


    @Resource
    private SettingService settingService;

    @Resource
    private MySettingService mySettingService;

    @Autowired
    private AuthService authService;

    @Autowired
    private Producer producer;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbApiService instdbApiService;


    /**
     * fairdMetadata acquisition interface
     *
     * @return
     */
    @RequestMapping("/getMetaData")
    public Map getMetaData(String identifier) {
        return instdbApiService.getMetaData(identifier);
    }


    /**
     * Service User Interface  Service User Interface
     *
     * @return
     */
    @GetMapping("/port/appStatus")
    public Map appStatus() {
        HashMap map = new HashMap();
        map.put("code", 200);
        map.put("status", "success");
        return map;
    }

    /**
     * Total Service Requests Interface   Total Service Requests Interface
     *
     * @return
     */
    @ApiOperation(value = "Version update notification", notes = "Version update notification", response = Result.class)
    @PostMapping("/port/getServiceTotalReqNum")
    public Map getServiceTotalReqNum() {
        HashMap map = new HashMap();
        map.put("code", 200);
        map.put("num", 100000);
        return map;
    }

    /**
     * Service User Interface Service User Interface
     *
     * @return
     */
    @PostMapping("/port/getServiceUsersNum")
    public Map getServiceUsersNum() {
        Query query = new Query();
        query.addCriteria(Criteria.where("state").is(1));
        long count = mongoTemplate.count(query, ConsumerDO.class);
        HashMap map = new HashMap();
        map.put("code", 200);
        map.put("count", count);
        return map;
    }



    /**
     *
     * Version update notification
     * @return
     */
    @ApiOperation(value = "Version update notification", notes = "Version update notification", response = Result.class)
    @PostMapping("/version/push")
    public Result versionPush(String version,String details) {
        settingService.versionPush(version,details);
        return success("NOTIFICATION_SUCCESSFUL");
    }

    /**
     *
     * Provided license agreement interface
     * @return
     */
    @GetMapping("/getlicenseAgreement")
    @ApiOperation(value = "license agreement", notes = "license agreement", response = Result.class)
    public Result getlicenseAgreement(){
        List<ConstantDictionary> licenseAgreement = settingService.getlicenseAgreement();
        return success(licenseAgreement);
    }

    /**
     * Provided license agreement interface
     *
     * @return
     */
    @GetMapping("/getlicenseData")
    @ApiOperation(value = "License Agreement Content", notes = "License Agreement Content", response = Result.class)
    public Result getlicenseData(String name) {
        return settingService.getlicenseData(name);
    }

    /**
     *
     *  Obtain a list of metadata standards
     * @return
     */
    @ApiOperation("Obtain a list of metadata standards")
    @RequestMapping("/getTemplatesByName")
    public String getTemplatesByName(String name){
        return settingService.getTemplatesByName(name);
    }

    @ApiOperation("Email activation address")
    @GetMapping("/email.activation")
    public Result emailActivation(@RequestParam(name = "code") String code,HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        return authService.emailActivation(code);
    }

    @GetMapping("/ps.av")
    public Result updatePwd(@RequestParam(name = "code") String code,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        CommonUtils.setLangToReq(request);
        return mySettingService.passActivation(code,request,response);
    }


    @GetMapping("/review")
    public Result review(@RequestParam(name = "code") String code,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        CommonUtils.setLangToReq(request);
        return mySettingService.expertReview(code,request,response);
    }

    /**
     * tokenVerify effectiveness
     *
     * @return
     */
    @ApiOperation("tokenVerify effectiveness")
    @RequestMapping("/check/token")
    public Result checkToken(@RequestHeader("Authorization") String token) {
        ConsumerDO consumerDO = authService.getUserBytoken(token);
        if (null != consumerDO) {
            return success();
        }
        return error("LOGIN_EXCEPTION");
    }


    /**
     * @MethodName createCaptcha
     * @Description Generate verification code
     * @Author wdd
     * @Date 2022/12/24 10:30
     */
    @GetMapping("/create/captcha")
    public Result createCaptcha() throws IOException {
        Map<String, String> map = new HashMap<>();
        // Generate verification code
        String capText = producer.createText();
//        String capStr = capText.substring(0, capText.lastIndexOf("@"));
//        String result = capText.substring(capText.lastIndexOf("@") + 1);
        BufferedImage image = producer.createImage(capText);
        // Save verification code information
        String randomStr = UUID.randomUUID().toString().replaceAll("-", "");
        captchaCache.put(Constant.DEFAULT_CODE_KEY + randomStr, capText);
        // Convert stream information writing
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {
            log.error("ImageIO write err", e);
            return error("SYSTEM_ERROR");
        }

        map.put("uuid", randomStr);
        map.put("img", Base64.encode(os.toByteArray()));
        return success(map);

    }

}
