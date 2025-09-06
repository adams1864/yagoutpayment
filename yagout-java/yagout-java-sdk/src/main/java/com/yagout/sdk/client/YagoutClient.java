package com.yagout.sdk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yagout.sdk.crypto.CryptoUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class YagoutClient {
    private final String apiUrl;
    private final String meId;
    private final String merchantKeyBase64;
    private final HttpClient http;
    private final ObjectMapper om = new ObjectMapper();

    public YagoutClient(String apiUrl, String meId, String merchantKeyBase64) {
        this.apiUrl = apiUrl;
        this.meId = meId;
        this.merchantKeyBase64 = merchantKeyBase64;
        this.http = HttpClient.newHttpClient();
    }

    public Map<String, Object> callDirectApi(Map<String, Object> plainObj) {
        try {
            String plainJson = om.writeValueAsString(plainObj);
            String merchantRequest = CryptoUtil.encryptBase64(plainJson, merchantKeyBase64);

            Map<String, Object> outReq = new HashMap<>();
            outReq.put("merchantId", meId);
            outReq.put("merchantRequest", merchantRequest);

            String body = om.writeValueAsString(outReq);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String text = resp.body();
            Map<String, Object> result = new HashMap<>();
            result.put("request", outReq);

            try {
                JsonNode json = om.readTree(text);
                result.put("response", om.convertValue(json, Map.class));
                if (json.hasNonNull("response")) {
                    String enc = json.get("response").asText();
                    try {
                        String decrypted = CryptoUtil.decryptBase64(enc, merchantKeyBase64);
                        result.put("decrypted", decrypted);
                        try {
                            result.put("decryptedJson", om.readTree(decrypted));
                        } catch (Exception ignored) {}
                    } catch (Exception e) {
                        result.put("decrypted", "Decryption error: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                result.put("raw", text);
            }
            result.put("status", resp.statusCode());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Direct API call failed: " + e.getMessage(), e);
        }
    }
}
