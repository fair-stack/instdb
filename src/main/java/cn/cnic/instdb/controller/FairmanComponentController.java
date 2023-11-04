package cn.cnic.instdb.controller;

import cn.cnic.instdb.model.system.Component;
import cn.cnic.instdb.model.system.ComponentUpdate;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.FairmanComponentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@RestController
@Api(tags = "FairmanComponents and Preview")
@RequestMapping("/fairman")
public class FairmanComponentController extends ResultUtils {


    @Resource
    private FairmanComponentService fairmanComponentService;

    @ApiOperation("List of installed market components")
    @GetMapping("/ist.com")
    public Result installList(@RequestHeader("Authorization") String token,
                              @RequestParam(value = "page", defaultValue = "1") Integer page,
                              @RequestParam(value = "size", defaultValue = "10") Integer size,
                              @RequestParam(value = "category", required = false) String category,
                              @RequestParam(value = "name", required = false) String name) {
        return fairmanComponentService.installList(token, page, size, category, name);
    }

    @ApiOperation("Obtain a list of market components")
    @GetMapping("/component")
    public Result component(@RequestHeader("Authorization") String token,
                            @RequestParam(value = "page", defaultValue = "1") Integer page,
                            @RequestParam(value = "size", defaultValue = "10") Integer size,
                            @RequestParam(value = "sort", defaultValue = "0") Integer sort,
                            @RequestParam(value = "category", required = false) String category,
                            @RequestParam(value = "name", required = false) String name) {
        return fairmanComponentService.component(token, page, size, sort, category, name);
    }

    @ApiOperation("Aggregated statistical data on the left side of market components")
    @GetMapping("/aggData")
    public Result aggData(@RequestHeader("Authorization") String token) {
        return fairmanComponentService.aggData(token);
    }

    @ApiOperation("Install market components")
    @PostMapping("/component.install")
    public Result componentInstall(@RequestHeader("Authorization") String token,
                                   @RequestBody Component component) {
        return fairmanComponentService.componentInstall(token, component);
    }

    @ApiOperation("Edit installed market component configuration information")
    @PutMapping("/component.edit")
    public Result componentEdit(@RequestHeader("Authorization") String token,
                                @RequestBody ComponentUpdate component) {
        return fairmanComponentService.componentEdit(token, component);
    }

    @ApiOperation("Remove installed market components")
    @GetMapping("/component.rm")
    public Result componentRemove(@RequestHeader("Authorization") String token,
                                  @RequestParam(value = "id") String id) {
        return fairmanComponentService.componentRemove(token, id);
    }


    @ApiOperation("File preview-File preview")
    @GetMapping("/getComponent")
    public Result getComponent(@RequestHeader("Authorization") String token,
                               @RequestParam("resourcesId") String resourcesId,
                               @RequestParam("fileId") String fileId
    ) {
        return fairmanComponentService.getComponent(token, resourcesId, fileId);
    }


    @ApiOperation("File preview-File preview")
    @GetMapping("/previewData")
    public Result previewData(@RequestHeader("Authorization") String token,
                             @RequestParam("resourcesId") String resourcesId,
                             @RequestParam("fileId") String fileId,
                             @RequestParam("componentId") String componentId,
                             HttpServletRequest request) {
        return fairmanComponentService.previewData(token, resourcesId, fileId, componentId, request);
    }

}
