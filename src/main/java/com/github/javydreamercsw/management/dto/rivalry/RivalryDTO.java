package com.github.javydreamercsw.management.dto.rivalry;

import com.github.javydreamercsw.management.dto.wrestler.WrestlerDTO;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

@Data
public class RivalryDTO implements Serializable {
  private Long id;
  private WrestlerDTO wrestler1;
  private WrestlerDTO wrestler2;
  private int heat;
  private Boolean isActive;
  private String storylineNotes;
  private Instant startedDate;
  private Instant endedDate;
}
