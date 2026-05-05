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

    @Test
    void parseDashboardResponse_withDashboardType_extractsResults() throws Exception {
        String dashboardResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "dashboard",
                    "data": {
                      "threads": [
                        {
                          "name": "main",
                          "state": "RUNNABLE",
                          "cpu": 0.5,
                          "deltaTime": 100,
                          "threadId": 1
                        }
                      ],
                      "memory": [
                        {
                          "name": "Heap Memory",
                          "used": 268435456,
                          "total": 536870912
                        }
                      ],
                      "gc": [
                        {
                          "name": "Copy",
                          "count": 15,
                          "time": 120
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        ArthasApiResponse response = objectMapper.readValue(dashboardResponse, ArthasApiResponse.class);

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getBody().getResults()).isNotEmpty();

        ArthasResult result = response.getBody().getResults().get(0);
        assertThat(result.getType()).isEqualTo("dashboard");
        assertThat(result.getData()).containsKey("threads");
        assertThat(result.getData()).containsKey("memory");
        assertThat(result.getData()).containsKey("gc");
    }

    @Test
    void serializeArthasResult_producesCorrectJsonFormat() throws Exception {
        ArthasResult statusResult = new ArthasResult();
        statusResult.setType("status");
        statusResult.addExtraField("statusCode", 0);
        statusResult.addExtraField("jobId", 123);

        String json = objectMapper.writeValueAsString(statusResult);
        System.out.println("Serialized ArthasResult: " + json);

        // 验证序列化后有 type 和 data 字段
        assertThat(json).contains("\"type\":\"status\"");
        assertThat(json).contains("\"data\":{");
        assertThat(json).contains("\"statusCode\":0");
        assertThat(json).contains("\"jobId\":123");

        // 验证反序列化回来也正确
        ArthasResult parsed = objectMapper.readValue(json, ArthasResult.class);
        assertThat(parsed.getType()).isEqualTo("status");
        assertThat(parsed.getData()).containsEntry("statusCode", 0);
        assertThat(parsed.getData()).containsEntry("jobId", 123);
    }

    @Test
    void parseStatusResult_fromArthasApiFormat() throws Exception {
        // 这是 Arthas API 实际可能返回的格式 - statusCode 在根级别
        String statusJson = """
            {
              "type": "status",
              "statusCode": 0,
              "jobId": 456,
              "message": "Command executed successfully"
            }
            """;

        ArthasResult result = objectMapper.readValue(statusJson, ArthasResult.class);

        assertThat(result.getType()).isEqualTo("status");
        assertThat(result.getData()).containsEntry("statusCode", 0);
        assertThat(result.getData()).containsEntry("jobId", 456);
        assertThat(result.getData()).containsEntry("message", "Command executed successfully");

        // 序列化回去
        String serialized = objectMapper.writeValueAsString(result);
        System.out.println("Serialized back: " + serialized);
        // 验证序列化后的格式有 data 字段
        assertThat(serialized).contains("\"data\":{");
    }
}
