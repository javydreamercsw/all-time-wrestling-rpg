package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FactionPage extends NotionPage {
  private NotionProperties properties;

  @Data
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Faction-specific properties only (common properties inherited from base)
    private Property Description;
    private Property Leader;
    private Property Members;
    private Property Teams;
    private Property Alignment;
    private Property Status;
    private Property FormedDate;
    private Property DisbandedDate;
  }
}
