package ai.humanitarians.pipeline.model.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fellows")
public class Fellow {

    @Id
    @Column(name = "fellow_id")
    private Integer fellowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "active";

    @Column(name = "display_label", length = 255)
    private String displayLabel;

    @OneToMany(mappedBy = "fellow", cascade = CascadeType.ALL)
    private List<VideoRecord> videos = new ArrayList<>();

    public Fellow() {}

    public Fellow(Integer fellowId, Project project, String fullName, String email, String status, String displayLabel) {
        this.fellowId = fellowId;
        this.project = project;
        this.fullName = fullName;
        this.email = email;
        this.status = status != null ? status : "active";
        this.displayLabel = displayLabel;
    }

    public Integer getFellowId() { return fellowId; }
    public void setFellowId(Integer fellowId) { this.fellowId = fellowId; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDisplayLabel() { return displayLabel; }
    public void setDisplayLabel(String displayLabel) { this.displayLabel = displayLabel; }

    public List<VideoRecord> getVideos() { return videos; }
    public void setVideos(List<VideoRecord> videos) { this.videos = videos; }
}
