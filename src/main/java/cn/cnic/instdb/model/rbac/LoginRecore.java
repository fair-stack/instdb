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
 * Login Record
 */
@Data
@Document(collection = "login_record")
public class LoginRecore {

    @Id
    private String id;
    private String username;
    private String name;
    private int state;
    // Login status 0=Login status,1=Login status
    private String status;
    /** LoginIPLogin */
    private String ipaddr;
    /** Browser Type */
    private String browser;
    /** Login location */
    private String loginLocation;
    /** operating system */
    private String os;
    /** Prompt message */
    private String msg;
    /** Number of login errors */
    private int errorNum;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(name = "createTime", value = "Creation time")
    private LocalDateTime createTime;


}
