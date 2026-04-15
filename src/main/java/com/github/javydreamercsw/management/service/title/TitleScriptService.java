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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for executing scripts associated with Titles to modify narration context. */
@Service
@Slf4j
@RequiredArgsConstructor
public class TitleScriptService {

  /**
   * Applies title-based effects to the segment narration context.
   *
   * @param context The segment narration context to modify.
   * @param titles The titles to check for effects.
   */
  public void applyTitleEffects(SegmentNarrationContext context, Collection<Title> titles) {
    if (titles == null || titles.isEmpty()) {
      return;
    }

    for (Title title : titles) {
      if (title.getEffectScript() == null || title.getEffectScript().isBlank()) {
        continue;
      }

      List<Wrestler> currentChampions = title.getCurrentChampions();
      if (currentChampions == null || currentChampions.isEmpty()) {
        continue;
      }

      for (Wrestler champion : currentChampions) {
        // Find corresponding WrestlerContext in the segment context
        WrestlerContext wrestlerContext =
            context.getWrestlers().stream()
                .filter(w -> w.getName().equalsIgnoreCase(champion.getName()))
                .findFirst()
                .orElse(null);

        if (wrestlerContext != null) {
          executeTitleScript(title.getEffectScript(), context, wrestlerContext);
        }
      }
    }
  }

  private void executeTitleScript(
      String script, SegmentNarrationContext context, WrestlerContext champion) {
    log.info("Executing title script for {}: {}", champion.getName(), script);
    try {
      Binding binding = new Binding();
      TitleEffectContext effectContext = new TitleEffectContext(context, champion);
      binding.setProperty("context", effectContext);

      // Use Delegate to allow direct method calls
      GroovyShell shell = new GroovyShell(binding);
      String fullScript = "context.with { " + script + " }";
      shell.evaluate(fullScript);
    } catch (Exception e) {
      log.error("Error executing title script: {}", script, e);
    }
  }
}
