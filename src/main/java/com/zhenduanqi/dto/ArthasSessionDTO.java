package com.zhenduanqi.dto;

import java.time.LocalDateTime;

public class ArthasSessionDTO {

    private Long id;
    private String serverId;
    private String arthasSessionId;
    private String arthasConsumerId;
    private Integer currentJobId;
    private String status;
    private String username;
    private Long sceneId;
    private Long stepId;
    private String command;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime closedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getArthasSessionId() { return arthasSessionId; }
    public void setArthasSessionId(String arthasSessionId) { this.arthasSessionId = arthasSessionId; }
    public String getArthasConsumerId() { return arthasConsumerId; }
    public void setArthasConsumerId(String arthasConsumerId) { this.arthasConsumerId = arthasConsumerId; }
    public Integer getCurrentJobId() { return currentJobId; }
    public void setCurrentJobId(Integer currentJobId) { this.currentJobId = currentJobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public static ArthasSessionDTO fromEntity(com.zhenduanqi.entity.ArthasSession entity) {
        ArthasSessionDTO dto = new ArthasSessionDTO();
        dto.setId(entity.getId());
        dto.setServerId(entity.getServerId());
        dto.setArthasSessionId(entity.getArthasSessionId());
        dto.setArthasConsumerId(entity.getArthasConsumerId());
        dto.setCurrentJobId(entity.getCurrentJobId());
        dto.setStatus(entity.getStatus());
        dto.setUsername(entity.getUsername());
        dto.setSceneId(entity.getSceneId());
        dto.setStepId(entity.getStepId());
        dto.setCommand(entity.getCommand());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastActiveAt(entity.getLastActiveAt());
        dto.setClosedAt(entity.getClosedAt());
        return dto;
    }
}
