/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.ai.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Pure unit tests for the Ollama request-body construction: configurable max_tokens, the /no_think
 * thinking directive, and the extra-params escape hatch (ATW-binc).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OllamaSegmentNarrationServiceTest {

  private static final String NARRATION_PROMPT = "Narrate this match.";
  private static final String SUMMARY_PROMPT =
      "Summarize the following segment narration: someone won a match.";

  @Mock private AiSettingsService aiSettingsService;

  private OllamaSegmentNarrationService service;

  @BeforeEach
  void setUp() {
    // Settings defaults; individual tests override what they exercise.
    when(aiSettingsService.getOllamaModel()).thenReturn("qwen3:30b");
    when(aiSettingsService.getOllamaMaxTokens()).thenReturn(4096);
    when(aiSettingsService.isOllamaDisableThinking()).thenReturn(false);
    when(aiSettingsService.getOllamaExtraParams()).thenReturn("{}");
    service = new OllamaSegmentNarrationService(aiSettingsService);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> systemMessage(Map<String, Object> body) {
    List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
    return messages.get(0);
  }

  // ----------------------------------------------------------------
  // Default body
  // ----------------------------------------------------------------

  @Test
  void defaultBody_hasModelTemperatureAndTokenDefaults() {
    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat(body.get("model")).isEqualTo("qwen3:30b");
    assertThat(body.get("temperature")).isEqualTo(0.2);
    assertThat(body.get("max_tokens")).isEqualTo(4096);
  }

  @Test
  void defaultBody_systemMessageHasNoThinkingDirective() {
    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat((String) systemMessage(body).get("content")).doesNotContain("/no_think");
  }

  @Test
  void body_userMessageCarriesPrompt() {
    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
    assertThat(messages).hasSize(2);
    assertThat(messages.get(0).get("role")).isEqualTo("system");
    assertThat(messages.get(1).get("role")).isEqualTo("user");
    assertThat(messages.get(1).get("content")).isEqualTo(NARRATION_PROMPT);
  }

  // ----------------------------------------------------------------
  // max_tokens
  // ----------------------------------------------------------------

  @Test
  void customMaxTokens_settingPresent_reflectedInBody() {
    when(aiSettingsService.getOllamaMaxTokens()).thenReturn(16384);

    assertThat(service.buildRequestBody(NARRATION_PROMPT).get("max_tokens")).isEqualTo(16384);
  }

  // ----------------------------------------------------------------
  // /no_think directive
  // ----------------------------------------------------------------

  @Test
  void disableThinking_appendsDirectiveToNarrationSystemMessage() {
    when(aiSettingsService.isOllamaDisableThinking()).thenReturn(true);

    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);
    assertThat((String) systemMessage(body).get("content")).endsWith("/no_think");
  }

  @Test
  void disableThinking_appendsDirectiveToSummarySystemMessage() {
    when(aiSettingsService.isOllamaDisableThinking()).thenReturn(true);

    Map<String, Object> body = service.buildRequestBody(SUMMARY_PROMPT);
    assertThat((String) systemMessage(body).get("content")).endsWith("/no_think");
  }

  @Test
  void thinkingEnabled_systemMessageUnchangedForBothPersonas() {
    String narrationSystem =
        (String) systemMessage(service.buildRequestBody(NARRATION_PROMPT)).get("content");
    String summarySystem =
        (String) systemMessage(service.buildRequestBody(SUMMARY_PROMPT)).get("content");

    assertThat(narrationSystem).doesNotContain("/no_think");
    assertThat(summarySystem).doesNotContain("/no_think");
    assertThat(narrationSystem).isNotEqualTo(summarySystem);
  }

  // ----------------------------------------------------------------
  // Extra params (escape hatch)
  // ----------------------------------------------------------------

  @Test
  void extraParams_mergedIntoBody() {
    when(aiSettingsService.getOllamaExtraParams()).thenReturn("{\"seed\":42}");

    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat(body.get("seed")).isEqualTo(42);
  }

  @Test
  void extraParams_overrideNamedDefaults() {
    when(aiSettingsService.getOllamaExtraParams())
        .thenReturn("{\"max_tokens\":1024,\"temperature\":0.9}");

    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat(body.get("max_tokens")).isEqualTo(1024);
    assertThat(body.get("temperature")).isEqualTo(0.9);
  }

  @Test
  void extraParams_reservedKeysStripped() {
    when(aiSettingsService.getOllamaExtraParams())
        .thenReturn("{\"model\":\"evil\",\"messages\":[],\"seed\":1}");

    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat(body.get("model")).isEqualTo("qwen3:30b");
    assertThat(String.valueOf(body.get("messages"))).contains("system");
    assertThat(body.get("seed")).isEqualTo(1);
  }

  @Test
  void extraParams_invalidJson_failsOpenWithDefaults() {
    when(aiSettingsService.getOllamaExtraParams()).thenReturn("{not json");

    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat(body.get("max_tokens")).isEqualTo(4096);
    assertThat(body.get("temperature")).isEqualTo(0.2);
    assertThat(body).doesNotContainKeys("seed");
  }

  @Test
  void extraParams_nonObjectJson_failsOpenWithDefaults() {
    when(aiSettingsService.getOllamaExtraParams()).thenReturn("[1,2,3]");

    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat(body.get("max_tokens")).isEqualTo(4096);
  }

  @Test
  void extraParams_blank_failsOpenWithDefaults() {
    when(aiSettingsService.getOllamaExtraParams()).thenReturn("  ");

    Map<String, Object> body = service.buildRequestBody(NARRATION_PROMPT);

    assertThat(body.get("max_tokens")).isEqualTo(4096);
  }
}
