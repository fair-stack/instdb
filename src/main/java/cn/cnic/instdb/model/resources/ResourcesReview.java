package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/06/07/15:58
 * @Description:
 */

@Data
@Document(collection = "resources_review")
public class ResourcesReview {
    @Id
    private String id;
    private String resourcesId;
    private String approvalId;
    //Expert'sid
    private String expertId;
    private String username;
    private String email;
    private String org;
    private String url;
    //Evaluation status
    private String status;
    //Evaluation opinions
    private String reason;
    //Evaluation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
    //Is the link invalid
    private String deadlineStatus;
    //Calculate if the latest failure record is available  Calculate if the latest failure record is available
    private String invitable;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
