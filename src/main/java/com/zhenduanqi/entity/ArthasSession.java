package com.zhenduanqi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "arthas_session")
public class ArthasSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", length = 50, nullable = false)
    private String serverId;

    @Column(name = "arthas_session_id", length = 100)
    private String arthasSessionId;

    @Column(name = "arthas_consumer_id", length = 100)
    private String arthasConsumerId;

    @Column(name = "current_job_id")
    private Integer currentJobId;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "scene_id")
    private Long sceneId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "command", columnDefinition = "TEXT")
    private String command;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActiveAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastActiveAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getArthasSessionId() {
        return arthasSessionId;
    }

    public void setArthasSessionId(String arthasSessionId) {
        this.arthasSessionId = arthasSessionId;
    }

    public String getArthasConsumerId() {
        return arthasConsumerId;
    }

    public void setArthasConsumerId(String arthasConsumerId) {
        this.arthasConsumerId = arthasConsumerId;
    }

    public Integer getCurrentJobId() {
        return currentJobId;
    }

    public void setCurrentJobId(Integer currentJobId) {
        this.currentJobId = currentJobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getSceneId() {
        return sceneId;
    }

    public void setSceneId(Long sceneId) {
        this.sceneId = sceneId;
    }

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
