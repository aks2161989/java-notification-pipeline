package ai.humanitarians.pipeline.model.entity;

import ai.humanitarians.pipeline.model.enums.EventType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "review_events",
    indexes = {
        @Index(name = "idx_review_events_video", columnList = "video_id")
    }
)
public class ReviewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private VideoRecord video;

    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "reviewer", nullable = false, length = 255)
    private String reviewer;

    @Column(name = "occurred_on", nullable = false)
    private OffsetDateTime occurredOn = OffsetDateTime.now();

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public ReviewEvent() {}

    public ReviewEvent(VideoRecord video, EventType eventType, String reviewer, String notes, OffsetDateTime occurredOn) {
        this.video = video;
        this.eventType = eventType;
        this.reviewer = reviewer;
        this.notes = notes;
        this.occurredOn = occurredOn != null ? occurredOn : OffsetDateTime.now();
    }

    public Integer getEventId() { return eventId; }
    public void setEventId(Integer eventId) { this.eventId = eventId; }

    public VideoRecord getVideo() { return video; }
    public void setVideo(VideoRecord video) { this.video = video; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public OffsetDateTime getOccurredOn() { return occurredOn; }
    public void setOccurredOn(OffsetDateTime occurredOn) { this.occurredOn = occurredOn; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
