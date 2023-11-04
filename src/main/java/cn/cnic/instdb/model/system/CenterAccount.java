package cn.cnic.instdb.model.system;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
* @Auther  wdd
* @Date  2021/9/9 18:58
* @Desc  Account configuration of the main center
*/
@Data
@Document(collection = "center_account")
public class CenterAccount {

    @Id
    private String id ;
    //Account password assigned by the main center
    private String username;
    private String password;
    //The domain name of this system orip
    private String host;
    private String cstr;
    private String clientId;
    private String secret;
    private String cstrCode;
    private int cstrLength;
    //Join the network or not(Join the network or not)
    private boolean isNetwork;

    //Organization of this system
    private String orgId;
    private String orgName;

    //Bind publishing institutionsid
    private String publisherId;

    //Do you want to customize the date(Do you want to customize the datedatePublishedDo you want to customize the date wdd)
    private String isCustomDate;


    //doiRelated configurations
    private String doiType;
    private String repositoryID;
    private String doiPassword;
    private String doiPrefiex;
    private String doiCode;
    private int doiLength;
}
