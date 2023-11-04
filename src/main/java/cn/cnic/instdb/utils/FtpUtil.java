package cn.cnic.instdb.utils;

import org.apache.commons.net.ftp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @Auther: wdd
 * @Date: 2023/03/28/16:31
 * @Description: FTPupload、upload、upload upload
 * convention:convention
 */
public class FtpUtil {

    private final Logger LOGGER = LoggerFactory.getLogger(FtpUtil.class);

    /** path separator  */
    private final String SEPARATOR_STR = "/";

    /** spot */
    private final String DOT_STR = ".";

    /** ftpserver address */
    private String hostname;

    /** Port number */
    private Integer port;

    /** ftpLogin account */
    private String username;

    /** ftpLogin password */
    private String password;

    /**
     * command command command(command)
     * as:as,as,as;(as)as
     * as:as,as,as;(as)as
     * as:as,as,as;asFTPas
     *
     * notes:notes(Server/Client)notes,notes
     */
    private String sendCommandStringEncoding = "UTF-8";

    /**
     * Download files,Download filesencodeDownload files
     *
     * notes:notes(Server/Client)notes,notes
     */
    private String downfileNameEncodingParam1 = "UTF-8";

    /**
     * Download files,Download filesdecodeDownload files
     *
     * notes:notes(Server/Client)notes,notes
     */
    private String downfileNameDecodingParam2 = "UTF-8";

    /**
     * Set file transfer format(Set file transfer formatFTPSet file transfer format)
     *
     * notes:notes,notes
     */
    private Integer transportFileType = FTP.BINARY_FILE_TYPE;

    /**
     * userFTPuser, user /var/ftpusers/justry_deng_root
     * notes:notes，notes null
     *
     */
    private String userRootDir;

    /** FTPclient */
    private FTPClient ftpClient;

    private FtpUtil(String hostname, Integer port, String username, String password) {
        super();
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }


    /**
     * When setting up download,When setting up download
     * Namely:new String(file.getName().getBytes(param1), param2) Namelyparam1
     * notes:notes(Server/Client)notes,notes
     *
     */
    public void setDownfileNameEncodingParam1(String downfileNameEncodingParam1) {
        this.downfileNameEncodingParam1 = downfileNameEncodingParam1;
    }

    /**
     * When setting up download,When setting up download
     * Namely:new String(file.getName().getBytes(param1), param2) Namelyparam2
     * notes:notes(Server/Client)notes,notes
     *
     */
    public void setDownfileNameDecodingParam2(String downfileNameDecodingParam2) {
        this.downfileNameDecodingParam2 = downfileNameDecodingParam2;
    }


    /**
     * Set file transfer format -> Set file transfer format
     * Based on one's own timing situation,Based on one's own timing situationFTP.BINARY_FILE_TYPEBased on one's own timing situationFTP.ASCII_FILE_TYPEBased on one's own timing situation
     * notes:notes,notes
     *
     */
    public void setTransportFileType(Integer transportFileType) {
        if( transportFileType != null) {
            this.transportFileType = transportFileType;
        }
    }

    /**
     * FTPUpload of、Upload of、Upload of,Upload of Upload of; Upload of
     * as:as,as,as;(as)as
     * as:as,as,as;(as)as
     * as:as,as,as;asFTPas
     *
     *  Saves the character encoding to be used by the FTP control connection.
     *  Some FTP servers require that commands be issued in a non-ASCII
     *  encoding like UTF-8 so that filenames with multi-byte character
     *  representations (e.g, Big 8) can be specified.
     */
    public void setSendCommandStringEncoding(String sendCommandStringEncoding) {
        this.sendCommandStringEncoding = sendCommandStringEncoding;
    }

    /**
     * @param hostname
     *            FTPServer ip
     * @param port
     *            FTPServer port
     * @param username
     *            user name
     * @param password
     *            password
     * @return FtpUtilexample
     */
    public static FtpUtil getFtpUtilInstance(String hostname, Integer port, String username, String password) {
        return new FtpUtil(hostname, port, username, password);
    }

