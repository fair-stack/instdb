package cn.cnic.instdb.controller;

import cn.cnic.instdb.model.system.Subject;
import cn.cnic.instdb.model.system.SubjectArea;
import cn.cnic.instdb.model.system.SubjectAreaDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SubjectAreaService;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/04/08/23:48
 * @Description: Discipline field
 */
@RestController
@Slf4j
@RequestMapping(value = "/subject")
@Api(value = "Discipline field", tags = "Discipline field")
public class SubjectAreaController extends ResultUtils {

    @Autowired
    private SubjectAreaService subjectAreaService;

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ApiOperation(value = "establish", notes = "establish", response = Result.class)
    public Result save(SubjectAreaDTO subjectAreaDTO) {
        return subjectAreaService.save(subjectAreaDTO);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ApiOperation(value = "modify", notes = "modify", response = Result.class)
    public Result update(SubjectAreaDTO subjectAreaDTO) {
        return subjectAreaService.update(subjectAreaDTO);
    }

    @RequestMapping(value = "/getSubjectAreaAll", method = RequestMethod.GET)
    @ApiOperation(value = "Query All", notes = "Query All", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "subject", dataType = "String",dataTypeClass = String.class),
            @ApiImplicitParam(name = "pageOffset", dataType = "Integer", required = true,dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", required = true,dataTypeClass = Integer.class)})
    public Result getSubjectAreaAll(String name, String subject,Integer pageOffset, Integer pageSize) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("name", name);
        condition.put("subject", subject);
        condition.put("pageOffset", pageOffset);
        condition.put("pageSize", pageSize);
        PageHelper allSubject = subjectAreaService.getSubjectAreaAll(condition);
        return success(allSubject);
    }

    @ApiOperation("Discipline Details")
    @RequestMapping(value = "/getSubjectAreaById", method = RequestMethod.GET)
    public Result getSubjectAreaById(String id) {
        SubjectArea subjectArea = subjectAreaService.getSubjectAreaById(id);
        return success(subjectArea);
    }


    @ApiOperation("delete")
    @RequestMapping(value = "/deleteById", method = RequestMethod.POST)
    public Result deleteResourcesInSpecial(String id) {
        subjectAreaService.deleteById(id);
        return success("DELETE_SUCCESS");
    }

    @ApiOperation("Batch deletion")
    @RequestMapping(value = "/deleteByIds", method = RequestMethod.POST)
    public Result deleteResourcesInSpecial(@RequestBody List<String> ids) {
        subjectAreaService.deleteByIds(ids);
        return success("DELETE_SUCCESS_BATCH");
    }

    @ApiOperation("Cascading query of subject fields")
    @RequestMapping(value = "/getSubjectAreaInfo", method = RequestMethod.GET)
    public Result getSubjectAreaInfo() {
        List<Subject> subject = subjectAreaService.getSubjectAreaInfo();
        return success(subject);
    }

    @ApiOperation("Cascading query of subject fieldsTwo")
    @RequestMapping(value = "/getSubjectAreaInfoTwo", method = RequestMethod.GET)
    public Result getSubjectAreaInfoTwo(String id,String no) {
        List<Subject> subject = subjectAreaService.getSubjectAreaInfoTwo(id,no);
        return success(subject);
    }

    @ApiOperation("Cascading query of subject fieldsThree")
    @RequestMapping(value = "/getSubjectAreaInfoThree", method = RequestMethod.GET)
    public Result getSubjectAreaInfoThree(String id,String no) {
        List<Subject> subject = subjectAreaService.getSubjectAreaInfoThree(id,no);
        return success(subject);
    }

//    @ApiOperation("Discipline fieldlogo")
//    @PostMapping("/subjectLogo")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "id", dataType = "String",value = "haveidhaveid  have0",dataTypeClass = String.class),
//            @ApiImplicitParam(name = "type", dataType = "String",value = "type pass subject",dataTypeClass = String.class)})
//    public Result subjectLogo(@RequestParam String id,String type,  @RequestParam("file") MultipartFile file) {
//        String path = settingService.uploadLogo(id,type, file);
//        return success("FILE_UPLOAD", path);
//    }

    @ApiOperation("Discipline field")
    @GetMapping("/getSubjectList")
    public Result getSubjectList(){
        return subjectAreaService.getSubjectList();
    }


    @RequestMapping(value = "/get/subjectArea", method = RequestMethod.GET)
    @ApiOperation(value = "Query all subject areas without pagination", notes = "Query all subject areas without pagination", response = Result.class)
    public Result getSubjectArea() {
        return  subjectAreaService.getSubjectArea();
    }


    @RequestMapping(value = "/uploadIco", method = RequestMethod.POST)
    @ApiOperation(value = "Upload small icons", notes = "Upload small icons", response = Result.class)
    public Result uploadIco(String id,  String icon, String iconColor) {
        return  subjectAreaService.uploadIco(id,icon,iconColor);
    }

}




