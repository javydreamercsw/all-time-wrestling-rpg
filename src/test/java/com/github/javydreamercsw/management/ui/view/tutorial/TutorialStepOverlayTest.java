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
package com.github.javydreamercsw.management.ui.view.tutorial;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.tutorial.TutorialDefinition;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep.InteractionMode;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class TutorialStepOverlayTest extends AbstractViewTest {

  @Mock private TutorialService tutorialService;
  @Mock private AccountService accountService;

  private Account account;
  private TutorialStepOverlay overlay;

  @BeforeEach
  void setUp() {
    account = new Account("player", "password", "player@test.com");
    ReflectionTestUtils.setField(account, "id", 1L);
    when(accountService.get(1L)).thenReturn(Optional.of(account));

    overlay = new TutorialStepOverlay(tutorialService, accountService);
  }

  private TutorialStep navStepMock(final String title, final String route, final String label) {
    TutorialStep step = mock(TutorialStep.class);
    when(step.getTitle()).thenReturn(title);
    when(step.getInstructions()).thenReturn("Instructions for " + title);
    when(step.getValidationHint()).thenReturn("hint");
    when(step.getTargetRoute()).thenReturn(route);
    when(step.getTargetViewLabel()).thenReturn(label);
    when(step.getInteractionMode()).thenReturn(InteractionMode.NAVIGATE);
    return step;
  }

  private TutorialStep inlineStepMock(final String title) {
    TutorialStep step = mock(TutorialStep.class);
    when(step.getTitle()).thenReturn(title);
    when(step.getInstructions()).thenReturn("Instructions for " + title);
    when(step.getInteractionMode()).thenReturn(InteractionMode.INLINE);
    return step;
  }

  private TutorialDefinition definitionOf(final TutorialStep... steps) {
    TutorialDefinition def = mock(TutorialDefinition.class);
    when(def.getSteps()).thenReturn(List.of(steps));
    when(def.getCompletionRoute()).thenReturn("tutorial");
    return def;
  }

  private void openAtStep(final int stepIndex, final TutorialStep... steps) {
    TutorialDefinition def = definitionOf(steps);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def);
    overlay.updateStep(account, Universe.UniverseType.GLOBAL, stepIndex, steps.length);
  }

  // ── updateStep rendering ──────────────────────────────────────────────────

  @Test
  @DisplayName("updateStep shows step title and instructions in overlay")
  void updateStep_setsTitle() {
    TutorialStep step = navStepMock("Create Your Show", "show-list", "Shows");
    openAtStep(0, step);

    assertThat(overlay.isOpened()).isTrue();
    assertThat(_find(overlay, com.vaadin.flow.component.html.H4.class))
        .extracting(com.vaadin.flow.component.html.H4::getText)
        .containsExactly("Create Your Show");
  }

  @Test
  @DisplayName("updateStep shows 'Next →' for non-last step")
  void updateStep_nonLastStep_showsNextButton() {
    TutorialStep s1 = navStepMock("Step One", "show-list", "Shows");
    TutorialStep s2 = navStepMock("Step Two", "show-list", "Shows");
    openAtStep(0, s1, s2);

    List<Button> btns = _find(overlay, Button.class);
    assertThat(btns).extracting(Button::getText).contains("Next →");
  }

  @Test
  @DisplayName("updateStep shows 'Complete ✓' for last step")
  void updateStep_lastStep_showsCompleteButton() {
    TutorialStep only = navStepMock("Final Step", "show-list", "Shows");
    openAtStep(0, only);

    List<Button> btns = _find(overlay, Button.class);
    assertThat(btns).extracting(Button::getText).contains("Complete ✓");
  }

  // ── handleNext: validation failure ───────────────────────────────────────

  @Test
  @DisplayName("Clicking Next when validation fails shows error and does not advance")
  void next_validationFails_showsError() {
    TutorialStep s1 = navStepMock("Step One", "show-list", "Shows");
    TutorialStep s2 = navStepMock("Step Two", "leagues", "Leagues");
    openAtStep(0, s1, s2);
    when(tutorialService.validateStep(any(), eq(Universe.UniverseType.GLOBAL), eq(0)))
        .thenReturn("No show found yet.");

    _get(overlay, Button.class, spec -> spec.withText("Next →")).click();

    verify(tutorialService, never()).advanceStep(anyLong(), any(), anyInt(), anyInt());
    List<Div> errorDivs = _find(overlay, Div.class);
    assertThat(errorDivs).anyMatch(d -> d.getText().contains("No show found yet."));
  }

  // ── handleNext: validation success, more steps ───────────────────────────

  @Test
  @DisplayName("Clicking Next on success calls runAfterStep and advanceStep")
  void next_validationPasses_advancesStep() {
    TutorialStep s1 = navStepMock("Step One", "show-list", "Shows");
    TutorialStep s2 = navStepMock("Step Two", "leagues", "Leagues");
    TutorialDefinition def = definitionOf(s1, s2);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def);
    when(tutorialService.validateStep(any(), eq(Universe.UniverseType.GLOBAL), eq(0)))
        .thenReturn(null);

    overlay.updateStep(account, Universe.UniverseType.GLOBAL, 0, 2);

    _get(overlay, Button.class, spec -> spec.withText("Next →")).click();

    verify(tutorialService).runAfterStep(account, Universe.UniverseType.GLOBAL, 0);
    verify(tutorialService).advanceStep(1L, Universe.UniverseType.GLOBAL, 1, 2);
  }

  // ── handleNext: last step ────────────────────────────────────────────────

  @Test
  @DisplayName("Clicking Complete on last step calls advanceStep and closes overlay")
  void next_lastStep_closesOverlay() {
    TutorialStep only = navStepMock("Final Step", "show-list", "Shows");
    TutorialDefinition def = definitionOf(only);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def);
    when(tutorialService.validateStep(any(), eq(Universe.UniverseType.GLOBAL), eq(0)))
        .thenReturn(null);

    overlay.updateStep(account, Universe.UniverseType.GLOBAL, 0, 1);

    _get(overlay, Button.class, spec -> spec.withText("Complete ✓")).click();

    verify(tutorialService).advanceStep(1L, Universe.UniverseType.GLOBAL, 1, 1);
    assertThat(overlay.isOpened()).isFalse();
  }

  // ── handleNext: next step is INLINE ──────────────────────────────────────

  @Test
  @DisplayName("When next step is INLINE, overlay closes and navigates to /tutorial")
  void next_nextStepIsInline_closesOverlay() {
    TutorialStep navStep = navStepMock("Create Show", "show-list", "Shows");
    TutorialStep inlineStep = inlineStepMock("Pick Wrestler");
    TutorialDefinition def = definitionOf(navStep, inlineStep);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def);
    when(tutorialService.validateStep(any(), eq(Universe.UniverseType.GLOBAL), eq(0)))
        .thenReturn(null);

    overlay.updateStep(account, Universe.UniverseType.GLOBAL, 0, 2);

    _get(overlay, Button.class, spec -> spec.withText("Next →")).click();

    verify(tutorialService).advanceStep(1L, Universe.UniverseType.GLOBAL, 1, 2);
    assertThat(overlay.isOpened()).isFalse();
  }

  // ── skip ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Skip button calls markSkipped and closes overlay")
  void skipButton_callsMarkSkippedAndCloses() {
    TutorialStep s1 = navStepMock("Step One", "show-list", "Shows");
    TutorialStep s2 = navStepMock("Step Two", "leagues", "Leagues");
    openAtStep(0, s1, s2);

    // The skip button has an empty text label — find it among empty-label buttons and click the
    // one that triggers markSkipped (it's the close-small icon button added second in buildHeader)
    List<Button> emptyBtns = _find(overlay, Button.class, spec -> spec.withText(""));
    // Two icon-only buttons: minimize (index 0) and skip (index 1)
    assertThat(emptyBtns).hasSizeGreaterThanOrEqualTo(2);
    emptyBtns.get(1).click();

    verify(tutorialService).markSkipped(1L, Universe.UniverseType.GLOBAL, 2);
    assertThat(overlay.isOpened()).isFalse();
  }
}
