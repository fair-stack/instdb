package cn.cnic.instdb.repository;

import cn.cnic.instdb.model.resources.ResourcesManage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
* @Auther  wdd
* @Date  2021/3/24 20:57
* @Desc
*/
@Repository
public interface ResourcesManageRepository extends MongoRepository<ResourcesManage, String> {


    @Override
    Optional<ResourcesManage> findById(String id);


}
