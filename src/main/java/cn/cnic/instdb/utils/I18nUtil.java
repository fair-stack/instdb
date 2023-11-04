package cn.cnic.instdb.utils;

import cn.cnic.instdb.constant.Constant;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author wdd
 * @version 1.0
 * @date 2022/2/10 10:44
 */
@Component
public class I18nUtil {

    private static MessageSource messageSource;

    private static final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    public I18nUtil(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /**
     * Obtain a single international translation value
     */
    public static String get(String msgKey) {
        try {
            Locale locale = null;
            String lang = tokenCache.getIfPresent("lang");
            if (null == lang || Constant.Language.chinese.equals(lang) || StringUtils.isBlank(lang)) {
                locale = Locale.CHINA;
            } else if (Constant.Language.english.equals(lang)) {
                locale = Locale.US;
            } else {
                locale = Locale.CHINA;
            }
            return messageSource.getMessage(msgKey, null, locale);
        } catch (Exception e) {
            return msgKey;
        }
    }
}
