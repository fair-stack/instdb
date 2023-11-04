package cn.cnic.instdb.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class Result<T> implements Serializable {
    private String message;
    private Integer code;
    private T data;

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    private Result(T data) {
        this.code = 200;
        this.data = data;
    }





    public static <T> Result<T> success() {
        return new Result();
    }

    public static <T> Result<T> success(T data) {
        return new Result(data);
    }




    @Override
    public String toString() {
        return "Status{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
