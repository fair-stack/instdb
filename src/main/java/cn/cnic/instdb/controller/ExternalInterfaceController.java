package cn.cnic.instdb.controller;


import cn.cnic.instdb.model.center.Org;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.ExternalInterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
* @Auther  wdd
* @Date  2021/6/7 10:29
* @Desc  External interface
*/
@RestController
@Api(tags = "External interface call")
@RequestMapping("access")
public class ExternalInterfaceController extends ResultUtils {

    @Autowired
    private ExternalInterService externalInterService;


    @ApiOperation("release-releasedataciteDoi")
    @GetMapping("/apply.dataciteDoi")
    public Result dataciteDoi(String resourcesId){
        return success(externalInterService.dataciteDoi(resourcesId));
    }

    @ApiOperation("release-releaseChinaDOI")
    @GetMapping("/apply.chinadoi")
    public Result chinadoiDoi(String resourcesId){
        return success(externalInterService.registerChinaDOI(resourcesId));
    }

    @ApiOperation("release-releaseDOI")
    @GetMapping("/apply.DOI")
    public Result registerDOI(String resourcesId){
        return success(externalInterService.registerDOI(resourcesId));
    }

    @ApiOperation("release-releaseCSTR")
    @PostMapping("/apply.CSTR")
    public Result applyCSTR(String resourcesId,String doi){
        return success(externalInterService.applyCSTR(resourcesId,doi));
    }

    @ApiOperation("release-releaseCSTRrelease")
    @GetMapping("/check.CSTR")
    public Result checkCstr(@RequestParam(name = "cstrCode")String cstrCode){
        return externalInterService.checkCstr(cstrCode);
    }

    @ApiOperation("release-releasedoirelease")
    @GetMapping("/check.doi")
    public Result checkDoi(@RequestParam(name = "doiCode")String doiCode){
        return externalInterService.checkDoi(doiCode);
    }


    @ApiOperation("According to the templateidAccording to the template")
    @GetMapping("/get.template")
    public Result findTemplateById(@RequestParam(name = "id")String id){
        return externalInterService.findTemplateById(id);
    }


    @ApiOperation(value = "release-releaseidrelease release", notes = "Person、Organization、Project、Paper", response = Result.class)
    @GetMapping("/get.data")
    public Result accessProjectList(@RequestParam(name = "id", required = false) String id,
                                    @RequestParam(name = "type", required = true) String type,
                                    @RequestParam(name = "keyword", required = false) String keyword) {
        return externalInterService.accessDataInfo(id, type, keyword);
    }


    @ApiOperation("release-release")
    @GetMapping("/orgList")
    public Result accessOrgList(String id){
        return externalInterService.accessOrgList(id);
    }

    @ApiOperation("Institution addition")
    @PostMapping("/orgAdd")
    public Result orgAdd(@RequestBody Org org){
        return externalInterService.orgAdd(org);
    }


}
