package cn.cnic.instdb.service;

import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.result.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface WOPIService {
    Result getOfficeInfo(HttpServletRequest request, HttpServletResponse response, String resourcesId, String fileId);

    Map<String, Object> getOfficeInfo(ResourceFileTree target, String spaceId, String token);

    Map<String, Object> getFileInfo(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl);

    void getContent(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl);
}
