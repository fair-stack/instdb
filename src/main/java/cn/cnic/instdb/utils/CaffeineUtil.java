package cn.cnic.instdb.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CaffeineUtil
 *
 * @author jmal
 */
@Component
public class CaffeineUtil {

    private static Cache<String,String>  CHECK;          //Short time operation verification

    private static Cache<String, List<String>> emailToken; //user+token

    private static Cache<String,String>  captcha;          //Verification code


    private static Cache<String,Object>  searchConfig;          //Advanced search cache on homepage

    /***
     * usertoken
     */
    private static Cache<String, String> tokenCache;

    /***
     * User identity permission cache
     * key: username
     * value: Permission identification list
     */
    private final static Cache<String, List<String>> AUTHORITIES_CACHE = Caffeine.newBuilder().build();

    /***
     * cacheuserId
     * key: username
     * value: userId
     */
    private final static Cache<String, String> USER_ID_CACHE = Caffeine.newBuilder().build();

    //Other system configurations (Other system configurations)
    private static Cache<String,Object> config;


    private static Cache<String,Integer> error_pwd;     //Password error count verification

    private static Cache<String,String> error_pwd_check;     //Password error lock24Password error lock


    private  static Cache<String, String> THIRD_PARTY;       //Third party information caching


    public static List<String> getAuthoritiesCache(String username) {
        return AUTHORITIES_CACHE.getIfPresent(username);
    }

    /***
     * Does this exist in the cacheusernamDoes this exist in the cache
     * @param username
     * @return
     */
    public static boolean existsAuthoritiesCache(String username) {
        return AUTHORITIES_CACHE.getIfPresent(username) != null;
    }

    public static void setAuthoritiesCache(String username, List<String> authorities) {
        AUTHORITIES_CACHE.put(username, authorities);
    }

    public static void removeAuthoritiesCache(String username) {
        AUTHORITIES_CACHE.invalidate(username);
    }

    public static String getUserIdCache(String username) {
        return USER_ID_CACHE.getIfPresent(username);
    }

    public static void setUserIdCache(String username, String userId) {
        USER_ID_CACHE.put(username, userId);
    }

    public static void removeUserIdCache(String username) {
        USER_ID_CACHE.invalidate(username);
    }

    @PostConstruct
    public void initCache(){
        initMyCache();
    }

//    expireAfterAccess :After each read，After each read
//    expireAfterWrite:After writing，After writing，After writing
    public static void initMyCache(){
        if(tokenCache == null){
            tokenCache = Caffeine.newBuilder().expireAfterAccess(3, TimeUnit.HOURS).build();
        }
        if(emailToken == null){
            emailToken = Caffeine.newBuilder().expireAfterAccess(3, TimeUnit.HOURS).build();
        }
        if(config == null){
            config = Caffeine.newBuilder().expireAfterWrite(6, TimeUnit.HOURS).build();
        }
        if(error_pwd == null){
            error_pwd = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
        }
        if(error_pwd_check == null){
            error_pwd_check = Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();
        }
        if(CHECK == null){
            CHECK = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
        }
        if(captcha == null){
            captcha = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
        }

        if(searchConfig == null){
            searchConfig = Caffeine.newBuilder().expireAfterAccess(3, TimeUnit.DAYS).build();
        }

        if(THIRD_PARTY == null){
            THIRD_PARTY = Caffeine.newBuilder().expireAfterAccess(3, TimeUnit.HOURS).build();
        }
    }

    public static Cache<String, List<String>> getEmailToken (){
        if(emailToken == null){
            initMyCache();
        }
        return emailToken;
    }

    public static Cache<String,String> getThirdParty(){
        if(THIRD_PARTY == null){
            initMyCache();
        }
        return THIRD_PARTY;
    }

    public static Cache<String,String> getCHECK(){
        if(CHECK == null){
            initMyCache();
        }
        return CHECK;
    }

    public static Cache<String, String> getTokenCache(){
        if(tokenCache == null){
            initMyCache();
        }
        return tokenCache;
    }

    public static Cache<String,Object> getConfig(){
        if(config == null){
            initMyCache();
        }
        return config;
    }

    public static Cache<String,Integer> getErrorPwd(){
        if(error_pwd == null){
            initMyCache();
        }
        return error_pwd;
    }
    public static Cache<String,String> getErrorPwdCheck(){
        if(error_pwd_check == null){
            initMyCache();
        }
        return error_pwd_check;
    }

    public static Cache<String,String> getCaptcha(){
        if(captcha == null){
            initMyCache();
        }
        return captcha;
    }

    public static Cache<String,Object> getSearchConfig(){
        if(searchConfig == null){
            initMyCache();
        }
        return searchConfig;
    }


}
