package ai.humanitarians.pipeline.repository;

import ai.humanitarians.pipeline.model.entity.NotificationLog;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {

    @Query("SELECT n FROM NotificationLog n WHERE n.video.videoId = :videoId AND n.deliveryStatus IN (:statuses) ORDER BY n.notificationId DESC")
    List<NotificationLog> findByVideoIdAndStatusIn(
            @Param("videoId") Integer videoId,
            @Param("statuses") List<NotificationDeliveryStatus> statuses
    );

    default Optional<NotificationLog> findActiveOrSent(Integer videoId) {
        List<NotificationLog> list = findByVideoIdAndStatusIn(
                videoId,
                List.of(NotificationDeliveryStatus.SENDING, NotificationDeliveryStatus.SENT)
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<NotificationLog> findByVideo_VideoIdOrderByNotificationIdDesc(Integer videoId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM NotificationLog n WHERE n.video.videoId = :videoId")
    void deleteByVideoId(@Param("videoId") Integer videoId);

    @Query("SELECT n FROM NotificationLog n JOIN FETCH n.video v LEFT JOIN FETCH v.fellow f LEFT JOIN FETCH f.project " +
           "WHERE n.deliveryStatus = :status AND n.attemptCount < :maxAttempts")
    List<NotificationLog> findRetryableFailed(
            @Param("status") NotificationDeliveryStatus status,
            @Param("maxAttempts") Integer maxAttempts
    );

    @Query("SELECT n FROM NotificationLog n JOIN FETCH n.video " +
           "WHERE n.deliveryStatus = ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus.SENDING AND (n.sentOn IS NULL OR n.sentOn <= :cutoff)")
    List<NotificationLog> findStrandedSending(@Param("cutoff") OffsetDateTime cutoff);

    long countByDeliveryStatus(NotificationDeliveryStatus status);
}
