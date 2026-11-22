package ai.humanitarians.pipeline.repository;

import ai.humanitarians.pipeline.model.entity.PM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PMRepository extends JpaRepository<PM, Integer> {
    List<PM> findByProject_ProjectId(Integer projectId);
}
