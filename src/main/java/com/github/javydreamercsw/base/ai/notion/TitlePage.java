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
    if (getRawProperties() != null && getRawProperties().containsKey("👤 #1 Contenders")) {
      Object contenderProp = getRawProperties().get("👤 #1 Contenders");
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
