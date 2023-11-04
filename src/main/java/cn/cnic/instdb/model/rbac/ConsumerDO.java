package cn.cnic.instdb.model.rbac;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description User Model
 * @author jmal
 */
@Data
@Document(collection = "db_user")
public class ConsumerDO {

    @Id
    private String id;

    @ApiModelProperty(name = "emailAccounts", value = "Email account", example = "admin", required = true)
    private String emailAccounts;

    @ApiModelProperty(name = "name", value = "name", example = "name**", required = true)
    private String name;

    @ApiModelProperty(name = "sex", value = "Gender", required = true)
    private String sex;

    @ApiModelProperty(name = "englishName" ,value = "English name" , example = "")
    private String englishName;

    @ApiModelProperty(name = "password", value = "password", example = "123456")
    private String password;

    @ApiModelProperty(value = "Avatar", example = "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif")
    private String avatar;


    @ApiModelProperty(name = "orgChineseName" ,value = "Chinese name of institution" , example = "")
    private String orgChineseName;

    @ApiModelProperty(name = "orgEnglishName" ,value = "Institution English Name" , example = "")
    private String orgEnglishName;

    @ApiModelProperty(name = "orcId" ,value = "ORCID" , example = "")
    private String orcId;

    @ApiModelProperty(name = "telephone" ,value = "Mobile phone number" , example = "")
    private String telephone;

    @ApiModelProperty(name = "IDNumber" ,value = "ID number" , example = "")
    private String IDNumber;

    @ApiModelProperty(name = "slogan", value = "slogan")
    private String slogan;

    @ApiModelProperty(name = "introduction", value = "brief introduction")
    private String introduction;


    @ApiModelProperty(name = "roles", value = "roleIdrole")
    private List<String> roles;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(name = "createTime", value = "Creation time")
    private LocalDateTime createTime;

    @ApiModelProperty(name = "updateTime", value = "Modification time", hidden = true)
    private LocalDateTime updateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(name = "createTime", value = "Next available login time")
    private LocalDateTime nextLoginTime;

    private String wechatId;   //WeChat binding user ID

    @ApiModelProperty(name = "state",value = "Is the user activated(0Is the user activated 1Is the user activated 2Is the user activated  3Is the user activated)")
    private int state;

    @ApiModelProperty(name = "spareEmail", value = "Alternate email")
    private String spareEmail;

    private String addWay;       //Add Method

}
