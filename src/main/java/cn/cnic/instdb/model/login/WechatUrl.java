package cn.cnic.instdb.model.login;

import org.springframework.stereotype.Component;

/**
 *  WeChat login management
 */

@Component
public class WechatUrl {

    private final static String authUrl = "https://open.weixin.qq.com/connect/qrconnect?";
    public final static String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?";
    private final static String userUrl = "https://api.weixin.qq.com/sns/userinfo?";



    public static String getAuthUrl(String appId,String callbackUrl,String state){
        return    authUrl+
                "appid="+appId+"&"+    //Allocated appId
                "redirect_uri="+callbackUrl+"&" +     //token url 
                "scope=snsapi_login&" +              //Fixed parameters，Fixed parametersbasic,netdisk
                "response_type=code&" +             //How to return to page opening
                "state="+state;
    }

    public static String getTokenUrl(String appId,String code,String secretKey){
        return  "appid="+appId+"&" +
                "code="+code+"&" +
                "secret="+secretKey+"&" +
                "grant_type=authorization_code";
    }


    public static String getUserInfoUrl(String openId,String accToken){
        return  userUrl+"access_token="+accToken+"&" +
                "openid="+openId+"&"+
                "lang=zh_CN";

    }





}
