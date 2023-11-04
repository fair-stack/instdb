package cn.cnic.instdb.model.commentNotice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/28/14:27
 * @Description:comment
 */

@Data
public class CommentVo {

    private String id;
    private String emailAccounts;
    private String username;
    private String avatar;
    private String content;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<CommentVo> children;





}
