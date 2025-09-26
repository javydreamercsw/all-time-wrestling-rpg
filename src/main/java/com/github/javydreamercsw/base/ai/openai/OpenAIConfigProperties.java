package com.github.javydreamercsw.base.ai.openai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.openai")
@Data
public class OpenAIConfigProperties {
  private String apiUrl;
  private String defaultModel;
  private String premiumModel;
  private int maxTokens;
}
