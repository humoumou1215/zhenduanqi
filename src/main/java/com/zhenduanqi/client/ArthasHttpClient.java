package com.zhenduanqi.client;

import com.zhenduanqi.model.ArthasResponse;
import com.zhenduanqi.model.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ArthasHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ArthasHttpClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;

    public ArthasHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public ArthasResponse executeCommand(ServerInfo server, String command) {
        ArthasResponse response = new ArthasResponse();

        try {
            String apiUrl = server.getApiUrl();
            String jsonBody = String.format("{\"action\":\"exec\",\"command\":\"%s\"}", escapeJson(command));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + server.getToken())
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Executing command on {}: {}", server.getName(), command);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                response.setState("failed");
                response.setError("HTTP " + httpResponse.statusCode() + ": " + httpResponse.body());
                log.error("Arthas API returned status {}: {}", httpResponse.statusCode(), httpResponse.body());
                return response;
            }

            String body = httpResponse.body();
            log.debug("Arthas raw response: {}", body);

            parseStreamingResponse(body, response);
            response.setRawResponse(body);

        } catch (java.net.ConnectException e) {
            response.setState("failed");
            response.setError("无法连接到服务器: " + e.getMessage());
            log.error("Connection failed: {}", e.getMessage());
        } catch (java.net.http.HttpConnectTimeoutException e) {
            response.setState("failed");
            response.setError("连接超时: " + e.getMessage());
        } catch (java.net.http.HttpTimeoutException e) {
            response.setState("failed");
            response.setError("请求超时: " + e.getMessage());
        } catch (Exception e) {
            response.setState("failed");
            response.setError("请求异常: " + e.getMessage());
            log.error("Arthas API request failed", e);
        }

        return response;
    }

    private void parseStreamingResponse(String body, ArthasResponse response) {
        String[] lines = body.split("\n");
        StringBuilder resultBuilder = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                if (line.contains("\"type\":\"status\"")) {
                    String status = extractJsonValue(line, "content");
                    if (status != null) {
                        response.setState(status);
                    }
                } else if (line.contains("\"type\":\"result\"")) {
                    String content = extractResultContent(line);
                    if (content != null && !content.isEmpty()) {
                        if (resultBuilder.length() > 0) {
                            resultBuilder.append("\n");
                        }
                        resultBuilder.append(content);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse response line: {}", line, e);
            }
        }

        if (resultBuilder.length() > 0) {
            response.getResults().add(resultBuilder.toString());
        }

        if (response.getState() == null) {
            if (body.contains("\"success\":true") || body.contains("succeeded")) {
                response.setState("succeeded");
            } else if (body.contains("\"success\":false") || body.contains("failed")) {
                response.setState("failed");
                response.setError(body);
            } else {
                response.setState("succeeded");
                response.getResults().add(body);
            }
        }
    }

    private String extractResultContent(String jsonLine) {
        try {
            int contentStart = jsonLine.indexOf("\"content\":");
            if (contentStart < 0) {
                return null;
            }

            String afterContent = jsonLine.substring(contentStart + "\"content\":".length());
            String trimmed = afterContent.trim();

            if (trimmed.startsWith("\"")) {
                if (trimmed.length() > 1) {
                    int endQuote = trimmed.indexOf("\"", 1);
                    if (endQuote > 0) {
                        return trimmed.substring(1, endQuote);
                    }
                    return trimmed.substring(1);
                }
                return "";
            }

            if (trimmed.startsWith("{")) {
                int braceDepth = 0;
                int contentObjEnd = -1;
                for (int i = 0; i < trimmed.length(); i++) {
                    char c = trimmed.charAt(i);
                    if (c == '{') {
                        braceDepth++;
                    } else if (c == '}') {
                        braceDepth--;
                        if (braceDepth == 0) {
                            contentObjEnd = i + 1;
                            break;
                        }
                    }
                }
                if (contentObjEnd > 0) {
                    String contentObj = trimmed.substring(0, contentObjEnd);
                    String resultValue = extractJsonValue(contentObj, "result");
                    if (resultValue != null) {
                        return resultValue;
                    }
                    return contentObj;
                }
            }

            return trimmed;
        } catch (Exception e) {
            log.warn("Failed to extract result content from: {}", jsonLine, e);
            return null;
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyStart = json.indexOf(searchKey);
        if (keyStart < 0) {
            return null;
        }

        int valueStart = keyStart + searchKey.length();
        String afterKey = json.substring(valueStart).trim();

        if (afterKey.startsWith("\"")) {
            int endQuote = afterKey.indexOf("\"", 1);
            if (endQuote > 0) {
                return afterKey.substring(1, endQuote);
            }
            return afterKey.substring(1);
        }

        if (afterKey.startsWith("true")) {
            return "true";
        }
        if (afterKey.startsWith("false")) {
            return "false";
        }

        int commaIndex = afterKey.indexOf(",");
        int braceIndex = afterKey.indexOf("}");
        int endIndex;
        if (commaIndex > 0 && braceIndex > 0) {
            endIndex = Math.min(commaIndex, braceIndex);
        } else if (commaIndex > 0) {
            endIndex = commaIndex;
        } else if (braceIndex > 0) {
            endIndex = braceIndex;
        } else {
            endIndex = afterKey.length();
        }

        return afterKey.substring(0, endIndex).trim();
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

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + server.getToken())
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return httpResponse.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Connection check failed for {}: {}", server.getName(), e.getMessage());
            return false;
        }
    }
}
