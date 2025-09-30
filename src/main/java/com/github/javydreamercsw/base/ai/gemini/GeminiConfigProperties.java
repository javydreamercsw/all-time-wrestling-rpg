package com.github.javydreamercsw.base.ai.gemini;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.gemini")
@Data
public class GeminiConfigProperties {
  private String modelName;
  private String apiUrl;
}
