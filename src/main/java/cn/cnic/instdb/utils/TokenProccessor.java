package cn.cnic.instdb.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Auther: wdd
 * @Date: 2021/08/23/15:09
 * @Description:
 */
@Slf4j
public class TokenProccessor {

    private TokenProccessor(){}
    private static TokenProccessor instance = new TokenProccessor();
    public static TokenProccessor tokenProccessor(){
        return instance;
    }

    /**
     * Generate based on authorization codetoken
     * @param secretKey
     * @return
     */
    public static String generateTokeCode(String secretKey){
        //current time 
        long stringTime = System.currentTimeMillis();
        return SMS4.Encryption(secretKey + "&" + stringTime);

    }

    /**
     * Authorization code parsing verification
     * @param Authorization
     * @param token
     * @return
     */
    public static int decryptTokeCode(String Authorization, String token){

        String decrypt = SMS4.Decrypt(Authorization);
        if(decrypt == null){
            return 503;
        }
        if(!decrypt.contains("&")){
            return 503;
        }
        String[] split = decrypt.split("&");
        String code = split[0];
        String time = split[1];
        if(!code.equals(token)){
            return 503;
        }
        if(time.trim().equals("")){
            return 503;
        }
        Long activationTime = Long.valueOf(time);
        if(activationTime.longValue() < DateUtils.yesterday(2)){
            log.error(decrypt + ": Authorization code generationtokenAuthorization code generation");
            return 502;
        }
        return 200;
    }

    public static void main(String[] args) {
       // System.out.println(processor.generateTokeCode("ssss"));
        //System.out.println(processor.decryptTokeCode(processor.generateTokeCode("ssss").getData().toString(), "ssss"));
        System.out.println(TokenProccessor.decryptTokeCode("ssss", "22222"));
    }



}
