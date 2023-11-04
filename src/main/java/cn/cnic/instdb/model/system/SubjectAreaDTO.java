package cn.cnic.instdb.model.system;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/04/09/23:27
 * @Description:
 */

@Data
public class SubjectAreaDTO {

    @Id
    private String id;

    private String name;
    private String nameEn;

    private String desc;
    private String descEn;
    private String logo;
    private String icon;
    private String iconColor ;


    //sort sort
    private String sort;

    private List<String> subject;

}
