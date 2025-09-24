package com.github.javydreamercsw.management.dto.wrestler;

import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.io.Serializable;
import lombok.Data;

@Data
public class WrestlerDTO implements Serializable {
  private Long id;
  private String name;
  private Long fans;
  private WrestlerTier tier;
  private String externalId;
  // Add other fields as needed for API response
}