    /**
     * initializationFTPinitialization
     *
     * @throws IOException IOabnormal
     */
    private void initFtpClient() throws IOException {
        if(ftpClient == null){
            ftpClient = new FTPClient();
        }
        // Set encoding(Set encoding，Set encodingftpClient.connect(hostname, port)Set encoding，Set encoding)
        ftpClient.setControlEncoding(sendCommandStringEncoding);
        // Returns the integer value of the reply code of the last FTP reply.
        int replyCode = ftpClient.getReplyCode();
        // 221express express，express
        int ftpDisconnectionStatusCode = 221;
        // Determine if a reply code is a positive completion response.
        // FTPReply.isPositiveCompletion(replyCode)Can be used to verify connectionFTP
        if (FTPReply.isPositiveCompletion(replyCode) && replyCode != ftpDisconnectionStatusCode) {
         //   LOGGER.info(" FtpUtil -> alreadly connected FTPServer !");
            return;
        } else {
            LOGGER.info(" FtpUtil -> connecting FTPServer -> {} : {}", this.hostname, this.port);
            // connectftpconnect
            ftpClient.connect(hostname, port);
            // LoginftpLogin
            ftpClient.login(username, password);
            LOGGER.info(" FtpUtil -> connect FTPServer success!");
            // initializationftpinitialization， initialization /var/ftpusers/justry_deng_root
            userRootDir = ftpClient.printWorkingDirectory();
            if (userRootDir == null || "".equals(userRootDir.trim()) || SEPARATOR_STR.equals(userRootDir)) {
                userRootDir = "";
            }
        }
        // Set file transfer format
        ftpClient.setFileType(transportFileType);
        // Set file transfer format
        ftpClient.setFileType(transportFileType);
        // set upFTPset up(set up)set up
        ftpClient.enterLocalPassiveMode();

        /* TODO This configuration mainly solves the problem of： This configuration mainly solves the problem ofLinuxThis configuration mainly solves the problem ofFTP，.listFiles(xxx)This configuration mainly solves the problem of This configuration mainly solves the problem of This configuration mainly solves the problem of(This configuration mainly solves the problem of)This configuration mainly solves the problem of
         *      After introducing this configuration，After introducing this configuration  After introducing this configurationWindowsAfter introducing this configurationFTPAfter introducing this configuration，.listFiles(xxx)After introducing this configuration After introducing this configuration After introducing this configuration(After introducing this configuration)，
         *      therefore，thereforeWindowsthereforeFTP,therefore，therefore therefore
         */
        // Due toapacheDue to，Due to
        ftpClient.configure(new FTPClientConfig("com.aspire.util.UnixFTPEntryParser"));
    }

    /**
     * Upload files toFTP
     * notes:notes,notes
     *
     * @param remoteDir
     *            Upload to the specified directory(FTPUpload to the specified directorypwdUpload to the specified directory)
     * @param remoteFileName
     *            Upload toFTP,Upload to
     * @param file
     *            Local file to upload
     *
     * @return Upload Results
     * @throws IOException IOabnormal
     */
    @SuppressWarnings("unused")
    public boolean uploadFile(String remoteDir, String remoteFileName, File file) throws IOException{
        boolean result;
        remoteDir = handleRemoteDir(remoteDir);
        try(InputStream inputStream = new FileInputStream(file)){
            // initialization
            initFtpClient();
            createDirecroty(remoteDir);
            ftpClient.changeWorkingDirectory(remoteDir);
            result = ftpClient.storeFile(remoteFileName, inputStream);
        }
        LOGGER.info(" FtpUtil -> uploadFile boolean result is -> {}", result);
        return result;
    }

