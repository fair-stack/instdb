//package cn.cnic.instdb.result;
//
//import org.springframework.context.i18n.LocaleContextHolder;
//
//import java.util.Locale;
//
///**
// * status enum
// */
//public enum Status {
//    SUCCESS(200, "success", "success"),
//    FAILED(500, "{0}", "{0}"),
//
//    ;
//
//    private final Integer code;
//    private final String enMsg;
//    private final String zhMsg;
//
//    Status(Integer code, String enMsg, String zhMsg) {
//        this.code = code;
//        this.enMsg = enMsg;
//        this.zhMsg = zhMsg;
//    }
//
//    public Integer getCode() {
//        return this.code;
//    }
//
//    public String getMsg() {
//        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
//            return this.zhMsg;
//        } else {
//            return this.enMsg;
//        }
//    }
//}
