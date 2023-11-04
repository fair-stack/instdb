package cn.cnic.instdb.repository;


import cn.cnic.instdb.model.rbac.ConsumerDO;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * UserRepository
 *
 * @author wangCc
 * @date 2021-03-19 15:45
 */
public interface UserRepository extends MongoRepository<ConsumerDO, String> {

    ConsumerDO findByEmailAccounts(String emailAccounts);
}
