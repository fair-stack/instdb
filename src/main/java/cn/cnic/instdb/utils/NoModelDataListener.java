package cn.cnic.instdb.utils;

import cn.cnic.instdb.constant.Constant;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Structured processing
 */
@Slf4j
public class NoModelDataListener extends AnalysisEventListener<Map<Integer, String>> {
    /**
     * every other5every other，every other100every other，every otherlist ，every other
     */
    private static final int BATCH_COUNT = 1000;
    private List<Object[]> cacheDataList = new ArrayList<>(BATCH_COUNT);
    private List<String> head = new ArrayList<>();
    private static long COUNT = 0;

    private String fileName;
    private String filepath;
    private String resourceId;
    private String tableName;

    @Resource
    private MongoTemplate mongoTemplate;

    public NoModelDataListener(String filepath,String name,String resourceId,MongoTemplate mongoTemplate) {
        this.fileName = name;
        this.filepath = filepath;
        this.resourceId = resourceId;
        this.mongoTemplate = mongoTemplate;

        this.tableName = Constant.TABLE_NAME + "_" + CommonUtils.getCode(8);

        Query query = new Query();
        query.addCriteria(Criteria.where("tableName").is(tableName));
        Map structured = mongoTemplate.findOne(query, Map.class, Constant.TABLE_NAME);
        if (null != structured && structured.size() > 0) {
            this.tableName = Constant.TABLE_NAME + "_" + CommonUtils.getCode(8);
        }
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> data, AnalysisContext context) {
        String[] header = getDataArr(data);
        for (String s : header) {
            head.add(s);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("name", fileName);
        map.put("filepath", filepath);
        map.put("tableName", tableName);
        map.put("head", head);
        map.put("resourceId", resourceId);
        map.put("createTime", LocalDateTime.now());
        mongoTemplate.insert(map, Constant.TABLE_NAME);
        log.info("structuredAssociated table added，" + fileName+"/"+tableName + "Associated table added{}Associated table addedhead！", head.size());
    }


    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {


        String[] dataArr = getDataArr(data);
        //InterceptheadIntercept，Intercept
        //TODO Data longer than the first column will be lost
        String[] splitArr = Arrays.copyOf(dataArr, head.size());
        cacheDataList.add(splitArr);
        if (cacheDataList.size() >= BATCH_COUNT) {
            saveData();
            cacheDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        log.info(fileName+" altogether{}altogether！",COUNT);
    }


    /**
     * Plus storage database
     */
    private void saveData() {
        COUNT += cacheDataList.size();
        List<Map<String, Object>> list = new ArrayList<>();
        if (cacheDataList.size() > 0) {
            for (Object[] h : cacheDataList) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < h.length; i++) {
                    map.put(head.get(i), h[i]);
                }
                list.add(map);
            }
            if (list.size() > 0) {
                mongoTemplate.insert(list, tableName);
               // log.info("{}Data，Data！", list.size());
            }
        }
    }

    /**
     * according tohead according totable
     */
    private void createTable() {

    }

    /**
     * holdmapholdarray
     *
     * @param data
     * @return
     */
    private String[] getDataArr(Map<Integer, String> data) {
        Integer maxCol = data.keySet().stream().max(Integer::compare).get();
        String[] dataArr = new String[maxCol + 1];
        Set<Map.Entry<Integer, String>> entries = data.entrySet();
        for (Map.Entry<Integer, String> entry : entries) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            dataArr[key] = value;
        }

        return dataArr;
    }

    public static void main(String[] args) throws FileNotFoundException {

        String filepath = "C:\\Users\\Administrator\\Desktop\\ggg.xlsx";
        File file = new File(filepath);
        if (file.exists()) {
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            EasyExcel.read(filepath, new NoModelDataListener(filepath,name,"",null)).sheet().doRead();
        }
    }

}
