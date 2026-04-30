package com.zhenduanqi.dto;

import com.zhenduanqi.entity.ArthasServerEntity;

public class ArthasServerDTO {

    private String id;
    private String name;
    private String host;
    private int httpPort;
    private String token;

    public static ArthasServerDTO fromEntity(ArthasServerEntity entity) {
        ArthasServerDTO dto = new ArthasServerDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setHost(entity.getHost());
        dto.setHttpPort(entity.getHttpPort());
        return dto;
    }

    public ArthasServerEntity toEntity() {
        ArthasServerEntity entity = new ArthasServerEntity();
        entity.setId(this.id);
        entity.setName(this.name);
        entity.setHost(this.host);
        entity.setHttpPort(this.httpPort);
        entity.setToken(this.token);
        return entity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
