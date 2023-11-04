package cn.cnic.instdb.model.commentNotice;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Auther: wdd
 * @Date: 2021/03/28/14:27
 * @Description:comment
 */

@Data
@Document(collection = "user_send_email")
public class UserSendEmail {

    // 0 Indicates open  1 Indicates open

    @Id
    private String id;
    private String emailAccounts;
    //Pending approval
    private int waitApproval;
    //Approved
    private int approved;
    //Approval rejection
    private int approvalRejected;
    //Approval revocation
    private int approvalRevocation;
    //Version upgrade
    private int versionUp;

}
