package cn.cnic.instdb.utils;

import cn.cnic.instdb.model.resources.ResourceFileTree;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

/**
 * @author wdd
 * @version 1.0
 * @date 2021/11/29 14:57
 */
public class FileTree {
    //Used to store every file found
    List<ResourceFileTree> node=new LinkedList();

    /**
     * Reading the file directory returns a tree structure
     * @param path File Path
     * @param id Each record'sid
     * @param pid fatherid
     * @param resourcesId Data resourceid
     * @param fileFilter Filter out a named file
     * @return
     */
    private  List<ResourceFileTree> getFile(String path, int id, int pid, String resourcesId,List<String> fileFilter) {
        File file = new File(path);
        if(file.exists()) {
            File[] array = file.listFiles();
            List fileList = Arrays.asList(array);
            //Sort the local folders read
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1.isDirectory() && o2.isFile()){
                        return -1;
                    }
                    if (o1.isFile() && o2.isDirectory()){
                        return 1;
                    }
                    return o1.getName().compareTo(o2.getName());
                }
            });

            for (int i = 0; i < array.length; i++) {
                ResourceFileTree resourceFileTree = new ResourceFileTree();
                //Filter files
                if(null != fileFilter && fileFilter.contains(array[i].getName())){
                    continue;
                }
                resourceFileTree.setResourcesId(resourcesId);
                resourceFileTree.setPid(pid);
                resourceFileTree.setTreeId(id);
                resourceFileTree.setFilePath(array[i].getPath());
                resourceFileTree.setFileName(array[i].getName());
                resourceFileTree.setIsFile(array[i].isFile());
                resourceFileTree.setExpanded(false);
                //Determine if it is a folder，Determine if it is a folder
                if (array[i].isDirectory()) {
                    resourceFileTree.setSize((long) array[i].list().length);
                    node.add(resourceFileTree);
                    //Recursive，RecursivepidRecursiveid
                    getFile(array[i].getPath(), id * 10 + 1 + i, id,resourcesId,fileFilter);
                    id++;
                } else {
                    resourceFileTree.setSize(array[i].length());
                    node.add(resourceFileTree);
                    id++;
                }
            }
        }
        return node;
    }
    public List<ResourceFileTree> getFileTree(String resourcesId, String path,List<String> fileFilter) {
        node.removeAll(node);
        FileTree counter = new FileTree();
        int level=0;
        List<ResourceFileTree> file = counter.getFile(path, 1, level,resourcesId,fileFilter);
        return file;
    }


    public static void main(String[] args) {
        FileTree counter = new FileTree();
        List<String> fileFilter = null;
        List<ResourceFileTree> fileResourceFileTree = counter.getFileTree("5asd5as6d8asdas4dqw873e4", "D:\\wdd_work\\Learning materials",fileFilter);

        System.out.println(fileResourceFileTree.size());
        System.out.println(fileResourceFileTree);
    }


}
