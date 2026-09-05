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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletion;
import com.github.javydreamercsw.management.domain.challenge.ChallengeCompletionStatus;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import com.github.javydreamercsw.management.service.challenge.ChallengeCompletionService;
import com.github.javydreamercsw.management.service.challenge.ChallengeService;
import com.github.javydreamercsw.management.service.challenge.ChallengeUpdateService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextArea;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * Covers the challenge detail dialog: section rendering (setup, conditions, modifiers, notes),
 * match type with and without a matching rule, and the player completion section (first-time
 * completion vs. editing an existing one).
 */
class ChallengeListViewDetailTest extends AbstractViewTest {

  @Mock private ChallengeService challengeService;
  @Mock private ChallengeUpdateService updateService;
  @Mock private ExpansionService expansionService;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private ChallengeCompletionService completionService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private SecurityUtils securityUtils;

  private static final ChallengeDTO FULL =
      ChallengeDTO.builder()
          .id("week_full")
          .title("Full Challenge")
          .season("Season 1")
          .expansionCode("BASE_GAME")
          .difficulty(Difficulty.MEDIUM)
          .objective("Win a match.")
          .setupInstructions("Standard setup with a twist.")
          .matchType("Normal")
          .conditions(List.of("No recovering."))
          .modifiers(List.of("Modifier one.", "Modifier two."))
          .notes("Track your progress on paper.")
          .active(true)
          .build();

  private Account account;

  @BeforeEach
  void setup() {
    account = new Account();
    account.setId(1L);
    account.setUsername("player1");
    when(securityUtils.getAuthenticatedUser())
        .thenReturn(Optional.of(new CustomUserDetails(account)));
    when(segmentRuleRepository.findByName(any())).thenReturn(Optional.empty());
    when(completionService.isCompleted(any(), any())).thenReturn(false);
    when(challengeService.getActiveChallenges()).thenReturn(List.of(FULL));
    when(challengeService.getAllChallenges()).thenReturn(List.of(FULL));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of());
  }

  private String dialogText(Dialog dialog) {
    StringBuilder sb = new StringBuilder();
    collectText(dialog, sb);
    return sb.toString();
  }

  private void collectText(com.vaadin.flow.component.Component c, StringBuilder sb) {
    String own = c.getElement().getText();
    if (own != null && !own.isBlank()) {
      sb.append(own).append('\n');
    }
    c.getChildren().forEach(child -> collectText(child, sb));
  }

  private ChallengeListView buildView() {
    ChallengeListView view =
        new ChallengeListView(
            challengeService,
            updateService,
            expansionService,
            segmentRuleRepository,
            completionService,
            imageStorageService,
            securityUtils);
    UI.getCurrent().add(view);
    return view;
  }

  private void openDetail(ChallengeListView view) {
    _get(view, Button.class, spec -> spec.withText("View Challenge")).click();
  }

  @Test
  @DisplayName("Detail dialog renders objective, setup, conditions, modifiers, and notes")
  void detailDialogRendersAllSections() {
    ChallengeListView view = buildView();
    openDetail(view);

    List<Dialog> dialogs = _find(UI.getCurrent(), Dialog.class);
    assertFalse(dialogs.isEmpty(), "Detail dialog should open");
    String text = dialogText(dialogs.get(0));
    assertTrue(text.contains("Full Challenge"));
    assertTrue(text.contains("Objective"));
    assertTrue(text.contains("Standard setup with a twist."));
    assertTrue(text.contains("Conditions"));
    assertTrue(text.contains("Modifier one."));
    assertTrue(text.contains("Modifier two."));
    assertTrue(text.contains("📌 Track your progress on paper."));
  }

  @Test
  @DisplayName("Multi-item conditions render as an unordered list")
  void conditionsRenderAsList() {
    ChallengeListView view = buildView();
    openDetail(view);

    List<com.vaadin.flow.component.html.UnorderedList> lists =
        _find(UI.getCurrent(), com.vaadin.flow.component.html.UnorderedList.class);
    assertFalse(lists.isEmpty(), "Conditions/modifiers with 2+ items should render as a list");
  }

  @Test
  @DisplayName("Unknown match type renders as plain paragraph")
  void unknownMatchTypeRendersParagraph() {
    ChallengeListView view = buildView();
    openDetail(view);

    String text = dialogText(_find(UI.getCurrent(), Dialog.class).get(0));
    assertTrue(text.contains("Match Type"));
    assertTrue(text.contains("Normal"));
  }

  @Test
  @DisplayName("Known match type renders an info button that opens MatchInfoDialog")
  void knownMatchTypeRendersInfoButton() {
    com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule rule =
        new com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule();
    when(segmentRuleRepository.findByName("Normal")).thenReturn(Optional.of(rule));

    ChallengeListView view = buildView();
    openDetail(view);

    List<Button> infoButtons =
        _find(UI.getCurrent(), Button.class, spec -> spec.withText("Normal"));
    assertFalse(infoButtons.isEmpty(), "Match type should render as an info button");
  }

  @Test
  @DisplayName("Authenticated player sees the completion section with Mark as Complete")
  void completionSectionShowsForAuthenticatedPlayer() {
    when(completionService.find(account, "week_full")).thenReturn(Optional.empty());

    ChallengeListView view = buildView();
    openDetail(view);

    Button complete =
        _get(UI.getCurrent(), Button.class, spec -> spec.withText("Mark as Complete"));
    assertTrue(complete.isEnabled());
    assertTrue(
        _find(UI.getCurrent(), TextArea.class).stream()
            .anyMatch(t -> "How did it go? Any tips for other players?".equals(t.getPlaceholder())),
        "Notes field should be present");
  }

  @Test
  @DisplayName("Already-completed challenge shows completed date and Save Changes")
  void completedChallengeShowsSaveChanges() {
    AccountChallengeCompletion completion = new AccountChallengeCompletion();
    completion.setStatus(ChallengeCompletionStatus.COMPLETED);
    completion.setCompletedAt(LocalDateTime.of(2026, 9, 1, 12, 0));
    completion.setPlayerNotes("Done with friends");
    when(completionService.find(account, "week_full")).thenReturn(Optional.of(completion));

    ChallengeListView view = buildView();
    openDetail(view);

    String text = dialogText(_find(UI.getCurrent(), Dialog.class).get(0));
    assertTrue(text.contains("Completed on:"), "Completed date should render");
    List<Button> saveBtns =
        _find(UI.getCurrent(), Button.class, spec -> spec.withText("Save Changes"));
    assertFalse(saveBtns.isEmpty(), "Save Changes should replace Mark as Complete");
    List<TextArea> notes = _find(UI.getCurrent(), TextArea.class);
    assertTrue(
        notes.stream().anyMatch(t -> "Done with friends".equals(t.getValue())),
        "Existing player notes should prefill");
  }

  @Test
  @DisplayName("Clicking Mark as Complete calls the service with notes and closes")
  void markCompleteCallsService() {
    when(completionService.find(account, "week_full")).thenReturn(Optional.empty());

    ChallengeListView view = buildView();
    openDetail(view);

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Mark as Complete")).click();

    verify(completionService).markComplete(eq(account), eq("week_full"), any(), any());
    assertTrue(
        _find(UI.getCurrent(), Dialog.class).isEmpty(), "Dialog should close after completion");
  }
}
