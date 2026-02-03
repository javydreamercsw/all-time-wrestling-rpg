/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.base.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.base.ai.claude.ClaudeConfigProperties;
import com.github.javydreamercsw.base.ai.gemini.GeminiConfigProperties;
import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import com.github.javydreamercsw.base.ai.openai.OpenAIConfigProperties;
import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.base.security.WithMockAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test"})
@SpringBootTest
@Import(TestSecurityConfig.class)
@WithMockAdmin
class AIProviderConfigTest {

  @Autowired private AiBaseProperties aiBaseProperties;
  @Autowired private GeminiConfigProperties geminiConfigProperties;
  @Autowired private ClaudeConfigProperties claudeConfigProperties;
  @Autowired private OpenAIConfigProperties openAIConfigProperties;
  @Autowired private LocalAIConfigProperties localAIConfigProperties;

  @Test
  void testBaseAiConfig() {
    assertNotNull(aiBaseProperties);
    assertEquals(300, aiBaseProperties.getTimeout());
  }

  @Test
  void testGeminiConfig() {
    assertNotNull(geminiConfigProperties);
    assertEquals("gemini-2.5-flash", geminiConfigProperties.getModelName());
    assertEquals(
        "https://generativelanguage.googleapis.com/v1beta/models/",
        geminiConfigProperties.getApiUrl());
  }

  @Test
  void testClaudeConfig() {
    assertNotNull(claudeConfigProperties);
    assertEquals("claude-3-haiku-20240307", claudeConfigProperties.getModelName());
    assertEquals("https://api.anthropic.com/v1/messages/", claudeConfigProperties.getApiUrl());
  }

  @Test
  void testOpenAIConfig() {
    assertNotNull(openAIConfigProperties);
    assertEquals("https://api.openai.com/v1/chat/completions", openAIConfigProperties.getApiUrl());
    assertEquals("gpt-3.5-turbo", openAIConfigProperties.getDefaultModel());
  }

  @Test
  void testLocalAIConfig() {
    assertNotNull(localAIConfigProperties);
    assertEquals("http://localhost:8088", localAIConfigProperties.getBaseUrl());
    assertEquals("llama-3.2-1b-instruct:q4_k_m", localAIConfigProperties.getModel());
  }
}
