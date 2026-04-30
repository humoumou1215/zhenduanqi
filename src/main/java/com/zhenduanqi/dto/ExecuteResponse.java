package com.zhenduanqi.dto;

import com.zhenduanqi.model.ArthasResponse;
import java.util.List;

public class ExecuteResponse {

    private String state;
    private List<String> results;
    private String error;
    private String rawResponse;

    public static ExecuteResponse fromArthasResponse(ArthasResponse arthasResponse) {
        ExecuteResponse resp = new ExecuteResponse();
        resp.setState(arthasResponse.getState());
        resp.setResults(arthasResponse.getResults());
        resp.setError(arthasResponse.getError());
        return resp;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getResults() {
        return results;
    }

    public void setResults(List<String> results) {
        this.results = results;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
