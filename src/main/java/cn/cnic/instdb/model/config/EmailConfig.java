package cn.cnic.instdb.model.config;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Auther: wdd
 * @Date: 2021/04/10/01:37
 * @Description:
 */

@Data
@Document(collection = "email_config")
public class EmailConfig {

    @Id
    private String id;
    //casdc   other
    private String type;
    private String host;
    private int port;
    private String protocol;
    private String from;
    private String username;
    private String password;
    //This stores passwords for other email addresses
    private String passwordOther;

}
