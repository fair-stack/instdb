package cn.cnic.instdb.controller;

import cn.cnic.instdb.model.config.*;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.SystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;


@RestController
@Api(tags = "System settings-System settings")
@RequestMapping("/system")
public class SystemConfigController extends ResultUtils {

    @Resource
    private SystemConfigService systemConfigService;

    @PostMapping("/basic_config")
    @ApiOperation("allocation-allocation")
    public Result setBasicConfig(@RequestHeader("Authorization") String token,@RequestBody  BasicConfigurationDTO basicConfigurationDTO) {
        return systemConfigService.setBasicConfig(token, basicConfigurationDTO);
    }

    @ApiOperation("Basic configuration-Basic configuration")
    @RequestMapping(value = "/getBasicConfig", method = RequestMethod.GET)
    public Result getBasicConfig() {
        BasicConfigurationVo copyrightLinks = systemConfigService.getBasicConfig();
        return success(copyrightLinks);
    }


    @ApiOperation("allocation-allocation")
    @RequestMapping(value = "/setResourcesConfig", method = RequestMethod.POST)
    public Result setResourcesConfig(String resourcesTopLogo,  String resourcesEndLogo) {
        return systemConfigService.setResourcesConfig(resourcesTopLogo, resourcesEndLogo);
    }


    @PostMapping("/about_config")
    @ApiOperation("allocation-allocation")
    public Result setAboutConfig(@RequestHeader("Authorization") String token,  AboutConfigurationDTO aboutConfiguration) {
        return systemConfigService.setAboutConfig(token, aboutConfiguration);
    }


    @ApiOperation("About Configuration-About Configuration")
    @RequestMapping(value = "/getAboutConfig", method = RequestMethod.GET)
    public Result getAboutConfig() {
        return success(systemConfigService.getAboutConfig());
    }



    @PostMapping("/index_config")
    @ApiOperation("allocation-allocation")
    public Result setIndexConfig(@RequestHeader("Authorization") String token, @RequestBody IndexConfigurationDTO indexConfig) {
        return systemConfigService.setIndexConfig(token, indexConfig);
    }


    @ApiOperation("Homepage Configuration-Homepage Configuration")
    @RequestMapping(value = "/getIndexConfig", method = RequestMethod.GET)
    public Result getIndexConfig() {
        IndexConfiguration indexConfig = systemConfigService.getIndexConfig();
        return success(indexConfig);
    }

    @ApiOperation("allocation-allocation")
    @RequestMapping(value = "/subjectArea__config", method = RequestMethod.POST)
    public Result setSubjectAreaConfig(@RequestBody SubjectAreaIndex subjectAreaIndex) {
         systemConfigService.setSubjectAreaConfig(subjectAreaIndex);
        return success();
    }

    @ApiOperation(value = "setDsUrl")
    @PostMapping("/setDsUrl")
    public Result claim(@RequestParam(value = "dsUrl") String dsUrl){
        return systemConfigService.setDsUrl(dsUrl);
    }


}
