package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MatchPage extends NotionPage {
  private NotionProperties properties;

  @Data
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Match-specific properties only (common properties inherited from base)
    private Property Participants;
    private Property Winner;
    private Property MatchType;
    private Property Show;
    private Property Duration;
    private Property Rating;
    private Property Stipulation;
  }
}
