package com.zhenduanqi.model;

import java.util.ArrayList;
import java.util.List;

public class ArthasResponse {

    private String state;
    private List<String> results = new ArrayList<>();
    private String error;
    private String rawResponse;

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

    public boolean isSuccess() {
        return "succeeded".equals(state);
    }
}
