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
package com.github.javydreamercsw.management.ui.component;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/** ATW-312z: the briefcase section shared by the career view and the wrestler profile. */
class BriefcaseSectionTest extends AbstractViewTest {

  @Mock private TitleOpportunityService titleOpportunityService;
  @Mock private TitleService titleService;

  private ShowService showService;
  private Wrestler wrestler;

  @BeforeEach
  void setup() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");

    showService = mock(ShowService.class);
    when(showService.getUpcomingShows(50)).thenReturn(List.of());
    when(titleService.findAll()).thenReturn(List.of());
  }

  private TitleOpportunity heldCase() {
    TitleOpportunity held = new TitleOpportunity();
    held.setId(30L);
    held.setName("Time Vault briefcase");
    held.setStatus(TitleOpportunityStatus.HELD);
    held.setWrestler(wrestler);
    held.setEarnedAt(LocalDate.now().minusDays(30));
    held.setExpiryDate(LocalDate.now().plusDays(335));
    return held;
  }

  @Test
  @DisplayName("Held case renders badge with name and Cash In button")
  void heldCase_rendersBadgeAndCashInButton() {
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(heldCase()));

    BriefcaseSection section =
        new BriefcaseSection(
            wrestler.getId(), titleOpportunityService, titleService, showService, () -> {});
    UI.getCurrent().add(section);

    List<Span> spans = _find(section, Span.class);
    assertThat(spans.stream().anyMatch(s -> s.getText() != null && s.getText().contains("HELD")))
        .as("held badge should be rendered")
        .isTrue();

    List<Button> buttons = _find(section, Button.class);
    assertThat(buttons.stream().anyMatch(b -> "Cash In".equals(b.getText())))
        .as("Cash In button should be rendered")
        .isTrue();
  }

  @Test
  @DisplayName("Cash In button opens the shared BriefcaseCashInDialog")
  void cashInButton_opensSharedDialog() {
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(heldCase()));

    BriefcaseSection section =
        new BriefcaseSection(
            wrestler.getId(), titleOpportunityService, titleService, showService, () -> {});
    UI.getCurrent().add(section);

    _find(section, Button.class).stream()
        .filter(b -> "Cash In".equals(b.getText()))
        .findFirst()
        .orElseThrow()
        .click();

    List<Dialog> dialogs = _find(UI.getCurrent(), Dialog.class);
    assertThat(dialogs).isNotEmpty();
    assertThat(dialogs.getFirst().getHeaderTitle()).isEqualTo("Cash In: Time Vault briefcase");
  }

  @Test
  @DisplayName("Held case with an image renders the artwork instead of the emoji badge")
  void heldCase_withImage_rendersArtwork() {
    TitleOpportunity held = heldCase();
    held.setImageUrl("img://briefcase.png");
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(held));

    BriefcaseSection section =
        new BriefcaseSection(
            wrestler.getId(), titleOpportunityService, titleService, showService, () -> {});
    UI.getCurrent().add(section);

    List<Image> images = _find(section, Image.class);
    assertThat(images)
        .as("the held badge row should show the uploaded artwork")
        .anyMatch(i -> "img://briefcase.png".equals(i.getSrc()));
  }

  @Test
  @DisplayName("Held case without an image keeps the emoji badge")
  void heldCase_withoutImage_keepsEmojiBadge() {
    TitleOpportunity held = heldCase(); // no imageUrl
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(held));

    BriefcaseSection section =
        new BriefcaseSection(
            wrestler.getId(), titleOpportunityService, titleService, showService, () -> {});
    UI.getCurrent().add(section);

    assertThat(_find(section, Image.class)).isEmpty();
    List<Span> spans = _find(section, Span.class);
    assertThat(spans.stream().anyMatch(s -> s.getText() != null && s.getText().contains("💼")))
        .isTrue();
  }

  @Test
  @DisplayName("No opportunities renders the empty-state paragraph and no grid")
  void noOpportunities_rendersEmptyState() {
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of());

    BriefcaseSection section =
        new BriefcaseSection(
            wrestler.getId(), titleOpportunityService, titleService, showService, () -> {});
    UI.getCurrent().add(section);

    List<Paragraph> paragraphs = _find(section, Paragraph.class);
    assertThat(
            paragraphs.stream()
                .anyMatch(p -> p.getText() != null && p.getText().contains("No briefcase")))
        .isTrue();
    assertThat(_find(section, Grid.class)).isEmpty();
  }

  @Test
  @DisplayName("History grid lists non-held opportunities with cashed-against title")
  void historyGrid_listsCashedInCases() {
    TitleOpportunity held = heldCase();
    TitleOpportunity cashed = new TitleOpportunity();
    cashed.setId(31L);
    cashed.setName("Time Vault briefcase");
    cashed.setStatus(TitleOpportunityStatus.CASHED_IN);
    cashed.setWrestler(wrestler);
    cashed.setEarnedAt(LocalDate.now().minusDays(400));
    cashed.setCashedAt(LocalDate.now().minusDays(40));
    Title cashedAgainst = new Title();
    cashedAgainst.setId(7L);
    cashedAgainst.setName("ATW Championship");
    cashed.setCashedAgainstTitle(cashedAgainst);
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(held, cashed));
    when(titleService.getTitleById(7L)).thenReturn(Optional.of(cashedAgainst));

    BriefcaseSection section =
        new BriefcaseSection(
            wrestler.getId(), titleOpportunityService, titleService, showService, () -> {});
    UI.getCurrent().add(section);

    List<Grid> grids = _find(section, Grid.class);
    assertThat(grids).hasSize(1);
    Grid grid = grids.getFirst();
    List<TitleOpportunity> items =
        (List<TitleOpportunity>) grid.getGenericDataView().getItems().toList();
    assertThat(items).hasSize(2);
  }

  @Test
  @DisplayName("Refresh callback is accepted and dialog opens for the embedding view")
  void cashIn_acceptsRefreshCallback() {
    TitleOpportunity held = heldCase();
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(held));

    Show upcoming = new Show();
    upcoming.setId(90L);
    upcoming.setName("Upcoming Show");
    when(showService.getUpcomingShows(50)).thenReturn(List.of(upcoming));

    boolean[] refreshed = {false};

    BriefcaseSection section =
        new BriefcaseSection(
            wrestler.getId(),
            titleOpportunityService,
            titleService,
            showService,
            () -> refreshed[0] = true);
    UI.getCurrent().add(section);

    _find(section, Button.class).stream()
        .filter(b -> "Cash In".equals(b.getText()))
        .findFirst()
        .orElseThrow()
        .click();

    List<Dialog> dialogs = _find(UI.getCurrent(), Dialog.class);
    assertThat(dialogs).isNotEmpty();
    // The callback only fires after a successful cash-in inside the dialog; opening must not
    // have triggered it.
    assertThat(refreshed[0]).isFalse();
  }
}
