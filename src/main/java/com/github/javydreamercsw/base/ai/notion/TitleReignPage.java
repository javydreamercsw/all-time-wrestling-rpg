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
