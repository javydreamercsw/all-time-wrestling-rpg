package com.github.javydreamercsw.base.ai.claude;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.claude")
@Data
public class ClaudeConfigProperties {
  private String modelName;
  private String apiUrl;
}