    /**
     * fromFTPfrom
     * notes:notesremoteDirOrRemoteFilenotes,notes
     * notes:notesremoteDirOrRemoteFilenotes,localDirnotes;notes,
     *    Nor will it be created locallylocalDirNor will it be created locally
     *
     * @param remoteDirOrRemoteFile
     *            FTPA directory in(A directory in,A directory in);
     *            or  FTPor(or)
     * @param localDir
     *            Local folder for saving downloaded files
     *
     * @return Number of downloaded files
     * @throws IOException IOabnormal
     */
    public int downloadFile(String remoteDirOrRemoteFile, String localDir) throws IOException{
        remoteDirOrRemoteFile = handleRemoteDir(remoteDirOrRemoteFile);
        int successSum = 0;
        int failSum = 0;
        initFtpClient();
        // according toremoteDirOrRemoteFileaccording to,according tochangeWorkingDirectory
        if (!remoteDirOrRemoteFile.contains(DOT_STR)) {
            // Switch to the directory where the file you want to download is located,Switch to the directory where the file you want to download is located0
            boolean flag = ftpClient.changeWorkingDirectory(remoteDirOrRemoteFile);
            // Not excluding those Not excluding those Not excluding those;
            // If switching to this directory fails,If switching to this directory fails,If switching to this directory fails
            if (!flag) {
                return downloadNonsuffixFile(remoteDirOrRemoteFile, localDir);
            }
        } else {
            String tempWorkingDirectory;
            int index = remoteDirOrRemoteFile.lastIndexOf(SEPARATOR_STR);
            if (index > 0) {
                tempWorkingDirectory = remoteDirOrRemoteFile.substring(0, index);
            } else {
                tempWorkingDirectory = SEPARATOR_STR;
            }
            // Switch to the directory where the file you want to download is located,Switch to the directory where the file you want to download is located0
            ftpClient.changeWorkingDirectory(tempWorkingDirectory);
        }
        File localFileDir = new File(localDir);
        // obtainremoteDirOrRemoteFileobtain obtain   obtain  obtain
        FTPFile[] ftpFiles = ftpClient.listFiles(remoteDirOrRemoteFile);
        for (FTPFile file : ftpFiles) {
            // If it is a folder,If it is a folder (If it is a folder:If it is a folder,If it is a folder)
            if (file.isDirectory()) {
                continue;
            }
            //If the folder does not exist, create it    
            if (!localFileDir.exists()) {
                boolean result = localFileDir.mkdirs();
             //   LOGGER.info(" {} is not exist, create this Dir! create result -> {}!",
                 //       localFileDir, result);
            }
            String name = new String(file.getName().getBytes(this.downfileNameEncodingParam1),
                    this.downfileNameDecodingParam2);
            String tempLocalFile = localDir.endsWith(SEPARATOR_STR) ?
                    localDir + name :
                    localDir + SEPARATOR_STR + name;
            File localFile = new File(tempLocalFile);
            try (OutputStream os = new FileOutputStream(localFile)) {
                boolean result = ftpClient.retrieveFile(file.getName(), os);
                if (result) {
                    successSum++;
                   // LOGGER.info(" already download normal file -> {}", name);
                } else {
                    failSum++;
                }
            }
        }
    //    LOGGER.info(" FtpUtil -> downloadFile success download file total -> {}", successSum);
      //  LOGGER.info(" FtpUtil -> downloadFile fail download file total -> {}", failSum);
        return successSum;
    }

