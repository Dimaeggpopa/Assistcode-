package com.smartkey.ai.keyboard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Thin synchronous wrapper around the Anthropic Messages API.
 * Must be called off the main/IME thread (see AIKeyboardService).
 */
public class AIClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String model;

    public AIClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface Callback {
        void onSuccess(String resultText);
        void onError(String message);
    }

    /**
     * Sends the given text with an instruction (e.g. "fix grammar") and
     * returns only the rewritten text, synchronously. Call from a background thread.
     */
    public String transform(String instruction, String sourceText) throws IOException {
        String systemPrompt = "You rewrite short pieces of text typed by a user on a mobile keyboard. "
                + "Follow the instruction exactly. Reply with ONLY the rewritten text — "
                + "no quotes, no explanation, no preamble.";

        String userPrompt = instruction + ":\n\n" + sourceText;

        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("max_tokens", 1024);
            body.put("system", systemPrompt);

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.put(userMessage);

            body.put("messages", messages);
        } catch (Exception e) {
            throw new IOException("Failed to build request body", e);
        }

        RequestBody requestBody = RequestBody.create(body.toString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("content-type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("API error " + response.code() + ": " + responseBody);
            }

            JSONObject json = new JSONObject(responseBody);
            JSONArray content = json.optJSONArray("content");
            if (content == null || content.length() == 0) {
                throw new IOException("Empty response from API");
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                JSONObject block = content.getJSONObject(i);
                if ("text".equals(block.optString("type"))) {
                    result.append(block.optString("text"));
                }
            }
            return result.toString().trim();

        } catch (org.json.JSONException e) {
            throw new IOException("Failed to parse API response", e);
        }
    }
}
