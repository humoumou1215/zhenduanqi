package com.zhenduanqi.dto;

public class ServerStatusDTO {
    private boolean connected;
    private String message;
    private String error;

    public static ServerStatusDTO success(String message) {
        ServerStatusDTO dto = new ServerStatusDTO();
        dto.connected = true;
        dto.message = message;
        return dto;
    }

    public static ServerStatusDTO failure(String error) {
        ServerStatusDTO dto = new ServerStatusDTO();
        dto.connected = false;
        dto.error = error;
        return dto;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
