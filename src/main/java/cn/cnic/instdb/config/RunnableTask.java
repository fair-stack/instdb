package cn.cnic.instdb.config;


import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.resources.ResourcesManage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

/**
 * @Auther: wdd
 * @Date: 2021/04/18/0:53
 * @Description:
 */

@Slf4j
public class RunnableTask implements Runnable{

    private String resourcesOnlyId;
    private String version;
    private String downloadFileUrl;
    private ExecutorService service;
    private String basePath;
    private MongoTemplate mongoTemplate;


    public RunnableTask(MongoTemplate mongoTemplate,ExecutorService service, String resourcesOnlyId, String version, String downloadFileUrl, String basePath){
        this.resourcesOnlyId = resourcesOnlyId;
        this.version = version;
        this.downloadFileUrl = downloadFileUrl+"?resourcesId="+resourcesOnlyId+"&version="+version;
        this.service = service;
        this.basePath = basePath+resourcesOnlyId+"_"+version;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run() {
            try {
                URL url = null;
                try {
                    url = new URL(downloadFileUrl);
                } catch (MalformedURLException e) {
                    log.error("context",e);
                    throw new RuntimeException("resource fileurlresource file", e);
                }
                log.info("=========================Start connecting to remoteurl====================================");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                urlConnection.setConnectTimeout(10000);
                log.info("=========================Connect to remoteurlConnect to remote====================================");
                InputStream inputStream = urlConnection.getInputStream();


                //filePathFile address，fileNameFile address
                File file = new File(basePath, resourcesOnlyId+"_"+version + ".zip");
                log.info("=========================Start downloading files====================================");
                log.info("Original file address" + downloadFileUrl);
                log.info("Download to file address" + basePath);
                FileUtils.copyInputStreamToFile(inputStream, file);

                if (file.exists()) {
                    log.info("=========================File download completed====================================");
                    log.info("=========================Start extracting files" + file.getName() + "====================================");
                    cn.cnic.instdb.utils.FileUtils.unZip(file, basePath);
                    log.info("=========================File decompression completed====================================");
                    //Update resource download file status
                    Query query = new Query();
                    Criteria criteria = Criteria.where("resourcesId").is(resourcesOnlyId);
                    criteria.and("version").is(version);
                    query.addCriteria(criteria);
                    Update update = new Update();
                    update.set("downloadFileFlag", Constant.VERSION_FLAG);
                    mongoTemplate.updateFirst(query, update, ResourcesManage.class);
                    log.info("=========================The download status of the resource file has been updated===================================");
                }

            } catch (Exception e) {
                log.error("context",e);
                log.info("Resource file download failed:{}", e.getMessage());
                throw new RuntimeException("Resource file download failed", e);
            }
    }
}
