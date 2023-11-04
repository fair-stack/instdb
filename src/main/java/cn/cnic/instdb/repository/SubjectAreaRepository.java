package cn.cnic.instdb.repository;

import cn.cnic.instdb.model.system.SubjectArea;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface SubjectAreaRepository extends MongoRepository<SubjectArea, String> {

    @Override
    Optional<SubjectArea> findById(String id);
}
