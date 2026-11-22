package ai.humanitarians.pipeline.repository;

import ai.humanitarians.pipeline.model.entity.Fellow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FellowRepository extends JpaRepository<Fellow, Integer> {
    
    @Query("SELECT f FROM Fellow f JOIN FETCH f.project WHERE f.fellowId = :id")
    Optional<Fellow> findByIdWithProject(@Param("id") Integer id);

    List<Fellow> findByStatus(String status);
}
