package cn.cnic.instdb.model.system;

import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/19/20:04
 * @Description:
 */

@Data
public class Template {


    //Template Name
    private String templateName;
    //Template Description
    private String templateDesc;


    //Templateauthor
    private String templateAuthor;

    //Templateauthor
    private String version;

    private List<Group> group;


    /**
     * Metadata information
     */
    @Data
    public static class Resource {
        //Correspondingkey
        private String name;
        //Chinesetitle
        private String title;
        private String placeholder;
        private String type;
        private String check;
        private String url;
        //1:* 1To many To many   0:1 0To many1 To many  1:1 To many
        private String multiply;
        // iri url
        private String iri;

        private String language;

        private Object value;
        //Generally used for formatting restrictions on dates
        private String formate;

        //Corresponding children
        private List<Options> options;

        private List<Options> operation;

        //inquiry inquiry
        private String mode;

        private List<Options> show;
    }

    /**
     * options
     */
    @Data
    public static class Options {
        private String name;
        private String url;
        private String title;
        private String type;
        private String formate;
        private String mode;
        private String placeholder;
        private List<Children> children;
    }

    @Data
    public static class Group {
        private String name;
        private String desc;
        //data set
        private List<Resource> resources;
    }



    /**
     * children
     */
    @Data
    public static class Children {
        private String name;
        private String title;
        private String type;
        private String formate;
        private String placeholder;
        private List<Options> options;
        private Object value;
        private String multiply;
    }

}
