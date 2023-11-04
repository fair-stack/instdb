package cn.cnic.instdb.model.special;

import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/29/14:37
 * @Description: Topic and resource correlation
 */
@Data
public class SpecialResourcesDTO {
    private String specialId;
    private String resourcesId;

    private List<String> specialIds;
    private List<String> resourcesIds;
}
