package ai.humanitarians.pipeline.repository;

import ai.humanitarians.pipeline.model.entity.VideoRecord;
import ai.humanitarians.pipeline.model.enums.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRecordRepository extends JpaRepository<VideoRecord, Integer> {

    @Query("SELECT v FROM VideoRecord v LEFT JOIN FETCH v.fellow f LEFT JOIN FETCH f.project WHERE v.videoId = :id")
    Optional<VideoRecord> findByIdWithFellowAndProject(@Param("id") Integer id);

    @Query("SELECT v FROM VideoRecord v LEFT JOIN FETCH v.fellow f LEFT JOIN FETCH f.project WHERE v.youtubeVideoId = :ytId")
    Optional<VideoRecord> findByYoutubeVideoId(@Param("ytId") String ytId);

    @Query("SELECT v FROM VideoRecord v LEFT JOIN FETCH v.fellow f LEFT JOIN FETCH f.project WHERE v.currentStatus = :status")
    List<VideoRecord> findByCurrentStatus(@Param("status") VideoStatus status);

    @Query("SELECT v FROM VideoRecord v LEFT JOIN FETCH v.fellow f LEFT JOIN FETCH f.project")
    List<VideoRecord> findAllWithFellow();

    long countByCurrentStatus(VideoStatus status);
}
