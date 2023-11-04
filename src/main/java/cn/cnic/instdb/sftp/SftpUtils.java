//package cn.cnic.instdb.sftp;
//
//
//import com.jcraft.jsch.*;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.IOUtils;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//import java.util.Vector;
//
//
///**
// * @author  wdd
// * @describe  sftpTool class
// * @date  2021/11/26 17:05
// */
//
//@Slf4j
//public class SftpUtils {
//
//
//    private ChannelSftp sftp;
//
//    private Session session;
//    /** SFTP Login username*/
//    private String username;
//    /** SFTP Login password*/
//    private String password;
//    /** Private key */
//    private String privateKey;
//    /** SFTP server addressIPserver address*/
//    private String host;
//    /** SFTP port*/
//    private int port;
//
//
//    /**
//     * Construct password based authenticationsftpConstruct password based authentication
//     */
//    public SftpUtils(String username, String password, String host, int port) {
//        this.username = username;
//        this.password = password;
//        this.host = host;
//        this.port = port;
//    }
//
//    /**
//     * Construct a key based authenticationsftpConstruct a key based authentication
//     */
//    public SftpUtils(String username, String host, int port, String privateKey) {
//        this.username = username;
//        this.host = host;
//        this.port = port;
//        this.privateKey = privateKey;
//    }
//
//    public SftpUtils(){}
//
//
//
//
//    /**
//     * connectsftpconnect
//     */
//    public ChannelSftp login() {
//        try {
//            JSch jsch = new JSch();
//            if (privateKey != null) {
//                jsch.addIdentity(privateKey);// Set private key
//            }
//
//            session = jsch.getSession(username, host, port);
//
//            if (password != null) {
//                session.setPassword(password);
//            }
//            Properties config = new Properties();
//            config.put("StrictHostKeyChecking", "no");
//
//            session.setConfig(config);
//            session.connect();
//
//            Channel channel = session.openChannel("sftp");
//            channel.connect();
//
//            sftp = (ChannelSftp) channel;
//        } catch (JSchException e) {
//            log.error("context",e);
//            log.info("getSftp exception：",e);
//        }
//        return  sftp;
//    }
//
//    /**
//     * Close Connection server
//     */
//    public void logout() {
//        if (sftp != null) {
//            if (sftp.isConnected()) {
//                sftp.disconnect();
//            }
//        }
//        if (session != null) {
//            if (session.isConnected()) {
//                session.disconnect();
//            }
//        }
//    }
//
//
//    public void cd(String directory) throws SftpException {
//        if (directory != null && !"".equals(directory) && !"/".equals(directory)) {
//            sftp.cd(directory);
//        }
//
//    }
//
//    /**
//     * Upload data from the input stream tosftpUpload data from the input stream to。Upload data from the input stream to=uploadPath+sftpFileName
//     *
//     * @param basePath Basic Path  Basic Path“/”
//     * @param uploadPath    Upload to this directory
//     * @param sftpFileName sftpEnd File Name
//     */
//    public boolean upload(String basePath,String uploadPath, String sftpFileName, InputStream input) {
//        try {
//            sftp.cd(basePath);
//            String[] folds = uploadPath.split("/");
//            for(String fold : folds){
//                if(fold.length()>0){
//                    try {
//                        sftp.cd(fold);
//                    }catch (Exception e){
//                        log.info("catalogue"+fold+",catalogue");
//                        sftp.mkdir(fold);
//                        sftp.cd(fold);
//                    }
//                }
//            }
//            //upload
//            sftp.put(input,sftpFileName);
//            log.info(sftpFileName+"File uploaded successfully");
//        }catch (SftpException e) {
//            log.info("Upload failed",e);
//            return false;
//        } finally{
//            try {
//                logout();
//            } catch (Exception e) {
//                log.info("Disconnect error",e);
//                return false;
//            }
//        }
//        return true;
//    }
//
//
//    /**
//     * Download files。
//     *
//     * @param directory    Download directory
//     * @param downloadFile Downloaded files
//     * @param saveFile     Local path exists
//     */
//    public void download(String directory, String downloadFile, String saveFile) {
//        System.out.println("download:" + directory + " downloadFile:" + downloadFile + " saveFile:" + saveFile);
//
//        File file = null;
//        try {
//            if (directory != null && !"".equals(directory)) {
//                sftp.cd(directory);
//            }
//            File initFile = new File(saveFile);
//            //If the directory file does not exist, create it
//            if (!initFile.getParentFile().exists()) {
//                initFile.getParentFile().mkdirs();
//            }
//            file = new File(saveFile);
//            sftp.get(downloadFile, new FileOutputStream(file));
//        } catch (SftpException e) {
//            log.error("context",e);
//            if (file != null) {
//                file.delete();
//            }
//        } catch (FileNotFoundException e) {
//            log.error("context",e);
//            if (file != null) {
//                file.delete();
//            }
//        }
//
//    }
//
//    /**
//     * Download files
//     *
//     * @param directory    Download directory
//     * @param downloadFile Download file name
//     * @return Byte array
//     */
//    public byte[] download(String directory, String downloadFile) throws SftpException, IOException {
//        if (directory != null && !"".equals(directory)) {
//            sftp.cd(directory);
//        }
//        InputStream is = sftp.get(downloadFile);
//
//        byte[] fileData = IOUtils.toByteArray(is);
//
//        return fileData;
//    }
//
//
//    /**
//     * Delete files
//     *
//     * @param directory  To delete the directory where the file is located
//     * @param deleteFile Files to be deleted
//     */
//    public void delete(String directory, String deleteFile) throws SftpException {
//        if (directory != null && !"".equals(directory)) {
//            sftp.cd(directory);
//        }
//        sftp.rm(deleteFile);
//    }
//
//
//    /**
//     * List files in the directory
//     *
//     * @param directory Directory to list
//     */
//    public Vector<ChannelSftp.LsEntry> listFiles(String directory) throws SftpException {
//        return sftp.ls(directory);
//    }
//
//    public boolean isExistsFile(String directory, String fileName) {
//
//        List<String> findFilelist = new ArrayList();
//        ChannelSftp.LsEntrySelector selector = new ChannelSftp.LsEntrySelector() {
//            @Override
//            public int select(ChannelSftp.LsEntry lsEntry) {
//                if (lsEntry.getFilename().equals(fileName)) {
//                    findFilelist.add(fileName);
//                }
//                return 0;
//            }
//        };
//
//        try {
//            sftp.ls(directory, selector);
//        } catch (SftpException e) {
//            log.error("context",e);
//        }
//
//        if (findFilelist.size() > 0) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    /**
//     * Determine if the directory exists
//     * @param path
//     * @param sftp
//     * @return
//     */
//    public boolean isExistDir(String path,ChannelSftp sftp){
//        boolean  isExist=false;
//        try {
//            SftpATTRS sftpATTRS = sftp.lstat(path);
//            isExist = true;
//            return sftpATTRS.isDir();
//        } catch (Exception e) {
//            if (e.getMessage().toLowerCase().equals("no such file")) {
//                isExist = false;
//            }
//        }
//        return isExist;
//
//    }
//
//
//    /**
//     * Obtain all files in the directory,Obtain all files in the directory(Obtain all files in the directory)
//     * @param directory
//     * @param saveFile
//     */
//    public void downloadByDirectory (String directory, String saveFile) {
//        System.out.println("download:" + directory +    " saveFile:" + saveFile);
//
//        File file = null;
//        try {
//            if (directory != null && !"".equals(directory)) {
//                sftp.cd(directory);
//            }
//            Vector<ChannelSftp.LsEntry>  listFile = listFiles(directory);
//
//            for (ChannelSftp.LsEntry file1 : listFile)
//            {
//                String fileName = file1.getFilename();
//                if(fileName.contains(".json") ||  fileName.contains(".csv")){
//                    System.out.println(fileName);
//                    file = new File(saveFile);
//                    sftp.get(fileName, new FileOutputStream(file));
//                }
//
//            }
//
//        } catch (SftpException e) {
//            log.error("context",e);
//            if (file != null) {
//                file.delete();
//            }
//        } catch (FileNotFoundException e) {
//            log.error("context",e);
//            if (file != null) {
//                file.delete();
//            }
//        }
//
//    }
//
//
//}
