package com.zhenduanqi.dto;

import com.zhenduanqi.model.ArthasResponse;
import com.zhenduanqi.model.ArthasResult;
import java.util.List;

public class ExecuteResponse {

    private String state;
    private List<String> results;
    private List<ArthasResult> structuredResults;
    private String error;
    private String rawResponse;

    public static ExecuteResponse fromArthasResponse(ArthasResponse arthasResponse) {
        ExecuteResponse resp = new ExecuteResponse();
        resp.setState(arthasResponse.getState());
        resp.setResults(arthasResponse.getResults());
        resp.setStructuredResults(arthasResponse.getStructuredResults());
        resp.setError(arthasResponse.getError());
        resp.setRawResponse(arthasResponse.getRawResponse());
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

    public List<ArthasResult> getStructuredResults() {
        return structuredResults;
    }

    public void setStructuredResults(List<ArthasResult> structuredResults) {
        this.structuredResults = structuredResults;
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

    @Override
    public String toString() {
        return "ExecuteResponse{state='" + state + "', results=" + results
                + ", structuredResults=" + structuredResults
                + ", error='" + error + "', rawResponse='" + rawResponse + "'}";
    }
}
