package cn.cnic.instdb.elasticsearch;

import com.tdunning.math.stats.Sort;
import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2020/09/16/19:26
 * @Description:
 */


@Data
public class EsServiceParams {

    /**
     * Filter conditions
     */
    private List<EsParameter> esParameter;

    /**
     * paging
     */
    private int page;//Current Page
    private int pageSize;//Number of displayed items per page
    //sort
    private List<Sort> sorts;

    //polymerization
    private List<Aggregation> aggregations;

    //Highlight Field
    private List<Highlight> highlight;


    @Data
    public static class Sort {
        private String field;//sort field
        private String fieldType; //Type of field
        private String order;//Positive and reverse order
    }

    @Data
    public static class Highlight {
        private String highlightField;//Highlight Field
        private String color;//Prefix used to setstyle
    }


    @Data
    public static class Aggregation {
        private String name;//Aggregated Fields
        private String field;//Aggregated Fields
        private String fieldType; //Type of field
        private int size; //Aggregate quantity
    }

    @Data
    public static class EsParameter {
        private String type;//left left side   searchleft side
        private String fieldName;//Parameter Name
        private String field;//Parameter Name
        private String value;//Parameter value
        private String operator;// Query operation keywords
        private String connector;// Connector
        private String fieldType; //Type of field
    }


}



