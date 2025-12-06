/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.base.ai.notion;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ShowTemplatePage extends NotionPage {

  /**
   * Get the show type name from the template. This will extract the actual show type from the
   * Notion property.
   *
   * @return show type name or null if not available
   */
  public String getShowTypeName() {
    if (getRawProperties() != null && getRawProperties().containsKey("Show Type")) {
      return (String) getRawProperties().get("Show Type");
    }
    return null;
  }

  /**
   * Get the template content as a structured object.
   *
   * @return content object with template details
   */
  public TemplateContent getTemplateContent() {
    TemplateContent content = new TemplateContent();
    Map<String, Object> props = getRawProperties();

    if (props != null) {
      content.setFormat((String) props.get("Format"));
      content.setDuration((String) props.get("Duration"));
      content.setMatchCount((String) props.get("Match Count"));
      content.setMainEvent((String) props.get("Main Event"));
      content.setVenue((String) props.get("Venue"));
      content.setPyrotechnics(Boolean.parseBoolean((String) props.get("Pyrotechnics")));
      content.setSpecialStaging((String) props.get("Special Staging"));
      content.setCommentary((String) props.get("Commentary"));
    }

    return content;
  }

  /**
   * Get the template description.
   *
   * @return description or null if not available
   */
  public String getTemplateDescription() {
    if (getRawProperties() != null && getRawProperties().containsKey("Description")) {
      return (String) getRawProperties().get("Description");
    }
    return null;
  }

  /**
   * Get the template format (e.g., "Premium Live Event", "Weekly Show").
   *
   * @return format or null if not available
   */
  public String getFormat() {
    if (getRawProperties() != null && getRawProperties().containsKey("Format")) {
      return (String) getRawProperties().get("Format");
    }
    return null;
  }

  /**
   * Get the duration of the show template.
   *
   * @return duration or null if not available
   */
  public String getDuration() {
    if (getRawProperties() != null && getRawProperties().containsKey("Duration")) {
      return (String) getRawProperties().get("Duration");
    }
    return null;
  }

  /**
   * Get the segment count for this template.
   *
   * @return segment count or null if not available
   */
  public String getMatchCount() {
    if (getRawProperties() != null && getRawProperties().containsKey("Match Count")) {
      return (String) getRawProperties().get("Match Count");
    }
    return null;
  }

  /**
   * Get the main event description.
   *
   * @return main event or null if not available
   */
  public String getMainEvent() {
    if (getRawProperties() != null && getRawProperties().containsKey("Main Event")) {
      return (String) getRawProperties().get("Main Event");
    }
    return null;
  }

  /**
   * Get the venue information.
   *
   * @return venue or null if not available
   */
  public String getVenue() {
    if (getRawProperties() != null && getRawProperties().containsKey("Venue")) {
      return (String) getRawProperties().get("Venue");
    }
    return null;
  }

  /**
   * Check if pyrotechnics are used.
   *
   * @return true if pyrotechnics are used, false otherwise
   */
  public boolean hasPyrotechnics() {
    if (getRawProperties() != null && getRawProperties().containsKey("Pyrotechnics")) {
      return Boolean.parseBoolean((String) getRawProperties().get("Pyrotechnics"));
    }
    return false;
  }

  /**
   * Get the special staging information.
   *
   * @return special staging or null if not available
   */
  public String getSpecialStaging() {
    if (getRawProperties() != null && getRawProperties().containsKey("Special Staging")) {
      return (String) getRawProperties().get("Special Staging");
    }
    return null;
  }

  /**
   * Get the commentary information.
   *
   * @return commentary or null if not available
   */
  public String getCommentary() {
    if (getRawProperties() != null && getRawProperties().containsKey("Commentary")) {
      return (String) getRawProperties().get("Commentary");
    }
    return null;
  }

  @Data
  public static class TemplateContent {
    private String format;
    private String duration;
    private String matchCount;
    private String mainEvent;
    private String venue;
    private Boolean pyrotechnics;
    private String specialStaging;
    private String commentary;
  }
}
