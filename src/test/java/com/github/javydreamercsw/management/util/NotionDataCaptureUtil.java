package com.github.javydreamercsw.management.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javydreamercsw.base.ai.notion.MatchPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Utility to capture real Notion API responses and save them as JSON files for unit tests. This
 * allows us to create realistic test data based on actual Notion responses.
 *
 * <p>Usage: mvn spring-boot:run -Dspring-boot.run.main-class=...NotionDataCaptureUtil
 * -DNOTION_TOKEN=xxx
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "com.github.javydreamercsw")
public class NotionDataCaptureUtil implements CommandLineRunner {

  @Autowired private NotionSyncService notionSyncService;

  private static final ObjectMapper objectMapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Override
  public void run(String... args) throws Exception {
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      log.error("❌ NOTION_TOKEN not available. Cannot capture real data.");
      log.info(
          "💡 Usage: mvn spring-boot:run -Dspring-boot.run.main-class=...NotionDataCaptureUtil"
              + " -DNOTION_TOKEN=xxx");
      return;
    }

    log.info("🎯 Starting Notion data capture for unit tests...");

    try {
      captureMatchData();
      log.info("🎉 Data capture completed successfully!");
    } catch (Exception e) {
      log.error("❌ Failed to capture Notion data", e);
    }
  }

  /** Captures sample match data from Notion and saves it for unit tests. */
  private void captureMatchData() throws IOException {
    log.info("📥 Capturing match data from Notion...");

    // Create output directory
    File outputDir = new File("src/test/resources/notion-samples");
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    // Use reflection to access the private loadMatchesFromNotion method
    try {
      java.lang.reflect.Method method =
          NotionSyncService.class.getDeclaredMethod("loadMatchesFromNotion");
      method.setAccessible(true);

      @SuppressWarnings("unchecked")
      List<MatchPage> matches = (List<MatchPage>) method.invoke(notionSyncService);

      if (matches.isEmpty()) {
        log.warn("⚠️ No matches found in Notion database");
        return;
      }

      log.info("✅ Found {} matches in Notion", matches.size());

      // Save a few sample matches for different test scenarios
      saveSampleMatches(matches, outputDir);

      // Create a summary of all property types found
      createPropertySummary(matches, outputDir);

    } catch (Exception e) {
      log.error("Failed to capture match data using reflection", e);

      // Fallback: try to capture data through sync operation
      log.info("🔄 Trying fallback approach through sync operation...");
      captureThroughSync(outputDir);
    }
  }

  /** Saves sample matches for different test scenarios. */
  private void saveSampleMatches(List<MatchPage> matches, File outputDir) throws IOException {
    // Save first few matches as individual samples
    int sampleCount = Math.min(5, matches.size());

    for (int i = 0; i < sampleCount; i++) {
      MatchPage match = matches.get(i);
      String filename = String.format("sample-match-%d.json", i + 1);
      File outputFile = new File(outputDir, filename);

      objectMapper.writeValue(outputFile, match);
      log.info("💾 Saved sample match: {} -> {}", match.getRawProperties().get("Name"), filename);
    }

    // Save complete sample set
    File allMatchesFile = new File(outputDir, "all-sample-matches.json");
    objectMapper.writeValue(allMatchesFile, matches.subList(0, sampleCount));
    log.info("💾 Saved {} sample matches to: {}", sampleCount, allMatchesFile.getName());
  }

  /** Creates a summary of property types and sample values found in matches. */
  private void createPropertySummary(List<MatchPage> matches, File outputDir) throws IOException {
    PropertySummary summary = new PropertySummary();

    for (MatchPage match : matches) {
      if (match.getRawProperties() != null) {
        match
            .getRawProperties()
            .forEach(
                (propertyName, propertyValue) -> {
                  summary.addProperty(propertyName, propertyValue);
                });
      }
    }

    File summaryFile = new File(outputDir, "property-summary.json");
    objectMapper.writeValue(summaryFile, summary);
    log.info("📊 Saved property summary to: {}", summaryFile.getName());

    // Log interesting findings
    summary
        .getProperties()
        .forEach(
            (name, info) -> {
              if (name.equals("Date") || name.equals("Title(s)") || name.equals("Winners")) {
                log.info(
                    "🔍 Property '{}': {} samples, types: {}",
                    name,
                    info.getSampleCount(),
                    info.getValueTypes());
              }
            });
  }

  /** Fallback method to capture data through sync operation. */
  private void captureThroughSync(File outputDir) {
    try {
      log.info("🔄 Attempting to capture data through sync operation...");
      var result = notionSyncService.syncMatches("data-capture-test");
      log.info("📈 Sync completed: {} matches processed", result.getSyncedCount());

      // Create a simple marker file to indicate we tried
      File markerFile = new File(outputDir, "sync-attempt.txt");
      java.nio.file.Files.write(
          markerFile.toPath(),
          ("Sync attempt completed: " + result.getSyncedCount() + " matches processed").getBytes());

    } catch (Exception e) {
      log.error("Fallback sync approach also failed", e);
    }
  }

  /** Helper class to summarize properties found in Notion data. */
  public static class PropertySummary {
    private java.util.Map<String, PropertyInfo> properties = new java.util.HashMap<>();

    public void addProperty(String name, Object value) {
      properties.computeIfAbsent(name, PropertyInfo::new).addSample(value);
    }

    public java.util.Map<String, PropertyInfo> getProperties() {
      return properties;
    }

    public static class PropertyInfo {
      private String name;
      private java.util.Set<String> valueTypes = new java.util.HashSet<>();
      private java.util.List<Object> sampleValues = new java.util.ArrayList<>();
      private int sampleCount = 0;

      public PropertyInfo(String name) {
        this.name = name;
      }

      public void addSample(Object value) {
        sampleCount++;
        if (value != null) {
          valueTypes.add(value.getClass().getSimpleName());
          if (sampleValues.size() < 3) { // Keep only first 3 samples
            sampleValues.add(value);
          }
        } else {
          valueTypes.add("null");
        }
      }

      // Getters
      public String getName() {
        return name;
      }

      public java.util.Set<String> getValueTypes() {
        return valueTypes;
      }

      public java.util.List<Object> getSampleValues() {
        return sampleValues;
      }

      public int getSampleCount() {
        return sampleCount;
      }
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(NotionDataCaptureUtil.class, args);
  }
}
