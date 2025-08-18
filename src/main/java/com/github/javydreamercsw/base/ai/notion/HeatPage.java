package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HeatPage extends NotionPage {
  private NotionProperties properties;

  @Data
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Heat-specific properties only (common properties inherited from base)
    private Property Wrestler1; // First wrestler in the feud
    private Property Wrestler2; // Second wrestler in the feud
    private Property HeatLevel; // Intensity of the feud (1-10, Hot/Cold, etc.)
    private Property HeatType; // Type of heat (Face vs Heel, Personal, Professional, etc.)
    private Property Reason; // Storyline reason for the heat
    private Property StartDate; // When the feud began
    private Property EndDate; // When the feud ended (if resolved)
    private Property Status; // Current status (Active, Resolved, On Hold, etc.)
    private Property Winner; // Who came out on top (if resolved)
    private Property RelatedMatches; // Matches that were part of this storyline
  }
}
