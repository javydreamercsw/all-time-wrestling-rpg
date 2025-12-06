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

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TitleReignPage extends NotionPage {

  public String getTitleRelationId() {
    if (getRawProperties() != null && getRawProperties().containsKey("Title")) {
      Object titleProp = getRawProperties().get("Title");
      if (titleProp instanceof String) {
        return (String) titleProp;
      }
    }
    return null;
  }

  public String getChampionRelationId() {
    if (getRawProperties() != null && getRawProperties().containsKey("Champion")) {
      Object championProp = getRawProperties().get("Champion");
      if (championProp instanceof String) {
        return (String) championProp;
      }
    }
    return null;
  }

  public String getStartDate() {
    if (getRawProperties() != null && getRawProperties().containsKey("Start Date")) {
      Object dateProp = getRawProperties().get("Start Date");
      if (dateProp instanceof String) {
        return (String) dateProp;
      }
    }
    return null;
  }

  public String getEndDate() {
    if (getRawProperties() != null && getRawProperties().containsKey("End Date")) {
      Object dateProp = getRawProperties().get("End Date");
      if (dateProp instanceof String) {
        return (String) dateProp;
      }
    }
    return null;
  }

  public Integer getReignNumber() {
    if (getRawProperties() != null && getRawProperties().containsKey("Reign Number")) {
      Object numberProp = getRawProperties().get("Reign Number");
      if (numberProp instanceof Integer) {
        return (Integer) numberProp;
      }
    }
    return null;
  }

  public String getNotes() {
    if (getRawProperties() != null && getRawProperties().containsKey("Notes")) {
      Object notesProp = getRawProperties().get("Notes");
      if (notesProp instanceof String) {
        return (String) notesProp;
      }
    }
    return null;
  }
}
