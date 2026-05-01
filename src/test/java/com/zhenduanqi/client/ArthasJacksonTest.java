package com.zhenduanqi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhenduanqi.model.ArthasApiResponse;
import com.zhenduanqi.model.ArthasResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ArthasJacksonTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseThreadResponse_withStructuredJson_extractsResults() throws Exception {
        String threadResponse = """
            {
              "state": "SUCCEEDED",
              "sessionId": "test-session",
              "consumerId": "test-consumer",
              "body": {
                "results": [
                  {
                    "type": "thread",
                    "data": {
                      "threadId": 1,
                      "threadName": "main",
                      "threadState": "RUNNABLE",
                      "cpu": 0.5
                    }
                  },
                  {
                    "type": "thread",
                    "data": {
                      "threadId": 2,
                      "threadName": "worker",
                      "threadState": "BLOCKED",
                      "cpu": 0.2
                    }
                  }
                ]
              }
            }
            """;

        ArthasApiResponse response = objectMapper.readValue(threadResponse, ArthasApiResponse.class);

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResults()).isNotEmpty();
        assertThat(response.getBody().getResults()).hasSize(2);

        List<ArthasResult> results = response.getBody().getResults();
        assertThat(results.get(0).getType()).isEqualTo("thread");
        assertThat(results.get(0).getData()).containsEntry("threadId", 1);
        assertThat(results.get(0).getData()).containsEntry("threadName", "main");
        assertThat(results.get(1).getData()).containsEntry("threadName", "worker");
    }

    @Test
    void parseMemoryResponse_withStructuredJson_extractsResults() throws Exception {
        String memoryResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "memory",
                    "data": {
                      "heap": 1073741824,
                      "used": 536870912,
                      "unit": "bytes"
                    }
                  }
                ]
              }
            }
            """;

        ArthasApiResponse response = objectMapper.readValue(memoryResponse, ArthasApiResponse.class);

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getBody().getResults()).isNotEmpty();

        ArthasResult result = response.getBody().getResults().get(0);
        assertThat(result.getType()).isEqualTo("memory");
        assertThat(result.getData()).containsEntry("heap", 1073741824);
    }

    @Test
    void parseEnhancerResponse_withEnhancerType_extractsResults() throws Exception {
        String enhancerResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "enhancer",
                    "data": {
                      "success": true,
                      "classCount": 2,
                      "methodCount": 5
                    }
                  }
                ]
              }
            }
            """;

        ArthasApiResponse response = objectMapper.readValue(enhancerResponse, ArthasApiResponse.class);

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getBody().getResults()).isNotEmpty();

        ArthasResult result = response.getBody().getResults().get(0);
        assertThat(result.getType()).isEqualTo("enhancer");
        assertThat(result.getData()).containsEntry("success", true);
    }
}
