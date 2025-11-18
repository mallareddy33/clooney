package com.clooney.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Real OpenAI-backed implementation of LLMClient.
 *
 * Uses Java 11 HttpClient and OpenAI's chat completions-style API.
 */
public class OpenAiLLMClient implements LLMClient {

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    // You can change this to the exact endpoint you want to hit.
    // Example placeholder:
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAiLLMClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String complete(String prompt) {
        try {
            String requestBody = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenAI API error: " + response.statusCode()
                        + " body=" + response.body());
            }

            return extractContent(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("OpenAI API call failed", e);
        }
    }

    private String buildRequestBody(String prompt) throws IOException {
        // Simple chat-completions style payload:
        // {
        //   "model": "gpt-4.1-mini",
        //   "messages": [
        //     { "role": "user", "content": "<prompt>" }
        //   ]
        // }

        // Construct JSON manually or via Jackson; here we just build a String:
        String escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");

        return """
               {
                 "model": "%s",
                 "messages": [
                   {
                     "role": "user",
                     "content": "%s"
                   }
                 ]
               }
               """.formatted(model, escapedPrompt);
    }

    private String extractContent(String responseJson) throws IOException {
        JsonNode root = mapper.readTree(responseJson);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("Unexpected OpenAI response: " + responseJson);
        }
        JsonNode first = choices.get(0);
        JsonNode message = first.get("message");
        if (message == null) {
            throw new RuntimeException("Unexpected OpenAI response: " + responseJson);
        }
        JsonNode contentNode = message.get("content");
        if (contentNode == null) {
            throw new RuntimeException("Unexpected OpenAI response: " + responseJson);
        }
        return contentNode.asText();
    }
}
