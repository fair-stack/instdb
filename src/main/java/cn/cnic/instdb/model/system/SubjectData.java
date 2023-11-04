package cn.cnic.instdb.model.system;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Auther: wdd
 * @Date: 2021/04/09/23:27
 * @Description:
 */

@Data
@Document(collection = "subject")
public class SubjectData {

    @Id
    private String id;

    private String one_rank_id;
    private String one_rank_no;
    private String one_rank_name;
    private String one_rank_name_en;


    private String two_rank_id;
    private String two_rank_no;
    private String two_rank_name;
    private String two_rank_name_en;

    private String three_rank_id;
    private String three_rank_no;
    private String three_rank_name;
    private String three_rank_name_en;

}
