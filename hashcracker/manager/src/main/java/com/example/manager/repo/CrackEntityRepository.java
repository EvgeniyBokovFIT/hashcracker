package com.example.manager.repo;

import com.example.manager.entity.RequestStatus;
import com.example.manager.entity.CrackEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CrackEntityRepository extends MongoRepository<CrackEntity, String> {
    List<CrackEntity> findByRequestStatusAndRequestTimeBefore(RequestStatus status, LocalDateTime time);
}
