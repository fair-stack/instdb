package cn.cnic.instdb.async;

import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.utils.FileCounter;
import cn.cnic.instdb.utils.FileTree;
import cn.cnic.instdb.utils.InstdbUrl;
import cn.cnic.instdb.utils.NoModelDataListener;
import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

@Slf4j
@Component
public class AsyncResource {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private InstdbUrl instdbUrl;

    /**
     * Structured processing
     *
     * @param dataType
     * @param resourceId
     */
    @Async
    public void structuredHandle(Query query, Map<String, Object> dataType, String resourceId) {
        List<String> fileLists = (List<String>) dataType.get("fileList");
        Update update = new Update();
        update.set("structuredCount", fileLists.size());
        long fileSize = 0;

        for (String f : fileLists) {
         //   log.info("============================Structured file processing" + f + "=================================");
            String filepath = instdbUrl.getResourcesFilePath() + resourceId + "/" + f + ".xlsx";
            log.info("Path is：" + filepath);
            File structured = new File(filepath);
            if (structured.exists()) {
                fileSize += structured.length();
                String name = structured.getName().substring(0, structured.getName().lastIndexOf("."));
                EasyExcel.read(filepath, new NoModelDataListener(filepath, name, resourceId, mongoTemplate)).sheet().doRead();
            }
            log.info("============================Structured file processing completed=================================");
        }
        //Unstructured situation Unstructured situation
        if ("1".equals(dataType.get("type").toString())) {
            update.set("storageNum", fileSize);
        }
        mongoTemplate.updateFirst(query, update, ResourcesManage.class);
    }

    /**
     * File processing  File processing
     *
     * @param filterFile
     * @param resourceId
     * @param query
     */
    @Async
    public void dataFileHandle(List<String> filterFile, String resourceId, Query query, String fileIsZip) {
        Update update = new Update();
        FileCounter counter = new FileCounter();
        if ("yes".equals(fileIsZip)) {
            filterFile.add(resourceId + ".zip");
        }
        Map fileInfo = counter.getFileInfo(instdbUrl.getResourcesFilePath() + resourceId, filterFile);
        update.set("storageNum", fileInfo.get("storageNum"));
        update.set("fileCount", fileInfo.get("fileCount"));

        if (null != fileInfo.get("fileFormat")) {
            Map fileFormat = (Map) fileInfo.get("fileFormat");
            if (null != fileFormat && fileFormat.size() > 0) {
                Set<String> set = fileFormat.keySet();
                List<Map> listMap = new ArrayList<>();
                for (String s : set) {
                    Map map = new HashMap();
                    map.put("name", s);
                    map.put("count", fileFormat.get(s));
                    listMap.add(map);
                }
                update.set("fileFormatNew", listMap);
            }
        }


        //fileFormatNewsituation   situation 2023situation6situation28situation15:12:28 wddsituation
        //PreviousfileFormatPreviouskeyPrevious，Previous$  Previous，PreviousfileFormatNewPrevious
        if (null != fileInfo.get("suffixStorageNum")) {
            Map suffixStorageNum = (Map) fileInfo.get("suffixStorageNum");
            if (null != suffixStorageNum && suffixStorageNum.size() > 0) {
                Set<String> set = suffixStorageNum.keySet();
                List<Map> listMap = new ArrayList<>();
                for (String s : set) {
                    Map map = new HashMap();
                    map.put("name", s);
                    map.put("storageNum", suffixStorageNum.get(s));
                    listMap.add(map);
                }
                update.set("suffixStorageNumNew", listMap);
            }
        }
        mongoTemplate.updateFirst(query, update, ResourcesManage.class);
      //  log.info("=========================Calculation of file size and number of files completed====================================");
        //Parsing File Content  Parsing File Content
        FileTree tree = new FileTree();
        List<ResourceFileTree> fileTree = tree.getFileTree(resourceId, instdbUrl.getResourcesFilePath() + resourceId, filterFile);
        if (null != fileTree && fileTree.size() > 0) {
            mongoTemplate.insertAll(fileTree);
        }
        log.info("============================Physical file processing completed=================================");
    }



}
