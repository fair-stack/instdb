package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.Resources;
import cn.cnic.instdb.result.EsDataPage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ApproveService;
import cn.cnic.instdb.service.InstdbApiService;
import cn.cnic.instdb.service.ResourcesService;
import cn.cnic.instdb.utils.DateUtils;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
* @Auther  wdd
* @Date  2021/5/26 10:58
* @Desc  External services provided
*/
@RestController
@Api(tags = "External services provided")
@Slf4j
@RequestMapping(value = "/fair")
public class InstdbApiController extends ResultUtils {

    @Resource
    private InstdbApiService instdbApiService;

    @Resource
    private ResourcesService resourcesService;

    @Resource
    private ApproveService approveService;

    /**
     * Authentication interface
     *
     * @return
     */
    @ApiOperation("Authentication interface")
    @RequestMapping("/entry")
    public Map entry(@RequestHeader("secretKey") String secretKey) {
        return instdbApiService.entry(secretKey);
    }


    /**
     * Get a list of all datasets
     *
     * @return
     */
    @ApiOperation("Get a list of all datasets")
    @RequestMapping("/dataset/list")
    public List<Map<String, Object>> datasetList(String publishDate) {
        if(StringUtils.isNotBlank(publishDate)){
            try {
                 DateUtils.getLocalDateTimeByString2(publishDate);
            }catch (Exception e){
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
        return instdbApiService.datasetList(publishDate);
    }


    @RequestMapping(value = "/dataset/publish", method = RequestMethod.POST)
    public Map<String, String> release(@RequestBody Resources resources) {
        return resourcesService.dataRelease(resources);
    }

    @RequestMapping(value = "/dataset/cancel")
    public Map<String, Object> revokeApprove(String resourceId) {
        return approveService.revokeApprove(resourceId);
    }

    /**
     *
     * modifyftpmodify
     * @return
     */
    @RequestMapping("/uploadCompleted")
    public Map<String, Object> uploadCompleted(String resourceId) {
        return instdbApiService.uploadCompleted(resourceId);
    }


    /**
     *
     *  Obtain a list of metadata standards
     * @return
     */
    @ApiOperation("Obtain a list of metadata standards")
    @RequestMapping("/getTemplates")
    public List<Map<String,String>> getTemplates(){
        return instdbApiService.getDataTemplate();
    }


    /**
     *
     * Data resource query service
     * Provide data resources for the central center and other platform systems to access and access the shared data resources of the center level service platform。
     * @return
     */
    @ApiOperation("Data resource query service")
    @GetMapping("/dataset/search")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", dataType = "String", value = "Retrieve content",dataTypeClass = String.class),
            @ApiImplicitParam(name = "filters", dataType = "String", value = "Group Statistics Field，Group Statistics Field",dataTypeClass = String.class),
            @ApiImplicitParam(name = "startDate", dataType = "String", value = "start time",dataTypeClass = String.class),
            @ApiImplicitParam(name = "endDate", dataType = "String", value = "End time",dataTypeClass = String.class),
            @ApiImplicitParam(name = "page", dataType = "Integer",dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer",dataTypeClass = Integer.class)})
    public Result datasetSearch(@RequestParam(name = "param") String param,
                             @RequestParam(name = "filters") String filters,
                             @RequestParam(name = "startDate") String startDate,
                             @RequestParam(name = "endDate") String endDate,
                             @RequestParam(name = "page") Integer page,
                             @RequestParam(name = "pageSize") Integer pageSize){

        if (pageSize > 100) {
            return error("PAGING_TIPS");
        }

        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate) && !DateUtils.belongCalendar(startDate, endDate)) {
            return error("TIME_VERIFICATION");
        }

        EsDataPage esDataPage = instdbApiService.datasetSearch(param,
                filters,
                startDate,
                endDate,
                page,
                pageSize);
        return success(Constant.StatusMsg.SUCCESS, esDataPage);
    }

    /**
     *
     * Single data resource query service
     * Provide access to single data resources shared by the central service platform at the station level for the central center and other platform systems。
     * @return
     */
    @ApiOperation("according to id according to")
    @RequestMapping("/dataset/details")
    public Map getInfo(String id, HttpServletRequest request){
        String version = request.getHeader("version");
        return instdbApiService.getDetails(id,version);
    }


    /**
     * Single data resource query service
     * Provide access to single data resources shared by the central service platform at the station level for the central center and other platform systems。
     *
     * @return
     */
    @ApiOperation("according to id according to--according to")
    @RequestMapping("/dataset/detailsV1")
    public Map getDetailsV1(String id) {
        return instdbApiService.getDetailsOld(id);
    }


    /**
     *
     * according to id according to
     * Statistical Information Query Service（Statistical Information Query Service、Statistical Information Query Service、Statistical Information Query Service）
     * @return
     */
    @ApiOperation("according to id according to")
    @RequestMapping("/dataset/info")
    public Map<String, Object> getDatasetInfo(String id){
        return  instdbApiService.getDatasetInfo(id);
    }


    /**
     * Obtain dataset update time
     * Obtain the update time of the published dataset。
     *
     * @return
     */
    @ApiOperation("Obtain dataset update time")
    @GetMapping("/dataset/status")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", dataType = "Integer", value = "Page number，Page number0Page number，Page number：0",dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "pageSize", dataType = "Integer", value = "Entry per page，Entry per page50，Entry per page：10",dataTypeClass = Integer.class)})
    public Result getDataFile(@RequestParam(name = "page") Integer page,
                              @RequestParam(name = "pageSize") Integer pageSize) {
        PageHelper datasetStatus = instdbApiService.getDatasetStatus(page, pageSize);
        return success(Constant.StatusMsg.SUCCESS,datasetStatus);
    }



}




