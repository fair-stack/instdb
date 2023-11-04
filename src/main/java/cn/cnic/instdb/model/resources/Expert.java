package cn.cnic.instdb.model.resources;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Auther: wdd
 * @Date: 2021/06/07/15:58
 * @Description:
 */

@Data
@Document(collection = "expert")
public class Expert {

    @Id
    private String id;
    private String username;
    private String email;
    private String org;

}
