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
package com.github.javydreamercsw.management.event.listener;

import com.github.javydreamercsw.management.event.FeudScriptCompletedEvent;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Ends the arc's rivalry when a story arc completes. The arc is the narrative wrapper for the feud
 * — once every beat is resolved the rivalry has nowhere to go, so leaving it active keeps show
 * planning proposing segments for a finished feud forever: heat re-accrues on every booked segment
 * and the planner keeps classifying it PLE_RESOLUTION_REQUIRED (the Shelton/Lashley 117-heat loop,
 * ATW-gaxw).
 *
 * <p>Events are synchronous — this runs inside the completion transaction, so the rivalry end
 * commits or rolls back with the arc completion. {@code endRivalry} filters on {@code isActive},
 * making re-completions and already-ended rivalries harmless no-ops. Feud-linked scripts (no
 * rivalry link) are skipped — MultiWrestlerFeud has its own lifecycle via FeudResolutionService. A
 * failure here must never fail the arc completion (nightly {@code RivalryDecayService} auto-close
 * remains a backstop), so exceptions are logged and swallowed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeudScriptCompletedRivalryListener {

  private final RivalryService rivalryService;

  @EventListener
  public void onFeudScriptCompleted(final FeudScriptCompletedEvent event) {
    var script = event.getScript();
    if (script.getRivalry() == null) {
      log.debug(
          "Arc '{}' has no rivalry link (feud-linked or legacy) — nothing to end",
          script.getName());
      return;
    }
    // completeBeat admits universe-scoped bookers via @universeAuthz.hasRoleInCurrentUniverse,
    // but endRivalry requires a global ADMIN/BOOKER/SYSTEM role — elevate like
    // RivalryDecayService, restoring the caller's context afterwards (this is a live user
    // thread, not a scheduler thread).
    SecurityContext previous = SecurityContextHolder.getContext();
    SecurityContext systemContext = SecurityContextHolder.createEmptyContext();
    systemContext.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "system", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))));
    String reason =
        "Story arc '%s' completed"
            .formatted(
                script.getName().length() > 150
                    ? script.getName().substring(0, 150)
                    : script.getName());
    try {
      SecurityContextHolder.setContext(systemContext);
      rivalryService.endRivalry(script.getRivalry().getId(), reason);
      log.info(
          "Ended rivalry {} because story arc '{}' completed",
          script.getRivalry().getId(),
          script.getName());
    } catch (RuntimeException e) {
      log.error(
          "Failed to end rivalry {} after story arc '{}' completed — rivalry stays active"
              + " (nightly decay auto-close remains a backstop)",
          script.getRivalry().getId(),
          script.getName(),
          e);
    } finally {
      SecurityContextHolder.setContext(previous);
    }
  }
}
