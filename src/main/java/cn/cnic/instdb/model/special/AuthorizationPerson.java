package cn.cnic.instdb.model.special;

import lombok.Data;

/**
 * authorization person
 *
 * @author wangCc
 * @date 2021-03-22 10:07
 */
@Data
public class AuthorizationPerson {
    private String userId;
    private String userName;
    private String email;

    public AuthorizationPerson(String userId, String userName, String email) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
    }
}
