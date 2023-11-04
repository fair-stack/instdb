package cn.cnic.instdb.model.login;

import org.springframework.stereotype.Component;

@Component
public class EscUrl {

    private final static String authUrl = "https://oauth.escience.org.cn/oauth/authorize?";
    public final static String tokenUrl = "https://oauth.escience.org.cn/oauth/token?";

    public final static String userUrl = "https://api.escience.org.cn/admin/user/info/me";

    public final static String logoutUrl = "https://oauth.escience.org.cn/token/logout";



    public static String getAuthUrl(String clientId,String callbackUrl){
        return    authUrl+
                 "client_id="+clientId+"&"+    //Allocated Allocatedid
                 "redirect_uri="+callbackUrl+"&" +     //token url 
                 "scope=all&" +
                 "response_type=code";

    }

    public static String getTokenUrl(String clientId,String code,String secretKey,String callback){
        return  "client_id="+clientId+"&" +
                "client_secret="+secretKey+"&" +
                "redirect_uri="+callback+"&" +     //token url 
                "code="+code+"&" +
                "scope=all&" +
                "grant_type=authorization_code";
    }


}
