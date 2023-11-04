package cn.cnic.instdb.model.system;

import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * @Auther: wdd
 * @Date: 2021/04/22/18:12
 * @Description:Constant Dictionary Class
 */

@Data
public class ConstantDictionaryDTO {

    private String name;

    private String code;

    private String type;

}
