package cn.cnic.instdb.utils;

import cn.cnic.instdb.constant.Constant;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ExtractionMethod {

    /**
     * recombination
     *
     * @return Map
     */
    public static void  editListParam(String[] code){
        for(int x=0;x<code.length;x++){
            int i = code[x].indexOf(" ");
            if(i>=0){
                code[x]=code[x].toLowerCase().substring(0,i);
            }else {
                code[x]=code[x].toLowerCase();
            }
        }
    }

    /**
     * request parameter parameter map parameter null & ""
     *
     * @return Map
     */
    public static Map<String,Object> getParamMap(HttpServletRequest request){
        Map<String,Object>  paramMap=new HashMap<String,Object>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<String> strings = parameterMap.keySet();
        for (String string : strings) {
            String[] values = parameterMap.get(string);
            if(values.length>0){
                if(string.contains(Constant.QueryType.TSQ)){
                    List<String> strings1 = new ArrayList<>();
                    for (String value : values) {
                        if(!value.equals("")){
                            strings1.add(value);
                        }
                    }
                    if(strings1.size()>0) {
                        paramMap.put(string,strings1);
                    }
                }else {
                    String value = values[0];
                    if(value!=null && !value.equals("")) {
                        paramMap.put(string, value);
                    }
                }
            }
        }
        return paramMap;
    }

    public static BoolQueryBuilder getNativeQuery(Map<String,Object> paramMap){
        Set<String> paramSet = paramMap.keySet();
        // Search criteria
        BoolQueryBuilder bqb = QueryBuilders.boolQuery();
        for (String paramName : paramSet) {
            if(paramName.contains("_")) {
                Object value = paramMap.get(paramName);
                String type = paramName.substring(0,paramName.indexOf("_")+1);
                paramName = paramName.substring(paramName.indexOf("_") + 1, paramName.length());
                switch (type){
                    case Constant.QueryType.MPQ : bqb.must(QueryBuilders.matchPhraseQuery(paramName, value));
                          break;
                    case Constant.QueryType.TSQ :  List<String>  tersValue = (List) paramMap.get(type+paramName);
                                                     bqb.must(QueryBuilders.termsQuery(paramName+".keyword", tersValue));
                          break;
                    case Constant.QueryType.RQG : bqb.must(QueryBuilders.rangeQuery(paramName).gte(value));
                          break;
                    case Constant.QueryType.RQL : bqb.must(QueryBuilders.rangeQuery(paramName).lte(value));
                          break;
                    case Constant.QueryType.MMQ :
                        if (paramName.contains(",")) {
                            String[] split = paramName.split(",");
                            if (null != split && split.length > 0) {
                                bqb.must(QueryBuilders.multiMatchQuery(value, split));
                                break;
                            }
                        } else {
                            bqb.must(QueryBuilders.multiMatchQuery(value, paramName));
                            break;
                        }
                    case Constant.QueryType.TQ :
                        if(paramName.equals("year")){
                            bqb.must(QueryBuilders.termQuery(paramName, value));
                        }else {
                            bqb.must(QueryBuilders.termQuery(paramName+".keyword", value));
                        }
                          break;
                }
            }
        }
        return bqb;
    }

    public static void addSort(Map<String,Object> paramMap,String sort){
        String sorName = (String) paramMap.get(Constant.Order.SORT_FIELD);
        if(sorName == null || sorName.equals("")){
            boolean judge = false;
            for (String s : paramMap.keySet()) {
                if(s.contains("_")){
                    judge = true;
                    break;
                }
            }
            if(!judge){
                paramMap.put("sortField",sort);
            }
        }else {
            if(sorName.equals("relevance")){
                paramMap.remove("sortField");
            }
        }
    }

    public static void addPaperSort(Map<String,Object> paramMap,String sort){
        String sorName = (String) paramMap.get(Constant.Order.SORT_FIELD);
        if(sorName == null || sorName.equals("")){
            boolean judge = false;
            for (String s : paramMap.keySet()) {
                if(s.contains("_") && !s.equals("tq_type")){
                    judge = true;
                    break;
                }
            }
            if(!judge){
                paramMap.put("sortField",sort);
            }
        }else {
            if(sorName.equals("relevance")){
                paramMap.remove("sortField");
            }
        }
    }
}
