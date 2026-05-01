package com.zhenduanqi.model;

public class SessionInfo {
    private String sessionId;
    private String consumerId;

    public SessionInfo() {
    }

    public SessionInfo(String sessionId, String consumerId) {
        this.sessionId = sessionId;
        this.consumerId = consumerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}
