package cn.cnic.instdb.filehandle;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.ftp.minimalftp.FTPConnection;
import cn.cnic.instdb.ftp.minimalftp.Utils;
import cn.cnic.instdb.model.config.BasicConfigurationVo;
import cn.cnic.instdb.model.resources.ResourceAccess;
import cn.cnic.instdb.model.resources.ResourceFileTree;
import cn.cnic.instdb.model.resources.ResourcesManage;
import cn.cnic.instdb.model.system.AccessRecords;
import cn.cnic.instdb.service.SystemConfigService;
import cn.cnic.instdb.utils.DateUtils;
import cn.cnic.instdb.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FtpCustomizationImpl implements FtpCustomization<File>{

    private final FTPConnection con;

    private final File rootDir;

    @Autowired
    private MongoTemplate mongoTemplate;


    @Autowired
    private SystemConfigService systemConfigService;


    public FtpCustomizationImpl() {
        this.con = null;
        this.rootDir = null;
    }

    public FtpCustomizationImpl(FTPConnection con) {
        this.con = con;
        this.rootDir = this.getRoot();
    }

    public FtpCustomizationImpl(File rootDir, FTPConnection con) {
        this.con = con;
        this.rootDir = rootDir;
        if(!rootDir.exists()) rootDir.mkdirs();
    }

    /*  ftp */

    @Override
    public List<ResourceFileTree> listFiles(String resourcesId, String path,String realUsers) throws IOException {

        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
        if(StringUtils.isNotBlank(basicConfig.getFtpSwitch()) && Constant.Approval.NO.equals(basicConfig.getFtpSwitch())){
            log.error("ftpThe function is turned off");
            return new ArrayList<>();
        }



        if (StringUtils.isBlank(resourcesId) || StringUtils.isBlank(path)) {
            return new ArrayList<>();
        }
        path = path.replaceAll("\\\\", "/");
        int one1 = path.lastIndexOf("/");
        String suffix = path.substring((one1 + 1));

        //Restricted Access Return
        if (StringUtils.isNotBlank(realUsers) && suffix.equals(resourcesId)) {
            Query queryResourceAccess = new Query();
            queryResourceAccess.addCriteria(Criteria.where("resourcesId").is(resourcesId));
            queryResourceAccess.addCriteria(Criteria.where("applyEmail").is(realUsers));
            queryResourceAccess.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.ADOPT));
            ResourceAccess one = mongoTemplate.findOne(queryResourceAccess, ResourceAccess.class);
            if (null != one) {
                //If it is a partial file, it can be viewed  If it is a partial file, it can be viewed
                if ("range".equals(one.getAccessData()) && null != one.getFilesId() && one.getFilesId().size() > 0) {
                    Query query = new Query();
                    query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
                    query.addCriteria(Criteria.where("_id").in(one.getFilesId()));
                    List<ResourceFileTree> list = mongoTemplate.find(query, ResourceFileTree.class);
                    return list;
                }
            }
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("resourcesId").is(resourcesId));

        //If the suffix matches the datasetidIf the suffix matches the dataset  If the suffix matches the dataset If the suffix matches the dataset
        if (suffix.equals(resourcesId)) {
            query.addCriteria(Criteria.where("pid").is(0));
        }else {
            query.addCriteria(Criteria.where("filePath").is(path));
            ResourceFileTree one = mongoTemplate.findOne(query, ResourceFileTree.class);
            if (null != one) {
                int treeId = one.getTreeId();
                Query queryF = new Query();
                queryF.addCriteria(Criteria.where("resourcesId").is(resourcesId));
                queryF.addCriteria(Criteria.where("pid").is(treeId));
                List<ResourceFileTree> list = mongoTemplate.find(queryF, ResourceFileTree.class);
                return list;
            }
            return new ArrayList<>();
        }
        List<ResourceFileTree> list = mongoTemplate.find(query, ResourceFileTree.class);
        return list;
    }

    @Override
    public String resourcesDownloadFile(String path, String resourcesId, String realUsers, String ipAddr) {
        String cityInfo = IPUtil.getIpPossession(ipAddr);
        Query queryFile = new Query(Criteria.where("filePath").is(path)).addCriteria(Criteria.where("resourcesId").is(resourcesId));
        ResourceFileTree resourceFileTree = mongoTemplate.findOne(queryFile, ResourceFileTree.class);
        if (null == resourceFileTree && StringUtils.isNotBlank(realUsers)) {
            Query queryResourceAccess = new Query();
            queryResourceAccess.addCriteria(Criteria.where("resourcesId").is(resourcesId));
            queryResourceAccess.addCriteria(Criteria.where("applyEmail").is(realUsers));
            queryResourceAccess.addCriteria(Criteria.where("approvalStatus").is(Constant.Approval.ADOPT));
            ResourceAccess one = mongoTemplate.findOne(queryResourceAccess, ResourceAccess.class);
            if (null != one) {
                //If it is a partial file, it can be viewed  If it is a partial file, it can be viewed
                if ("range".equals(one.getAccessData()) && null != one.getFilesId() && one.getFilesId().size() > 0) {
                    Query query = new Query();
                    query.addCriteria(Criteria.where("resourcesId").is(resourcesId));
                    query.addCriteria(Criteria.where("_id").in(one.getFilesId()));
                    query.addCriteria(Criteria.where("filePath").regex(path));
                    ResourceFileTree fileTree = mongoTemplate.findOne(query, ResourceFileTree.class);
                    if (null != fileTree && StringUtils.isNotBlank(fileTree.getFilePath())) {
                        downloadStatistics(resourcesId, ipAddr, cityInfo, fileTree.getSize());
                        return fileTree.getFilePath();
                    }
                }
            }
        }
        if (null != resourceFileTree) {
            downloadStatistics(resourcesId, ipAddr, cityInfo, resourceFileTree.getSize());
        }
        return "ok";
    }

    private void downloadStatistics(String resourcesId, String ipAddr, String cityInfo, long size) {
        //Interface Download Count Settings
        Query query = new Query(Criteria.where("_id").is(resourcesId));
        ResourcesManage resourcesManage = mongoTemplate.findOne(query, ResourcesManage.class);
        if (null == resourcesManage) {
            return;
        }
        if (!Constant.Approval.ADOPT.equals(resourcesManage.getStatus())) {
            return;
        }
        Update update = new Update();
        update.inc("downloadNum", 1);
        mongoTemplate.updateFirst(query, update, ResourcesManage.class);

        //Download Record
        String dateString = DateUtils.getDateString(new Date());
        Query queryAccess = new Query(Criteria.where("resourcesId").is(resourcesId)).addCriteria(Criteria.where("name").is(cityInfo)).addCriteria(Criteria.where("ip").is(ipAddr)).addCriteria(Criteria.where("createTime").is(dateString));
        AccessRecords one = mongoTemplate.findOne(queryAccess, AccessRecords.class);
        if (one == null) {
            AccessRecords accessRecords = new AccessRecords();
            accessRecords.setIp(ipAddr);
            accessRecords.setName(cityInfo);
            accessRecords.setVisitNum(0);
            accessRecords.setDownloadNum(1);
            accessRecords.setDownloadStorage(size);
            accessRecords.setCreateTime(dateString);
            accessRecords.setResourcesId(resourcesId);
            mongoTemplate.save(accessRecords);
        } else {
            //Interface Download Count Settings
            Query queryUpdateAccess = new Query(Criteria.where("_id").is(one.getId()));
            Update updateAccess = new Update();
            updateAccess.inc("downloadNum", 1);
            updateAccess.inc("downloadStorage", size);
            mongoTemplate.findAndModify(queryUpdateAccess, updateAccess, new FindAndModifyOptions().returnNew(true).upsert(true), AccessRecords.class);
        }
    }


    @Override
    public File getRoot() {
        return rootDir;
    }

    @Override
    public String getPath(File file) {
        return rootDir.toURI().relativize(file.toURI()).getPath();
    }

    @Override
    public boolean exists(File file) {
        return file.exists();
    }

    @Override
    public boolean isDirectory(File file) {
        return file.isDirectory();
    }

    @Override
    public int getPermissions(File file) {
        int perms = 0;
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_READ, file.canRead());
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_WRITE, file.canWrite());
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_EXECUTE, file.canExecute());
        return perms;
    }

    @Override
    public long getSize(File file) {
        return file.length();
    }

    @Override
    public long getLastModified(File file) {
        return file.lastModified();
    }

    @Override
    public int getHardLinks(File file) {
        return file.isDirectory() ? 3 : 1;
    }

    @Override
    public String getName(File file) {
        return file.getName();
    }

    @Override
    public String getOwner(File file) {
        return "-";
    }

    @Override
    public String getGroup(File file) {
        return "-";
    }

    @Override
    public File getParent(File file) throws IOException {
        if(file.equals(rootDir)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file.getParentFile();
    }

    @Override
    public File[] listFiles(File dir) throws IOException {
        if(!dir.isDirectory()) throw new IOException("Not a directory");

        return dir.listFiles();
    }

    @Override
    public File findFile(String path) throws IOException {
        File file = new File(rootDir, path);

        if(!isInside(rootDir, file)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file;
    }

    @Override
    public File findFile(File cwd, String path) throws IOException {
        File file = new File(cwd, path);

        if(!isInside(rootDir, file)) {
            throw new FileNotFoundException("No permission to access this file");
        }

        return file;
    }

    @Override
    public InputStream readFile(File file, long start) throws IOException {
        // Not really needed, but helps a bit in performance
        if(start <= 0) {
            return new FileInputStream(file);
        }

        // Use RandomAccessFile to seek a file
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(start);

        // Create a stream using the RandomAccessFile
        return new FileInputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    @Override
    public OutputStream writeFile(File file, long start) throws IOException {
        // Not really needed, but helps a bit in performance
        if(start <= 0) {
            return new FileOutputStream(file, false);
        } else if(start == file.length()) {
            return new FileOutputStream(file, true);
        }

        // Use RandomAccessFile to seek a file
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(start);

        // Create a stream using the RandomAccessFile
        return new FileOutputStream(raf.getFD()) {
            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }


    @Override
    public void chmod(File file, int perms) throws IOException {
        boolean read = Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_READ);
        boolean write = Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_WRITE);
        boolean execute = Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_EXECUTE);

        if(!file.setReadable(read, true)) throw new IOException("Couldn't update the readable permission");
        if(!file.setWritable(write, true)) throw new IOException("Couldn't update the writable permission");
        if(!file.setExecutable(execute, true)) throw new IOException("Couldn't update the executable permission");
    }

    @Override
    public void touch(File file, long time) throws IOException {
        if(!file.setLastModified(time)) throw new IOException("Couldn't touch the file");
    }

    private boolean isInside(File dir, File file) {
        if(file.equals(dir)) return true;

        try {
            return file.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator);
        } catch(IOException ex) {
            return false;
        }
    }
}
