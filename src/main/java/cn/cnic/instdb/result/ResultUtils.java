package cn.cnic.instdb.result;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.utils.I18nUtil;

import java.util.Map;

/**
 * base controller
 */
public class ResultUtils {

    /**
     * success
     *
     * @return success result code
     */
    public static Result success() {
        Result result = new Result();
        result.setCode(Constant.StatusCode.SUCCESS);
        result.setMessage(I18nUtil.get(Constant.StatusMsg.SUCCESS));

        return result;
    }

    /**
     * success does not need to return data
     *
     * @param msg success message
     * @return success result code
     */
    public static Result success(String msg) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.SUCCESS);
        result.setMessage(I18nUtil.get(msg));

        return result;
    }

    public static Result successOld(String msg) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.SUCCESS);
        result.setMessage(msg);
        return result;
    }


    /**
     * success does not need to return data
     *
     * @param msg success message
     * @return success result code
     */
    public static Result successAfter(String msg,String info) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.SUCCESS);
        result.setMessage(I18nUtil.get(msg)+info);
        return result;
    }

    /**
     * return data no paging
     *
     * @param msg  success message
     * @param list data list
     * @return success result code
     */
    public static Result success(String msg, Object list) {
        return getResult(msg, list);
    }

    /**
     * return data no paging
     *
     * @param list success
     * @return success result code
     */
    public static Result success(Object list) {
        return getResult(Constant.StatusMsg.SUCCESS, list);
    }

    /**
     * return the data use Map format, for example, passing the value of key, value, passing a value
     * eg. "/user/add"  then return user name: zhangsan
     *
     * @param msg    message
     * @param object success object data
     * @return success result code
     */
    public static Result success(String msg, Map<String, Object> object) {
        return getResult(msg, object);
    }


    /**
     * error handle
     *
     * @param code result code
     * @param msg  result message
     * @return error result code
     */
    public static Result error(Integer code, String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMessage(I18nUtil.get(msg));
        return result;
    }


    /**
     * error does not need to return data
     *
     * @param msg error message
     * @return error result code
     */
    public static Result error(String msg) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.ERROR);
        result.setMessage(I18nUtil.get(msg));

        return result;
    }

    public static Result errorOld(String msg) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.ERROR);
        result.setMessage(msg);
        return result;
    }


    /**
     * error does not need to return data
     *
     * @param msg error message
     * @return error result code
     */
    public static Result errorBefore(String info,String msg) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.ERROR);
        result.setMessage(info+I18nUtil.get(msg));

        return result;
    }

    /**
     * error does not need to return data
     *
     * @param msg error message
     * @return error result code
     */
    public static Result errorAfter(String info,String msg) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.ERROR);
        result.setMessage(I18nUtil.get(msg)+info);

        return result;
    }



    /**
     * get result
     *
     * @param msg  message
     * @param list object list
     * @return result code
     */
    private static Result getResult(String msg, Object list) {
        Result result = new Result();
        result.setCode(Constant.StatusCode.SUCCESS);
        result.setMessage(I18nUtil.get(msg));

        result.setData(list);
        return result;
    }




}
