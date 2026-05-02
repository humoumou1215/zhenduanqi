package com.zhenduanqi.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "scene_step")
public class SceneStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    @JsonIgnore
    private DiagnoseScene scene;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "command", columnDefinition = "TEXT", nullable = false)
    private String command;

    @Column(name = "expected_hint", columnDefinition = "TEXT")
    private String expectedHint;

    @Column(name = "continuous")
    private boolean continuous = false;

    @Column(name = "max_exec_time")
    private int maxExecTime = 30000;

    @Column(name = "extract_rules", columnDefinition = "TEXT")
    private String extractRules;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DiagnoseScene getScene() {
        return scene;
    }

    public void setScene(DiagnoseScene scene) {
        this.scene = scene;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getExpectedHint() {
        return expectedHint;
    }

    public void setExpectedHint(String expectedHint) {
        this.expectedHint = expectedHint;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public int getMaxExecTime() {
        return maxExecTime;
    }

    public void setMaxExecTime(int maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    public String getExtractRules() {
        return extractRules;
    }

    public void setExtractRules(String extractRules) {
        this.extractRules = extractRules;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
