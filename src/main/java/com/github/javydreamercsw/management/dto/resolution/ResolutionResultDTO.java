package com.github.javydreamercsw.management.dto.resolution;

import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionResultDTO implements Serializable {
  private RivalryDTO resolvedEntity;
  private String message;
}
