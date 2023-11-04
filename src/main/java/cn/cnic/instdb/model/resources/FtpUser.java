package cn.cnic.instdb.model.resources;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * @Auther: wdd
 * @Date: 2021/06/07/15:58
 * @Description:
 */

@Data
@Document(collection = "ftp_user")
public class FtpUser {
    @Id
    private String id;
    private String resourcesId;
    private String auth;
    private String username;
    private String password;
    private String homedirectory; //ftpHome directory
    private String realUsers; // Real users  Real users
    private LocalDateTime createTime;
}