    /**
     * downloadFileUpgraded version of -> Upgraded version of:
     *     1.remoteDirOrRemoteFileCan beFTPCan be（Can be）
     *       -> Download this file,Download this filedownloadFileDownload this file
     *
     *     2.remoteDirOrRemoteFileCan beFTPCan be
     *       -> Download all files in this directory、Download all files in this directory(Download all files in this directory)
     *           notes:notesdownloadFilenotes,downloadFilenotes,notes
     *
     */
    public int recursiveDownloadFile(String remoteDirOrRemoteFile, String localDir) throws IOException {
        remoteDirOrRemoteFile = handleRemoteDir(remoteDirOrRemoteFile);
        int successSum = 0;
        // remoteDirOrRemoteFileIt is a clear document  It is a clear document  It is a clear document
        if (remoteDirOrRemoteFile.contains(DOT_STR)) {
            successSum = downloadFile(remoteDirOrRemoteFile, localDir);
        } else {
            /// Preliminary assembly data,Preliminary assembly data;Preliminary assembly dataFTPPreliminary assembly data,Preliminary assembly dataFTPPreliminary assembly dataMap
            // Orderly storageFTP remoteOrderly storage
            // Actually, the logic is:Actually, the logic isalreadyQueriedDirListActually, the logic is,Actually, the logic is。Actually, the logic is。
            List<String> alreadyQueryDirList = new ArrayList<>(16);
            alreadyQueryDirList.add(remoteDirOrRemoteFile);
            // Orderly storageFTP remoteOrderly storage
            List<String> requiredQueryDirList = new ArrayList<>(16);
            requiredQueryDirList.add(remoteDirOrRemoteFile);
            // recordFTPrecord record
            Map<String, String> storeDataMap = new HashMap<>(16);
            storeDataMap.put(remoteDirOrRemoteFile, localDir);
            queryFTPAllChildrenDirectory(storeDataMap, alreadyQueryDirList, requiredQueryDirList);
            String tempPath;
            // Loop calldownloadFile()Loop call,Loop call
            for(String str : alreadyQueryDirList) {
                // takeFTPtakepwdtake，take(take downloadFiletakepwdtake)
                // prompt:promptFTPprompt，promptpwd,prompt"/"，promptFTPpromptLinuxprompt，
                //     This is related toFTPThis is related to，This is related to《This is related to(This is related to)》This is related toFTPThis is related to
                tempPath = str.length() > userRootDir.length() ?
                        str.substring(userRootDir.length()) :
                        SEPARATOR_STR;
                int thiscount = downloadFile(tempPath, storeDataMap.get(str));
                successSum += thiscount;
            }
        }
      //  System.out.println(" FtpUtil -> recursiveDownloadFile(excluded created directories) "
          //      + " success download file total -> " + successSum);
        return successSum;
    }

    /**
     * Delete files Delete files Delete files
     * notes:notes  notes
     * notes: notes，notes“/”notes，notes，notesfalse
     *
     * @param deletedBlankDirOrFile
     *            The full pathname of the file to be deleted  The full pathname of the file to be deleted  The full pathname of the file to be deleted
     *        unified:unified unified“/”,unified“\”;
     *
     * @return Whether the deletion was successful or not
     * @throws IOException IOabnormal
     */
    public boolean deleteBlankDirOrFile(String deletedBlankDirOrFile) throws IOException{
        if(deletedBlankDirOrFile == null || SEPARATOR_STR.equals(deletedBlankDirOrFile)) {
            return false;
        }
        deletedBlankDirOrFile = handleRemoteDir(deletedBlankDirOrFile);
        boolean flag;
        initFtpClient();
        // according toremoteDirOrRemoteFileaccording to,according tochangeWorkingDirectory
        if (deletedBlankDirOrFile.lastIndexOf(DOT_STR) < 0) {
            // Due to protective mechanisms:Due to protective mechanisms,Due to protective mechanisms
            flag = ftpClient.removeDirectory(deletedBlankDirOrFile);
            // Not excluding those Not excluding those Not excluding those;
            // If deleting an empty folder fails,If deleting an empty folder fails,If deleting an empty folder fails
            if (!flag) {
                flag = ftpClient.deleteFile(deletedBlankDirOrFile);
            }
            // If it is a file,If it is a file
        } else {
            String tempWorkingDirectory;
            int index = deletedBlankDirOrFile.lastIndexOf(SEPARATOR_STR);
            if (index > 0) {
                tempWorkingDirectory = deletedBlankDirOrFile.substring(0, index);
            } else {
                tempWorkingDirectory = SEPARATOR_STR;
            }
            // Switch to the directory where the file you want to download is located,Switch to the directory where the file you want to download is located0
            ftpClient.changeWorkingDirectory(tempWorkingDirectory);
            flag = ftpClient.deleteFile(deletedBlankDirOrFile.substring(index + 1));
        }
        LOGGER.info(" FtpUtil -> deleteBlankDirOrFile [{}] boolean result is -> {}",
                deletedBlankDirOrFile, flag);
        return flag;
    }


