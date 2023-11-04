package cn.cnic.instdb.model.resources;

import cn.cnic.instdb.model.system.Template;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description: Approval editing usage Approval editing usage
 */

@Data
public class ResourcesManageUpdate {

    @Id
    private String id;

    private List<Resources> resources;

    @lombok.Data
    public static class Resources {
        private String name;
        private String desc;
        private List<Template.Resource> resources;
    }

}
