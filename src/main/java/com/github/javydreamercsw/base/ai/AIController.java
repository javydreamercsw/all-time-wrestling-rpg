package com.github.javydreamercsw.base.ai;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for AI-powered wrestling analysis and content generation. */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

  private final AIServiceFactory aiServiceFactory;

  /** Get information about available AI services. */
  @GetMapping("/services")
  public ResponseEntity<Map<String, Object>> getServices() {
    return ResponseEntity.ok(
        Map.of(
            "currentProvider",
            aiServiceFactory.getCurrentProviderName(),
            "availableServices",
            aiServiceFactory.getAvailableServices(),
            "anyAvailable",
            aiServiceFactory.isAnyServiceAvailable()));
  }

  /** Analyze a wrestler by name using AI. */
  @GetMapping("/analyze/wrestler/{name}")
  public ResponseEntity<Map<String, Object>> analyzeWrestler(@PathVariable String name) {
    AIService aiService = aiServiceFactory.getAIService();
    if (aiService == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No AI service available"));
    }

    try {
      // Load wrestler data from Notion
      Optional<com.github.javydreamercsw.base.ai.notion.WrestlerPage> wrestlerPage =
          NotionHandler.loadWrestlerStatic(name);

      if (wrestlerPage.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      // Convert to map for AI analysis
      Map<String, Object> wrestlerData =
          Map.of(
              "Name",
              name,
              "ID",
              wrestlerPage.get().getId(),
              "URL",
              wrestlerPage.get().getUrl(),
              "Properties",
              wrestlerPage.get().getRawProperties() != null
                  ? wrestlerPage.get().getRawProperties()
                  : Map.of());

      String analysis = aiService.analyzeWrestler(wrestlerData);

      return ResponseEntity.ok(
          Map.of(
              "wrestler",
              name,
              "provider",
              aiService.getProviderName(),
              "analysis",
              analysis,
              "data",
              wrestlerData));

    } catch (Exception e) {
      log.error("Error analyzing wrestler: {}", name, e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Predict match outcome between two wrestlers. */
  @GetMapping("/predict/match")
  public ResponseEntity<Map<String, Object>> predictMatch(
      @RequestParam String wrestler1, @RequestParam String wrestler2) {

    AIService aiService = aiServiceFactory.getAIService();
    if (aiService == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No AI service available"));
    }

    try {
      // Load both wrestlers
      Optional<com.github.javydreamercsw.base.ai.notion.WrestlerPage> wrestler1Page =
          NotionHandler.loadWrestlerStatic(wrestler1);
      Optional<com.github.javydreamercsw.base.ai.notion.WrestlerPage> wrestler2Page =
          NotionHandler.loadWrestlerStatic(wrestler2);

      if (wrestler1Page.isEmpty() || wrestler2Page.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      // Convert to maps for AI analysis
      Map<String, Object> wrestler1Data =
          Map.of(
              "Name",
              wrestler1,
              "Properties",
              wrestler1Page.get().getRawProperties() != null
                  ? wrestler1Page.get().getRawProperties()
                  : Map.of());

      Map<String, Object> wrestler2Data =
          Map.of(
              "Name",
              wrestler2,
              "Properties",
              wrestler2Page.get().getRawProperties() != null
                  ? wrestler2Page.get().getRawProperties()
                  : Map.of());

      String prediction = aiService.predictMatch(wrestler1Data, wrestler2Data);

      return ResponseEntity.ok(
          Map.of(
              "wrestler1",
              wrestler1,
              "wrestler2",
              wrestler2,
              "provider",
              aiService.getProviderName(),
              "prediction",
              prediction));

    } catch (Exception e) {
      log.error("Error predicting match: {} vs {}", wrestler1, wrestler2, e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Analyze a show by name using AI. */
  @GetMapping("/analyze/show/{name}")
  public ResponseEntity<Map<String, Object>> analyzeShow(@PathVariable String name) {
    AIService aiService = aiServiceFactory.getAIService();
    if (aiService == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No AI service available"));
    }

    try {
      // Load show data from Notion
      Optional<com.github.javydreamercsw.base.ai.notion.ShowPage> showPage =
          NotionHandler.loadShowStatic(name);

      if (showPage.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      // Get matches from the show
      List<com.github.javydreamercsw.base.ai.notion.MatchPage> matches =
          showPage.get().getMatches();

      // Convert to maps for AI analysis
      Map<String, Object> showData =
          Map.of(
              "Name",
              name,
              "ID",
              showPage.get().getId(),
              "URL",
              showPage.get().getUrl(),
              "Properties",
              showPage.get().getRawProperties() != null
                  ? showPage.get().getRawProperties()
                  : Map.of());

      List<Map<String, Object>> matchesData =
          matches.stream()
              .map(
                  match ->
                      Map.of(
                          "ID",
                          match.getId(),
                          "Properties",
                          match.getRawProperties() != null
                              ? match.getRawProperties()
                              : Map.<String, Object>of()))
              .toList();

      String analysis = aiService.analyzeShow(showData, matchesData);

      return ResponseEntity.ok(
          Map.of(
              "show",
              name,
              "provider",
              aiService.getProviderName(),
              "analysis",
              analysis,
              "matchCount",
              matches.size()));

    } catch (Exception e) {
      log.error("Error analyzing show: {}", name, e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Generate creative content based on a prompt. */
  @PostMapping("/generate/creative")
  public ResponseEntity<Map<String, Object>> generateCreativeContent(
      @RequestBody Map<String, Object> request) {

    AIService aiService = aiServiceFactory.getAIService();
    if (aiService == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No AI service available"));
    }

    try {
      String prompt = (String) request.get("prompt");
      @SuppressWarnings("unchecked")
      Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", Map.of());

      if (prompt == null || prompt.trim().isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
      }

      String content = aiService.generateCreativeContent(prompt, context);

      return ResponseEntity.ok(
          Map.of("prompt", prompt, "provider", aiService.getProviderName(), "content", content));

    } catch (Exception e) {
      log.error("Error generating creative content", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Summarize data with specified summary type. */
  @PostMapping("/summarize")
  public ResponseEntity<Map<String, Object>> summarizeData(
      @RequestBody Map<String, Object> request) {

    AIService aiService = aiServiceFactory.getAIService();
    if (aiService == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No AI service available"));
    }

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) request.get("data");
      String summaryTypeStr = (String) request.getOrDefault("summaryType", "BRIEF");

      AIService.SummaryType summaryType =
          AIService.SummaryType.valueOf(summaryTypeStr.toUpperCase());

      String summary = aiService.summarizeData(data, summaryType);

      return ResponseEntity.ok(
          Map.of(
              "summaryType",
              summaryType,
              "provider",
              aiService.getProviderName(),
              "summary",
              summary));

    } catch (Exception e) {
      log.error("Error summarizing data", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
}
