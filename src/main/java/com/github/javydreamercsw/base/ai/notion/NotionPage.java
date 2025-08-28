package com.github.javydreamercsw.base.ai.notion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/** Base class for all Notion page types containing common fields and functionality. */
@Data
public abstract class NotionPage {
  private String object;
  private String id;
  private String created_time;
  private String last_edited_time;
  private NotionUser created_by;
  private NotionUser last_edited_by;
  private Object cover;
  private Object icon;
  private NotionParent parent;
  private boolean archived;
  private boolean in_trash;
  private String url;
  private String public_url;
  private String request_id;

  // Generic properties map to store all Notion properties
  private java.util.Map<String, Object> rawProperties;

  @Data
  public static class NotionUser {
    private String object;
    private String id;
    private String name;
    private String avatar_url;
    private String type;
    private NotionPerson person;
  }

  @Data
  public static class NotionPerson {
    private String email;
  }

  @Data
  public static class NotionParent {
    private String type;
    private String database_id;
  }

  @Data
  public static class Property {
    private String id;
    private String type;
    private Object title;
    private Object rich_text;
    private Object date;
    private Object select;
    private Object relation;
    private Object people;
    private Object number;
    private Object created_time;
    private Object last_edited_time;
    private Object created_by;
    private Object last_edited_by;
    private Object unique_id;
    private Object formula;
    private Boolean has_more;
  }

  /**
   * Base class for all NotionProperties containing common properties that appear in every Notion
   * page.
   */
  @Data
  public static class BaseNotionProperties {
    private Property Name;
    private Property CreatedTime;
    private Property LastEditedTime;
    private Property CreatedBy;
    private Property LastEditedBy;
    private Property ID;
  }

  /**
   * Converts this NotionPage object to a pretty-formatted JSON string.
   *
   * @return Pretty-formatted JSON representation of this object
   */
  public String toPrettyJson() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "Error converting to JSON: " + e.getMessage();
    }
  }

  /**
   * Converts this NotionPage object to a compact JSON string.
   *
   * @return Compact JSON representation of this object
   */
  public String toJson() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "Error converting to JSON: " + e.getMessage();
    }
  }
}
