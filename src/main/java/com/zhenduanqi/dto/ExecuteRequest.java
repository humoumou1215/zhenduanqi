package com.zhenduanqi.dto;

public class ExecuteRequest {

    private String serverId;
    private String command;

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

    @Override
    public String toString() {
        return "ExecuteRequest{serverId='" + serverId + "', command='" + command + "'}";
    }
}
