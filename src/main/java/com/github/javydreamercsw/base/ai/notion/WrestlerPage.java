package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class WrestlerPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  public static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Wrestler-specific properties only (common properties inherited from base)
    private Property player;
    private Property titleBonus;
    private Property totalBonus;
    private Property titles;
    private Property heat1;
    private Property fans;
    private Property bumps;
    private Property faction;
    private Property heatBonus;
    private Property matches;
    private Property fanWeight;
    private Property heat;
    private Property gender;
  }
}
