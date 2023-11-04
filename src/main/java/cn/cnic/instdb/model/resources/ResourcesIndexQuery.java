package cn.cnic.instdb.model.resources;

import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description:
 */

@Data
public class ResourcesIndexQuery {

    private String titleZh;
    //Resource Type
    private List<String> resourceType;

    //Privacy Policy
    private List<String> privacyPolicy;

    private List<String> keywordZh;

    //subject
    private List<String> subject;

    private String pageOffset;
    private String pageSize;



}
