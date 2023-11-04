package cn.cnic.instdb.controller;

import cn.cnic.instdb.model.rbac.ConsumerInfoDTO;
import cn.cnic.instdb.model.rbac.ManualAdd;
import cn.cnic.instdb.model.rbac.ManualAddList;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.service.UserService;
import cn.cnic.instdb.utils.CommonUtils;
import cn.cnic.instdb.utils.I18nUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 *  user management 
 * @author chl
 * @date 2021/3/19
 */
@RestController
@RequestMapping("user")
@Api(tags = "user management ")
@Slf4j
public class UserController extends ResultUtils {

    @Autowired
    private UserService userService;


    @Resource
    private SettingService settingService;

    @ApiOperation(value = "Add User")
    @PostMapping("/add")
    public Result add(@RequestHeader("Authorization") String token,@RequestBody ManualAdd manualAdd){
        return userService.add(token,manualAdd);
    }


    @ApiOperation(value = "User modification")
    @PostMapping("/adminUserUpdate")
    public Result adminUserUpdate(@RequestHeader("Authorization") String token,
                                                  @RequestBody ManualAdd manualAdd) {
        return userService.adminUserUpdate(token, manualAdd);
    }

    @ApiOperation(value = "Improve user information")
    @PostMapping("/update")
    public Result add(@RequestHeader("Authorization") String token,@RequestBody ConsumerInfoDTO consumerInfoDTO){
        return userService.update(token,consumerInfoDTO);
    }


    @ApiOperation("Replace avatar")
    @PostMapping("/userLogo")
    public Result changeThePicture(@RequestHeader("Authorization") String token,@RequestParam String id,@RequestParam String avatar) {
        return userService.changeThePicture(token,id,avatar);
       // return success("USER_AVATAR");
    }

    @ApiOperation(value = "Query personal information")
    @GetMapping("/getUserInfoByUserId")
    public Result getUserInfoByUserId(String userId){
        return userService.getUserInfoByUserId(userId);
    }

    @ApiOperation("Query user information")
    @GetMapping("/userList")
    public Result userList(@RequestHeader("Authorization") String token,
                                   @RequestParam(value = "pageOffset",defaultValue = "0") int pageOffset,
                                   @RequestParam(value = "pageSize",defaultValue = "10") int pageSize,
                                   @RequestParam(value = "name",required = false) String name,
                                   @RequestParam(value = "email",required = false) String email,
                           @RequestParam(value = "orgChineseName",required = false) String orgChineseName,
                           @RequestParam(value = "role",required = false) String role,String sort, HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        return success(userService.userList(token, pageOffset,pageSize,name,email,orgChineseName,role,sort));
    }

    @ApiOperation(value = "Disabled/Disabled")
    @GetMapping("/disable")
    public Result disable(@RequestHeader("Authorization") String token,
                                          @RequestParam(value = "userId") String userId,
                                          @RequestParam(value = "state") String state){
        userService.disable(token,userId,state);
        return success("1".equals(state) ? "USER_ENABLED" : "USER_DISABLE");
    }


    @ApiOperation(value = "Role List")
    @GetMapping("/roleList")
    public Result roleList(@RequestHeader("Authorization") String token){
        return success(userService.roleList(token));
    }

    @ApiOperation(value = "List of administrators and auditors")
    @GetMapping("/adminUserList")
    public Result adminUserList() {
        return success(userService.adminUserList());
    }

    //Export Template
    @ApiOperation(value = "Export User Template")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        HSSFWorkbook export = userService.export();
        //Obtain output flow based on the response ancestor
        OutputStream outputStream = null;
        try{
            //Declare the corresponding text type
            response.setContentType("application/application/vnd.ms-excel");
            //set name
            String filename = "user template.xls";
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(filename,"UTF-8"));
            outputStream = response.getOutputStream();
            //Calling browser functions through output streamsexcelCalling browser functions through output streams
            export.write(outputStream);
        }catch (Exception e){
            log.error("context",e);
            response.sendError(500, I18nUtil.get("FILE_EXPORT"));
        }
        return;
    }


    @ApiOperation(value = "Batch Import Users")
    @PostMapping("/import.user")
    public Result importUser(@RequestHeader("Authorization") String token,
                                             MultipartFile blobAvatar)  {
        return userService.importUser(token,blobAvatar);
    }


    @ApiOperation(value = "Batch Add Users")
    @PostMapping("/addUserList")
    public Result addUserList(@RequestHeader("Authorization") String token,
                                             @RequestBody ManualAddList manualAddList)  {
        if (null == manualAddList.getPerson() || 0 == manualAddList.getPerson().size()) {
            return error("PARAMETER_ERROR");
        }
        return userService.addUserList(token,manualAddList);
    }



    @ApiOperation("user management -user management ")
    @RequestMapping(value = "/deleteUserById", method = RequestMethod.POST)
    public Result deleteTemplateConfigById(@RequestHeader("Authorization") String token,String id) {
        return userService.deleteUserById(token,id);
    }
}
