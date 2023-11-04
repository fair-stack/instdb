package cn.cnic.instdb.elasticsearch;

import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2020/09/16/19:26
 * @Description:
 */


@Data
public class EsParams {
    private String field;//Parameter Name
    private String value;//Parameter value
    private List values;//Parameter value
    private String operator;// Query operation keywords
    private String connector;// Connector
    private String fieldType; //Type of field
}



