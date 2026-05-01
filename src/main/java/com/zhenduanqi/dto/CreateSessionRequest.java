package com.zhenduanqi.dto;

public class CreateSessionRequest {

    private String serverId;
    private String command;
    private Long sceneId;
    private Long stepId;
    private Integer maxExecTime;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
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

    public Integer getMaxExecTime() {
        return maxExecTime;
    }

    public void setMaxExecTime(Integer maxExecTime) {
        this.maxExecTime = maxExecTime;
    }
}
