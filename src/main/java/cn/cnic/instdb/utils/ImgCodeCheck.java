package cn.cnic.instdb.utils;

import cn.cnic.instdb.constant.Constant;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;

/**
 * Verification code verification
 */
public class ImgCodeCheck {

    private final static Cache<String, String> captchaCache = CaffeineUtil.getCaptcha();


    public static int  checkCode(String code, String randomStr) {
        if (StringUtils.isBlank(code)) {
            //throw new RuntimeException("The verification code cannot be empty");
            return 400;
        }
        if (StringUtils.isBlank(randomStr)) {
            // throw new RuntimeException("The verification code is illegal");
            return 403;
        }
        String key = Constant.DEFAULT_CODE_KEY + randomStr;
        String result = captchaCache.getIfPresent(key);
       // captchaCache.invalidate(key);
        if (!code.equalsIgnoreCase(result)) {
            // throw new RuntimeException("The verification code is illegal");
            return 403;
        }
        return 200;
    }


}
