package ai.humanitarians.pipeline.model.entity;

import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_video_id", columnList = "video_id"),
        @Index(name = "idx_notif_delivery_status", columnList = "delivery_status"),
        @Index(name = "idx_notif_video_status", columnList = "video_id, delivery_status")
    }
)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private VideoRecord video;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "sent_on")
    private OffsetDateTime sentOn;

    @Column(name = "delivery_status", nullable = false, length = 50)
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.PENDING;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    public NotificationLog() {}

    public NotificationLog(VideoRecord video, String recipientEmail, NotificationDeliveryStatus deliveryStatus, String reason) {
        this.video = video;
        this.recipientEmail = recipientEmail;
        this.deliveryStatus = deliveryStatus != null ? deliveryStatus : NotificationDeliveryStatus.PENDING;
        this.reason = reason;
        this.attemptCount = (deliveryStatus == NotificationDeliveryStatus.SENDING || deliveryStatus == NotificationDeliveryStatus.SENT) ? 1 : 0;
        if (deliveryStatus == NotificationDeliveryStatus.SENT) {
            this.sentOn = OffsetDateTime.now();
        }
    }

    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }

    public VideoRecord getVideo() { return video; }
    public void setVideo(VideoRecord video) { this.video = video; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public OffsetDateTime getSentOn() { return sentOn; }
    public void setSentOn(OffsetDateTime sentOn) { this.sentOn = sentOn; }

    public NotificationDeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(NotificationDeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
}
