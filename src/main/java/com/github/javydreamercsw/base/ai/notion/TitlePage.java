package com.github.javydreamercsw.base.ai.notion;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TitlePage extends NotionPage {
  // This class can be expanded to include specific properties from the Notion
  // database for Titles, such as relation IDs for the current champion.

  public String getChampionRelationId() {
    if (getRawProperties() != null && getRawProperties().containsKey("Current Champion")) {
      Object championProp = getRawProperties().get("Current Champion");
      if (championProp instanceof String) {
        return (String) championProp;
      }
    }
    return null;
  }

  public String getContenderRelationId() {
    if (getRawProperties() != null && getRawProperties().containsKey("👤 #1 Contenders")) {
      Object contenderProp = getRawProperties().get("👤 #1 Contenders");
      if (contenderProp instanceof String) {
        return (String) contenderProp;
      }
    }
    return null;
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
