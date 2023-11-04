package cn.cnic.instdb.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class FtpFileUtils {

    /**
     * Description: towardsFTPtowards
     * @Version      1.0
     * @param url FTPserverhostname
     * @param port  FTPServer Port
     * @param username FTPLogin account
     * @param password  FTPLogin password
     * @param filename  Upload toFTPUpload to
     * @param input   Input stream
     * @return Successfully returnedtrue，Successfully returnedfalse *
     */
    public static boolean uploadFile(String url,// FTPserverhostname
                                     int port,// FTPServer Port
                                     String username, // FTPLogin account
                                     String password, // FTPLogin password
//                                     String path, // FTPServer Save Directory
                                     String filename, // Upload toFTPUpload to
                                     InputStream input // Input stream
    ){
        boolean success = false;
        FTPClient ftp = new FTPClient();
        ftp.setControlEncoding("GBK");
        try {
            int reply;
            ftp.setDataTimeout(120*600000);
            ftp.setConnectTimeout(10*10*6000);
            ftp.connect(url, port);// connectFTPconnect
            ftp.enterLocalPassiveMode();      //Passive mode
            ftp.setControlEncoding("UTF-8");
            ftp.setRemoteVerificationEnabled(false);
            // If using the default port，If using the default portftp.connect(url)If using the default portFTPIf using the default port
            ftp.login(username, password);// Login
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return success;
            }
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftp.storeFile(filename, input);
            ftp.logout();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(null != input){
                try {
                    input.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return success;
    }

    /**
     * Description: downloadFTPdownload
     * @Version      1.0
     * @param url FTPserverhostname
     * @param port  FTPServer Port
     * @param username FTPLogin account
     * @param password  FTPLogin password
     * @return Successfully returnedtrue，Successfully returnedfalse *
     */
    public static int downloadFile(String url,// FTPserverhostname
                                       int port,// FTPServer Port
                                       String username, // FTPLogin account
                                       String password, // FTPLogin password
                                       String remoteBaseDir, // Upload toFTPUpload to
                                       String localPath//Save to local path after downloading

    ) {
        int result = 0;
        /// Basic configuration
        FtpUtil ftpUtil = FtpUtil.getFtpUtilInstance(url, port, username, password);
        // notes:notes(Server/Client)notes,notes;
        ftpUtil.setDownfileNameEncodingParam1("UTF-8");
        // notes:notes(Server/Client)notes,notes
        ftpUtil.setDownfileNameDecodingParam2("UTF-8");
        // Key factors in controlling coding(Key factors in controlling coding，Key factors in controlling coding；Key factors in controlling codingftpClient.listFiles(String pathname)Key factors in controlling coding)
        ftpUtil.setSendCommandStringEncoding("UTF-8");

        try {
            // Execute Download
            result = ftpUtil.recursiveDownloadFile(remoteBaseDir,
                    localPath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Release resources
            try {
                ftpUtil.releaseResource();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.info(" The number of successfully downloaded files is -> " + result);
        return result;
    }

}
