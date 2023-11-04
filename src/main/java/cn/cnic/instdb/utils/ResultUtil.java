//package cn.cnic.instdb.utils;
//
///**
// * ResultUtil
// *
// * @author jmal
// */
//public class ResultUtil {
//
//    private ResultUtil() {
//        throw new IllegalStateException("Utility class");
//    }
//
//    public static <T> ResponseResult<T> success(T data) {
//        ResponseResult<T> result = new ResponseResult<>();
//        result.setCode(200);
//        result.setMessage("success");
//        result.setData(data);
//        return result;
//    }
//    public static <T> ResponseResult<T> success(String message,T data) {
//        ResponseResult<T> result = new ResponseResult<>();
//        result.setCode(200);
//        result.setMessage(message);
//        result.setData(data);
//        return result;
//    }
//
//    public static <T> ResponseResult<T> success(String message) {
//        ResponseResult<T> result = new ResponseResult<>();
//        result.setCode(200);
//        result.setMessage(message);
//        return result;
//    }
//
//    public static <T> ResponseResult<T> success() {
//        ResponseResult<T> result = new ResponseResult<>();
//        result.setCode(200);
//        result.setMessage("success");
//        return result;
//    }
//
//    public static <T> ResponseResult<T> error(int code, String msg) {
//        ResponseResult<T> result = new ResponseResult<>();
//        result.setCode(code);
//        result.setMessage(msg);
//        return result;
//    }
//
//    public static <T> ResponseResult<T> error(String msg) {
//        ResponseResult<T> result = new ResponseResult<>();
//        result.setCode(500);
//        result.setMessage(msg);
//        return result;
//    }
//
//
//
//
//
//
//
//}
