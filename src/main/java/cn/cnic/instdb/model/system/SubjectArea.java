package cn.cnic.instdb.model.system;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/04/09/23:27
 * @Description:
 */

@Data
@Document(collection = "subject_area")
public class SubjectArea {


    @Id
    private String id;

    private String name;
    private String nameEn;
    private String desc;
    private String descEn;

    private String sort;

    private List<String> subject;
    private List<String> subjectEn;

    private String logo;
    private String icon;
    private String iconColor ;

    private long resourcesNum ;


    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //Update time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
