package cn.cnic.instdb.model.openApi;

import lombok.Data;
import org.springframework.data.annotation.Id;


/**
 * @Auther  wdd
 * @Date  2021/9/9 18:58
 * @Desc  Authorization Code Management
 */
@Data
public class SecretKeyDTO {

    @Id
    private String id ;
    //Authorization code
    private String value ;
    private String organId;
    private String organName;
    private String applicationName;

}
