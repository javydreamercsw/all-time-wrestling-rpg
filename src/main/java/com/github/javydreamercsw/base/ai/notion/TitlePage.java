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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TitlePage extends NotionPage {

  public String getName() {
    return extractPropertyAsString("Name");
  }

  public List<String> getChampionRelationIds() {
    return extractRelationIds("Current Champion");
  }

  public List<String> getContenderRelationIds() {
    return extractRelationIds("#1 Contender");
  }

  public String getTier() {
    return extractPropertyAsString("Tier");
  }

  public String getGender() {
    return extractPropertyAsString("Gender");
  }

  public String getDescription() {
    return extractPropertyAsString("Description");
  }

  public String getChampionshipType() {
    return extractPropertyAsString("Title Type");
  }

  public Boolean getIsActive() {
    Object prop = getRawProperties() != null ? getRawProperties().get("Active") : null;
    return prop instanceof Boolean ? (Boolean) prop : null;
  }

  public Integer getDefenseFrequency() {
    Object prop =
        getRawProperties() != null ? getRawProperties().get("Defense Cadence (weeks)") : null;
    return prop instanceof Number ? ((Number) prop).intValue() : null;
  }

  private String extractPropertyAsString(String name) {
    if (getRawProperties() != null && getRawProperties().containsKey(name)) {
      Object prop = getRawProperties().get(name);
      if (prop instanceof String) {
        return (String) prop;
      }
    }
    return null;
  }

  private List<String> extractRelationIds(String name) {
    List<String> ids = new ArrayList<>();
    if (getRawProperties() != null && getRawProperties().containsKey(name)) {
      Object prop = getRawProperties().get(name);
      if (prop instanceof List<?> list) {
        for (Object item : list) {
          if (item instanceof String str) {
            ids.add(str);
          } else if (item instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (id instanceof String str) ids.add(str);
          }
        }
      } else if (prop instanceof String str) {
        ids.add(str);
      }
    }
    return ids;
  }
}
