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
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TitlePage extends NotionPage {
  // This class can be expanded to include specific properties from the Notion
  // database for Titles, such as relation IDs for the current champion.

  public String getName() {
    if (getRawProperties() != null && getRawProperties().containsKey("Name")) {
      Object nameProp = getRawProperties().get("Name");
      if (nameProp instanceof String) {
        return (String) nameProp;
      }
      // Handle other Notion property types for 'Name' if necessary in future
    }
    return null;
  }

  public List<String> getChampionRelationIds() {
    if (getRawProperties() != null && getRawProperties().containsKey("Current Champion")) {
      Object championProp = getRawProperties().get("Current Champion");
      if (championProp instanceof String) {
        return List.of(((String) championProp).split(",")).stream()
            .map(String::trim)
            .collect(Collectors.toList());
      }
    }
    return new ArrayList<>();
  }

  public List<String> getContenderRelationIds() {
    if (getRawProperties() != null && getRawProperties().containsKey("#1 Contender")) {
      Object contenderProp = getRawProperties().get("#1 Contender");
      if (contenderProp instanceof String) {
        return List.of(((String) contenderProp).split(",")).stream()
            .map(String::trim)
            .collect(Collectors.toList());
      }
    }
    return new ArrayList<>();
  }

  public String getTier() {
    if (getRawProperties() != null && getRawProperties().containsKey("Tier")) {
      Object tierProp = getRawProperties().get("Tier");
      if (tierProp instanceof String) {
        return (String) tierProp;
      }
    }
    return null;
  }

  public String getGender() {
    if (getRawProperties() != null && getRawProperties().containsKey("Gender")) {
      Object genderProp = getRawProperties().get("Gender");
      if (genderProp instanceof String) {
        return (String) genderProp;
      }
    }
    return null;
  }
}
