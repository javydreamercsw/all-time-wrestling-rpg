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
package com.github.javydreamercsw.management.ui.view.challenge;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import com.github.javydreamercsw.management.service.challenge.ChallengeCompletionService;
import com.github.javydreamercsw.management.service.challenge.ChallengeService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ChallengeListViewTest extends AbstractViewTest {

  @Mock private ChallengeService challengeService;
  @Mock private ExpansionService expansionService;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private ChallengeCompletionService completionService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private SecurityUtils securityUtils;

  private static final ChallengeDTO UNLOCKED =
      ChallengeDTO.builder()
          .id("test_unlocked")
          .title("Unlocked Challenge")
          .season("Season 1")
          .expansionCode("BASE_GAME")
          .difficulty(Difficulty.EASY)
          .objective("Win a match.")
          .active(true)
          .build();

  private static final ChallengeDTO LOCKED_EXTREME =
      ChallengeDTO.builder()
          .id("test_locked")
          .title("Locked Challenge")
          .season("Season 1")
          .expansionCode("BASE_GAME")
          .requiredExpansions(List.of("EXTREME"))
          .difficulty(Difficulty.HARD)
          .objective("Win an extreme match.")
          .active(true)
          .build();

  private static final ChallengeDTO CUSTOM =
      ChallengeDTO.builder()
          .id("test_custom")
          .title("Custom Challenge")
          .season("Season 1")
          .expansionCode("CUSTOM")
          .difficulty(Difficulty.MEDIUM)
          .objective("A custom challenge.")
          .active(true)
          .build();

  @BeforeEach
  void setup() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    when(segmentRuleRepository.findByName(any())).thenReturn(Optional.empty());
    when(completionService.isCompleted(any(), any())).thenReturn(false);
  }

  private ChallengeListView buildView(
      final List<ChallengeDTO> challenges, final List<String> enabledCodes) {
    when(challengeService.getActiveChallenges()).thenReturn(challenges);
    when(challengeService.getAllChallenges()).thenReturn(challenges);
    when(expansionService.getEnabledExpansionCodes()).thenReturn(enabledCodes);
    ChallengeListView view =
        new ChallengeListView(
            challengeService,
            expansionService,
            segmentRuleRepository,
            completionService,
            imageStorageService,
            securityUtils);
    UI.getCurrent().add(view);
    return view;
  }

  @Test
  @DisplayName("Unlocked challenge shows enabled View Challenge button")
  void availableChallengeShowsViewButton() {
    when(securityUtils.isAdmin()).thenReturn(false);
    ChallengeListView view = buildView(List.of(UNLOCKED), List.of());

    List<Button> buttons = _find(view, Button.class, spec -> spec.withText("View Challenge"));
    assertFalse(buttons.isEmpty(), "Should render View Challenge button for unlocked challenge");
    assertTrue(buttons.get(0).isEnabled(), "View Challenge button must be enabled");
  }

  @Test
  @DisplayName("Locked challenge shows Locked badge")
  void lockedChallengeShowsLockedBadge() {
    when(securityUtils.isAdmin()).thenReturn(false);
    ChallengeListView view = buildView(List.of(LOCKED_EXTREME), List.of());

    List<Span> spans = _find(view, Span.class);
    boolean hasLockedBadge = spans.stream().anyMatch(s -> s.getText().contains("Locked"));
    assertTrue(hasLockedBadge, "Locked badge should appear on locked card");
  }

  @Test
  @DisplayName("Non-admin sees disabled Locked button on locked card")
  void nonAdminLockedCardShowsDisabledButton() {
    when(securityUtils.isAdmin()).thenReturn(false);
    ChallengeListView view = buildView(List.of(LOCKED_EXTREME), List.of());

    List<Button> lockedBtns = _find(view, Button.class, spec -> spec.withText("🔒 Locked"));
    assertFalse(lockedBtns.isEmpty(), "Non-admin should see Locked button");
    assertFalse(lockedBtns.get(0).isEnabled(), "Locked button must be disabled for non-admin");
  }

  @Test
  @DisplayName("Non-admin locked button tooltip says to contact admin")
  void nonAdminLockedButtonTooltipMentionsAdmin() {
    when(securityUtils.isAdmin()).thenReturn(false);
    ChallengeListView view = buildView(List.of(LOCKED_EXTREME), List.of());

    Button lockedBtn = _get(view, Button.class, spec -> spec.withText("🔒 Locked"));
    assertTrue(
        lockedBtn.getTooltip().getText().contains("Contact your administrator"),
        "Tooltip should direct non-admin to their administrator");
  }

  @Test
  @DisplayName("Admin sees enabled Enable button instead of disabled Locked button")
  void adminLockedCardShowsEnabledEnableButton() {
    when(securityUtils.isAdmin()).thenReturn(true);
    ChallengeListView view = buildView(List.of(LOCKED_EXTREME), List.of());

    List<Button> enableBtns = _find(view, Button.class, spec -> spec.withText("Enable EXTREME"));
    assertFalse(enableBtns.isEmpty(), "Admin should see Enable EXTREME button");
    assertTrue(enableBtns.get(0).isEnabled(), "Enable button must be enabled for admin");

    List<Button> lockedBtns = _find(view, Button.class, spec -> spec.withText("🔒 Locked"));
    assertTrue(lockedBtns.isEmpty(), "Admin should not see disabled Locked button");
  }

  @Test
  @DisplayName("Admin clicking Enable calls setExpansionEnabled with correct code")
  void adminClickEnableCallsService() {
    when(securityUtils.isAdmin()).thenReturn(true);
    when(expansionService.getEnabledExpansionCodes())
        .thenReturn(List.of())
        .thenReturn(List.of("EXTREME"));
    when(challengeService.getActiveChallenges()).thenReturn(List.of(LOCKED_EXTREME));
    when(challengeService.getAllChallenges()).thenReturn(List.of(LOCKED_EXTREME));
    ChallengeListView view =
        new ChallengeListView(
            challengeService,
            expansionService,
            segmentRuleRepository,
            completionService,
            imageStorageService,
            securityUtils);
    UI.getCurrent().add(view);

    _get(view, Button.class, spec -> spec.withText("Enable EXTREME")).click();

    verify(expansionService).setExpansionEnabled("EXTREME", true);
  }

  @Test
  @DisplayName("Admin clicking Enable unlocks the card — View Challenge appears")
  void adminClickEnableUnlocksCard() {
    when(securityUtils.isAdmin()).thenReturn(true);
    when(expansionService.getEnabledExpansionCodes())
        .thenReturn(List.of())
        .thenReturn(List.of("EXTREME"));
    when(challengeService.getActiveChallenges()).thenReturn(List.of(LOCKED_EXTREME));
    when(challengeService.getAllChallenges()).thenReturn(List.of(LOCKED_EXTREME));
    ChallengeListView view =
        new ChallengeListView(
            challengeService,
            expansionService,
            segmentRuleRepository,
            completionService,
            imageStorageService,
            securityUtils);
    UI.getCurrent().add(view);

    _get(view, Button.class, spec -> spec.withText("Enable EXTREME")).click();

    List<Button> viewBtns = _find(view, Button.class, spec -> spec.withText("View Challenge"));
    assertFalse(viewBtns.isEmpty(), "View Challenge button should appear after enabling expansion");
    List<Button> enableBtns = _find(view, Button.class, spec -> spec.withText("Enable EXTREME"));
    assertTrue(enableBtns.isEmpty(), "Enable button should be gone after unlocking");
  }

  @Test
  @DisplayName("Official-only filter defaults to unchecked — custom challenges visible")
  void officialOnlyFilterDefaultsUnchecked() {
    when(securityUtils.isAdmin()).thenReturn(false);
    ChallengeListView view = buildView(List.of(CUSTOM), List.of());

    Checkbox officialOnly = _get(view, Checkbox.class, spec -> spec.withLabel("Official only"));
    assertFalse(officialOnly.getValue(), "Official-only filter must default to unchecked");

    List<Button> viewBtns = _find(view, Button.class, spec -> spec.withText("View Challenge"));
    assertFalse(viewBtns.isEmpty(), "Custom challenge should be visible when filter is unchecked");
  }

  @Test
  @DisplayName("Official-only filter checked hides custom challenges")
  void officialOnlyFilterHidesCustomChallenges() {
    when(securityUtils.isAdmin()).thenReturn(false);
    ChallengeListView view = buildView(List.of(CUSTOM), List.of());

    _get(view, Checkbox.class, spec -> spec.withLabel("Official only")).setValue(true);

    List<Button> viewBtns = _find(view, Button.class, spec -> spec.withText("View Challenge"));
    assertTrue(
        viewBtns.isEmpty(), "Custom challenge should be hidden when Official only is checked");
  }
}
