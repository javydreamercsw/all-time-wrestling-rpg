package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SeasonPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Season-specific properties only (common properties inherited from base)
    private Property Description;
    private Property StartDate;
    private Property EndDate;
    private Property IsActive;
    private Property ShowsPerPpv;
    private Property Shows; // Relation to shows in this season
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  static class NotionParent extends NotionPage.NotionParent {
    // Season-specific parent properties (if any)
  }
}
