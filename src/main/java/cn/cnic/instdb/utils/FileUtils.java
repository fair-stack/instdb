package cn.cnic.instdb.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.util.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.util.UriUtils;
import sun.nio.ch.FileChannelImpl;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * File processing tool class
 */

@Slf4j
public class FileUtils {

    /**
     * Upload method
     *
     * @param file
     * @param path
     * @param filename Custom Name
     * @return
     */
    public static String upload(MultipartFile file, String path, String filename) {
        Map<String, String> rtnMap = new HashMap<String, String>();
        rtnMap.put("code", "500");
        if (!file.isEmpty()) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss");
            Date nowDate = new Date();
            String nowDateformat = sdf.format(nowDate);

            //file name
            String saveFileName = file.getOriginalFilename();

            //Obtain the content before the decimal point,Obtain the content before the decimal point
            String prefix = saveFileName.substring(0, saveFileName.lastIndexOf("."));

            //suffix.suffix
            int one = saveFileName.lastIndexOf(".");
            String Suffix = saveFileName.substring((one + 1), saveFileName.length());

            if (StringUtils.isNotBlank(filename)) {
                saveFileName = nowDateformat + "-" + filename + "." + Suffix;
            }

            File saveFile = new File(path + saveFileName);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            long fileSize = 0;
            BufferedOutputStream out = null;
            try {

                out = new BufferedOutputStream(new FileOutputStream(saveFile));
                out.write(file.getBytes());
                out.flush();
                out.close();
                FileInputStream fis = new FileInputStream(saveFile);
                fileSize = fis.available();
                rtnMap.put("url", path + saveFileName);
                rtnMap.put("fileName", saveFileName);
                rtnMap.put("fileSize", String.valueOf(fileSize));
                rtnMap.put("msgInfo", "Upload successful");
                rtnMap.put("code", "200");
                log.info(saveFile.getName() + " File uploaded successfully");
            } catch (Exception e) {
                log.error("Upload failed", e);
                rtnMap.put("msgInfo", "Upload failed" + e.getMessage());
            } finally {
                if (null != out) {
                    try {
                        out.close();
                    } catch (Exception e) {
                        log.error("context",e);
                    }
                }
            }
        } else {
            log.info("Upload failed，Upload failed.");
            rtnMap.put("msgInfo", "Upload failed，Upload failed.");
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }

