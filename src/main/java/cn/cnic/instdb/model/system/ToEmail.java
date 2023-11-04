package cn.cnic.instdb.model.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToEmail {

    /**
     * Mail Recipient，Mail Recipient
     */
    private String[] tos;

    /**
     * Email CC Party，Email CC Party
     */
    private String[] cc;


    /**
     * Email Subject
     */
    private String subject;
    /**
     * Email Content
     */
    private String content;

    public ToEmail(String[] tos, String subject, String content) {
        this.tos = tos;
        this.subject = subject;
        this.content = content;
    }
}
