package com.project.complaintsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // Override with gemini.model in application.properties.
    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private final RestTemplate restTemplate;

    public AiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, String> analyzeComplaint(String text) {

        try {
            return callAI(text);
        } catch (Exception e) {
            System.out.println("AI FAILED → Using fallback: " + e.getMessage());
            return fallbackAnalysis(text);
        }
    }

    // ===================== AI CALL =====================
    private Map<String, String> callAI(String text) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        String prompt = "Classify complaint into category and priority.\n" +
                "Categories: WIFI, ELECTRICITY, HOSTEL, MAINTENANCE\n" +
                "Priority: LOW, MEDIUM, HIGH\n\n" +
                "Return JSON only:\n" +
                "{\"category\":\"ELECTRICITY\",\"priority\":\"HIGH\"}\n\n" +
                "Complaint: " + text;

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map responseBody = response.getBody();

        Map candidate = (Map) ((List) responseBody.get("candidates")).get(0);
        Map content = (Map) candidate.get("content");
        List parts = (List) content.get("parts");
        Map textPart = (Map) parts.get(0);

        String output = (String) textPart.get("text");

        return extractFromOutput(output);
    }

    // ===================== PARSE AI =====================
    private Map<String, String> extractFromOutput(String output) {

        String upper = output.toUpperCase();

        String category = "GENERAL";
        String priority = "LOW";

        if (upper.contains("ELECTRICITY")) category = "ELECTRICITY";
        else if (upper.contains("WIFI")) category = "WIFI";
        else if (upper.contains("HOSTEL")) category = "HOSTEL";
        else if (upper.contains("MAINTENANCE")) category = "MAINTENANCE";

        if (upper.contains("HIGH")) priority = "HIGH";
        else if (upper.contains("MEDIUM")) priority = "MEDIUM";

        return Map.of("category", category, "priority", priority);
    }

    // ===================== FALLBACK LOGIC =====================
    private Map<String, String> fallbackAnalysis(String text) {

        String lower = text.toLowerCase();

        String category = "GENERAL";
        String priority = "LOW";

        // ===== CATEGORY DETECTION =====
        if (containsAny(lower, "spark", "shock", "wire", "electric", "switch", "power", "short circuit")) {
            category = "ELECTRICITY";
        } else if (containsAny(lower, "wifi", "internet", "network", "slow net", "no connection")) {
            category = "WIFI";
        } else if (containsAny(lower, "room", "hostel", "bed", "washroom", "water", "mess")) {
            category = "HOSTEL";
        } else if (containsAny(lower, "fan", "light", "broken", "repair", "maintenance", "leak")) {
            category = "MAINTENANCE";
        }

        // ===== PRIORITY DETECTION =====
        if (containsAny(lower, "fire", "spark", "shock", "danger", "urgent", "burn")) {
            priority = "HIGH";
        } else if (containsAny(lower, "not working", "issue", "problem", "slow")) {
            priority = "MEDIUM";
        }

        System.out.println("FALLBACK RESULT → " + category + " | " + priority);

        return Map.of("category", category, "priority", priority);
    }

    // ===================== HELPER =====================
    private boolean containsAny(String text, String... keywords) {
        for (String word : keywords) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}