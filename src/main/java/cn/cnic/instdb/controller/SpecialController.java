package cn.cnic.instdb.controller;

import cn.cnic.instdb.model.special.Special;
import cn.cnic.instdb.model.special.SpecialResourcesDTO;
import cn.cnic.instdb.model.special.SpecialVo;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SettingService;
import cn.cnic.instdb.service.SpecialResourcesService;
import cn.cnic.instdb.service.SpecialService;
import cn.cnic.instdb.utils.CommonUtils;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/19:48
 * @Description: special
 */
@RestController
@Slf4j
@RequestMapping(value = "/special")
@Api(value = "Special control class", tags = "Special control class")
public class SpecialController extends ResultUtils {

    @Autowired
    private SpecialService specialService;

    @Resource
    private SpecialResourcesService specialResourcesService;

    @Resource
    private SettingService settingService;



    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ApiOperation(value = "Creating a Theme", notes = "Creating a Theme", response = Result.class)
    public Result save(@RequestHeader("Authorization") String token,@Valid @RequestBody Special.SpecialDTO specialDTO) {
        return specialService.save(token,specialDTO);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ApiOperation(value = "Modify Topic", notes = "Modify Topic", response = Result.class)
    public Result update(@RequestHeader("Authorization") String token,@Valid @RequestBody Special.SpecialDTO specialDTO) {
        return specialService.update(token,specialDTO);
    }


    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ApiOperation(value = "Delete Topic", notes = "Delete Topic", response = Result.class)
    public Result delete(@RequestHeader("Authorization") String token,String id) {
        return  specialService.delete(token,id);
    }


    @RequestMapping(value = "/findAllSpecial", method = RequestMethod.GET)
    @ApiOperation(value = "Query all topics", notes = "Query all topics", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "specialName", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result findAllSpecial(String specialName, Integer pageOffset, Integer pageSize,String sort, HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        PageHelper allSpecial = specialService.findAllSpecial(specialName, pageOffset, pageSize,sort);
        return success(allSpecial);
    }

    @ApiOperation("Special collection resource function")
    @RequestMapping(value = "/specialAddResources", method = RequestMethod.POST)
    public Result specialAddResources(@RequestHeader("Authorization") String token,@RequestBody SpecialResourcesDTO specialResourcesDTO) {
        specialResourcesService.specialAddResources(token,specialResourcesDTO);
        return success("SPECIAL_RESOURCE_ADD");
    }

    @ApiOperation("Obtain resources under the topic")
    @RequestMapping(value = "/getResourcesBySpecialId", method = RequestMethod.GET)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "specialId", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "resourcesName", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result getResourcesBySpecialId(@RequestHeader("Authorization") String token, String specialId, String resourcesName, String resourceType, Integer pageOffset, Integer pageSize, String sort, HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        PageHelper resourcesBySpecialId = specialResourcesService.getResourcesBySpecialId(token,specialId, resourcesName, resourceType,pageOffset, pageSize,sort);
        return success(resourcesBySpecialId);
    }


    @ApiOperation("Obtaining resources not added under the theme")
    @RequestMapping(value = "/getResourcesByNoSpecial", method = RequestMethod.GET)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "specialId", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "resourcesName", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result getResourcesByNoSpecial(String specialId, String resourcesName, Integer pageOffset, Integer pageSize) {
        PageHelper resources = specialResourcesService.getResourcesByNoSpecial(specialId, resourcesName, pageOffset, pageSize);
        return success(resources);
    }


    @ApiOperation("Topic Details")
    @RequestMapping(value = "/getSpecialById", method = RequestMethod.GET)
    public Result getSpecialById(String id) {
        SpecialVo special = specialService.getSpecialById(id);
        return success(special);
    }

    @ApiOperation("Upload Topiclogo")
    @PostMapping("/specialLogo")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "String",value = "haveidhaveid  have0",dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", dataType = "String",value = "type pass special",dataTypeClass = String.class)})
    public Result spaceLogo(@RequestParam String id,String type, @RequestParam("file") MultipartFile file) {
        String path = settingService.uploadLogo(id,type, file);
        return success("FILE_UPLOAD", path);
    }




    @ApiOperation("Delete resources under the theme")
    @RequestMapping(value = "/deleteResourcesInSpecial", method = RequestMethod.POST)
    public Result deleteResourcesInSpecial(@RequestBody SpecialResourcesDTO specialResourcesDTO) {
        specialResourcesService.deleteResourcesInSpecial(specialResourcesDTO);
        return success("DELETE_SUCCESS");
    }


//    @ApiOperation("managers")
//    @GetMapping("/userList")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "specialId", dataType = "String"),
//            @ApiImplicitParam(name = "username", dataType = "String"),
//            @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "Integer", required = true, defaultValue = "0"),
//            @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "Integer", required = true, defaultValue = "10")})
//    public Result userList(String specialId, String username, Integer pageOffset, Integer pageSize) {
//        List<Map<String, String>> maps = specialService.userList(specialId, username, pageOffset, pageSize);
//        return success(maps);
//    }

    @ApiOperation("Create a list of topic administrators")
    @GetMapping("/addSpecialUserList")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", dataType = "String"),
            @ApiImplicitParam(paramType = "query", name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(paramType = "query", name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result addSpecialUserList(String username, Integer pageOffset, Integer pageSize) {
        List<Map<String, String>> maps = specialService.addSpecialUserList(username, pageOffset, pageSize);
        return success(maps);
    }

//    @ApiOperation("Add administrator")
//    @PostMapping("/addAdministrators")
//    public Result addAdministrators(String specialId, String userId) {
//        specialService.addAdministrators(specialId, userId);
//        return success("Successfully added");
//    }
//
//    @ApiOperation("Remove administrator")
//    @PostMapping("/deleteAdministrators")
//    public Result deleteAdministrators(String specialId, String userId) {
//        specialService.deleteAdministrators(specialId, userId);
//        return success("Successfully deleted");
//    }

}