    /**
     * deleteBlankDirOrFileEnhanced version of -> Enhanced version of、Enhanced version of、Enhanced version of
     * notes: notes，notes“/”notes，notes，notesfalse
     *
     * @param deletedBlankDirOrFile
     *            The file path or folder to delete
     *
     * @return Whether the deletion was successful or not
     * @throws IOException IOabnormal
     */
    public boolean recursiveDeleteBlankDirOrFile(String deletedBlankDirOrFile) throws IOException{
        if(deletedBlankDirOrFile == null || SEPARATOR_STR.equals(deletedBlankDirOrFile)) {
            return false;
        }
        String realDeletedBlankDirOrFile = handleRemoteDir(deletedBlankDirOrFile);
        boolean result = true;
        initFtpClient();
        if(!destDirExist(realDeletedBlankDirOrFile)) {
            LOGGER.info(" {} maybe is a  non-suffix file!, try delete!", realDeletedBlankDirOrFile);
            boolean flag = deleteBlankDirOrFile(deletedBlankDirOrFile);
            String flagIsTrue = " FtpUtil -> recursiveDeleteBlankDirOrFile "
                    + realDeletedBlankDirOrFile + " -> success!";
            String flagIsFalse = " FtpUtil -> recursiveDeleteBlankDirOrFile "
                    + realDeletedBlankDirOrFile + " -> target file is not exist!";
            LOGGER.info(flag ? flagIsTrue : flagIsFalse);
            return true;
        }
        // remoteDirOrRemoteFileIt is a clear document  It is a clear document  It is a clear document
        if (realDeletedBlankDirOrFile.contains(DOT_STR) || !ftputilsChangeWorkingDirectory(realDeletedBlankDirOrFile)) {
            result = deleteBlankDirOrFile(deletedBlankDirOrFile);
        } else {
            /// Preliminary assembly data,Preliminary assembly data;Preliminary assembly dataFTPPreliminary assembly data、Preliminary assembly data        (Preliminary assembly data)
            // deposit  deposit
            // Actually, the logic is:Actually, the logic isalreadyQueriedDirListActually, the logic is,Actually, the logic is。Actually, the logic is。
            List<String> alreadyQueriedDirList = new ArrayList<>(16);
            alreadyQueriedDirList.add(realDeletedBlankDirOrFile);
            // deposit  deposit
            List<String> alreadyQueriedFileList = new ArrayList<>(16);
            // deposit deposit
            List<String> requiredQueryDirList = new ArrayList<>(16);
            requiredQueryDirList.add(realDeletedBlankDirOrFile);
            queryAllChildrenDirAndChildrenFile(alreadyQueriedDirList,
                    alreadyQueriedFileList,
                    requiredQueryDirList);
            String tempPath;
            // Loop calldeleteBlankDirOrFile()Loop call,Loop call
            for (String filePath : alreadyQueriedFileList) {
                tempPath = filePath.length() > userRootDir.length() ?
                           filePath.substring(userRootDir.length()) :
                           SEPARATOR_STR;
                deleteBlankDirOrFile(tempPath);
            }
            // rightalreadyQueriedDirListright,right,right right
            String[] alreadyQueriedDirArray = new String[alreadyQueriedDirList.size()];
            alreadyQueriedDirArray = alreadyQueriedDirList.toArray(alreadyQueriedDirArray);
            sortArray(alreadyQueriedDirArray);
            // Loop calldeleteBlankDirOrFile()Loop call,Loop call
            for (String str : alreadyQueriedDirArray) {
                tempPath = str.length() > userRootDir.length() ?
                           str.substring(userRootDir.length()) :
                           SEPARATOR_STR;
                boolean isSuccess = deleteBlankDirOrFile(tempPath);
                if (!isSuccess) {
                    result = false;
                }
            }
        }
        LOGGER.info(" FtpUtil -> recursiveDeleteBlankDirOrFile {} boolean result is -> {}",
                realDeletedBlankDirOrFile, result);
        return result;
    }

