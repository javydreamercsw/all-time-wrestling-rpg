package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class WrestlerPage extends NotionPage {
  private NotionProperties properties;

  @Data
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Wrestler-specific properties only (common properties inherited from base)
    private Property Player;
    private Property TitleBonus;
    private Property TotalBonus;
    private Property Titles;
    private Property Heat1;
    private Property Fans;
    private Property Bumps;
    private Property Faction;
    private Property HeatBonus;
    private Property Matches;
    private Property FanWeight;
    private Property Heat;
  }
}
