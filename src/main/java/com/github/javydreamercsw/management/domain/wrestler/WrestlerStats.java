package com.github.javydreamercsw.management.domain.wrestler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrestlerStats {
  private long wins;
  private long losses;
  private long titlesHeld;
}
