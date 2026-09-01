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

import com.github.javydreamercsw.Application;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link OllamaSegmentNarrationService}.
 *
 * <p>Skipped automatically when {@code OLLAMA_BASE_URL} is not set — safe to run in normal CI. Runs
 * nightly in the {@code ollama-it} GitHub Actions job which sets the env var and pulls the model.
 * Run locally with: {@code OLLAMA_BASE_URL=http://localhost:11434 mvn -Pintegration-test verify
 * -Dit.test=OllamaSegmentNarrationServiceIT -Dsurefire.skip=true}
 */
@Tag("ollama")
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "OLLAMA_BASE_URL", matches = ".+")
class OllamaSegmentNarrationServiceIT {

  @Autowired private OllamaSegmentNarrationService ollamaService;

  @Test
  void ollamaServiceReportsAvailable() {
    assertThat(ollamaService.isAvailable())
        .as("OllamaSegmentNarrationService must report available when OLLAMA_BASE_URL is set")
        .isTrue();
  }

  @Test
  void narrateSegment_returnsNonEmptyText() {
    WrestlerContext w1 = new WrestlerContext();
    w1.setName("Wrestler A");
    w1.setAlignment("FACE");

    WrestlerContext w2 = new WrestlerContext();
    w2.setName("Wrestler B");
    w2.setAlignment("HEEL");

    SegmentTypeContext segType = new SegmentTypeContext();
    segType.setSegmentType("One-on-One Match");

    SegmentNarrationContext ctx = new SegmentNarrationContext();
    ctx.setWrestlers(List.of(w1, w2));
    ctx.setSegmentType(segType);
    ctx.setDeterminedOutcome("Wrestler A wins by pinfall");

    String result = ollamaService.narrateSegment(ctx);

    assertThat(result).as("Narration must not be blank").isNotBlank();
    assertThat(result).as("Narration must have some content").hasSizeGreaterThan(10);
  }

  @Test
  void getProviderName_returnsOllama() {
    assertThat(ollamaService.getProviderName()).isEqualTo("Ollama");
  }
}
