package cn.cnic.instdb.controller;

import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.MySettingService;
import cn.cnic.instdb.service.ResourcesService;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@RestController
@Api(tags = "Personal Center")
@RequestMapping("/mysetting")
public class MySettingController extends ResultUtils {

    @Resource
    private MySettingService mySettingService;
    @Resource
    private ResourcesService resourcesService;


    @GetMapping("/check")
    @ApiOperation("security setting-security setting")
    public Result checkPassword(String password) {
        return success(mySettingService.checkPassword(password));
    }

    @GetMapping("/verification")
    @ApiOperation("security setting-security setting")
    public Result verification(@RequestHeader("Authorization") String token) {
        return success(mySettingService.verification(token));
    }


    @GetMapping("/pwdEmail")
    @ApiOperation("Retrieve password-Retrieve password")
    public Result pwdEmail(@RequestParam(name = "email") String email,
                                           @RequestParam(name = "name", required = false) String name,String randomStr,String captcha,
                                           HttpServletRequest request) {
        return mySettingService.pwdEmail(email, name, randomStr,captcha,request);
    }

    @PostMapping("/updatePwd")
    @ApiOperation("security setting-security setting-security setting")
    public Result updatePwd(String emailAccounts, String confirmPassword, HttpServletRequest request) {
        return mySettingService.updatePwd(emailAccounts, confirmPassword,request);
    }

    @PostMapping("/setEmail")
    @ApiOperation("security setting-security setting")
    public Result setEmail(@RequestHeader("Authorization") String token, String spareEmail) {
        return  mySettingService.setEmail(token, spareEmail);
    }

//    @GetMapping("/set/host/port")
//    @ApiOperation("Set up theipSet up the")
//    public Result setHostPort(String ip) {
//        return settingService.setHostPort(ip);
//    }

    @GetMapping("/spareEmail")
    @ApiOperation("security setting-security setting")
    public Result spareEmail(@RequestHeader("Authorization") String token) {
        return success("success", mySettingService.spareEmail(token));
    }


    @GetMapping("/getOverview")
    @ApiOperation("Overview statistics")
    public Result getOverview(@RequestHeader("Authorization") String token) {
        return mySettingService.getOverview(token);
    }


    @GetMapping("/getResourcesHistory")
    @ApiOperation("My History Visits")
    public Result getResourcesHistory(@RequestHeader("Authorization") String token,Integer pageOffset, Integer pageSize) {
        PageHelper resourcesHistory = resourcesService.getResourcesHistory(token, pageOffset, pageSize);
        return success(resourcesHistory);
    }

    @GetMapping("/getStatisticsResources")
    @ApiOperation("Recently added data resource statistics")
    public Result getStatisticsResources(@RequestHeader("Authorization") String token) {
        return mySettingService.getStatisticsResources(token);
    }
    @GetMapping("/getStatisticsUser")
    @ApiOperation("Recently added user statistics")
    public Result getStatisticsUser(@RequestHeader("Authorization") String token) {
        return mySettingService.getStatisticsUser(token);
    }

}
