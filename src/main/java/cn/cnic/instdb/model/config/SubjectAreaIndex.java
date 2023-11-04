package cn.cnic.instdb.model.config;

import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/04/09/23:27
 * @Description:
 */

@Data
public class SubjectAreaIndex {
    private List<Info> InfoSort;
    @Data
    public static class Info {
        private String id;
        private String sort;
    }
}
