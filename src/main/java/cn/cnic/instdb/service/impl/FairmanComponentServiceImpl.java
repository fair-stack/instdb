package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.model.system.Component;
import cn.cnic.instdb.model.system.ComponentShow;
import cn.cnic.instdb.model.system.ComponentUpdate;
import cn.cnic.instdb.model.system.ValuesResult;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.FairmanComponentService;
import cn.cnic.instdb.service.WOPIService;
import cn.cnic.instdb.utils.*;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.springframework.data.domain.Sort.Direction.DESC;


@Service
@Slf4j
public class FairmanComponentServiceImpl implements FairmanComponentService {


    @Autowired
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private WOPIService wopiService;

    @Override
    public Result installList(String token, Integer page, Integer size, String category, String name) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotEmpty(category) && StringUtils.isNotEmpty(category.trim())) {
            criteria.and("category").is(category.trim());
        }
        if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(name.trim())) {
            criteria.and("name").regex(name);
        }

        long count = mongoTemplate.count(new Query().addCriteria(criteria), Component.class);
        List<ComponentShow> componentList = null;
        if (count > 0) {
            Query query = new Query().addCriteria(criteria);
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(DESC, "installTime"));
            componentList = mongoTemplate.find(query, ComponentShow.class, "component");

            //returnparameters returnmap
            for (ComponentShow componentShow : componentList) {
                List<Map<String, Object>> parameters = componentShow.getParameters();
                Map<String, Object> parametersMap = new HashMap<>();
                if (parameters != null) {
                    for (Map<String, Object> parameter : parameters) {
                        Object key = parameter.get("key");
                        if (key != null) {
                            parametersMap.put(key.toString(), parameter.get("value"));
                        }
                    }
                }
                componentShow.setParameterMap(parametersMap);
                componentShow.setParameters(null);
            }
        }
        HashMap<Object, Object> result = new HashMap<>(2);
        result.put("total", count);
        result.put("data", componentList);
        return ResultUtils.success(result);
    }

    @Override
    public Result component(String token, Integer page, Integer size, Integer sort, String category, String name) {

        HttpClient httpClient = new HttpClient();

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("page", String.valueOf(page)));
        params.add(new BasicNameValuePair("pageSize", String.valueOf(size)));
        params.add(new BasicNameValuePair("sort", String.valueOf(sort)));

        if (StringUtils.isNotEmpty(category)) {
            params.add(new BasicNameValuePair("category", category));
        }

        if (StringUtils.isNotEmpty(name)) {
            params.add(new BasicNameValuePair("name", name));
        }

        params.add(new BasicNameValuePair("software", "InstDB"));

        Map<String, String> header = new HashMap<>(1);
        header.put("accessKey", "8c6daabc-7253-11ed-96b5-305a3ac88b53");

        String fairManMarketUrl = instdbUrl.getFairmanMarketUrl();
        String result = "";

        try {
            result = httpClient.doGetWayTwo(params, fairManMarketUrl, header);
        } catch (Exception e) {
            return ResultUtils.error("GET_COMPONENT_ERROR");
        }

        Map map = JSONObject.parseObject(result, Map.class);
        if (!map.containsKey("code") || ((int) map.get("code")) != 0) {
            return ResultUtils.error("COMPONENT_DATA_CANNOT_PARSED");
        }
        Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
        List<Map<String, Object>> list = (List<Map<String, Object>>) dataMap.get("list");
        for (Map<String, Object> m : list) {
            String downloadId = m.get("downloadId").toString();
            String bundle = m.get("bundle").toString();
            //Verify if it has been installed
            boolean exists = mongoTemplate.exists(new Query().addCriteria(Criteria.where("componentId").is(downloadId).and("bundle").is(bundle)), Component.class);
            m.put("isInstall", exists);

            //assemblyparametersassemblyListassemblyMap
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) m.get("parameters");

            Map<String, Object> parametersMap = new HashMap<>();
            if (parameters != null) {
                parameters.stream().forEach(var -> {
                    Object key = var.get("key");
                    if (key != null) {
                        parametersMap.put(key.toString(), var.get("value"));
                    }
                });
            }
            m.put("parameters", parametersMap);
        }

        dataMap.put("list", list);
        return ResultUtils.success(dataMap);
    }

    @Override
    public Result aggData(String token) {
        HttpClient httpClient = new HttpClient();

        Map<String, String> header = new HashMap<>(1);
        header.put("accessKey", "8c6daabc-7253-11ed-96b5-305a3ac88b53");

        String fairManMarketUrl = instdbUrl.getFairmanMarketUrl() + "/source?softwareName=InstDB";
      //  String fairManMarketUrl = instdbUrl.getFairmanMarketUrl() + "/source";
        String result = "";

        try {
            result = httpClient.doGetWayTwo(fairManMarketUrl, header);
        } catch (Exception e) {
            return ResultUtils.error("GET_COMPONENT_ERROR");
        }

        Map map = JSONObject.parseObject(result, Map.class);
        if (!map.containsKey("code") || ((int) map.get("code")) != 0) {
            return ResultUtils.error("COMPONENT_DATA_CANNOT_PARSED");
        }
        Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
        return ResultUtils.success(dataMap);
    }

    @Override
    public Result componentInstall(String token, Component component) {
        //"https://market.casdc.cn/api/v2/component/621f4987583197d50685102b/ExcelRead.jar?bundle=cn.cnic.bundle.Excel.ExcelRead";

        List<String> validation = CommonUtils.validation(component);
        if (!validation.isEmpty()) {
            return ResultUtils.error("Parameter error: {} " + validation.toString());
        }


        String bucket = component.getComponentId();

        String bundle = component.getBundle();

        //Verify if it has been installed
        boolean exists = mongoTemplate.exists(new Query().addCriteria(Criteria.where("bundle").is(bundle)), Component.class);
        if (exists) {
            return ResultUtils.error("COMPONENT_INSTALLED");
        }

//        if(!key.contains(".zip")){
//            return ResultUtil.error("Unrecognized component type，Unrecognized component type!");
//        }

        String fairManMarketUrl = instdbUrl.getFairmanMarketUrl() + "/download";
        fairManMarketUrl = fairManMarketUrl + "?id=" + bucket + "&bundle=" + bundle;

        String installComSource = instdbUrl.getInstallComSource();
        File sourceFile = new File(installComSource, bucket);
        if (!sourceFile.exists()) {
            sourceFile.mkdirs();
        }

        boolean res = installDownloadFile(fairManMarketUrl, sourceFile.getPath());
        if (!res) {
            return ResultUtils.error("INSTALLATION_FAILED");
        }


        File[] files = sourceFile.listFiles();

        if (files.length == 0) {
            return ResultUtils.error("INSTALLATION_FAILED");
        }

        //File decompression
        try {
            FileUtils.unZip(files[0], sourceFile.getPath());
        } catch (Exception e) {
            return ResultUtils.error("COMPONENT_DECOMPRESSION_FAILED");
        }

        String fileName = "";
        for (File file2 : sourceFile.listFiles()) {
            if (file2.isDirectory()) {
                fileName = file2.getName();
                break;
            }
        }

        //Installation Data Record
        List<Map<String, Object>> parameters = component.getParameters();
        List<String> fileTypes = new ArrayList<>();
        if (null != parameters) {
            for (Map<String, Object> parameter : parameters) {
                if (parameter.containsValue("suffix")) {
                    String value = parameter.get("value").toString();
                    String[] split = value.contains(",") ? value.split(",") : value.split("，");
                    fileTypes.addAll(Arrays.asList(split));
                }
            }
        }
        component.setFileTypes(fileTypes);
        component.setSourcePath(sourceFile.getPath());
        component.setWebPath(instdbUrl.getInstallComWeb() + "/" + bucket + "/" + fileName);
        component.setInstallTime(new Date());
        mongoTemplate.insert(component);
        return ResultUtils.success();
    }

    @Override
    public Result componentEdit(String token, ComponentUpdate component) {
        Update update = new Update();
        update.set("parameters", component.getParameters());
        mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(component.getId())), update, Component.class);
        return ResultUtils.success();
    }

    @Override
    public Result componentRemove(String token, String id) {
        Component component = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), Component.class);
        if (null == component) {
            return ResultUtils.error("DELETE_FAILED");
        }

        String sourcePath = component.getSourcePath();
        //remove file 
        try {
            File file = new File(sourcePath);
            if (file.isDirectory()) {
                Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } else {
                file.delete();
            }
        } catch (IOException io) {
            log.error(io.getMessage());
            return ResultUtils.error("DELETE_FAILED");
        }
        mongoTemplate.remove(component);
        return ResultUtils.success();
    }

    @Override
    public Result getComponent(String token, String resourcesId, String fileId) {

        if (StringUtils.isEmpty(resourcesId) || StringUtils.isEmpty(fileId)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }

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

        //Obtain the type of file
        String fileSuffix = FileUtils.getSuffixByFileName(one.getFileName());

        Query query1 = new Query().addCriteria(Criteria.where("fileTypes").is(fileSuffix));
        List<ComponentShow> components = mongoTemplate.find(query1, ComponentShow.class, "component");

        Map<String, Object> ret = new HashMap<>();
        ret.put("components", components);
        ret.put("status", 0);

        if (!CollectionUtils.isEmpty(components)) {//Components already installed locally
            return ResultUtils.success(ret);
        }

        //Check if there are any components that can be used
        Result<Object> componentResult = component(null, 1, 10, 0, null, null);
        List<Map<String, Object>> matchComponents = new ArrayList<>();
        if (componentResult.getCode() == 200) { //Successfully obtained component remotely
            Map<String, Object> data = (Map<String, Object>) componentResult.getData();
            Object list = data.get("list");
            if (list != null) {
                List<Map<String, Object>> list2 = (List<Map<String, Object>>) list;
                for (Map<String, Object> comRemote : list2) {
                    if ("front-end".equals(comRemote.get("componentType").toString())) {//Front end components
                        if ((Boolean)comRemote.get("isInstall") == true) {//Already installed
                            continue;
                        }
                        Object parameters = comRemote.get("parameters");
                        if (parameters != null) {
                            boolean isMatch = false;
                            Object suffix = ((Map<String, Object>) parameters).get("suffix");
                            if (suffix != null) {
                                String[] suffixs = suffix.toString().contains(",") ? suffix.toString().split(",") : suffix.toString().split("，");
                                for (String s : suffixs) {
                                    if (fileSuffix.equals(s)) {
                                        isMatch = true;
                                        break;
                                    }
                                }
                                if (isMatch) {
                                    matchComponents.add(comRemote);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!CollectionUtils.isEmpty(matchComponents)) {//Components not installed locally Components not installed locally
            ret.put("status", 1);
            int maxDownloadCount = 0;
            Map<String, Object> retMatchComponent = new HashMap<>();
            //Find the one with the highest number of installations
            for (Map<String, Object> matchComponent : matchComponents) {
                int currentDownloadCount = (Integer) matchComponent.get("downloadCount");
                if (currentDownloadCount >= maxDownloadCount) {
                    maxDownloadCount = currentDownloadCount;
                    retMatchComponent = matchComponent;
                }
            }
            ret.put("matchComponent", retMatchComponent);
        } else {//Components not installed locally Components not installed locally
            ret.put("status", 2);
        }

        return ResultUtils.success(ret);
    }

    @Override
    public Result previewData(String token, String resourcesId, String fileId, String componentId, HttpServletRequest request) {

        if (StringUtils.isEmpty(resourcesId) || StringUtils.isEmpty(fileId)) {
            return ResultUtils.error("PARAMETER_ERROR");
        }


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
        Query query1 = new Query().addCriteria(Criteria.where("_id").is(componentId));
        Component component = mongoTemplate.findOne(query1, Component.class);

        List<String> fileTypes = component.getFileTypes();
        if (!fileTypes.contains(fileSuffix) && !fileTypes.contains(fileSuffix.toUpperCase())) {
            return ResultUtils.error("ASSEMBLY_NO_PREVIEWED");
        }

        List<Map<String, Object>> parameters = component.getParameters();
        String type = "";
        String data = "";
        ArrayList<Map<String, Object>> custom = null;
        for (Map<String, Object> parameter : parameters) {
            String key = parameter.get("key").toString();
            if (key.contains("type")) {
                type = parameter.get("value").toString();
            }
            if (key.equals("data")) {
                data = parameter.get("value").toString();
            }
            if (key.equals("custom")) {
                custom = (ArrayList<Map<String, Object>>) parameter.get("value");
            }
        }
        if (StringUtils.isEmpty(type)) {
            return ResultUtils.success();
        }

        Map resultMap = JSONObject.parseObject(data, Map.class);



        if (type.equals("fileUrl")) {

            if (StringUtils.isNotBlank(one.getFilePath())) {
                String resourcesAddress = instdbUrl.getResourcesFilePath();
                String url = one.getFilePath().replaceAll(resourcesAddress, "");
                String s = instdbUrl.getCallHost() + "/api/dwn/" + url;
                List<String> list = new ArrayList<>(1);
                list.add(s);
                resultMap.put("fileUrl", list);
            }

        } else if (type.equals("fileStr")) {
            String content = FileUtils.readJsonFile(file.getPath());
            resultMap.put("content", content);
        } else if ("onlyoffice".equals(type)) {

            Map<String, Object> officeInfo = wopiService.getOfficeInfo(one, resourcesId, token);
            String actionUrl = officeInfo.get("actionUrl").toString();
            if (custom != null) {
                for (int i = 0; i < custom.size(); i++) {
                    if ("baseUrl".equals(custom.get(i).get("key"))) {
                        String baseUrl = custom.get(i).get("value").toString();
                        //Simple judgment on the server sideofficeSimple judgment on the server side
                        HttpClient httpClient = new HttpClient();
                        String respCode = httpClient.doGetWayTwo(baseUrl,new HashMap<>());
                        if (StringUtils.isBlank(respCode)) {
                            return ResultUtils.error("CHECK_DOCUMENT_SERVER_ADDRESS");
                        }
                        actionUrl = baseUrl + actionUrl;
                    }
                }
            }
            resultMap.put("actionUrl", actionUrl);
            resultMap.put("access_token", token);
        } else {
            return ResultUtils.success();
        }
        return ResultUtils.success(resultMap);
    }





    private List<Map<String,Object>>  getStatistics(AggregationResults<ValuesResult> valuesResults,int num ){
        //format date
        List<Date> lDate = DateUtils.getTimeInterval(new Date(), num);//Get all this weekdate
        List<String> list = new ArrayList<>();
        for (Date date : lDate) {
            list.add(DateUtils.getDateString(date));
        }
        String[] strs = list.toArray(new String[list.size()]);
        List<Map<String, Object>> objMapList = new ArrayList();
        for (String t : strs) {
            Map objMap = new HashMap();
            objMap.put("date", t);
            if (null != valuesResults.getMappedResults() && valuesResults.getMappedResults().size() > 0) {
                for (ValuesResult valuesResult : valuesResults) {
                    if (valuesResult.getName().equals(t)) {
                        objMap.put("data", valuesResult.getValue());
                    }
                }
            }
            if (!objMap.containsKey("data")) {
                objMap.put("data", 0);
            }
            objMapList.add(objMap);
        }
        return objMapList;
    }

    private boolean installDownloadFile(String remoteFileUrl, String localPath) {
        CloseableHttpResponse response = null;
        InputStream input = null;
        FileOutputStream output = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(remoteFileUrl);
            httpget.setHeader("accessKey", "8c6daabc-7253-11ed-96b5-305a3ac88b53");
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            if (entity == null) {
                return false;
            }

            String fileName = "";
            Header[] headers = response.getHeaders("Content-Disposition");
            for (Header header : headers) {
                String value = header.getValue();
                if (value.contains("filename")) {
                    fileName = value.substring(value.indexOf("=") + 1);
                }
            }

            if (StringUtils.isEmpty(fileName)) {
                return false;
            }

            File localFile = new File(localPath, fileName);

            input = entity.getContent();
            output = new FileOutputStream(localFile);
            byte datas[] = new byte[1024];//Create handling tools
            int len = 0;
            while ((len = input.read(datas)) != -1) { //Loop reading data
                output.write(datas, 0, len);
            }
            output.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            // Close low-level flow。
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Exception e) {
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                }
            }
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
