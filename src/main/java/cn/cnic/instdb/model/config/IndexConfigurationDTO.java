package cn.cnic.instdb.model.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/04/10/01:37
 * @Description:
 */

@Data
public class IndexConfigurationDTO {

    private List<PathInfo> banaerLogo;
    private String name;
    private String name_en;

    private String dsUrl;

    //Homepage copy
    private String contact;
    private String contact_en;

    //Special Introduction
    private String SpecialName;
    private String SpecialName_en;

    @Data
    public static class PathInfo {
        private String path;
        private String sort;
    }

}
