package cn.cnic.instdb.model.findata;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:
 */

@Data
public class PushFinDatasParamVo {

    //Recommended cycle  //executeOnce Recommended cycle day Recommended cycle  week Recommended cycle  monthRecommended cycle closeRecommended cycle //Recommended cycle manual
    private String type;

    //Resource Type
    private List<String>   resourceType;

    //Privacy Policy
    private  List<String>  privacyPolicy;

    //license agreement
    private List<String>  license;


    private   List<String> keywords;

    //specialid
    private   List<String> special;

    //subject
    private List<String> subject;


    //subject
    private List<String> year;

    //Whether to execute
    private String run;

}