    /**
     * read resourceFile
     * @param filePath
     * @return
     */
    public static File getResourceFile(String filePath) {

        try {
            ClassPathResource classPathResource = new ClassPathResource(filePath);
            int one = filePath.lastIndexOf(".");
            String Suffix = filePath.substring((one + 1), filePath.length());
            InputStream inputStream = classPathResource.getInputStream();
            //Generate target file
            File somethingFile = File.createTempFile("classPathResourceFile", "." + Suffix);
            try {
                org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, somethingFile);
            } finally {
                if (null != inputStream) {
                    try {
                        IOUtils.closeQuietly(inputStream);
                        inputStream.close();
                    } catch (Exception e) {
                        log.error("context", e);
                    }
                }
            }

            return somethingFile;
        } catch (Exception e) {
            log.error("context", e);
            return null;
        }
    }


    /**
     * readjsonread
     * @param fileName
     * @return
     */
    public static String readJsonFile(String fileName) {
        String jsonStr = "";
        FileReader fileReader = null;
        Reader reader = null;
        try {
            File jsonFile = new File(fileName);
            fileReader = new FileReader(jsonFile);
            reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            log.error("context",e);
            return null;
        } finally {
            if (null != fileReader) {
                try {
                    fileReader.close();
                } catch (Exception e) {
                    log.error("context",e);
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    log.error("context",e);
                }
            }
        }
    }


    /**
     * Obtain file suffix based on file name
     *
     * @return
     */
    public static String getSuffixByFileName(String fileName) {
        int one = fileName.lastIndexOf(".");
        //The suffix format type of the file
        return fileName.substring((one + 1), fileName.length());
    }


    /**
     * Determine if the file is larger than2M
     * @param file
     * @return
     */
    public static boolean judgeSize(File file) {
        long length = file.length() / 1024;
        return length > 2048 ? true : false;
    }



    /**
     * Delete directories and files under them
     *
     * @param dir：The file path of the directory to be deleted
     * @return Directory deletion successful returntrue，Directory deletion successful returnfalse
     */
    public static boolean deleteDirectory(String dir) {
        // IfdirIf，If
        if (!dir.endsWith(File.separator)) {
            dir = dir + File.separator;
        }
        File dirFile = new File(dir);
        // IfdirIf，If，If
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            return false;
        }
        boolean flag = true;
        // Delete all files in the folder, including subdirectories
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // Delete sub files
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
            // delete a sub dir
            else if (files[i].isDirectory()) {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            return false;
        }
        // remove the current directory
        if (dirFile.delete()) {
           // log.info("Delete directory" + dir + "Delete directory！");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Delete files，Delete files
     *
     * @param fileName：The file name to be deleted
     * @return Successfully deleted returntrue，Successfully deleted returnfalse
     */
    public static boolean deleteFileAndDir(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                return deleteFile(fileName);
            } else {
                return deleteDirectory(fileName);
            }
        }
    }

    /**
     * Delete individual files
     *
     * @param fileName：The file name of the file to be deleted
     * @return Single file deletion successful returntrue，Single file deletion successful returnfalse
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        // If the file corresponding to the file path exists，If the file corresponding to the file path exists，If the file corresponding to the file path exists
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    /**
     * file turn MultipartFile
     * @param file
     * @return
     */
    public static MultipartFile getMultipartFile(File file) {
        FileItem item = new DiskFileItemFactory().createItem("file"
                , MediaType.MULTIPART_FORM_DATA_VALUE
                , true
                , file.getName());
        try (InputStream input = new FileInputStream(file);
             OutputStream os = item.getOutputStream()) {
            // Flow transfer
            IOUtils.copy(input, os);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid file: " + e, e);
        }

        return new CommonsMultipartFile(item);
    }

    /**
     * MultipartFile turn File
     *
     * @param file
     * @throws Exception
     */
    public static File multipartFileToFile(MultipartFile file) {

        File toFile = null;
        if (file.equals("") || file.getSize() <= 0) {
            file = null;
        } else {
            InputStream ins = null;
            try {
                ins = file.getInputStream();
                toFile = new File(file.getOriginalFilename());
                inputStreamToFile(ins, toFile);
                ins.close();
            } catch (Exception e) {
                log.error("context",e);
            } finally {
                if (null != ins) {
                    try {
                        ins.close();
                    } catch (Exception e) {
                        log.error("context",e);
                    }
                }
            }

        }
        return toFile;
    }

    //Get Stream File
    private static void inputStreamToFile(InputStream ins, File file) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            log.error("context",e);
        } finally {
            if (null != ins) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (Exception e) {
                }
            }
        }
    }


    /**
     * Determine if the file size is within the limit
     *
     * @param fileLen file length
     * @param fileSize Limit size
     * @param fileUnit Restricted Units（B,K,M,G）
     * @return
     */
    public static boolean checkFileSizeIsLimit(Long fileLen, int fileSize, String fileUnit) {
        double fileSizeCom = 0;
        if ("B".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen;
        } else if ("K".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen / 1024;
        } else if ("M".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen / (1024*1024);
        } else if ("G".equals(fileUnit.toUpperCase())) {
            fileSizeCom = (double) fileLen / (1024*1024*1024);
        }
        if (fileSizeCom > fileSize) {
            return false;
        }
        return true;

    }

    //judgexmljudge
    public static boolean isXmlDocument(File file){
        boolean flag = true;
        try {
            DocumentBuilderFactory foctory =DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = foctory.newDocumentBuilder();
            builder.parse(file);
            flag = true;
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }


    /**
     * zipdecompression
     * @param srcFile        zipsource file
     * @param destDirPath     The target folder after decompression
     * @throws RuntimeException Decompression failure will throw a runtime exception
     */
    public static void unZip(File srcFile, String destDirPath) throws RuntimeException {
        long start = System.currentTimeMillis();
        // Determine if the source file exists
        if (!srcFile.exists()) {
            throw new RuntimeException(srcFile.getPath() + "The specified file does not exist");
        }
        // Start decompressing
        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(srcFile);
            Enumeration<?> entries = zipFile.entries();
            log.info("Start extracting files"+srcFile.getName());
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                // If it is a folder，If it is a folder
                if (entry.isDirectory()) {
                    String dirPath = destDirPath + "/" + entry.getName();
                    File dir = new File(dirPath);
                    dir.mkdirs();
                } else {
                    // If it is a file，If it is a file，If it is a fileioIf it is a filecopyIf it is a file
                    File targetFile = new File(destDirPath + "/" + entry.getName());
                    // Ensure that the parent folder of this file must exist
                    if(!targetFile.getParentFile().exists()){
                        targetFile.getParentFile().mkdirs();
                    }
                    targetFile.createNewFile();
                    // Write the compressed file content to this file
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    int size = 0 == is.available() ? 1024 : is.available();
                    byte[] buf = new byte[size];

                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    // Shutoff sequence，Shutoff sequence
                    fos.close();
                    is.close();
                }
            }
            long end = System.currentTimeMillis();
            log.info("Decompression completed，Decompression completed：" + (end - start) +" ms");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("unzip error from ZipUtils", e);
        } finally {
            if(zipFile != null){
                try {
                    zipFile.close();
                } catch (IOException e) {
                    log.error("context",e);
                }
            }
        }
    }

    /**
     *  Compress all files in the specified folder，Compress all files in the specified folderzipCompress all files in the specified folder
     *
     * @param sourcePath List of file names that need to be compressed(List of file names that need to be compressed)
     * @param zipOutPath Compressed file name
     **/
    public static void batchZipFiles(String sourcePath, String zipOutPath) {
        ZipOutputStream zipOutputStream = null;
        WritableByteChannel writableByteChannel = null;
        MappedByteBuffer mappedByteBuffer = null;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(zipOutPath));
            writableByteChannel = Channels.newChannel(zipOutputStream);
            File file = new File(sourcePath);
            for (File source : file.listFiles()) {
                long fileSize = source.length();
                //utilizeputNextEntryutilize
                zipOutputStream.putNextEntry(new ZipEntry(source.getName()));
                long read = Integer.MAX_VALUE;
                int count = (int) Math.ceil((double) fileSize / read);
                long pre = 0;
                //Due to the fact that the file size for one mapping cannot exceed2GB，Due to the fact that the file size for one mapping cannot exceed
                for (int i = 0; i < count; i++) {
                    if (fileSize - pre < Integer.MAX_VALUE) {
                        read = fileSize - pre;
                    }
                    mappedByteBuffer = new RandomAccessFile(source, "r").getChannel()
                            .map(FileChannel.MapMode.READ_ONLY, pre, read);
                    writableByteChannel.write(mappedByteBuffer);
                    pre += read;
                }
            }
            //Release resources
            Method m = FileChannelImpl.class.getDeclaredMethod("unmap", MappedByteBuffer.class);
            m.setAccessible(true);
            m.invoke(FileChannelImpl.class, mappedByteBuffer);
            mappedByteBuffer.clear();
        } catch (Exception e) {
            log.error("Zip more file error， fileNames: " + sourcePath, e);
        } finally {
            try {
                if (null != zipOutputStream) {
                    zipOutputStream.close();
                }
                if (null != writableByteChannel) {
                    writableByteChannel.close();
                }
                if (null != mappedByteBuffer) {
                    mappedByteBuffer.clear();
                }
            } catch (Exception e) {
                log.error("Zip more file error, file names are:" + sourcePath, e);
            }
        }
    }




    /**
     * writejsonStringwritefile
     *
     * @param jsonStr
     * @param filePath
     */
    public static void writeJsonToFile(String jsonStr, String filePath) {

        Writer write = null;
        try {

            File jsonFile = new File(filePath);
            //If it already exists If it already exists
            if (jsonFile.exists()) {
                jsonFile.delete();
            }
            File parent = jsonFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            //establishjsonestablish
            jsonFile.createNewFile();
            //writejsonwrite
            write = new OutputStreamWriter(new FileOutputStream(jsonFile), "UTF-8");
            write.write(jsonStr);
            write.flush();
        } catch (Exception e) {
            log.error("context",e);
        } finally {
            try {
                if (null != write) {
                    write.close();
                }
            } catch (IOException e) {
                log.error("context",e);
            }
        }
    }

    /**
     * File Download
     * @param path
     * @param response
     */
    public static void downloadFile(String path, HttpServletResponse response,String name) {
        OutputStream os = null;
        InputStream fis = null;
        try {
            // pathIt is concatenated based on the log path and file name
            File file = new File(path);
            if (file.exists()) {
                String filename = file.getName();// Get Log File Name;
                //suffix.suffix
                int one = filename.lastIndexOf(".");
                String suffix = filename.substring((one + 1), filename.length());
                if (StringUtils.isNotBlank(name)) {
                    filename = name + "." + suffix;
                } else {
                    filename = file.getName();// Get Log File Name
                }
                fis = new BufferedInputStream(new FileInputStream(path));
                byte[] buffer = new byte[fis.available()];
                fis.read(buffer);
                fis.close();
                response.reset();
                // Remove the spaces in the file name first,Remove the spaces in the file name firstutf-8,Remove the spaces in the file name first,Remove the spaces in the file name first
                //  response.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.replaceAll(" ", "").getBytes("utf-8"), "iso8859-1"));
                response.setHeader("Content-Disposition",
                        "attachment;fileName*=UTF-8''" + UriUtils.encode(filename, "UTF-8"));
                response.addHeader("Content-Length", "" + file.length());
                os = new BufferedOutputStream(response.getOutputStream());
                // response.setContentType("application/octet-stream");
                os.write(buffer);// output file
                os.flush();
                os.close();
            }
        } catch (Exception e) {
            log.error("File download failed"+e);
            throw new RuntimeException("File download failed");
        } finally {
            try {
                if (null != os) {
                    os.close();
                }
            } catch (Exception e) {
                log.error("context", e);
            }
            try {
                if (null != fis) {
                    fis.close();
                }
            } catch (Exception e) {
                log.error("context", e);
            }
        }
    }

