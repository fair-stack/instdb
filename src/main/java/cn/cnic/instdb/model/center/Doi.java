package cn.cnic.instdb.model.center;

import lombok.Data;

/**
 *  Doi Registered entity
 */

@Data
public class Doi {

    //Chinese Title
    private String[] titles;
    //creator
    private String[] creators;

    //Subtitle/Subtitle
   // private String subtitle;

    //Generated by the main center
   // private String doi;

    //brief introduction
   // private String description;

    //Publishing Institution Name
    private String publisher;

    //Year of publication
    private String publicationYear;
    private String resourceTypeGeneral;

    //Resource Access Link
    private String url;

    private String account;
    private String password;

    //Architecture version
    private String schemaVersion;
    //Responsible person information Responsible person information
  //  private String organization;




}
