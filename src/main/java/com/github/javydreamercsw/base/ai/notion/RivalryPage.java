package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Represents a Rivalry page (a row in the Heat database) from Notion. */
@Data
@EqualsAndHashCode(callSuper = true)
public class RivalryPage extends NotionPage {

  /**
   * Inner class representing the parent of a Notion page, which is typically a database. This is
   * required for the generic mapping functionality in NotionHandler.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class NotionParent extends NotionPage.NotionParent {}
}
