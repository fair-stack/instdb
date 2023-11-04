package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.findata.PushFinDatasParam;
import cn.cnic.instdb.model.findata.PushFinDatasParamVo;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.CommunityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * @Auther wdd
 * @Date 2022/7/12 22:58
 * @Desc scidbData extraction
 */
@RestController
@Api(tags = "scidbCommunity data extraction")
@Slf4j
@RequestMapping(value = "/community")
public class CommunityController extends ResultUtils {

    @Resource
    private CommunityService communityService;


    @RequestMapping(value = "/getScidbCommunity", method = RequestMethod.POST)
    @ApiOperation(value = "Creating Community Data", notes = "Creating Community DataapiKey Creating Community Data", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "apiKey", dataType = "String", value = "apiKey", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", dataType = "String", value = "name", dataTypeClass = String.class)})
    public Result getScidbCommunity(@RequestHeader("Authorization") String token, String apiKey, String name) {
        return communityService.getScidbCommunity(token, apiKey, name);
    }


    @RequestMapping(value = "/update/scidbCommunity", method = RequestMethod.POST)
    @ApiOperation(value = "Modifying Community Data", notes = "Modifying Community Data", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "apiKey", dataType = "String", value = "apiKey", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", dataType = "String", value = "name", dataTypeClass = String.class)})
    public Result updateScidbCommunity(@RequestHeader("Authorization") String token, String apiKey, String name) {
        return communityService.updateScidbCommunity(token, apiKey, name);
    }

    @RequestMapping(value = "/delete/scidbCommunity", method = RequestMethod.POST)
    @ApiOperation(value = "Delete Community Data", notes = "Delete Community Data", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "String", value = "name", dataTypeClass = String.class)})
    public Result deleteScidbCommunity(@RequestHeader("Authorization") String token, String id) {
        return communityService.deleteScidbCommunity(token, id);
    }


    /**
     * Query all community data
     *
     * @return
     */
    @RequestMapping(value = "/getCommunityList", method = RequestMethod.GET)
    @ApiOperation(value = "Community List", notes = "Community List", response = Result.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "String", value = "Community Name", dataTypeClass = String.class)})
    public Result getCommunityList(@RequestHeader("Authorization") String token, String name) {
        return communityService.getCommunityList(token, name);
    }


    @ApiOperation(value = "cease/cease")
    @PostMapping("/disable/communityState")
    public Result disable(@RequestHeader("Authorization") String token,
                          @RequestParam(value = "apiKey") String apiKey,
                          @RequestParam(value = "state") String state) {
        return communityService.disableCommunityState(token, apiKey, state);
    }


    /**
     * Perform a synchronization of community data once
     *
     * @return
     */
    @RequestMapping(value = "/start", method = RequestMethod.GET)
    @ApiOperation(value = "Execute once", notes = "Execute once", response = Result.class)
    public Result start() {
        communityService.getScidbCommunityData();
        return success();
    }

    @RequestMapping(value = "/getFtpFileByDoi", method = RequestMethod.GET)
    public Result getFtpFileByDoi() {
        communityService.getFtpFileByDoi();
        return success();
    }

    @ApiOperation("Manual push tofindata")
    @RequestMapping(value = "/push/findata", method = RequestMethod.POST)
    public Result manualPushFinData(@RequestBody List<String> ids,String type) {
        return communityService.manualPushFinData(type,ids);
    }

    @ApiOperation("findataReturned Statistics")
    @RequestMapping(value = "/getFindataStatistics", method = RequestMethod.GET)
    public Result getFindataStatistics() {
        return communityService.getFindataStatistics();
    }


    @ApiOperation("PushfindataPush")
    @RequestMapping(value = "/getPushFinDatas", method = RequestMethod.GET)
    public Result getPushFinDatas(String type, String resourceType, String version, String name, String startDate, String endDate, Integer pageOffset, Integer pageSize) {
        return success(communityService.getPushFinDatas(type, resourceType, version, name, startDate, endDate, pageOffset, pageSize));
    }

    @ApiOperation("Batch recommendation tofindata")
    @RequestMapping(value = "/batchPushDataToFindata", method = RequestMethod.POST)
    Result batchPushDataToFindata(@RequestHeader("Authorization") String token,@RequestBody PushFinDatasParamVo pushFinDatasParam) {
        if (null == pushFinDatasParam || StringUtils.isBlank(pushFinDatasParam.getType())){
            return error("PARAMETER_ERROR");
        }
        return communityService.batchPushDataToFindata(token,pushFinDatasParam);
    }

    @ApiOperation("Batch recommendationfindataBatch recommendation")
    @RequestMapping(value = "/getPushDataToFindataConfig", method = RequestMethod.POST)
    Result getPushDataToFindataConfig(@RequestHeader("Authorization") String token) {

        return communityService.getPushDataToFindataConfig(token);
    }

    @PostMapping("/setfindata/status")
    @ApiOperation(value = "allocationfindataallocation", notes = "allocationfindataallocation", response = Result.class)
    public Result setfindataStatus(@RequestHeader("Authorization") String token,String status) {
        return communityService.setfindataStatus(token,status);
    }

}




