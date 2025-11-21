package com.github.javydreamercsw.management.service.sync.entity;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FactionDTO {
  private String name;
  private String description;
  private String externalId; // Notion page ID
  private String leader;
  private Boolean isActive;
  private String formedDate;
  private String disbandedDate;
  private List<String> members;
  private List<String> teams;
}