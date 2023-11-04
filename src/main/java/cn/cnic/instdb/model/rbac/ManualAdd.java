package cn.cnic.instdb.model.rbac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *  Administrator adds user model
 */

@Data
public class ManualAdd {

    private String userId;

    @NotNull(message = "The email account cannot be empty")
    @ApiModelProperty(name = "emailAccounts", value = "Email account", example = "admin", required = true)
    String emailAccounts;

    @NotNull(message = "Name cannot be empty")
    @ApiModelProperty(name = "name", value = "name", example = "name**", required = true)
    String name;

    @NotNull(message = "Unit cannot be empty")
    @ApiModelProperty(name = "orgChineseName", value = "Chinese name of institution", example = "")
    String orgChineseName;

    @NotNull(message = "The role cannot be empty")
    @ApiModelProperty(name = "roles", value = "rolelogo")
    List<String> roles;

}
