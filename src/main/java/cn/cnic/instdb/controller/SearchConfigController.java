package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.system.SearchConfigDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SearchConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @Author：wdd
 * @describe：Retrieve Configuration
 * @Date：2023/2/13 17:48
 */
@RestController
@Slf4j
@RequestMapping(value = "/search/config")
@Api(value = "Retrieve Configuration", tags = "Retrieve Configuration")
public class SearchConfigController extends ResultUtils {

    @Resource
    private SearchConfigService searchConfigService;

    @ApiOperation("Set Configuration List")
    @RequestMapping(value = "/setSearchConfigs", method = RequestMethod.POST)
    public Result setSearchConfigs(@RequestHeader("Authorization") String token, @RequestBody SearchConfigDTO searchConfig) {
        return searchConfigService.setSearchConfigs(token, searchConfig);
    }

    @ApiOperation("Delete Configuration List")
    @RequestMapping(value = "/deleteSearchConfigs", method = RequestMethod.POST)
    public Result deleteSearchConfigs(@RequestHeader("Authorization") String token, String id) {
        return searchConfigService.deleteSearchConfigs(token, id);
    }

    @ApiOperation("Edit Configuration List")
    @RequestMapping(value = "/updateSearchConfigs", method = RequestMethod.POST)
    public Result updateSearchConfigs(@RequestHeader("Authorization") String token, String id,String name) {
        return searchConfigService.updateSearchConfigs(token, id,name);
    }

    @ApiOperation("Initialize retrieval data")
    @RequestMapping(value = "/resetSearchConfigs", method = RequestMethod.POST)
    public Result resetSearchConfigs(@RequestHeader("Authorization") String token) {
        return searchConfigService.resetSearchConfigs(token);
    }


    @ApiOperation("Get Configuration List")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", dataType = "String", value = "typestatistics/search", required = false, dataTypeClass = String.class)})
    @RequestMapping(value = "/getSearchConfigs", method = RequestMethod.GET)
    public Result getSearchConfigs(@RequestHeader("Authorization") String token, String type) {
        if (StringUtils.isBlank(type) || StringUtils.isBlank(token)) {
            return error("PARAMETER_ERROR");
        }
        if (!Constant.STATISTICS.equals(type) && !Constant.SEARCH.equals(type)) {
            return error("PARAMETER_ERROR");
        }
        return searchConfigService.getSearchConfigs(token, type);
    }


}




