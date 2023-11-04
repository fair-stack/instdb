package cn.cnic.instdb.model.rbac;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ManualAddList {

    @NotNull(message = "Unit cannot be empty")
    @ApiModelProperty(name = "org", value = "unit", example = "*****")
    private String org;

    @NotNull(message = "The role cannot be empty")
    @ApiModelProperty(name = "role", value = "role", example = "*****")
    private String role;


    @NotNull(message = "User information cannot be empty")
    private List<Person> person;


    @Data
    public static class Person {

        @NotNull(message = "Email cannot be empty")
        @ApiModelProperty(name = "email", value = "mailbox", example = "*****")
        private String email;

        @NotNull(message = "Name cannot be empty")
        @ApiModelProperty(name = "name", value = "name", example = "*****")
        private String name;
    }

}
