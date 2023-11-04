package cn.cnic.instdb.model.login;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 *  casAccount configuration
 */
@Data
@Document(collection = "login_conf_cas")
public class LoginConfigCas {
    @Id
    private String id;
    private String casServerUrl;
    private String casServerUrlLogin;
    private String casServerUrlLogoutUrl;
    private String homePage;
    private String username;
    private String name;

    private Boolean isOpen;
    private Date createTime;
    private Date lastUpdateTime;

}
