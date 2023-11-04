package cn.cnic.instdb.model.system;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Document(collection = "email_error_info")
public final class EmailErrorInfo {

    @Id
    private String id;
    private String fromOrg;

    //Email Subject
    private String subject;
    //Email Template Name
    private String template;

    //Recipient Information
    private  String[] tos;

    private Map<String, Object> attachment;


}