    /**
     * Based on the length of array elements,Based on the length of array elements(Based on the length of array elements,Based on the length of array elements)
     * Array elements cannot benull
     *
     */
    private void sortArray(String[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            for(int j = 0; j < array.length - 1 - i; j++) {
                if (array[j].length() - array[j+1].length() < 0) {
                    String flag=array[j];
                    array[j] = array[j+1];
                    array[j+1] = flag;
                }
            }
        }
    }

    /**
     *
     * Based on the givenFTPBased on the given、Based on the given; Based on the givenFTPBased on the given , Based on the given
     * The local directory corresponding to the directory(The local directory corresponding to the directory)
     *
     * @param storeDataMap
     *            storageFTPstorage;key -> FTPstorage, value -> storagekeystorage
     * @param alreadyQueriedDirList
     *            All that have been checkedFTPAll that have been checked,All that have been checked:keyAll that have been checked
     * @param requiredQueryDirList
     *            What else needs to be queriedFTPWhat else needs to be queried
     *
     * @throws IOException IOabnormal
     */
    private void queryFTPAllChildrenDirectory(Map<String, String> storeDataMap,
                                              List<String> alreadyQueriedDirList,
                                              List<String> requiredQueryDirList) throws IOException {
        List<String> newRequiredQueryDirList = new ArrayList<>(16);
        if(requiredQueryDirList.size() == 0) {
            return;
        }
        for (String str : requiredQueryDirList) {
            String rootLocalDir = storeDataMap.get(str);
            // obtainrootRemoteDirobtain obtain(obtain  obtain)
            FTPFile[] ftpFiles = ftpClient.listFiles(str);
            for(FTPFile file : ftpFiles){
                if (file.isDirectory()) {
                    String tempName = file.getName();
                    String ftpChildrenDir = str.endsWith(SEPARATOR_STR) ?
                            str + tempName :
                            str + SEPARATOR_STR + tempName;
                    String localChildrenDir = rootLocalDir.endsWith(SEPARATOR_STR) ?
                            rootLocalDir + tempName :
                            rootLocalDir + SEPARATOR_STR + tempName;
                    alreadyQueriedDirList.add(ftpChildrenDir);
                    newRequiredQueryDirList.add(ftpChildrenDir);
                    storeDataMap.put(ftpChildrenDir, localChildrenDir);
                }
            }
        }
        this.queryFTPAllChildrenDirectory(storeDataMap, alreadyQueriedDirList, newRequiredQueryDirList);
    }

    /**
     * Based on the givenFTPBased on the given,Based on the given(Based on the given)
     *
     * @param alreadyQueriedDirList
     *            All the directories that have been found
     * @param alreadyQueriedFileList
     *            All the files that have been found
     * @param requiredQueryDirList
     *            What else needs to be queriedFTPWhat else needs to be queried
     * @throws IOException IOabnormal
     */
    private void queryAllChildrenDirAndChildrenFile(List<String> alreadyQueriedDirList,
                                                    List<String> alreadyQueriedFileList,
                                                    List<String> requiredQueryDirList) throws IOException {
        List<String> newRequiredQueryDirList = new ArrayList<>(16);
        if (requiredQueryDirList.size() == 0) {
            return;
        }
        initFtpClient();
        for (String dirPath : requiredQueryDirList) {
            // obtaindirPathobtain obtain(obtain  obtain)
            FTPFile[] ftpFiles = ftpClient.listFiles(dirPath);
            for (FTPFile file : ftpFiles) {
                String tempName = file.getName();
                String ftpChildrenName = dirPath.endsWith(SEPARATOR_STR) ?
                        dirPath + tempName :
                        dirPath + SEPARATOR_STR + tempName;
                if (file.isDirectory()) {
                    alreadyQueriedDirList.add(ftpChildrenName);
                    newRequiredQueryDirList.add(ftpChildrenName);
                } else {
                    alreadyQueriedFileList.add(ftpChildrenName);
                }
            }

        }
        this.queryAllChildrenDirAndChildrenFile(alreadyQueriedDirList, alreadyQueriedFileList, newRequiredQueryDirList);
    }


