package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.WOPIService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.io.IOUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WOPIServiceImpl implements WOPIService {

    private static final String VIEW_PARAM = "&ui=zh-cn&embed=true&dchat=1";
    public static final Map<String, String> ACTION_URLS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("doc", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
        put("docx", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
        put("pdf", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
        //put("txt", "/hosting/wopi/word/view?wopisrc=%s" + VIEW_PARAM);
        put("xls", "/hosting/wopi/cell/view?wopisrc=%s" + VIEW_PARAM);
        put("xlsx", "/hosting/wopi/cell/view?wopisrc=%s" + VIEW_PARAM);
        put("csv", "/hosting/wopi/cell/view?wopisrc=%s" + VIEW_PARAM);
        put("ppt", "/hosting/wopi/slide/view?wopisrc=%s" + VIEW_PARAM);
        put("pptx", "/hosting/wopi/slide/view?wopisrc=%s" + VIEW_PARAM);
    }});


    private static final Map<String, String> FILE_VERSION = new HashMap<>();
    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public Result getOfficeInfo(HttpServletRequest request, HttpServletResponse response, String resourcesId, String fileId) {


        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        query.addCriteria(Criteria.where("_id").in(fileId));
        ResourceFileTree one = mongoTemplate.findOne(query, ResourceFileTree.class);
        if (null == one) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if (!one.getIsFile()) {
            return ResultUtils.error("FOLDER_NO_PREVIEWED");
        }

        File file = new File(one.getFilePath());
        if (!file.exists()) {
            return ResultUtils.error("FILE_DOESNOT_EXIST");
        }

        //Obtain the type of file
        String fileSuffix = FileUtils.getSuffixByFileName(one.getFileName());


        String actionU = ACTION_URLS.get(fileSuffix);
        if (actionU == null) {
           return ResultUtils.error("FORMAT_NOT_PREVIEW");
        }

        Map<String, Object> ret = new HashMap<>();

        String resourcesAddress = instdbUrl.getResourcesFilePath();
        String url = one.getFilePath().replaceAll(resourcesAddress, "");

        String downloadFileUrl =  instdbUrl.getCallHost()+"api/dwn/"+url;
        System.out.println(downloadFileUrl);
        ret.put("fileId", one.getId());
        ret.put("actionUrl", String.format(actionU, downloadFileUrl));

        return ResultUtils.success(ret);
    }

    @Override
    public Map<String, Object> getOfficeInfo(ResourceFileTree target, String resourcesId, String token) {
        Map<String, Object> ret = new HashMap<>();
        //Obtain the type of file
        String fileSuffix = FileUtils.getSuffixByFileName(target.getFileName());
        String actionU = ACTION_URLS.get(fileSuffix);
        if (actionU == null) {
            log.error(fileSuffix + "FORMAT_NOT_PREVIEW");
            return ret;
        }
        String downloadFileUrl = String.format("%s/wopi/files/%s-%s/", instdbUrl.getCallHost() + "/api", resourcesId, target.getId());
        ret.put("actionUrl", String.format(actionU, downloadFileUrl));
        return ret;
    }

    @Override
    public Map<String, Object> getFileInfo(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl) {
        if (!fileid.contains("-")) {
            return (Map<String, Object>) new RuntimeException("PARAMETER_ERROR");
        }
        String resourcesId = fileid.split("-")[0];
        String fileId = fileid.split("-")[1];

        Token usersToken = getToken(access_token);

        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        query.addCriteria(Criteria.where("_id").in(fileId));
        ResourceFileTree one = mongoTemplate.findOne(query, ResourceFileTree.class);
        if (null == one) {
            return (Map<String, Object>) new RuntimeException("DATA_QUERY_EXCEPTION");
        }
        if (!one.getIsFile()) {
            return (Map<String, Object>) new RuntimeException("FOLDER_NO_PREVIEWED");
        }

        File file = new File(one.getFilePath());
        if (!file.exists()) {
            return (Map<String, Object>) new RuntimeException("FILE_DOESNOT_EXIST");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("BaseFileName", one.getFileName());
      //  info.put("OwnerId", fileMapping.getAuthor().getPersonId());
        info.put("UserFriendlyName", usersToken.getName());

        info.put("Size", "" + file.length());
        info.put("UserId", usersToken.getUserId());
        String version = CommonUtils.generateUUID();
        FILE_VERSION.put(fileId, version);
         info.put("Version", version);

        //info.put("CopyPasteRestrictions", "BlockAll");
        info.put("DisablePrint", true);
        info.put("EditModePostMessage", false);
        info.put("EditNotificationPostMessage", false);
        info.put("ClosePostMessage", false);
        info.put("ReadOnly", true);

        info.put("SupportsLocks", false);
        info.put("SupportsUpdate", false);
        info.put("SupportsRename", false);

        info.put("HidePrintOption", true);

        return info;

    }

    @Override
    public void getContent(String fileid, HttpServletRequest request, HttpServletResponse response, String access_token, String access_token_ttl) {
        if (!fileid.contains("-")) {
            return;
        }
        String resourcesId = fileid.split("-")[0];
        String fileId = fileid.split("-")[1];

        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
        query.addCriteria(Criteria.where("_id").is(fileId));
        ResourceFileTree one = mongoTemplate.findOne(query, ResourceFileTree.class);
        if (null == one) {
            return;
        }
        if (!one.getIsFile()) {
            return;
        }

        File file = new File(one.getFilePath());
        if (!file.exists()) {
            return;
        }

        //localPath = localPath + fileName;
        File localFile = new File(one.getFilePath());
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        response.addHeader("X-WOPI-ItemVersion", FILE_VERSION.get(fileId));
        response.addHeader("Content-Disposition", "attachment;filename=" + new String(localFile.getName().getBytes(StandardCharsets.UTF_8)));
        response.setContentType("application/octet-stream");
        try {
          IOUtils.copyBytes(new FileInputStream(localFile), response.getOutputStream(), 2048, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Token getToken(String token) {
        if (null == token) {
          //  throw new RuntimeException(401, "Please log in");
        }
        if (!jwtTokenUtils.validateToken(token)) {
         //   throw new CommonException(401, "Please log in");
        } else {
            String ifPresent = tokenCache.getIfPresent(token);
            if (ifPresent == null) {
           //     throw new CommonException(401, "Please log in");
            }
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        return usersToken;
    }
}
