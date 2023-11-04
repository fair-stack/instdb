package cn.cnic.instdb.controller;


import cn.cnic.instdb.model.system.ConstantDictionary;
import cn.cnic.instdb.model.system.ConstantDictionaryDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ConstantDictionaryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping(value = "/constant")
@Api(value = "Constant Dictionary", tags = "Constant Dictionary")
@Slf4j
public class ConstantController extends ResultUtils {


    @Resource
    private ConstantDictionaryService constantDictionaryService;


    @RequestMapping(value = "/getDataByType", method = RequestMethod.GET)
    @ApiOperation(value = "Obtain dictionary data based on type", notes = "Obtain dictionary data based on type", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "constant type ", required = true, dataType = "String",dataTypeClass = String.class),
    })
    public Result getConstantDictionaryByType(String type){
        List<ConstantDictionary> list = constantDictionaryService.getConstantDictionaryByType(type);
        return success(list);
    }

    @RequestMapping(value = "/deleteById", method = RequestMethod.POST)
    @ApiOperation(value = "delete", notes = "delete", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "constantid", required = true, dataType = "String",dataTypeClass = String.class),
    })
    public Result deleteById(String id){
        constantDictionaryService.deleteById(id);
        return success("DELETE_SUCCESS");
    }

    @ApiOperation("Add")
    @PostMapping("/save")
    public Result save(@RequestBody ConstantDictionaryDTO constantDictionary){
        constantDictionaryService.save(constantDictionary);
        return success("ADD_SUCCESS");
    }


}