    /**
     * Create specified directory(Create specified directory:Create specified directory,Create specified directoryfalse)
     *
     * @param dir
     *            Directory Path,Directory Path,Directory Path: /abc Directory Path  /abc/ Directory Path
     *                   Relative path,Relative path:  sss Relative path    sss/ Relative path
     *                  notes:notes,notessessionnotes。
     *                  prompt: .changeWorkingDirectory() promptsessionprompt
     * @return Create successfully or not
     * @throws IOException IOabnormal
     */
    private boolean makeDirectory(String dir) throws IOException {
        boolean flag;
        flag = ftpClient.makeDirectory(dir);
        if (flag) {
          //  LOGGER.info(" FtpUtil -> makeDirectory -> create Dir [{}] success!", dir);
        } else {
            LOGGER.info(" FtpUtil -> makeDirectory -> create Dir [{}] fail!", dir);
        }
        return flag;
    }

    /**
     * stayFTPstayremoteDirstay(stay,stay;stay,stay)
     *
     * @param directory
     *            Directory to be created
     *            notes:notesFTPnotespwdnotes（notesFTPnotes）
     *            notes:pwdRemoteDirnotesnullnotes“”，pwdRemoteDirnotes
     *
     * @throws IOException IOabnormal
     */
    private void createDirecroty(String directory) throws IOException {
        if (!directory.equals(userRootDir) && !ftpClient.changeWorkingDirectory(directory)) {
            if (!directory.endsWith(SEPARATOR_STR)) {
                directory = directory  + SEPARATOR_STR;
            }
            // Obtain the starting position of each node directory
            int start = userRootDir.length() + 1;
            int end = directory.indexOf(SEPARATOR_STR, start);
            // Circular directory creation
            String dirPath = userRootDir;
            String subDirectory;
            boolean result;
            while (end >= 0) {
                subDirectory = directory.substring(start, end);
                dirPath = dirPath + SEPARATOR_STR + subDirectory;
                if (!ftpClient.changeWorkingDirectory(dirPath)) {
                    result = makeDirectory(dirPath);
                    LOGGER.info(" FtpUtil -> createDirecroty -> invoke makeDirectory got retrun -> {}!", result);
                }
                start = end + 1;
                end = directory.indexOf(SEPARATOR_STR, start);
            }
        }
    }


    /**
     * Avoid frequent occurrences in code initFtpClient、logout、disconnect;
     * Pack it hereFTPClientPack it here.changeWorkingDirectory(String pathname)Pack it here
     *
     * @param pathname
     *            To switch(session)To switchFTPTo switch
     */
    private boolean ftputilsChangeWorkingDirectory(String pathname) throws IOException{
        boolean result;
        initFtpClient();
        result = ftpClient.changeWorkingDirectory(pathname);
        return result;
    }

    /**
     * judgeFTPjudge
     *
     * @param pathname
     *            The path to determine(The path to determine、The path to determine)
     *            notes:notes
     */
    private boolean destDirExist(String pathname) throws IOException{
        boolean result;
        if (pathname.contains(DOT_STR)) {
            int index = pathname.lastIndexOf(SEPARATOR_STR);
            if (index != 0) {
                pathname = pathname.substring(0, index);
            } else {
                return true;
            }
        }
        result = ftpClient.changeWorkingDirectory(pathname);
        return result;
    }

