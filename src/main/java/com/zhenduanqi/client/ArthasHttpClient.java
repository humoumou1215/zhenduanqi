package com.zhenduanqi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhenduanqi.model.ArthasApiResponse;
import com.zhenduanqi.model.ArthasResponse;
import com.zhenduanqi.model.ArthasResult;
import com.zhenduanqi.model.ServerInfo;
import com.zhenduanqi.model.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class ArthasHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ArthasHttpClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;

    public ArthasHttpClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public ArthasHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ArthasResponse executeCommand(ServerInfo server, String command) {
        ArthasResponse response = new ArthasResponse();

        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format("{\"action\":\"exec\",\"command\":\"%s\"}", escapeJson(command));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            
            addAuthorizationHeader(requestBuilder, server);
            
            HttpRequest request = requestBuilder.build();

            log.info("Executing command on {}: {}", server.getName(), command);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = httpResponse.body();
            response.setRawResponse(body);
            log.debug("Arthas raw response: {}", body);

            if (httpResponse.statusCode() != 200) {
                response.setState("failed");
                String errorMessage = body != null && !body.isEmpty() 
                        ? body 
                        : getHttpErrorMessage(httpResponse.statusCode());
                response.setError("HTTP " + httpResponse.statusCode() + ": " + errorMessage);
                log.error("Arthas API returned status {}: {}", httpResponse.statusCode(), body);
                return response;
            }

            parseResponseWithJackson(body, response);

        } catch (java.net.ConnectException e) {
            response.setState("failed");
            response.setError("无法连接到服务器: " + e.getMessage());
            response.setRawResponse("连接错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            log.error("Connection failed: {}", e.getMessage());
        } catch (java.net.http.HttpConnectTimeoutException e) {
            response.setState("failed");
            response.setError("连接超时: " + e.getMessage());
            response.setRawResponse("连接超时: " + e.getMessage());
        } catch (java.net.http.HttpTimeoutException e) {
            response.setState("failed");
            response.setError("请求超时: " + e.getMessage());
            response.setRawResponse("请求超时: " + e.getMessage());
        } catch (Exception e) {
            response.setState("failed");
            response.setError("请求异常: " + e.getMessage());
            response.setRawResponse("请求异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            log.error("Arthas API request failed", e);
        }

        return response;
    }
    
    private void addAuthorizationHeader(HttpRequest.Builder requestBuilder, ServerInfo server) {
        if (server.getUsername() != null && server.getPassword() != null) {
            String credentials = server.getUsername() + ":" + server.getPassword();
            String encodedCredentials = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8));
            requestBuilder.header("Authorization", "Basic " + encodedCredentials);
        } else if (server.getToken() != null) {
            requestBuilder.header("Authorization", "Bearer " + server.getToken());
        }
    }

    private void parseResponseWithJackson(String body, ArthasResponse response) {
        try {
            ArthasApiResponse apiResponse = objectMapper.readValue(body, ArthasApiResponse.class);
            
            response.setState(apiResponse.getState());
            
            if (apiResponse.getBody() != null && apiResponse.getBody().getResults() != null) {
                response.setStructuredResults(apiResponse.getBody().getResults());
                
                List<String> textResults = new ArrayList<>();
                for (ArthasResult result : apiResponse.getBody().getResults()) {
                    try {
                        textResults.add(objectMapper.writeValueAsString(result));
                    } catch (Exception e) {
                        log.warn("Failed to serialize result to string", e);
                    }
                }
                response.setResults(textResults);
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse structured JSON, falling back to raw text", e);
            fallbackToRawText(body, response);
        }
    }

    private void fallbackToRawText(String body, ArthasResponse response) {
        if (body.contains("\"success\":true") || body.contains("succeeded") || body.contains("SUCCEEDED")) {
            response.setState("succeeded");
        } else if (body.contains("\"success\":false") || body.contains("failed") || body.contains("FAILED")) {
            response.setState("failed");
            response.setError(body);
        } else {
            response.setState("succeeded");
        }
        response.getResults().add(body);
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public boolean checkConnection(ServerInfo server) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = "{\"action\":\"exec\",\"command\":\"version\"}";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            
            addAuthorizationHeader(requestBuilder, server);
            
            HttpRequest request = requestBuilder.build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return httpResponse.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Connection check failed for {}: {}", server.getName(), e.getMessage());
            return false;
        }
    }

    public ArthasResponse executeCommand(ServerInfo server, String sessionId, String command) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format("{\"action\":\"exec\",\"command\":\"%s\",\"sessionId\":\"%s\"}",
                    escapeJson(command), sessionId);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(requestBuilder, server);

            HttpRequest request = requestBuilder.build();

            log.info("在会话 {} 执行命令: {}", sessionId, command);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = httpResponse.body();
            ArthasResponse response = new ArthasResponse();
            response.setRawResponse(body);

            if (httpResponse.statusCode() != 200) {
                response.setState("failed");
                response.setError("HTTP " + httpResponse.statusCode());
                return response;
            }

            parseResponseWithJackson(body, response);
            return response;
        } catch (Exception e) {
            log.error("在会话中执行命令失败", e);
            ArthasResponse response = new ArthasResponse();
            response.setState("failed");
            response.setError(e.getMessage());
            return response;
        }
    }

    public ArthasResponse executeSystemCommand(ServerInfo server, String sessionId, String command) {
        return executeCommand(server, sessionId, command);
    }

    private String getHttpErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "请求格式错误";
            case 401 -> "未授权，请检查 Token";
            case 403 -> "权限不足";
            case 404 -> "API 地址不存在";
            case 500 -> "Arthas 服务内部错误";
            case 502 -> "Arthas 服务不可用或未启动";
            case 503 -> "Arthas 服务暂时不可用";
            case 504 -> "Arthas 服务响应超时";
            default -> "未知错误";
        };
    }

    public SessionInfo initSession(ServerInfo server) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = "{\"action\":\"init_session\"}";

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(requestBuilder, server);

            HttpRequest request = requestBuilder.build();

            log.info("在 {} 初始化会话", server.getName());
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.error("初始化会话失败，状态码: {}", httpResponse.statusCode());
                return null;
            }

            String body = httpResponse.body();
            ArthasApiResponse apiResponse = objectMapper.readValue(body, ArthasApiResponse.class);

            if (apiResponse.getSessionId() != null && apiResponse.getConsumerId() != null) {
                return new SessionInfo(apiResponse.getSessionId(), apiResponse.getConsumerId());
            }

            log.error("会话初始化响应缺少 sessionId 或 consumerId: {}", body);
            return null;

        } catch (Exception e) {
            log.error("初始化会话失败", e);
            return null;
        }
    }

    public String joinSession(ServerInfo server, String sessionId) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format("{\"action\":\"join_session\",\"sessionId\":\"%s\"}", sessionId);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(requestBuilder, server);

            HttpRequest request = requestBuilder.build();

            log.info("加入会话 {} 在 {}", sessionId, server.getName());
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.error("加入会话失败，状态码: {}", httpResponse.statusCode());
                return null;
            }

            String body = httpResponse.body();
            ArthasApiResponse apiResponse = objectMapper.readValue(body, ArthasApiResponse.class);

            if (apiResponse.getConsumerId() != null) {
                return apiResponse.getConsumerId();
            }

            log.error("加入会话响应缺少 consumerId: {}", body);
            return null;

        } catch (Exception e) {
            log.error("加入会话失败", e);
            return null;
        }
    }

    public ArthasApiResponse asyncExecuteCommand(ServerInfo server, String command, String sessionId) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format(
                    "{\"action\":\"async_exec\",\"command\":\"%s\",\"sessionId\":\"%s\"}",
                    escapeJson(command), sessionId);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(requestBuilder, server);

            HttpRequest request = requestBuilder.build();

            log.info("异步执行命令在 {} 会话 {}: {}", server.getName(), sessionId, command);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = httpResponse.body();

            if (httpResponse.statusCode() != 200) {
                ArthasApiResponse errorResponse = new ArthasApiResponse();
                errorResponse.setState("failed");
                errorResponse.setRawResponse(body);
                return errorResponse;
            }

            ArthasApiResponse apiResponse = objectMapper.readValue(body, ArthasApiResponse.class);
            apiResponse.setRawResponse(body);
            return apiResponse;

        } catch (Exception e) {
            log.error("异步执行命令失败", e);
            ArthasApiResponse errorResponse = new ArthasApiResponse();
            errorResponse.setState("failed");
            errorResponse.setRawResponse("Exception: " + e.getMessage());
            return errorResponse;
        }
    }

    public List<ArthasResult> pullResults(ServerInfo server, String sessionId, String consumerId) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format(
                    "{\"action\":\"pull_results\",\"sessionId\":\"%s\",\"consumerId\":\"%s\"}",
                    sessionId, consumerId);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(requestBuilder, server);

            HttpRequest request = requestBuilder.build();

            log.debug("拉取结果，会话 {} 消费者 {}", sessionId, consumerId);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.error("拉取结果失败，状态码: {}", httpResponse.statusCode());
                return new ArrayList<>();
            }

            String body = httpResponse.body();
            ArthasApiResponse apiResponse = objectMapper.readValue(body, ArthasApiResponse.class);

            if (apiResponse.getBody() != null && apiResponse.getBody().getResults() != null) {
                return apiResponse.getBody().getResults();
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("拉取结果失败", e);
            return new ArrayList<>();
        }
    }

    public boolean interruptJob(ServerInfo server, String sessionId) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format(
                    "{\"action\":\"interrupt_job\",\"sessionId\":\"%s\"}", sessionId);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(requestBuilder, server);

            HttpRequest request = requestBuilder.build();

            log.info("中断任务，会话 {}", sessionId);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return httpResponse.statusCode() == 200;

        } catch (Exception e) {
            log.error("中断任务失败", e);
            return false;
        }
    }

    public boolean closeSession(ServerInfo server, String sessionId) {
        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format(
                    "{\"action\":\"close_session\",\"sessionId\":\"%s\"}", sessionId);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            addAuthorizationHeader(requestBuilder, server);

            HttpRequest request = requestBuilder.build();

            log.info("关闭会话 {}", sessionId);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return httpResponse.statusCode() == 200;

        } catch (Exception e) {
            log.error("关闭会话失败", e);
            return false;
        }
    }
}
