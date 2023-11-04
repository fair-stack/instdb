package cn.cnic.instdb.model.center;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 *  Cstr Registered entity
 */

@Data
public class CSTR {

    @NotBlank(message = "Chinese name Chinese name")
    private String titleZh;           //Chinese name

    @NotBlank(message = "English name English name")
    private String titleEn;           //English name

   // private String cstr;     Annotated the general center queryset         //CSTR

    @NotBlank(message = "Institution codeid Institution code")
    private String serviceOrgId;            //Institution codeid

    @NotBlank(message = "Resource Type Resource Type")
    private String resourceType;      //Resource Type

    @NotBlank(message = "keyword keyword")
    private String keywords;          //keyword

    @NotBlank(message = "Descriptive information Descriptive information")
    private String description;       //Descriptive information

    @NotBlank(message = "Resource Generation Date Resource Generation Date")
    private String resourceDate;      //Resource Generation Date

    @NotBlank(message = "Resource information link address Resource information link address")
    private String url;               //Resource information link address

    @NotBlank(message = "Sharing pathways Sharing pathways")
    private String shareChannel;      //Sharing pathways

    @NotBlank(message = "Shared Scope Shared Scope")
    private String shareRange;        //Shared Scope

    @NotBlank(message = "Application process Application process")
    private String process;           //Application process

    @NotBlank(message = "Discipline classification Discipline classification")
    private String categoryName;      //Discipline classification

    @NotBlank(message = "Topic classification Topic classification")
    private String standardName;      //Topic classification

    private String account;
    private String password;
}
