package ai.humanitarians.pipeline.repository;

import ai.humanitarians.pipeline.model.entity.ReviewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewEventRepository extends JpaRepository<ReviewEvent, Integer> {
    List<ReviewEvent> findByVideo_VideoIdOrderByOccurredOnAsc(Integer videoId);
}
