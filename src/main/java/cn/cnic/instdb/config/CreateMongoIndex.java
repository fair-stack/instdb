package cn.cnic.instdb.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * tomongoto
 */
@Repository
@Slf4j
public class CreateMongoIndex {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * Create a single or federated index
     *
     * @param index_key      The name of the index，The name of the index
     * @param collectionName Collection Name
     * @return
     */
    public boolean createInboxIndex(String collectionName, String... index_key) {
        boolean success = true;
        try {
            Index index = new Index();
            for (int i = 0; i < index_key.length; i++) {
                index.on(index_key[i], Sort.Direction.ASC);
            }
            mongoTemplate.indexOps(collectionName).ensureIndex(index);

        } catch (Exception ex) {
            success = false;
        }
        return success;
    }

    /**
     * Get existing index collection
     *
     * @return
     */
    public List<IndexInfo> getInboxIndex(String collectionName) {
        List<IndexInfo> indexInfoList = mongoTemplate.indexOps(collectionName).getIndexInfo();
        return indexInfoList;
    }

    /**
     * Delete Index
     *
     * @param indexName      The name of the index
     * @param collectionName Collection Name
     * @return
     */
    public boolean deleteInboxIndex(String indexName, String collectionName) {
        boolean success = true;
        try {
            mongoTemplate.indexOps(collectionName).dropIndex(indexName);
        } catch (Exception ex) {
            success = false;
        }
        return success;
    }

    /**
     * obtainmongoobtain
     */
    public Set<String> getNames() {
        Set<String> res = mongoTemplate.getCollectionNames();
        return res;
    }


    public void createIndex() {
        List<IndexInfo> resources_manage = getInboxIndex("resources_manage");
        if (resources_manage.size() > 1) {
            log.info("----- mongodb database(resources_manage) database!  ------");
            return;
        }
        String[] index = {"createTime", "approveTime"};
        for (String s : index) {
            createInboxIndex("resources_manage", s);
        }
        log.info("mongodb resources_manage create index {} : " + index.toString() + " Creation completed!  ------");
    }

    public void createResourceFileTreeIndex() {
        List<IndexInfo> resource_file_tree = getInboxIndex("resource_file_tree");
        if (resource_file_tree.size() > 1) {
            log.info("----- mongodb database(resource_file_tree) database!  ------");
            return;
        }
        String[] index = {"resourcesId"};
        for (String s : index) {
            createInboxIndex("resource_file_tree", s);
        }
        log.info("mongodb resource_file_tree create index {} : " + index.toString() + " Creation completed!  ------");
    }


    public void createApproveIndex() {
        List<IndexInfo> approve = getInboxIndex("approve");

        if (approve.size() > 1) {
            log.info("----- mongodb database(approve) database!  ------");
            return;
        }
        String[] index = {"createTime", "approvalTime"};
        for (String s : index) {
            createInboxIndex("approve", s);
        }
        log.info("mongodb approve create index {} : " + index.toString() + " Creation completed!  ------");
    }

}
