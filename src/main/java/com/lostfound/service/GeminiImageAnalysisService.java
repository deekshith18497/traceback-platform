
package com.lostfound.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GeminiImageAnalysisService {

        private final RestClient restClient;
        private final JsonMapper jsonMapper;

        @Value("${app.gemini.api-key:}")
        private String apiKey;

        @Value("${app.gemini.model:gemini-3.5-flash-lite}")
        private String model;

        private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

        private static final Set<String> ALLOWED_TYPES = Set.of(
                        "image/jpeg",
                        "image/png",
                        "image/webp");

        private static final Set<String> ALLOWED_CATEGORIES = Set.of(
                        "Electronics",
                        "Bags",
                        "Documents",
                        "Keys",
                        "Clothing",
                        "Accessories",
                        "Other");

        public GeminiImageAnalysisService() {
                this.restClient = RestClient.create();
                this.jsonMapper = JsonMapper.builder().build();
        }

        public Map<String, Object> analyzeImage(MultipartFile file)
                        throws IOException {

                // 1. Validate the uploaded file.
                if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Please upload an image.");
                }

                if (file.getSize() > MAX_FILE_SIZE) {
                        throw new IllegalArgumentException(
                                        "Image size must not exceed 5 MB.");
                }

                String contentType = file.getContentType();

                if (!StringUtils.hasText(contentType)
                                || !ALLOWED_TYPES.contains(
                                                contentType.toLowerCase())) {
                        throw new IllegalArgumentException(
                                        "Only JPEG, PNG, and WebP images are allowed.");
                }

                // 2. Check Gemini configuration.
                if (!StringUtils.hasText(apiKey)) {
                        throw new IllegalStateException(
                                        "Gemini API key is not configured. "
                                                        + "Set GEMINI_API_KEY in your environment.");
                }

                if (!StringUtils.hasText(model)) {
                        throw new IllegalStateException(
                                        "Gemini model is not configured.");
                }

                // 3. Prepare the image and prompt.
                String base64Image = Base64.getEncoder()
                                .encodeToString(file.getBytes());

                String prompt = """
                                You are the image analysis assistant for HERO FIND,
                                a lost-and-found application.

                                Decide whether this photograph clearly shows a physical
                                object that someone could lose or find.

                                Suitable examples:
                                - Mobile phones, laptops, headphones and chargers
                                - Wallets, bags, backpacks and purses
                                - Keys and keychains
                                - Books, notebooks, documents and ID cards
                                - Watches, jewellery, glasses and clothing
                                - Bottles and other personal belongings

                                Unsuitable examples:
                                - Food or drinks being consumed
                                - Selfies and portraits
                                - Landscapes and unrelated scenes
                                - Images with no identifiable lost-and-found object

                                Return ONLY a valid JSON object with these fields:
                                {
                                  "suitable": true,
                                  "title": "short object title",
                                  "category": "one allowed category",
                                  "description": "visible identifying characteristics",
                                  "confidence": 0.95,
                                  "message": "short explanation"
                                }

                                Allowed categories:
                                Electronics, Bags, Documents, Keys, Clothing,
                                Accessories, Other.

                                Rules:
                                1. Set suitable to false if no reasonably identifiable
                                   lost-and-found object is visible.
                                2. For unsuitable images, use title "Unsuitable image",
                                   category "Other", description "", confidence 0.0,
                                   and explain the reason in message.
                                3. Describe only details visible in the photograph.
                                4. Never invent an owner, location, serial number,
                                   brand, or other unobservable detail.
                                5. Use Other if the object's category is uncertain.
                                6. Confidence must be between 0 and 1.
                                7. Keep the title short and description useful.
                                8. Do not include Markdown fences or text outside JSON.
                                """;

                // 4. Build the Gemini request.
                Map<String, Object> imageData = new LinkedHashMap<>();
                imageData.put("mime_type", contentType);
                imageData.put("data", base64Image);

                Map<String, Object> imagePart = new LinkedHashMap<>();
                imagePart.put("inline_data", imageData);

                Map<String, Object> promptPart = new LinkedHashMap<>();
                promptPart.put("text", prompt);

                Map<String, Object> content = new LinkedHashMap<>();
                content.put("parts", List.of(promptPart, imagePart));

                Map<String, Object> generationConfig = new LinkedHashMap<>();
                generationConfig.put("responseMimeType", "application/json");
                generationConfig.put("temperature", 0.2);

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("contents", List.of(content));
                requestBody.put("generationConfig", generationConfig);

                String url = "https://generativelanguage.googleapis.com"
                                + "/v1beta/models/" + model + ":generateContent";

                // 5. Call Gemini and receive the raw JSON response.
                String responseBody = restClient.post()
                                .uri(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("x-goog-api-key", apiKey)
                                .body(requestBody)
                                .retrieve()
                                .body(String.class);

                if (!StringUtils.hasText(responseBody)) {
                        throw new IllegalStateException(
                                        "Gemini returned an empty response.");
                }

                // 6. Extract the AI-generated text.
                JsonNode geminiResponse = jsonMapper.readTree(responseBody);

                JsonNode candidates = geminiResponse.path("candidates");

                if (!candidates.isArray() || candidates.isEmpty()) {
                        throw new IllegalStateException(
                                        "Gemini did not return an analysis. "
                                                        + "Please try another image.");
                }

                JsonNode parts = candidates.get(0)
                                .path("content")
                                .path("parts");

                StringBuilder aiText = new StringBuilder();

                if (parts.isArray()) {
                        for (JsonNode part : parts) {
                                JsonNode textNode = part.path("text");

                                if (!textNode.isMissingNode()
                                                && !textNode.isNull()) {
                                        String text = textNode.asString();

                                        if (text != null) {
                                                aiText.append(text);
                                        }
                                }
                        }
                }

                if (aiText.isEmpty()) {
                        throw new IllegalStateException(
                                        "Gemini returned no usable analysis text.");
                }

                // 7. Parse the JSON generated by Gemini.
                String cleanJson = aiText.toString().trim();

                if (cleanJson.startsWith("```")) {
                        cleanJson = cleanJson
                                        .replaceFirst("^```(?:json)?\\s*", "")
                                        .replaceFirst("\\s*```$", "")
                                        .trim();
                }

                JsonNode parsed = jsonMapper.readTree(cleanJson);

                boolean suitable = parsed.path("suitable")
                                .asBoolean(false);

                String title = getText(parsed, "title", "Unknown item");
                String category = getText(parsed, "category", "Other");
                String description = getText(parsed, "description", "");
                String message = getText(parsed, "message", "");

                double confidence = parsed.path("confidence")
                                .asDouble(0.0);

                // 8. Validate and normalize the AI output.
                if (!ALLOWED_CATEGORIES.contains(category)) {
                        category = "Other";
                }

                confidence = Math.max(0.0, Math.min(1.0, confidence));

                if (!suitable) {
                        title = "Unsuitable image";
                        category = "Other";
                        description = "";
                        confidence = 0.0;

                        if (!StringUtils.hasText(message)) {
                                message = "Please upload a clear photo of a "
                                                + "lost-and-found item.";
                        }
                } else {
                        if (!StringUtils.hasText(title)) {
                                title = "Unknown item";
                        }

                        if (!StringUtils.hasText(message)) {
                                message = "Image analyzed. Please review the "
                                                + "suggestions before submitting your report.";
                        }
                }

                // 9. Return all fields to the frontend.
                Map<String, Object> result = new LinkedHashMap<>();

                result.put("suitable", suitable);
                result.put("title", title);
                result.put("category", category);
                result.put("description", description);
                result.put("confidence", confidence);
                result.put("message", message);

                return result;
        }

        private String getText(
                        JsonNode node,
                        String field,
                        String defaultValue) {

                JsonNode value = node.path(field);

                if (value.isMissingNode() || value.isNull()) {
                        return defaultValue;
                }

                String text = value.asString();

                return text == null ? defaultValue : text;
        }
}
