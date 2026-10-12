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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.event.FeudScriptCompletedEvent;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for the arc-completion → rivalry-end listener (ATW-gaxw). */
@ExtendWith(MockitoExtension.class)
class FeudScriptCompletedRivalryListenerTest {

  @Mock private RivalryService rivalryService;

  @InjectMocks private FeudScriptCompletedRivalryListener listener;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private FeudScript rivalryScript(long rivalryId, String name) {
    Rivalry rivalry = new Rivalry();
    rivalry.setId(rivalryId);
    FeudScript script = new FeudScript();
    script.setName(name);
    script.setRivalry(rivalry);
    return script;
  }

  @Test
  void rivalryLinkedScript_endsRivalry() {
    listener.onFeudScriptCompleted(
        new FeudScriptCompletedEvent(this, rivalryScript(731L, "Shelton vs Lashley")));

    verify(rivalryService).endRivalry(731L, "Story arc 'Shelton vs Lashley' completed");
  }

  @Test
  void feudLinkedScript_noRivalryEnd() {
    FeudScript script = new FeudScript();
    script.setName("Desolation's Smile implosion");
    script.setRivalry(null);

    listener.onFeudScriptCompleted(new FeudScriptCompletedEvent(this, script));

    verify(rivalryService, never()).endRivalry(anyLong(), any());
  }

  @Test
  void serviceFailure_swallowed() {
    doThrow(new RuntimeException("boom")).when(rivalryService).endRivalry(anyLong(), any());

    var script = rivalryScript(1L, "Rerun arc");

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> listener.onFeudScriptCompleted(new FeudScriptCompletedEvent(this, script)));
  }

  @Test
  void restoresCallerSecurityContext_evenOnFailure() {
    // The listener elevates to ROLE_SYSTEM for the endRivalry call (universe-scoped bookers
    // pass completeBeat but not endRivalry's global @PreAuthorize); the caller's context must
    // be restored afterwards — including when the service call throws.
    var callerAuth =
        new UsernamePasswordAuthenticationToken(
            "booker", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_BOOKER")));
    SecurityContextHolder.getContext().setAuthentication(callerAuth);

    doThrow(new RuntimeException("boom")).when(rivalryService).endRivalry(anyLong(), any());
    var script = rivalryScript(1L, "Rerun arc");

    listener.onFeudScriptCompleted(new FeudScriptCompletedEvent(this, script));

    assertEquals(callerAuth, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void alreadyEndedRivalry_endRivalryFiltersItOutOfNoOp() {
    // endRivalry() filters on isActive, so ending an ended rivalry is a no-op in the service;
    // the listener still delegates unconditionally for rivalry-linked scripts (idempotence by
    // contract, verified at the service level).
    Rivalry rivalry = new Rivalry();
    rivalry.setId(1L);
    FeudScript script = new FeudScript();
    script.setName("Rerun arc");
    script.setRivalry(rivalry);

    listener.onFeudScriptCompleted(new FeudScriptCompletedEvent(this, script));

    verify(rivalryService).endRivalry(1L, "Story arc 'Rerun arc' completed");
  }
}
