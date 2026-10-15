
package com.lostfound.controller;

import com.lostfound.service.GeminiImageAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final GeminiImageAnalysisService geminiImageAnalysisService;

    public AiController(
            GeminiImageAnalysisService geminiImageAnalysisService) {
        this.geminiImageAnalysisService = geminiImageAnalysisService;
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> analyzeImage(
            @RequestPart("file") MultipartFile file) throws IOException {

        try {
            Map<String, Object> suggestions = geminiImageAnalysisService.analyzeImage(file);

            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            log.error("HERO FIND AI image analysis failed: {}",
                    e.getMessage(), e);
            throw e;
        }
    }
}
