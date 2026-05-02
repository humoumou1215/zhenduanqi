package com.zhenduanqi.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

public class ArthasResult {
    private String type;
    private Map<String, Object> data = new HashMap<>();

    public ArthasResult() {
    }

    public ArthasResult(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data != null ? data : new HashMap<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }

    @JsonAnySetter
    public void addExtraField(String key, Object value) {
        if (!"type".equals(key)) {
            data.put(key, value);
        }
    }
}
