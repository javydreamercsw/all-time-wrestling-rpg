package com.github.javydreamercsw.management.service.sync.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RivalryDTO {
  private String wrestler1;
  private String wrestler2;
  private Integer heat;
  private Boolean isActive;
  private String startedDate;
  private String endedDate;
  private String storylineNotes;
  private String externalId;
}
