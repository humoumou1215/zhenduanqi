package com.zhenduanqi.client;

import com.zhenduanqi.model.ArthasApiResponse;
import com.zhenduanqi.model.ArthasResponse;
import com.zhenduanqi.model.ArthasResult;
import com.zhenduanqi.model.ServerInfo;
import com.zhenduanqi.model.SessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArthasHttpClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ArthasHttpClient arthasHttpClient;

    @BeforeEach
    void setUp() {
        arthasHttpClient = new ArthasHttpClient(httpClient);
    }

    private ServerInfo createTestServer() {
        ServerInfo server = new ServerInfo();
        server.setId("server1");
        server.setName("Test Server");
        server.setHost("localhost");
        server.setHttpPort(8563);
        server.setToken("test-token");
        return server;
    }

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

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(threadResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "thread -n 5");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getStructuredResults()).isNotEmpty();
        assertThat(response.getStructuredResults()).hasSize(2);
        
        List<ArthasResult> results = response.getStructuredResults();
        assertThat(results.get(0).getType()).isEqualTo("thread");
        assertThat(results.get(0).getData()).containsEntry("threadId", 1);
        assertThat(results.get(1).getData()).containsEntry("threadName", "worker");

        assertThat(response.getRawResponse()).isEqualTo(threadResponse);
    }

    @Test
    void parseMemoryResponse_withStructuredJson_extractsResults() throws Exception {
        String memoryResponse = """
            {
              "state": "SUCCEEDED",
              "sessionId": "test-session",
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

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(memoryResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "memory");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getStructuredResults()).isNotEmpty();
        
        List<ArthasResult> results = response.getStructuredResults();
        assertThat(results.get(0).getType()).isEqualTo("memory");
        assertThat(results.get(0).getData()).containsEntry("heap", 1073741824);
    }

    @Test
    void parseStatusResponse_withSuccessStatus_setsCorrectState() throws Exception {
        String statusResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "status",
                    "data": {
                      "status": "true",
                      "message": "OK"
                    }
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(statusResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "version");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
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

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(enhancerResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "watch *.* {*} -n 5");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getStructuredResults()).isNotEmpty();
        
        List<ArthasResult> results = response.getStructuredResults();
        assertThat(results.get(0).getType()).isEqualTo("enhancer");
        assertThat(results.get(0).getData()).containsEntry("success", true);
    }

    @Test
    void parseUnknownTypeResponse_fallsBackToRawText() throws Exception {
        String unknownResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "custom",
                    "data": {
                      "field1": "value1",
                      "field2": "value2"
                    }
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(unknownResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "custom-command");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getStructuredResults()).isNotEmpty();
        assertThat(response.getResults()).isNotEmpty();
    }

    @Test
    void parseMalformedJson_returnsRawResponseWithoutException() throws Exception {
        String malformedJson = "This is not valid JSON at all";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(malformedJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "thread");

        assertThat(response.getState()).isEqualTo("succeeded");
        assertThat(response.getRawResponse()).isEqualTo(malformedJson);
    }

    @Test
    void httpError_returnsFailedState() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "thread");

        assertThat(response.getState()).isEqualTo("failed");
        assertThat(response.getError()).contains("500");
    }

    @Test
    void backwardCompatible_withLegacyTextResponse_works() throws Exception {
        String legacyResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "command",
                    "data": {
                      "content": "thread information here"
                    }
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(legacyResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasResponse response = arthasHttpClient.executeCommand(server, "thread");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
        assertThat(response.getRawResponse()).isEqualTo(legacyResponse);
        assertThat(response.getStructuredResults()).isNotEmpty();
    }

    @Test
    void httpBasicAuth_usesCorrectAuthorizationHeader() throws Exception {
        String successResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "status",
                    "data": {
                      "status": "true"
                    }
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(successResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    assertThat(request.headers().firstValue("Authorization")).isPresent();
                    String authHeader = request.headers().firstValue("Authorization").get();
                    assertThat(authHeader).startsWith("Basic ");
                    // arthas:pswd123 编码后是 "YXJ0aGFzOnBzd2QxMjM="
                    assertThat(authHeader).isEqualTo("Basic YXJ0aGFzOnBzd2QxMjM=");
                    return httpResponse;
                });

        ServerInfo server = new ServerInfo();
        server.setId("server1");
        server.setName("Test Server");
        server.setHost("localhost");
        server.setHttpPort(8563);
        server.setUsername("arthas");
        server.setPassword("pswd123");
        ArthasResponse response = arthasHttpClient.executeCommand(server, "version");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
    }

    @Test
    void httpBasicAuth_checkConnection_usesCorrectHeader() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    String authHeader = request.headers().firstValue("Authorization").get();
                    assertThat(authHeader).isEqualTo("Basic YXJ0aGFzOnBzd2QxMjM=");
                    return httpResponse;
                });

        ServerInfo server = new ServerInfo();
        server.setUsername("arthas");
        server.setPassword("pswd123");
        boolean connected = arthasHttpClient.checkConnection(server);

        assertThat(connected).isTrue();
    }

    @Test
    void bearerToken_stillSupported_forBackwardCompatibility() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"state\":\"SUCCEEDED\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    String authHeader = request.headers().firstValue("Authorization").get();
                    assertThat(authHeader).isEqualTo("Bearer legacy-token");
                    return httpResponse;
                });

        ServerInfo server = new ServerInfo();
        server.setToken("legacy-token");
        ArthasResponse response = arthasHttpClient.executeCommand(server, "version");

        assertThat(response.getState()).isEqualTo("SUCCEEDED");
    }

    @Test
    void initSession_returnsSessionInfo() throws Exception {
        String initResponse = """
            {
              "sessionId": "new-session-id",
              "consumerId": "new-consumer-id",
              "state": "SUCCEEDED"
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(initResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        SessionInfo sessionInfo = arthasHttpClient.initSession(server);

        assertThat(sessionInfo).isNotNull();
        assertThat(sessionInfo.getSessionId()).isEqualTo("new-session-id");
        assertThat(sessionInfo.getConsumerId()).isEqualTo("new-consumer-id");
    }

    @Test
    void initSession_returnsNullOnFailure() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        SessionInfo sessionInfo = arthasHttpClient.initSession(server);

        assertThat(sessionInfo).isNull();
    }

    @Test
    void joinSession_returnsConsumerId() throws Exception {
        String joinResponse = """
            {
              "consumerId": "joined-consumer-id",
              "state": "SUCCEEDED"
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(joinResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        String consumerId = arthasHttpClient.joinSession(server, "existing-session");

        assertThat(consumerId).isEqualTo("joined-consumer-id");
    }

    @Test
    void joinSession_returnsNullOnFailure() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        String consumerId = arthasHttpClient.joinSession(server, "bad-session");

        assertThat(consumerId).isNull();
    }

    @Test
    void asyncExecuteCommand_returnsScheduledResponse() throws Exception {
        String asyncResponse = """
            {
              "state": "SCHEDULED",
              "sessionId": "test-session",
              "body": {
                "jobId": 42
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(asyncResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasApiResponse response = arthasHttpClient.asyncExecuteCommand(
                server, "trace *.* *", "test-session");

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("SCHEDULED");
        assertThat(response.getSessionId()).isEqualTo("test-session");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getJobId()).isEqualTo(42);
        assertThat(response.getRawResponse()).isEqualTo(asyncResponse);
    }

    @Test
    void asyncExecuteCommand_returnsFailedOnError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        ArthasApiResponse response = arthasHttpClient.asyncExecuteCommand(
                server, "trace *.* *", "test-session");

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("failed");
    }

    @Test
    void asyncExecuteCommand_handlesException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.ConnectException("Connection refused"));

        ServerInfo server = createTestServer();
        ArthasApiResponse response = arthasHttpClient.asyncExecuteCommand(
                server, "trace *.* *", "test-session");

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("failed");
        assertThat(response.getRawResponse()).contains("Connection refused");
    }

    @Test
    void pullResults_returnsResultList() throws Exception {
        String pullResponse = """
            {
              "state": "SUCCEEDED",
              "body": {
                "results": [
                  {
                    "type": "trace",
                    "data": {
                      "class": "com.example.Test",
                      "method": "doSomething",
                      "cost": 1234
                    }
                  },
                  {
                    "type": "trace",
                    "data": {
                      "class": "com.example.Test",
                      "method": "doSomething",
                      "cost": 5678
                    }
                  }
                ]
              }
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(pullResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        List<ArthasResult> results = arthasHttpClient.pullResults(
                server, "test-session", "test-consumer");

        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getType()).isEqualTo("trace");
        assertThat(results.get(0).getData()).containsEntry("cost", 1234);
    }

    @Test
    void pullResults_returnsEmptyListOnFailure() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        List<ArthasResult> results = arthasHttpClient.pullResults(
                server, "test-session", "test-consumer");

        assertThat(results).isNotNull();
        assertThat(results).isEmpty();
    }

    @Test
    void pullResults_returnsEmptyListOnException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.ConnectException("Connection refused"));

        ServerInfo server = createTestServer();
        List<ArthasResult> results = arthasHttpClient.pullResults(
                server, "test-session", "test-consumer");

        assertThat(results).isNotNull();
        assertThat(results).isEmpty();
    }

    @Test
    void interruptJob_returnsTrueOnSuccess() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        boolean result = arthasHttpClient.interruptJob(server, "test-session");

        assertThat(result).isTrue();
    }

    @Test
    void interruptJob_returnsFalseOnFailure() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        boolean result = arthasHttpClient.interruptJob(server, "test-session");

        assertThat(result).isFalse();
    }

    @Test
    void interruptJob_returnsFalseOnException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.ConnectException("Connection refused"));

        ServerInfo server = createTestServer();
        boolean result = arthasHttpClient.interruptJob(server, "test-session");

        assertThat(result).isFalse();
    }

    @Test
    void closeSession_returnsTrueOnSuccess() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        boolean result = arthasHttpClient.closeSession(server, "test-session");

        assertThat(result).isTrue();
    }

    @Test
    void closeSession_returnsFalseOnFailure() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        ServerInfo server = createTestServer();
        boolean result = arthasHttpClient.closeSession(server, "test-session");

        assertThat(result).isFalse();
    }

    @Test
    void closeSession_returnsFalseOnException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.net.ConnectException("Connection refused"));

        ServerInfo server = createTestServer();
        boolean result = arthasHttpClient.closeSession(server, "test-session");

        assertThat(result).isFalse();
    }
}
