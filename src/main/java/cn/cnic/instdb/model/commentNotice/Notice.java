package cn.cnic.instdb.model.commentNotice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/03/28/14:27
 * @Description:notice
 */

@Data
@Document(collection = "notice")
public class Notice {

    private String id;
    private String username;
    private String type;
    private String title;
    private String titleEn;
    private String content;
    private String contentEn;

    //0Read 1Read
    private String is_read;

    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Mainly for the convenience of directly approving when prompted for approval
    private String resourcesId;


}
