package com.zhenduanqi.model;

import java.util.List;

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

    public static class Body {
        private List<ArthasResult> results;

        public Body() {
        }

        public List<ArthasResult> getResults() {
            return results;
        }

        public void setResults(List<ArthasResult> results) {
            this.results = results;
        }
    }
}
