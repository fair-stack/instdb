package cn.cnic.instdb.model.resources;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/03/17/19:17
 * @Description:
 */

@lombok.Data
public class Resources {
    //version
    private String version;

    //resourcesIdUnique tags for resources Unique tags for resources
    private String rootId;

    //Resource Type
    private String resourceType;

    //Metadata template name
    private String templateName;

    //Metadata information
    private JSONObject metadata;

    //Approval callback and address modification
    private CallbackUrl callbackUrl;

    //Release institutional information
    private Organization organization;

    //Publisher Information
    private Publish publish;

    //0  File Publishing  1 File Publishing   2 File Publishing+File Publishing
    private Map<String,Object> dataType;
    //Yes No zip   yes /no
    private String fileIsZip;

    @lombok.Data
    public static class Organization {
        private String id;
        private String name;
    }

    @lombok.Data
    public static class Publish {
        private String name;
        private String email;
        private String org;
    }

    @lombok.Data
    public static class CallbackUrl {
        private String onSuccess;
        private String onUpdate;
    }



}
