package cn.cnic.instdb.model.resources;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
* @Auther  wdd
* @Date  2021/9/9 18:58
* @Desc  File structure information
*/
@Data
@Document(collection = "resource_file_tree")
public class ResourceFileTree {
    @Id
    private String id;
    private int treeId;
    private int pid;
    private String resourcesId;
    private String fileName;
    private String filePath;
    private Long size;
    private Boolean isFile;
    private Date createTime;
    private boolean expanded = false;
}
