package cn.cnic.instdb.model.rbac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Description User Model Transfer Object
 * @author jmal
 */
@Data
public class ConsumerDTO {

    @NotNull(message = "The email account cannot be empty")
    @ApiModelProperty(name = "emailAccounts", value = "Email account", example = "admin", required = true)
    String emailAccounts;

    @NotNull(message = "Name cannot be empty")
    @ApiModelProperty(name = "name", value = "name", example = "name**", required = true)
    String name;

    String orgChineseName;

    @NotNull(message = "Password cannot be empty")
    @ApiModelProperty(name = "password", value = "password", example = "123456")
    String password;

    @NotNull(message = "Confirm password cannot be empty")
    @ApiModelProperty(name = "confirmPassword", value = "password", example = "123456")
    String confirmPassword;

    String randomStr;
    String captcha;

}
