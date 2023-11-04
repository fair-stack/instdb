package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.elasticsearch.EsServiceParams;
import cn.cnic.instdb.model.resources.ResourcesIndexQuery;
import cn.cnic.instdb.model.resources.ResourcesListManage;
import cn.cnic.instdb.model.system.SearchConfig;
import cn.cnic.instdb.result.EsDataPage;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.IndexService;
import cn.cnic.instdb.utils.CommonUtils;
import cn.cnic.instdb.utils.InstdbUrl;
import cn.cnic.instdb.utils.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/04/07/10:01
 * @Description:
 */

@RestController
@Slf4j
@RequestMapping(value = "/index")
@Api(value = "home page", tags = "home page")
public class IndexController extends ResultUtils {

    @Resource
    private IndexService indexService;

    @Resource
    private InstdbUrl instdbUrl;

    @ApiOperation("Homepage Resource Search")
    @RequestMapping(value = "/getIndexAllResource", method = RequestMethod.POST)
    public Result getIndexAllResource(@RequestBody ResourcesIndexQuery resourcesIndexQuery) {
        PageHelper resourceList = indexService.getIndexAllResource(resourcesIndexQuery);
        return success(Constant.StatusMsg.SUCCESS, resourceList);
    }


    @ApiOperation("Homepage Resource Searches")
    @RequestMapping(value = "/getIndexAllResourceByES", method = RequestMethod.POST)
    public Result getIndexAllResourceByES(@RequestHeader("Authorization") String token,@RequestBody EsServiceParams EsServiceParams) {

        if (100 < EsServiceParams.getPageSize()) {
            return error("PAGING_TIPS");
        }

//        List<EsServiceParams.Aggregation> aggregations = new ArrayList<>();
//        EsServiceParams.Aggregation aggregation = new EsServiceParams.Aggregation();
//        aggregation.setSize(10);
//        aggregation.setName("Resource Type");
//        aggregation.setField("resourceType");
//        aggregation.setFieldType("resourceType");
//        aggregations.add(aggregation);
//        EsServiceParams.setAggregations(aggregations);

        EsDataPage indexAllResourceByES = indexService.getIndexAllResourceByES(token,EsServiceParams);
        return success(Constant.StatusMsg.SUCCESS,indexAllResourceByES);
    }


    @ApiOperation("Homepage resource search left aggregation")
    @RequestMapping(value = "/aggregation/resourceByES", method = RequestMethod.POST)
    public Result getaggregation(@RequestHeader("Authorization") String token,@RequestBody EsServiceParams EsServiceParams) {

        if (100 < EsServiceParams.getPageSize()) {
            return error("PAGING_TIPS");
        }

        //Aggregation parameter processing
        List<SearchConfig> indexSearchitems = indexService.getIndexSearchitems(Constant.STATISTICS);
        if (null != indexSearchitems && indexSearchitems.size() > 0) {
            List<EsServiceParams.Aggregation> aggregations = new ArrayList<>();
            for (SearchConfig item : indexSearchitems) {
                EsServiceParams.Aggregation aggregation = new EsServiceParams.Aggregation();
                aggregation.setSize(10);
                aggregation.setName(item.getName());
                aggregation.setField(item.getField());
                aggregation.setFieldType(item.getFieldType());
                aggregations.add(aggregation);
            }
            EsServiceParams.setAggregations(aggregations);
        }

        EsDataPage indexAllResourceByES = indexService.getIndexAllResourceByES(token,EsServiceParams);
        return success(Constant.StatusMsg.SUCCESS,indexAllResourceByES);
    }


    @ApiOperation("Latest resource list on homepage")
    @RequestMapping(value = "/getIndexNewResource", method = RequestMethod.GET)
    public Result getIndexNewResource() {
        List<ResourcesListManage> indexNewResource = indexService.getIndexNewResource();
        return success(Constant.StatusMsg.SUCCESS, indexNewResource);
    }


    @ApiOperation("Retrieve search items on the homepage")
    @RequestMapping(value = "/getIndexSearchitems", method = RequestMethod.GET)
    public Result getIndexSearchitems(String type) {
        return success(indexService.getIndexSearchitems(type));
    }


    @ApiOperation("Resource Retrieval History")
    @RequestMapping(value = "/getHistorySearch", method = RequestMethod.GET)
    public Result getHistorySearch() {
        return indexService.getHistorySearch();
    }



    @ApiOperation("Hot search terms on homepage")
    @RequestMapping(value = "/getIndexHotSearch", method = RequestMethod.GET)
    public Result getIndexHotSearch() {
        List<String> list = indexService.getIndexHotSearch();
        return success(Constant.StatusMsg.SUCCESS, list);
    }

    @ApiOperation("Obtain all data resource types")
    @RequestMapping(value = "/getIndexResourceType", method = RequestMethod.GET)
    public Result getIndexResourceType() {
        return success(Constant.StatusMsg.SUCCESS, indexService.getIndexResourceType());
    }


    @ApiOperation("Search data based on the type of data resource")
    @RequestMapping(value = "/getResourceByType", method = RequestMethod.GET)
    public Result getResourceByType(String resourceType, HttpServletRequest request) {
        CommonUtils.setLangToReq(request);
        return success(Constant.StatusMsg.SUCCESS, indexService.getResourceByType(resourceType));
    }

    @ApiOperation("Homepage boutique topic")
    @RequestMapping(value = "/getIndexBoutiqueSpecial", method = RequestMethod.GET)
    public Result getIndexBoutiqueSpecial() {
        return success(Constant.StatusMsg.SUCCESS, indexService.getIndexBoutiqueSpecial());
    }

    @ApiOperation("Data Resource Ranking")
    @RequestMapping(value = "/getIndexResourceRank", method = RequestMethod.GET)
    public Result getIndexResourceRank() {
        return success(Constant.StatusMsg.SUCCESS, indexService.getIndexResourceRank());
    }


    @ApiOperation("Five statistics on the homepage")
    @RequestMapping(value = "/getIndexStatisticsNum", method = RequestMethod.GET)
    public Result getIndexStatisticsNum() {
        Map<String, Object> indexStatisticsNum = indexService.getIndexStatisticsNum();
        return success(Constant.StatusMsg.SUCCESS, indexStatisticsNum);
    }


    @ApiOperation("Homepage Discipline Domain Classification")
    @RequestMapping(value = "/getIndexSubjectArea", method = RequestMethod.GET)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "num", dataType = "Integer", required = true, defaultValue = "6",dataTypeClass = Integer.class)})
    public Result getIndexSubjectArea(Integer num) {
        Map<String,Object> indexSubjectArea = indexService.getIndexSubjectArea(num);
        return success(Constant.StatusMsg.SUCCESS, indexSubjectArea);
    }




}

