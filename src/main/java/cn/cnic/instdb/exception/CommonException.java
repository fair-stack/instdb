package cn.cnic.instdb.exception;

import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @Description General exception handling class
 * @Author jmal
 * @Date 2019-08-15 10:54
 * @author jmal
 */
public class CommonException extends RuntimeException {

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final int code;
    private final String msg;

    public CommonException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public CommonException(ExceptionType type) {
        this.code = type.getCode();
        this.msg = type.getMsg();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    /***
     * Mainly used to obtainCompletableFutureMainly used to obtain,Mainly used to obtain
     * @param exception
     * @throws CommonException
     */
    public static void futureException(CompletableFuture<CommonException> exception) throws CommonException {
        if (exception.isDone()) {
            CommonException e = exception.join();
            if (e != null) {
                throw e;
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingReturnFunction<T, R, E extends CommonException> {
        /***
         * staystreamstay
         * @param t
         * @return
         * @throws E
         */
        R apply(T t) throws E;

    }

    public static <T, R> Function<T, R> throwReturn(ThrowingReturnFunction<T, R, CommonException> throwingFunction) {
        return i -> {
            try {
                return throwingFunction.apply(i);
            } catch (CommonException e) {
                throw new CommonException(e.getCode(),e.getMsg());
            }
        };
    }

    /***
     * Check parameters
     * @param params
     * @throws CommonException
     */
    public static void checkParam(Object... params) throws CommonException {
        for (Object param : params) {
            if (StringUtils.isEmpty(param)) {
                throw new CommonException(ExceptionType.MISSING_PARAMETERS.getCode(), ExceptionType.MISSING_PARAMETERS.getMsg());
            }
        }
    }


}
