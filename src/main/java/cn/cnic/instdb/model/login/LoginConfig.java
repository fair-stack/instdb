package cn.cnic.instdb.model.login;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 *  Technology Cloud Account Configuration
 */
@Data
@Document(collection = "login_conf")
public class LoginConfig {
    @Id
    private String id;
    private String appKey;
    private String appSecret;
    private String indexPage;
    private String callback;

    private Boolean isOpen;
    private Date createTime;
    private Date lastUpdateTime;

}
