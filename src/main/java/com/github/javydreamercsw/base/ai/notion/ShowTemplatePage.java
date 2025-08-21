package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ShowTemplatePage extends NotionPage {
  private NotionProperties properties;

  @Data
  public static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Show template-specific properties only (common properties inherited from base)
    private Property Description;
    private Property ShowType;
    private Property Format;
    private Property Duration;
    private Property MatchCount;
    private Property MainEvent;
    private Property SpecialFeatures;
    private Property MatchTypes;
    private Property StorylineElements;
    private Property Venue;
    private Property Pyrotechnics;
    private Property SpecialStaging;
    private Property Commentary;
    private Property Content;
  }

  /**
   * Get the show type name from the template. This will extract the actual show type from the
   * Notion property.
   *
   * @return show type name or null if not available
   */
  public String getShowTypeName() {
    if (properties != null && properties.getShowType() != null) {
      // Extract the actual value from the Notion property
      return extractTextFromProperty(properties.getShowType());
    }
    return null;
  }

  /**
   * Get the template content as a structured object.
   *
   * @return content object with template details
   */
  public TemplateContent getTemplateContent() {
    TemplateContent content = new TemplateContent();

    if (properties != null) {
      // Extract content from Notion properties
      // This would need to be implemented based on the actual Notion property structure
      content.setFormat(extractTextFromProperty(properties.getFormat()));
      content.setDuration(extractTextFromProperty(properties.getDuration()));
      content.setMatchCount(extractTextFromProperty(properties.getMatchCount()));
      content.setMainEvent(extractTextFromProperty(properties.getMainEvent()));
      content.setVenue(extractTextFromProperty(properties.getVenue()));
      content.setPyrotechnics(extractBooleanFromProperty(properties.getPyrotechnics()));
      content.setSpecialStaging(extractTextFromProperty(properties.getSpecialStaging()));
      content.setCommentary(extractTextFromProperty(properties.getCommentary()));
    }

    return content;
  }

  /**
   * Get the template description.
   *
   * @return description or null if not available
   */
  public String getTemplateDescription() {
    if (properties != null && properties.getDescription() != null) {
      return extractTextFromProperty(properties.getDescription());
    }
    return null;
  }

  /**
   * Get the template format (e.g., "Premium Live Event", "Weekly Show").
   *
   * @return format or null if not available
   */
  public String getFormat() {
    if (properties != null && properties.getFormat() != null) {
      return extractTextFromProperty(properties.getFormat());
    }
    return null;
  }

  /**
   * Get the duration of the show template.
   *
   * @return duration or null if not available
   */
  public String getDuration() {
    if (properties != null && properties.getDuration() != null) {
      return extractTextFromProperty(properties.getDuration());
    }
    return null;
  }

  /**
   * Get the match count for this template.
   *
   * @return match count or null if not available
   */
  public String getMatchCount() {
    if (properties != null && properties.getMatchCount() != null) {
      return extractTextFromProperty(properties.getMatchCount());
    }
    return null;
  }

  /**
   * Get the main event description.
   *
   * @return main event or null if not available
   */
  public String getMainEvent() {
    if (properties != null && properties.getMainEvent() != null) {
      return extractTextFromProperty(properties.getMainEvent());
    }
    return null;
  }

  /**
   * Get the venue information.
   *
   * @return venue or null if not available
   */
  public String getVenue() {
    if (properties != null && properties.getVenue() != null) {
      return extractTextFromProperty(properties.getVenue());
    }
    return null;
  }

  /**
   * Check if pyrotechnics are used.
   *
   * @return true if pyrotechnics are used, false otherwise
   */
  public boolean hasPyrotechnics() {
    if (properties != null && properties.getPyrotechnics() != null) {
      return extractBooleanFromProperty(properties.getPyrotechnics());
    }
    return false;
  }

  /**
   * Get the special staging information.
   *
   * @return special staging or null if not available
   */
  public String getSpecialStaging() {
    if (properties != null && properties.getSpecialStaging() != null) {
      return extractTextFromProperty(properties.getSpecialStaging());
    }
    return null;
  }

  /**
   * Get the commentary information.
   *
   * @return commentary or null if not available
   */
  public String getCommentary() {
    if (properties != null && properties.getCommentary() != null) {
      return extractTextFromProperty(properties.getCommentary());
    }
    return null;
  }

  /**
   * Helper method to extract text from a Notion property. This is a placeholder - actual
   * implementation would depend on Notion property structure.
   */
  private String extractTextFromProperty(NotionPage.Property property) {
    // Placeholder implementation
    return property != null ? "Extracted text" : null;
  }

  /**
   * Helper method to extract boolean from a Notion property. This is a placeholder - actual
   * implementation would depend on Notion property structure.
   */
  private Boolean extractBooleanFromProperty(NotionPage.Property property) {
    // Placeholder implementation
    return property != null ? true : false;
  }

  @Data
  public static class TemplateContent {
    private String format;
    private String duration;
    private String matchCount;
    private String mainEvent;
    private String venue;
    private Boolean pyrotechnics;
    private String specialStaging;
    private String commentary;
  }
}
