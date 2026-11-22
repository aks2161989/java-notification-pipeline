package ai.humanitarians.pipeline.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pms")
public class PM {

    @Id
    @Column(name = "pm_id")
    private Integer pmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "role", nullable = false, length = 50)
    private String role = "pm";

    @Column(name = "status", nullable = false, length = 50)
    private String status = "active";

    public PM() {}

    public PM(Integer pmId, Project project, String fullName, String email, String role, String status) {
        this.pmId = pmId;
        this.project = project;
        this.fullName = fullName;
        this.email = email;
        this.role = role != null ? role : "pm";
        this.status = status != null ? status : "active";
    }

    public Integer getPmId() { return pmId; }
    public void setPmId(Integer pmId) { this.pmId = pmId; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
