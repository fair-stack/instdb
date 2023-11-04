package cn.cnic.instdb.model.rbac;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description Role model
 * @blame jmal
 * @Date 2021/1/7 7:41 afternoon
 */
@Data
@Document(collection = "role")
public class Role {
    @Id
    String roleId;
    /***
     * Role Name
     */
    String name;
    String nameEn;
    /***
     * Role identification
     */
    String logo;
    /***
     * Remarks
     */
    String remarks;

    /***
     * Permission Path
     */
    List <String> pathList;
    /***
     * Creation time
     */
    LocalDateTime createTime;
    /***
     * Modification time
     */
    LocalDateTime updateTime;

    public Role() {
    }

    public Role(String name,String nameEn, String logo, String remarks, List<String> pathList, LocalDateTime createTime) {
        this.name = name;
        this.nameEn = nameEn;
        this.logo = logo;
        this.remarks = remarks;
        this.pathList = pathList;
        this.createTime = createTime;
    }
}

