package cn.cnic.instdb.model.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/04/10/01:37
 * @Description:
 */

@Data
@Document(collection = "index_config")
public class IndexConfiguration {

    private String id;
    private List<PathInfo> banaerLogo;
    private String name;
    private String name_en;
    //dsJump address for
    private String dsUrl;

    //Homepage copy
    private String contact;
    private String contact_en;

    //Special Introduction
    private String SpecialName;
    private String SpecialName_en;

    //Update time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //Modified by
    private String updateByUser;

    @Data
    public static class PathInfo {
        private String path;
        private String sort;
    }

}
