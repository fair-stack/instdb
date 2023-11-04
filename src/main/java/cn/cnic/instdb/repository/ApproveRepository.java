package cn.cnic.instdb.repository;

import cn.cnic.instdb.model.resources.Approve;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * ResourcesDataRepository
 *
 * @author wangCc
 * @date 2021-03-19 15:45
 */
public interface ApproveRepository extends MongoRepository<Approve, String> {


}
