package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MatchPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  public static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Match-specific properties only (common properties inherited from base)
    private NotionPage.Property Participants;
    private NotionPage.Property Winners;
    private NotionPage.Property Shows;
    private NotionPage.Property Match_Type;
    private NotionPage.Property Referee_s;
    private NotionPage.Property Rules;
    private NotionPage.Property Title_s;
    private NotionPage.Property Notes;
    private NotionPage.Property Date;
  }
}
