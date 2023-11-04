package cn.cnic.instdb.model.rbac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ConsumerInfoDTO {

    private String userId;

    @ApiModelProperty(name = "emailAccounts", value = "Email account", example = "admin", required = true)
    private String emailAccounts;

    @ApiModelProperty(name = "name", value = "name", example = "name**", required = true)
    private String name;
    private String avatar;

    @ApiModelProperty(name = "englishName" ,value = "English name" , example = "")
    private String englishName;

    @ApiModelProperty(name = "orgChineseName" ,value = "Chinese name of institution" , example = "")
    private String orgChineseName;

    @ApiModelProperty(name = "orgEnglishName" ,value = "Institution English Name" , example = "")
    private String orgEnglishName;

    @ApiModelProperty(name = "orcId" ,value = "ORCID" , example = "")
    private String orcId;

    @ApiModelProperty(name = "telephone" ,value = "Mobile phone number" , example = "")
    private String telephone;

    @ApiModelProperty(name = "introduction", value = "brief introduction")
    private String introduction;
}
