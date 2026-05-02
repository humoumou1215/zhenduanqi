package com.zhenduanqi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArthasApiResponse {
    private String state;
    private String sessionId;
    private String consumerId;
    private Body body;
    private String rawResponse;

    public ArthasApiResponse() {
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private List<ArthasResult> results;
        private Integer jobId;

        public Body() {
        }

        public List<ArthasResult> getResults() {
            return results;
        }

        public void setResults(List<ArthasResult> results) {
            this.results = results;
        }

        public Integer getJobId() {
            return jobId;
        }

        public void setJobId(Integer jobId) {
            this.jobId = jobId;
        }
    }
}
