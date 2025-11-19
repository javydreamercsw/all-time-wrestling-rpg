package com.github.javydreamercsw.management.dto;

import java.util.List;
import lombok.Data;

@Data
public class DeckDTO {
  private String wrestler;
  private List<DeckCardDTO> cards;
}
