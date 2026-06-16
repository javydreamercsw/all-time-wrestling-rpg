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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.image.ImageResolution;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.tutorial.TutorialDefinition;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep.InteractionMode;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class TutorialViewTest extends AbstractViewTest {

  @Mock private TutorialService tutorialService;
  @Mock private SecurityUtils securityUtils;
  @Mock private UniverseContextService universeContextService;
  @Mock private AccountService accountService;
  @Mock private WrestlerService wrestlerService;
  @Mock private AiSettingsService aiSettingsService;

  @Mock
  private com.github.javydreamercsw.management.service.expansion.ExpansionService expansionService;

  private Account testAccount;
  private TutorialView view;

  @BeforeEach
  void setUp() {
    testAccount = new Account("player", "password", "player@test.com");
    ReflectionTestUtils.setField(testAccount, "id", 1L);

    when(securityUtils.getCurrentAccountId()).thenReturn(Optional.of(1L));
    when(accountService.get(1L)).thenReturn(Optional.of(testAccount));
    // Default: no tutorial universe exists → show mode-selection
    when(tutorialService.findTutorialUniverse("player")).thenReturn(Optional.empty());

    when(expansionService.getExpansions()).thenReturn(java.util.List.of());
    when(expansionService.isExpansionEnabled(any())).thenReturn(true);

    // Stub getDefinition for all three modes so modeCard() can call isAdvanced()
    TutorialDefinition simpleDef = mock(TutorialDefinition.class);
    when(simpleDef.isAdvanced()).thenReturn(false);
    when(tutorialService.getDefinition(Universe.UniverseType.CAMPAIGN)).thenReturn(simpleDef);
    when(tutorialService.getDefinition(Universe.UniverseType.LEAGUE)).thenReturn(simpleDef);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(simpleDef);

    view =
        new TutorialView(
            tutorialService,
            securityUtils,
            universeContextService,
            accountService,
            wrestlerService,
            aiSettingsService,
            expansionService);
    UI.getCurrent().add(view);
  }

  private void enter() {
    view.beforeEnter(mock(BeforeEnterEvent.class));
  }

  // ── SETUP_MODE phase ──────────────────────────────────────────────────────

  @Test
  @DisplayName("No active universe shows mode selection heading")
  void noUniverse_showsModeSelectionHeading() {
    enter();

    H2 heading = _get(view, H2.class);
    assertThat(heading.getText()).isEqualTo("Welcome! Choose Your Play Style");
  }

  @Test
  @DisplayName("No active universe shows three mode cards")
  void noUniverse_showsThreeModeCards() {
    enter();

    List<Button> buttons = _find(view, Button.class);
    assertThat(buttons)
        .extracting(Button::getText)
        .contains("Choose Campaign", "Choose League", "Choose Universe");
  }

  @Test
  @DisplayName("Clicking a mode card transitions to feature config screen")
  void clickModeCard_transitionsToFeatureConfig() {
    enter();

    _get(view, Button.class, spec -> spec.withText("Choose Campaign")).click();

    // Feature config has the tutorial feature checkboxes
    assertThat(_find(view, Checkbox.class)).isNotEmpty();
    List<Button> buttons = _find(view, Button.class);
    assertThat(buttons).extracting(Button::getText).anyMatch(t -> t.contains("Create My Universe"));
  }

  @Test
  @DisplayName(
      "Feature config screen: non-AI checkboxes default to checked; AI disabled when no provider")
  void featureConfig_allCheckboxesDefaultChecked() {
    // setUp() default: findTutorialUniverse returns empty → SETUP_MODE phase
    // No AI provider configured (mock default returns false/empty)
    enter();
    _get(view, Button.class, spec -> spec.withText("Choose League")).click();

    List<Checkbox> checkboxes = _find(view, Checkbox.class);
    assertThat(checkboxes).isNotEmpty();
    // Non-AI checkboxes should be checked; AI News should be unchecked and disabled
    List<Checkbox> enabled = checkboxes.stream().filter(Checkbox::isEnabled).toList();
    List<Checkbox> disabled = checkboxes.stream().filter(cb -> !cb.isEnabled()).toList();
    assertThat(enabled).allMatch(Checkbox::getValue);
    assertThat(disabled).allMatch(cb -> !cb.getValue());
  }

  @Test
  @DisplayName("Feature config screen: AI News checkbox enabled when AI is configured")
  void featureConfig_aiNewsEnabledWhenAiConfigured() {
    // setUp() default: findTutorialUniverse returns empty → SETUP_MODE phase
    when(aiSettingsService.isClaudeEnabled()).thenReturn(true);
    enter();
    _get(view, Button.class, spec -> spec.withText("Choose League")).click();

    List<Checkbox> checkboxes = _find(view, Checkbox.class);
    assertThat(checkboxes).isNotEmpty();
    assertThat(checkboxes).allMatch(Checkbox::isEnabled);
    assertThat(checkboxes).allMatch(Checkbox::getValue);
  }

  @Test
  @DisplayName("Back button from feature config returns to mode selection")
  void featureConfig_backButton_returnsModeSelection() {
    enter();
    _get(view, Button.class, spec -> spec.withText("Choose Universe")).click();

    _get(view, Button.class, spec -> spec.withText("← Back")).click();

    H2 heading = _get(view, H2.class);
    assertThat(heading.getText()).isEqualTo("Welcome! Choose Your Play Style");
  }

  @Test
  @DisplayName("Create Universe button calls tutorialService.createTutorialUniverse")
  void featureConfig_createButton_callsService() {
    enter();
    _get(view, Button.class, spec -> spec.withText("Choose Campaign")).click();

    Universe created = mock(Universe.class);
    when(created.getName()).thenReturn("Tutorial – player");
    when(created.getType()).thenReturn(Universe.UniverseType.CAMPAIGN);
    when(tutorialService.createTutorialUniverse(any(), any(), any(), any())).thenReturn(created);

    // Set up wizard state for after creation
    TutorialStep inlineStep = inlineStepMock("Assign Your Wrestler", "hint");
    TutorialDefinition definition = definitionOf(Universe.UniverseType.CAMPAIGN, inlineStep);
    when(tutorialService.getDefinition(Universe.UniverseType.CAMPAIGN)).thenReturn(definition);
    when(tutorialService.getCurrentStep(1L, Universe.UniverseType.CAMPAIGN)).thenReturn(0);
    when(wrestlerService.findAllActiveWithAlignments()).thenReturn(List.of());

    _get(view, Button.class, spec -> spec.withText("Create My Universe & Start Tutorial")).click();

    verify(tutorialService)
        .createTutorialUniverse(eq(testAccount), eq(Universe.UniverseType.CAMPAIGN), any(), any());
  }

  // ── WIZARD phase ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("Active universe skips setup and shows wizard heading")
  void activeUniverse_showsWizardStep() {
    Universe universe = universeOf(Universe.UniverseType.GLOBAL);
    when(tutorialService.findTutorialUniverse("player")).thenReturn(Optional.of(universe));

    TutorialStep step = inlineStepMock("Pick Your Featured Wrestler", "We'll check...");
    TutorialDefinition def = definitionOf(Universe.UniverseType.GLOBAL, step);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def);
    when(tutorialService.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).thenReturn(0);
    when(wrestlerService.findAllActiveWithAlignments()).thenReturn(List.of());

    enter();

    H2 heading = _get(view, H2.class);
    assertThat(heading.getText()).isEqualTo("Pick Your Featured Wrestler");
  }

  @Test
  @DisplayName("INLINE step renders wrestler picker — no Go-To button")
  void inlineStep_rendersWrestlerPicker_noNavigateButton() {
    Universe universe = universeOf(Universe.UniverseType.GLOBAL);
    when(tutorialService.findTutorialUniverse("player")).thenReturn(Optional.of(universe));

    TutorialStep step = inlineStepMock("Assign Your Wrestler", "hint");
    TutorialDefinition def1 = definitionOf(Universe.UniverseType.GLOBAL, step);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def1);
    when(tutorialService.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).thenReturn(0);
    when(wrestlerService.findAllActiveWithAlignments()).thenReturn(List.of());

    enter();

    // "Go to X" button should NOT appear — this is an inline step
    List<Button> buttons = _find(view, Button.class);
    assertThat(buttons).extracting(Button::getText).noneMatch(t -> t.startsWith("Go to "));

    // Skip Tutorial button should still be present
    assertThat(buttons).extracting(Button::getText).contains("Skip Tutorial");
  }

  @Test
  @DisplayName("INLINE step shows wrestler cards for each returned wrestler")
  void inlineStep_showsWrestlerCards() {
    Universe universe = universeOf(Universe.UniverseType.GLOBAL);
    when(tutorialService.findTutorialUniverse("player")).thenReturn(Optional.of(universe));

    TutorialStep step = inlineStepMock("Pick Your Featured Wrestler", "hint");
    TutorialDefinition defWrestlers = definitionOf(Universe.UniverseType.GLOBAL, step);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(defWrestlers);
    when(tutorialService.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).thenReturn(0);

    Wrestler w1 = wrestlerMock(10L, "Stone Cold", AlignmentType.FACE);
    Wrestler w2 = wrestlerMock(11L, "Triple H", AlignmentType.HEEL);
    when(wrestlerService.findAllActiveWithAlignments()).thenReturn(List.of(w1, w2));

    enter();

    List<Button> selectButtons = _find(view, Button.class, spec -> spec.withText("Select"));
    assertThat(selectButtons).hasSize(2);
  }

  @Test
  @DisplayName("NAVIGATE step redirects to target route (overlay takes over)")
  void navigateStep_redirectsToTargetRoute() {
    Universe universe = universeOf(Universe.UniverseType.GLOBAL);
    when(tutorialService.findTutorialUniverse("player")).thenReturn(Optional.of(universe));

    TutorialStep inlineStep = inlineStepMock("Pick Your Featured Wrestler", "hint1");
    TutorialStep navStep = navigateStepMock("Create a Show", "show-list", "Shows", "hint");
    TutorialStep finalStep = navigateStepMock("Run Your Show", "show-list", "Shows", "hint3");
    TutorialDefinition def =
        definitionOf(Universe.UniverseType.GLOBAL, inlineStep, navStep, finalStep);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def);
    when(tutorialService.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).thenReturn(1);

    enter();

    // NAVIGATE steps hand off to the floating overlay; TutorialView itself becomes empty
    // and navigation to the target route is triggered.
    List<Button> buttons = _find(view, Button.class);
    assertThat(buttons).extracting(Button::getText).noneMatch(t -> t.startsWith("Go to"));
    assertThat(buttons).extracting(Button::getText).doesNotContain("Next →");
  }

  @Test
  @DisplayName("Skip button calls tutorialService.markSkipped")
  void skipButton_callsMarkSkipped() {
    Universe universe = universeOf(Universe.UniverseType.GLOBAL);
    when(tutorialService.findTutorialUniverse("player")).thenReturn(Optional.of(universe));
    when(securityUtils.isPlayer()).thenReturn(true);

    TutorialStep step = inlineStepMock("Step", "hint");
    TutorialDefinition defSkip = definitionOf(Universe.UniverseType.GLOBAL, step);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(defSkip);
    when(tutorialService.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).thenReturn(0);
    when(wrestlerService.findAllActiveWithAlignments()).thenReturn(List.of());

    enter();

    _get(view, Button.class, spec -> spec.withText("Skip Tutorial")).click();

    verify(tutorialService).markSkipped(eq(1L), eq(Universe.UniverseType.GLOBAL), eq(1));
  }

  @Test
  @DisplayName("Selecting a wrestler calls setActiveWrestlerId and advances step")
  void selectWrestler_callsServiceAndAdvances() {
    Universe universe = universeOf(Universe.UniverseType.GLOBAL);
    when(tutorialService.findTutorialUniverse("player")).thenReturn(Optional.of(universe));

    TutorialStep inlineStep = inlineStepMock("Pick Your Featured Wrestler", "hint");
    // Add a trailing step so selection doesn't trigger the completion-screen navigation
    TutorialStep nextStep = navigateStepMock("Create a Show", "show-list", "Shows", "hint2");

    TutorialDefinition def = definitionOf(Universe.UniverseType.GLOBAL, inlineStep, nextStep);
    when(tutorialService.getDefinition(Universe.UniverseType.GLOBAL)).thenReturn(def);
    when(tutorialService.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).thenReturn(0);
    // validateStep is called after wrestler selection — return success
    when(tutorialService.validateStep(any(), eq(Universe.UniverseType.GLOBAL), eq(0)))
        .thenReturn(null);

    Wrestler w = wrestlerMock(42L, "Undertaker", null);
    when(wrestlerService.findAllActiveWithAlignments()).thenReturn(List.of(w));

    // After selection, reload account reflects new wrestler
    Account updatedAccount = new Account("player", "password", "player@test.com");
    ReflectionTestUtils.setField(updatedAccount, "id", 1L);
    ReflectionTestUtils.setField(updatedAccount, "activeWrestlerId", 42L);
    when(accountService.get(1L))
        .thenReturn(Optional.of(testAccount))
        .thenReturn(Optional.of(updatedAccount));
    when(accountService.setActiveWrestlerId(1L, 42L)).thenReturn(updatedAccount);

    enter();

    _get(view, Button.class, spec -> spec.withText("Select")).click();

    verify(accountService).setActiveWrestlerId(1L, 42L);
    verify(tutorialService).advanceStep(eq(1L), eq(Universe.UniverseType.GLOBAL), eq(1), eq(2));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static Universe universeOf(final Universe.UniverseType type) {
    Universe u = mock(Universe.class);
    when(u.getType()).thenReturn(type);
    return u;
  }

  private static TutorialStep inlineStepMock(final String title, final String hint) {
    TutorialStep step = mock(TutorialStep.class);
    when(step.getInteractionMode()).thenReturn(InteractionMode.INLINE);
    when(step.getTitle()).thenReturn(title);
    when(step.getValidationHint()).thenReturn(hint);
    when(step.getImagePath()).thenReturn(null);
    when(step.getTargetRoute()).thenReturn("player");
    when(step.getTargetViewLabel()).thenReturn("Player Dashboard");
    when(step.getStepNumber()).thenReturn(1);
    return step;
  }

  private static TutorialStep navigateStepMock(
      final String title, final String route, final String label, final String hint) {
    TutorialStep step = mock(TutorialStep.class);
    when(step.getInteractionMode()).thenReturn(InteractionMode.NAVIGATE);
    when(step.getTitle()).thenReturn(title);
    when(step.getValidationHint()).thenReturn(hint);
    when(step.getImagePath()).thenReturn(null);
    when(step.getTargetRoute()).thenReturn(route);
    when(step.getTargetViewLabel()).thenReturn(label);
    when(step.getStepNumber()).thenReturn(1);
    return step;
  }

  private static TutorialDefinition definitionOf(
      final Universe.UniverseType type, final TutorialStep... steps) {
    TutorialDefinition def = mock(TutorialDefinition.class);
    when(def.getMode()).thenReturn(type);
    when(def.getSteps()).thenReturn(List.of(steps));
    return def;
  }

  private Wrestler wrestlerMock(
      final Long id, final String name, final AlignmentType alignmentType) {
    Wrestler w = mock(Wrestler.class);
    when(w.getId()).thenReturn(id);
    when(w.getName()).thenReturn(name);
    when(w.getActive()).thenReturn(Boolean.TRUE);
    when(w.getDescription()).thenReturn(name + " description");
    when(w.getImageUrl()).thenReturn(null);
    // resolveWrestlerImage is called inside runAsAdmin — stub it on the service mock
    when(wrestlerService.resolveWrestlerImage(w))
        .thenReturn(new ImageResolution("/images/placeholder.png", true));
    if (alignmentType != null) {
      WrestlerAlignment alignment = mock(WrestlerAlignment.class);
      when(alignment.getAlignmentType()).thenReturn(alignmentType);
      when(w.getAlignment()).thenReturn(alignment);
    } else {
      when(w.getAlignment()).thenReturn(null);
    }
    return w;
  }
}
