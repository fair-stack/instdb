package cn.cnic.instdb.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wdd
 * @version 1.0
 * @date 2021/11/29 14:57
 */
public class FileCounter {
    //Used to store every file found
    ArrayList<File> fileList;
    //root directory
    File root;

    public Map getFileInfo(String pathName, List<String> fileFilter) {
        Map<String, Object> map = new HashMap<>();
        root = new File(pathName);
        fileList = new ArrayList<>();
        ArrayList arrayList = countFiles(fileFilter);
        //default-1
        map.put("fileCount", arrayList.size());
        lengthFiles(map);
        return map;
    }

    //Recursive algorithm for finding files
    public ArrayList countFiles(List<String> fileFilter) {
        File[] files = root.listFiles();
        int length = files.length;
        for (int i = 0; i < length; i++) {
            //Filter out files
            if(fileFilter.contains(files[i].getName())){
           // if(fileFilter.equals(files[i].getName())){
                continue;
            }
            if (files[i].isDirectory()) {
                root = files[i];
                countFiles(fileFilter);
            } else {
                fileList.add(files[i]);
            }
        }

        return fileList;
    }

    //Count the number of file formats and total size
    public void lengthFiles( Map<String, Object> map) {
        long totalSize = 0;
        HashMap<String, Integer> mapSuffix = new HashMap<>();
        HashMap<String, Long> mapSuffixStorageNum = new HashMap<>();
        for (int i = 0; i < fileList.size(); i++) {
            //Returns the length of this file，Returns the length of this file
            totalSize += fileList.get(i).length();

            //Obtain the type of file
            String suffix = FileUtils.getSuffixByFileName(fileList.get(i).getName());
            if (mapSuffix.containsKey(suffix)) {
                //Determine if the set already exists，Determine if the set already exists+1
                mapSuffix.put(suffix, mapSuffix.get(suffix) + 1);
                mapSuffixStorageNum.put(suffix, mapSuffixStorageNum.get(suffix) + fileList.get(i).length());
            } else {
                //There is no setting for the first time
                mapSuffix.put(suffix, 1);
                mapSuffixStorageNum.put(suffix, fileList.get(i).length());
            }
        }
        map.put("suffixStorageNum", mapSuffixStorageNum);
        map.put("storageNum", 0 > totalSize ? 0 : totalSize);
        map.put("fileFormat", mapSuffix);
    }


    public static void main(String[] args) {
        FileCounter counter = new FileCounter();
        List<String> fileFilter = new ArrayList<>();
        Map fileInfo = counter.getFileInfo("D:\\wdd_work\\Personal Documents",fileFilter);
        System.out.println(fileInfo);
    }



}
