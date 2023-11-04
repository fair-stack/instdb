package cn.cnic.instdb.model.commentNotice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/03/28/14:27
 * @Description:comment
 */

@Data
@Document(collection = "comment")
public class Comment {

    @Id
    private String id;
    private String emailAccounts;
    private String username;
    private String avatar;
    private String resourcesId;
    private String content;
    //Define the relationship between comments and replies as parent-child level Define the relationship between comments and replies as parent-child level，Define the relationship between comments and replies as parent-child level，Define the relationship between comments and replies as parent-child level【-1】，Define the relationship between comments and replies as parent-child level
    private String parentId;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;



}
