package cn.cnic.instdb.controller;

import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.service.WOPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping
@Slf4j
public class WOPIController {

    @Resource
    private WOPIService wopiService;
//
//    @PostMapping("/getOfficeInfo")
//    @ResponseBody
//    public Result getOfficeInfo(HttpServletRequest request,
//                                HttpServletResponse response,
//                                String resourcesId,
//                                String fileId) {
//        return wopiService.getOfficeInfo(request, response, resourcesId, fileId);
//
//    }

    @ResponseBody
    @GetMapping("/wopi/files/{fileid}")
    public Map<String, Object> getFileInfo(@PathVariable("fileid") String fileid,
                                           HttpServletRequest request,
                                           HttpServletResponse response,
                                           String access_token,
                                           String access_token_ttl) {

        return wopiService.getFileInfo(fileid, request, response, access_token, access_token_ttl);

    }

    @GetMapping("/wopi/files/{fileid}/contents")
    public void getContent(@PathVariable("fileid") String fileid,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           String access_token,
                           String access_token_ttl) throws IOException {

        wopiService.getContent(fileid, request, response, access_token, access_token_ttl);
    }

}
