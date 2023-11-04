package cn.cnic.instdb.model.resources;

import cn.cnic.instdb.model.system.Template;
import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description: Approval editing usage Approval editing usage
 */

@Data
public class ApplyAccessSubmit {

    private String resourceId;
//    private String startTime;
//    private String endTime;
    private String org;
    private String phone;
    private List<Template.Group> resources;

}
