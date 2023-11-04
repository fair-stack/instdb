package cn.cnic.instdb.repository;

import cn.cnic.instdb.model.special.Special;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ResourcesDataRepository
 *
 * @author wangCc
 * @date 2021-03-19 15:45
 */
@Repository
public interface SpecialRepository extends MongoRepository<Special, String> {

    Page<Special> findBySpecialNameLike(String specialName, Pageable pageable);


    @Override
    Optional<Special> findById(String id);
}