    /**
     * Processing user input FTPProcessing user input
     * notes:notes notes  FTP(notes)notes
     *
     * @param remoteDirOrFile
     *            User inputFTPUser input
     * @return  Processed path
     */
    private String handleRemoteDir(String remoteDirOrFile) throws IOException {
        initFtpClient();
        if(remoteDirOrFile == null
                || "".equals(remoteDirOrFile.trim())
                || SEPARATOR_STR.equals(remoteDirOrFile)) {
            remoteDirOrFile = userRootDir + SEPARATOR_STR;
        } else if(remoteDirOrFile.startsWith(SEPARATOR_STR)) {
            remoteDirOrFile = userRootDir + remoteDirOrFile;
        } else {
            remoteDirOrFile = userRootDir + SEPARATOR_STR + remoteDirOrFile;
        }
        return remoteDirOrFile;
    }

    /**
     * download download
     *
     * @param remoteDirOrFile
     *            afterhandleRemoteDir()after   FTPafter
     *
     * @return  Number of successful entries
     * @throws IOException IOabnormal
     */
    private int downloadNonsuffixFile(String remoteDirOrFile, String localDir) throws IOException {
        int successSum = 0;
        int failSum = 0;
        File localFileDir = new File(localDir);
        String tempWorkingDirectory;
        String tempTargetFileName;
        int index = remoteDirOrFile.lastIndexOf(SEPARATOR_STR);
        tempTargetFileName = remoteDirOrFile.substring(index + 1);
        if(tempTargetFileName.length() > 0) {
            if (index > 0) {
                tempWorkingDirectory = remoteDirOrFile.substring(0, index);
            }else {
                tempWorkingDirectory = SEPARATOR_STR;
            }
            ftpClient.changeWorkingDirectory(tempWorkingDirectory);
            // obtaintempWorkingDirectoryobtain obtain   obtain  obtain
            FTPFile[] ftpFiles = ftpClient.listFiles(tempWorkingDirectory);
            for(FTPFile file : ftpFiles){
                String name = new String(file.getName().getBytes(this.downfileNameEncodingParam1),
                        this.downfileNameDecodingParam2);
                // If it's not the target file,If it's not the target file
                if(!tempTargetFileName.equals(name)) {
                    continue;
                }
                //If the folder does not exist, create it    
                if (!localFileDir.exists()) {
                    boolean result = localFileDir.mkdirs();
                 //   LOGGER.info(" {} is not exist, create this Dir! create result -> {}!",
                        //    localFileDir, result);
                }
                String tempLocalFile = localDir.endsWith(SEPARATOR_STR) ?
                        localDir + name :
                        localDir + SEPARATOR_STR + name;
                File localFile = new File(tempLocalFile);
                try (OutputStream os = new FileOutputStream(localFile)) {
                    boolean result = ftpClient.retrieveFile(file.getName(), os);
                    if (result) {
                        successSum++;
                    //    LOGGER.info(" already download nonsuffixname file -> {}", name);
                    } else {
                        failSum++;
                    }
                }
                LOGGER.info(" FtpUtil -> downloadFile success download item count -> {}", successSum);
                LOGGER.info(" FtpUtil -> downloadFile fail download item count -> {}", failSum);
            }
        }
        return successSum;
    }

    /**
     * Release resources
     *
     * notes:notes notes、notes，notes、notes，notes
     *    When there are many files，When there are many filesFTP、When there are many filesFTP；When there are many files；
     *    So why not provide a method，So why not provide a methodFTPSo why not provide a method，So why not provide a method So why not provide a method
     *
     *
     */
    public void releaseResource() throws IOException {
        if (ftpClient == null) {
            return;
        }
        try {
            ftpClient.logout();
        } catch (IOException e) {
            // If the connection is not open
            // ignore java.io.IOException: Connection is not open
        }
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
        ftpClient = null;
    }
}
