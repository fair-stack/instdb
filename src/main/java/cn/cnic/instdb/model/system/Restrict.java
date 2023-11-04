package cn.cnic.instdb.model.system;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 *  limit limit，limit，limit
 */
@Data
@Document(collection = "restrict")
public class Restrict {

    @Id
    private String id;

    private String main;  //Email orip

    private int type;     //0 Retrieve password 、 1  Retrieve password

    private long count;  //Number of times used on that day

    private String date;

    private Boolean result;

    private Date createTime;


}