//    /**
//     *  Parse all files under the file
//     * @param fileList
//     * @param sendFile
//     */
//    private static void getRootFile(File[] fileList, ResourcesManage.SendFile sendFile){
//        List<ResourcesManage.SendFile> sendFileList = new ArrayList<>();
//        long totalSize = 0L;
//        if(fileList !=null && fileList.length >0){
//            for (File file : fileList) {
//                if(file.exists()){
//                    ResourcesManage.SendFile send = new ResourcesManage.SendFile();
//                    send.setFileName(file.getName());
//                    if(file.isFile()){  //Is it a file
//                        send.setIsFile(true);
//                        send.setSize(file.length());
//                    }else {
//                        send.setIsFile(false);
//                        if(null != file.listFiles() && file.listFiles().length > 0){
//                            getRootFile(file.listFiles(),send);
//                        }
//                    }
//                    totalSize += send.getSize();
//                    sendFileList.add(send);
//                }
//            }
//            sendFile.setSize(totalSize);
//            sendFile.setChildren(sendFileList);
//        }
//    };
//
//    /**
//     * Recursively obtain information about all files in the directory
//     * @param childrens
//     * @param fileList
//     */
//    public static void getFiles(File[] childrens, List<ResourcesManage.SendFile> fileList,String fileFilter) {
//        if (childrens != null && childrens.length > 0) {
//            for (File file : childrens) {
//                if(fileFilter.equals(file.getName())){
//                    continue;
//                }
//                ResourcesManage.SendFile sendFile = new ResourcesManage.SendFile();
//                sendFile.setFileName(file.getName());
//                sendFile.setIsFile(file.isFile());
//                if (!file.isFile()) {
//                    File[] children = file.listFiles();
//                    if (children != null && children.length > 0) {
//                        List<ResourcesManage.SendFile> sendFiles = new ArrayList<>();
//                        getFiles(children, sendFiles,fileFilter);
//                        sendFile.setChildren(sendFiles);
//                    } else {
//                        getRootFile(file.listFiles(), sendFile);
//                    }
//                } else {
//                    sendFile.setSize(file.length());
//                }
//                fileList.add(sendFile);
//            }
//        }
//    }

    /**
     * Byte conversion，Byte conversionB、KB、MB、GB、TB
     * @param size
     * @return
     */
    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("0.00").format(size / Math.pow(1024, digitGroups)) + units[digitGroups];
    }


    /**
     *  Verify if it is a folder
     * @param path
     * @return
     */
    public static boolean isFolder(Path path) {
        return Files.isDirectory(path);
    }

    // Does the file exist
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    // Last modification time of the file
    public static long getLastModifiedTime(Path path) throws IOException {
        return Files.getLastModifiedTime(path).to(TimeUnit.SECONDS);
    }

    /**
     * initialization cell initialization
     * 	   cell Original content length limit 32767  Original content length limitInteger.MAX_VALUE
     */
    public static   void initCellMaxTextLength() {
        SpreadsheetVersion excel2007 = SpreadsheetVersion.EXCEL2007;
        if (Integer.MAX_VALUE != excel2007.getMaxTextLength()) {
            Field field;
            try {
                field = excel2007.getClass().getDeclaredField("_maxTextLength");
                field.setAccessible(true);
                field.set(excel2007,Integer.MAX_VALUE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    public static void main(String[] args) {
        System.out.println(readableFileSize(16564724626L));

    }

}
