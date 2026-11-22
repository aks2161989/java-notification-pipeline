package ai.humanitarians.pipeline.model.entity;

import ai.humanitarians.pipeline.model.enums.VideoStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "videos",
    indexes = {
        @Index(name = "idx_video_yt_id", columnList = "youtube_video_id"),
        @Index(name = "idx_video_status", columnList = "current_status"),
        @Index(name = "idx_video_status_yt", columnList = "current_status, youtube_video_id")
    }
)
public class VideoRecord {

    @Id
    @Column(name = "video_id")
    private Integer videoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fellow_id")
    private Fellow fellow;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "youtube_link", length = 500)
    private String youtubeLink;

    @Column(name = "drive_link", length = 500)
    private String driveLink;

    @Column(name = "drive_file_id", length = 255)
    private String driveFileId;

    @Column(name = "youtube_video_id", length = 255)
    private String youtubeVideoId;

    @Column(name = "current_status", nullable = false, length = 50)
    private VideoStatus currentStatus = VideoStatus.SUBMITTED;

    @Column(name = "status_note", columnDefinition = "TEXT")
    private String statusNote;

    @Column(name = "submitted_on")
    private OffsetDateTime submittedOn;

    @Column(name = "published_on")
    private OffsetDateTime publishedOn;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationLog> notifications = new ArrayList<>();

    public VideoRecord() {}

    public VideoRecord(Integer videoId, Fellow fellow, String title, String youtubeVideoId, VideoStatus currentStatus) {
        this.videoId = videoId;
        this.fellow = fellow;
        this.title = title;
        this.youtubeVideoId = youtubeVideoId;
        this.currentStatus = currentStatus != null ? currentStatus : VideoStatus.SUBMITTED;
    }

    public Integer getVideoId() { return videoId; }
    public void setVideoId(Integer videoId) { this.videoId = videoId; }

    public Fellow getFellow() { return fellow; }
    public void setFellow(Fellow fellow) { this.fellow = fellow; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getYoutubeLink() { return youtubeLink; }
    public void setYoutubeLink(String youtubeLink) { this.youtubeLink = youtubeLink; }

    public String getDriveLink() { return driveLink; }
    public void setDriveLink(String driveLink) { this.driveLink = driveLink; }

    public String getDriveFileId() { return driveFileId; }
    public void setDriveFileId(String driveFileId) { this.driveFileId = driveFileId; }

    public String getYoutubeVideoId() { return youtubeVideoId; }
    public void setYoutubeVideoId(String youtubeVideoId) { this.youtubeVideoId = youtubeVideoId; }

    public VideoStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(VideoStatus currentStatus) { this.currentStatus = currentStatus; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String statusNote) { this.statusNote = statusNote; }

    public OffsetDateTime getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(OffsetDateTime submittedOn) { this.submittedOn = submittedOn; }

    public OffsetDateTime getPublishedOn() { return publishedOn; }
    public void setPublishedOn(OffsetDateTime publishedOn) { this.publishedOn = publishedOn; }

    public List<ReviewEvent> getEvents() { return events; }
    public void setEvents(List<ReviewEvent> events) { this.events = events; }

    public List<NotificationLog> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationLog> notifications) { this.notifications = notifications; }
}
