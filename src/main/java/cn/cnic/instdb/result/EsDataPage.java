package cn.cnic.instdb.result;


import lombok.Data;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ESPaging data entity encapsulation
 * @author wdd
 *
 */
@Data
public class EsDataPage {

    private Integer page;

    private Integer pageSize;

    private Integer total;

    private List<Map<String,Object>> sourceList;
    //Aggregated data
    private List<Map<String, Object>> filters;


    public EsDataPage(){
        this.page = 0;
        this.pageSize = 0;
        this.total = 0;
        this.sourceList = new ArrayList<>();
        this.filters = new ArrayList<>();
    }

    //Parametric construction
    public EsDataPage(Integer currentPage, Integer pageSize, Integer totalHits, List<Map<String,Object>> sourceList){
        this.page = currentPage;
        this.pageSize = pageSize;
        this.total = totalHits;
        this.sourceList = sourceList;
    }


    public EsDataPage(Integer currentPage, Integer pageSize, Integer totalHits, List<Map<String,Object>> sourceList, List<Map<String, Object>> aggregationList){
        this.page = currentPage;
        this.pageSize = pageSize;
        this.total = totalHits;
        this.sourceList = sourceList;
        this.filters = aggregationList;
    }


}
