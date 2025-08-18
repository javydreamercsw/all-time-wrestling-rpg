package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeamPage extends NotionPage {
  private NotionProperties properties;

  @Data
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Team-specific properties only (common properties inherited from base)
    private Property Members;
    private Property Leader;
    private Property TeamType;
    private Property Status;
    private Property FormedDate;
    private Property DisbandedDate;
    private Property Faction;
  }
}
