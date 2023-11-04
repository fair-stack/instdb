package cn.cnic.instdb.model.system;

import lombok.Data;

import java.util.List;


@Data
public class SearchConfigDTO {


    private List<Info> InfoSort;
    @Data
    public static class Info {
        private String id;
        private String sort;
        //1 apply -1  apply
        private String status;
    }

}
