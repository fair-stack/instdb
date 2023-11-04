//package cn.cnic.instdb.utils;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.commons.net.ftp.FTPClient;
//import org.apache.commons.net.ftp.FTPFile;
//import org.apache.commons.net.ftp.FTPReply;
//import org.apache.ftpserver.FtpServer;
//import org.apache.ftpserver.FtpServerFactory;
//import org.apache.ftpserver.ftplet.Authority;
//import org.apache.ftpserver.listener.ListenerFactory;
//import org.apache.ftpserver.usermanager.impl.BaseUser;
//import org.apache.ftpserver.usermanager.impl.WritePermission;
///**
// * @Auther: wdd
// * @Date: 2021/06/07/18:57
// * @Description:
// */
//public class FavFTPUtil {
//
//
//    public static void startServer() {
//        FtpServer ftpServer;
//        int PORT = 0;
//        String USERNAME = "";
//        String PASSWORD = "";
//        String nvis_root="";
//        try {
//            // Used to createserver
//            FtpServerFactory serverFactory = new FtpServerFactory();
//            // configuration information，configuration information，configuration informationIPconfiguration information
//            ListenerFactory listenerFactory = new ListenerFactory();
//            // Set Port
//            listenerFactory.setPort(PORT);
//            // If there is no action in five minutes，If there is no action in five minutes
//            // listenerFactory.setIdleTimeout(5*60*1000);
//
//            // Set anonymous users Set anonymous users
//
//            // Set username and password
//            BaseUser user = new BaseUser();
//            user.setName(USERNAME);
//            user.setPassword(PASSWORD);
//
//            // set upPCset up
//            user.setHomeDirectory(nvis_root);
//
//            // Grant users write permission
//            List<Authority> authorities = new ArrayList<Authority>();
//            authorities.add(new WritePermission());
//            user.setAuthorities(authorities);
//            serverFactory.getUserManager().save(user);
//
//            // Create and listen to the network
//            serverFactory.addListener("default", listenerFactory.createListener());
//
//            // Create Service
//            ftpServer = serverFactory.createServer();
//
//            // Start Service
//            ftpServer.start();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    private String intToIp(int i) {
//        return (i & 0xFF)+"."+((i >> 8) & 0xFF)+"."+((i >> 16) & 0xFF)+"."+(i >> 24 & 0xFF);
//    }
//    /**
//     * Upload files（Upload filesAction/ControllerUpload files）
//     * @param hostname FTPserver address
//     * @param port FTPServer Port Number
//     * @param username FTPLogin account
//     * @param password FTPLogin password
//     * @param pathname FTPServer Save Directory
//     * @param fileName Upload toFTPUpload to
//     * @param inputStream Input file stream
//     * @return
//     */
//    public static boolean uploadFile(String hostname, int port, String username, String password, String pathname, String fileName, InputStream inputStream){
//        boolean flag = false;
//        FTPClient ftpClient = new FTPClient();
//        ftpClient.setControlEncoding("UTF-8");
//        try {
//            //connectFTPconnect
//            ftpClient.connect(hostname, port);
//            //LoginFTPLogin
//            ftpClient.login(username, password);
//            //Successfully logged inFTPSuccessfully logged in
//            int replyCode = ftpClient.getReplyCode();
//            if(!FTPReply.isPositiveCompletion(replyCode)){
//                return flag;
//            }
//
//            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
//            ftpClient.makeDirectory(pathname);
//            ftpClient.changeWorkingDirectory(pathname);
//            ftpClient.storeFile(fileName, inputStream);
//            inputStream.close();
//            ftpClient.logout();
//            flag = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally{
//            if(ftpClient.isConnected()){
//                try {
//                    ftpClient.disconnect();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return flag;
//    }
//
//
//    /**
//     * Upload files（Upload files）
//     * @param hostname FTPserver address
//     * @param port FTPServer Port Number
//     * @param username FTPLogin account
//     * @param password FTPLogin password
//     * @param pathname FTPServer Save Directory
//     * @param filename Upload toFTPUpload to
//     * @param originfilename The name of the file to be uploaded（The name of the file to be uploaded）
//     * @return
//     */
//    public static boolean uploadFileFromProduction(String hostname, int port, String username, String password, String pathname, String filename, String originfilename){
//        boolean flag = false;
//        try {
//            InputStream inputStream = new FileInputStream(new File(originfilename));
//            flag = uploadFile(hostname, port, username, password, pathname, filename, inputStream);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return flag;
//    }
//
//    /**
//     * Upload files（Upload files）
//     * @param hostname FTPserver address
//     * @param port FTPServer Port Number
//     * @param username FTPLogin account
//     * @param password FTPLogin password
//     * @param pathname FTPServer Save Directory
//     * @param originfilename The name of the file to be uploaded（The name of the file to be uploaded）
//     * @return
//     */
//    public static boolean uploadFileFromProduction(String hostname, int port, String username, String password, String pathname, String originfilename){
//        boolean flag = false;
//        try {
//            String fileName = new File(originfilename).getName();
//            InputStream inputStream = new FileInputStream(new File(originfilename));
//            flag = uploadFile(hostname, port, username, password, pathname, fileName, inputStream);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return flag;
//    }
//
//
//    /**
//     * Delete files
//     * @param hostname FTPserver address
//     * @param port FTPServer Port Number
//     * @param username FTPLogin account
//     * @param password FTPLogin password
//     * @param pathname FTPServer Save Directory
//     * @param filename The name of the file to be deleted
//     * @return
//     */
//    public static boolean deleteFile(String hostname, int port, String username, String password, String pathname, String filename){
//        boolean flag = false;
//        FTPClient ftpClient = new FTPClient();
//        try {
//            //connectFTPconnect
//            ftpClient.connect(hostname, port);
//            //LoginFTPLogin
//            ftpClient.login(username, password);
//            //validateFTPvalidate
//            int replyCode = ftpClient.getReplyCode();
//            if(!FTPReply.isPositiveCompletion(replyCode)){
//                return flag;
//            }
//            //switchFTPswitch
//            ftpClient.changeWorkingDirectory(pathname);
//            ftpClient.dele(filename);
//            ftpClient.logout();
//            flag = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally{
//            if(ftpClient.isConnected()){
//                try {
//                    ftpClient.logout();
//                } catch (IOException e) {
//
//                }
//            }
//        }
//        return flag;
//    }
//
//    /**
//     * Download files
//     * @param hostname FTPserver address
//     * @param port FTPServer Port Number
//     * @param username FTPLogin account
//     * @param password FTPLogin password
//     * @param pathname FTPServer File Directory
//     * @param filename File Name
//     * @param localpath Download file path
//     * @return
//     */
//    public static boolean downloadFile(String hostname, int port, String username, String password, String pathname, String filename, String localpath){
//        boolean flag = false;
//        FTPClient ftpClient = new FTPClient();
//        try {
//            //connectFTPconnect
//            ftpClient.connect(hostname, port);
//            //LoginFTPLogin
//            ftpClient.login(username, password);
//            //validateFTPvalidate
//            int replyCode = ftpClient.getReplyCode();
//            if(!FTPReply.isPositiveCompletion(replyCode)){
//                return flag;
//            }
//            //switchFTPswitch
//            ftpClient.changeWorkingDirectory(pathname);
//            FTPFile[] ftpFiles = ftpClient.listFiles();
//            for(FTPFile file : ftpFiles){
//                if(filename.equalsIgnoreCase(file.getName())){
//                    File localFile = new File(localpath + "/" + file.getName());
//                    OutputStream os = new FileOutputStream(localFile);
//                    ftpClient.retrieveFile(file.getName(), os);
//                    os.close();
//                }
//            }
//            ftpClient.logout();
//            flag = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally{
//            if(ftpClient.isConnected()){
//                try {
//                    ftpClient.logout();
//                } catch (IOException e) {
//
//                }
//            }
//        }
//        return flag;
//    }
//
//}
