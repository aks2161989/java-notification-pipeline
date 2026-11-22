package ai.humanitarians.pipeline.model.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "project_type", nullable = false, length = 50)
    private String projectType = "internal";

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fellow> fellows = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PM> pms = new ArrayList<>();

    public Project() {}

    public Project(Integer projectId, String projectName, String projectType) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectType = projectType != null ? projectType : "internal";
    }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public List<Fellow> getFellows() { return fellows; }
    public void setFellows(List<Fellow> fellows) { this.fellows = fellows; }

    public List<PM> getPms() { return pms; }
    public void setPms(List<PM> pms) { this.pms = pms; }
}
