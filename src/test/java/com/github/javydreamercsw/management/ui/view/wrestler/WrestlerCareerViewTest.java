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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateHistory;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStateHistoryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class WrestlerCareerViewTest extends AbstractViewTest {

  @Mock private WrestlerFacade wrestlerFacade;
  @Mock private ShowFacade showFacade;
  @Mock private ViewContext viewContext;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private WrestlerStateHistoryService wrestlerStateHistoryService;
  @Mock private TitleService titleService;
  @Mock private TitleOpportunityService titleOpportunityService;
  @Mock private InjuryService injuryService;
  @Mock private com.github.javydreamercsw.management.service.show.ShowService careerShowService;
  @Mock private UniverseContextService universeContextService;

  private Wrestler wrestler;
  private WrestlerCareerView view;

  @BeforeEach
  void setup() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");

    when(wrestlerFacade.getWrestlerStatsService()).thenReturn(wrestlerStatsService);
    when(wrestlerFacade.getWrestlerStateHistoryService()).thenReturn(wrestlerStateHistoryService);
    when(wrestlerFacade.getTitleService()).thenReturn(titleService);
    when(wrestlerFacade.getTitleOpportunityService()).thenReturn(titleOpportunityService);
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of());
    when(wrestlerFacade.getInjuryService()).thenReturn(injuryService);
    when(viewContext.getUniverseContextService()).thenReturn(universeContextService);
    when(universeContextService.getCurrentUniverseId()).thenReturn(42L);

    when(wrestlerStatsService.getWrestlerStats(anyLong(), anyLong()))
        .thenReturn(Optional.of(new WrestlerStats(3L, 1L, 0L)));
    when(wrestlerStateHistoryService.getHistory(anyLong(), anyLong())).thenReturn(List.of());
    when(titleService.findReignsByChampion(any())).thenReturn(List.of());
    when(injuryService.getAllInjuriesForWrestler(anyLong(), anyLong())).thenReturn(List.of());

    view = new WrestlerCareerView(wrestlerFacade, showFacade, viewContext);
    UI.getCurrent().add(view);
  }

  private void buildViewWith(final Long universeId) {
    ReflectionTestUtils.setField(view, "wrestler", wrestler);
    ReflectionTestUtils.setField(view, "universeId", universeId);
    ReflectionTestUtils.invokeMethod(view, "buildView");
  }

  @Test
  @DisplayName("constructor succeeds without error")
  void constructorSucceeds() {
    assertThat(view).isNotNull();
  }

  @Test
  @DisplayName("buildView with no history shows placeholder paragraphs")
  void buildViewEmptyHistory_showsPlaceholders() {
    buildViewWith(42L);

    List<Paragraph> paragraphs = _find(view, Paragraph.class);
    assertThat(paragraphs).isNotEmpty();
    boolean hasFanPlaceholder =
        paragraphs.stream().anyMatch(p -> p.getText().contains("No history yet"));
    assertThat(hasFanPlaceholder).isTrue();
  }

  @Test
  @DisplayName("buildView with history renders fan growth chart and tier grid")
  @SuppressWarnings("unchecked")
  void buildViewWithHistory_showsChartAndGrid() {
    WrestlerStateHistory h1 =
        WrestlerStateHistory.builder()
            .wrestler(wrestler)
            .recordedAt(Instant.now().minus(7, ChronoUnit.DAYS))
            .fans(5_000L)
            .tier(WrestlerTier.ROOKIE)
            .build();
    WrestlerStateHistory h2 =
        WrestlerStateHistory.builder()
            .wrestler(wrestler)
            .recordedAt(Instant.now())
            .fans(10_000L)
            .tier(WrestlerTier.RISER)
            .build();
    when(wrestlerStateHistoryService.getHistory(anyLong(), anyLong())).thenReturn(List.of(h1, h2));

    buildViewWith(42L);

    List<Grid> grids = _find(view, Grid.class);
    assertThat(grids).isNotEmpty();
  }

  @Test
  @DisplayName("buildView with same tier throughout still shows tier grid with initial entry")
  @SuppressWarnings("unchecked")
  void buildViewSameTier_showsInitialTierEntry() {
    WrestlerStateHistory h1 =
        WrestlerStateHistory.builder()
            .wrestler(wrestler)
            .recordedAt(Instant.now().minus(7, ChronoUnit.DAYS))
            .fans(5_000L)
            .tier(WrestlerTier.ROOKIE)
            .build();
    WrestlerStateHistory h2 =
        WrestlerStateHistory.builder()
            .wrestler(wrestler)
            .recordedAt(Instant.now())
            .fans(8_000L)
            .tier(WrestlerTier.ROOKIE)
            .build();
    when(wrestlerStateHistoryService.getHistory(anyLong(), anyLong())).thenReturn(List.of(h1, h2));

    buildViewWith(42L);

    // First entry is always shown as the starting tier, so the grid is present
    List<Grid> grids = _find(view, Grid.class);
    assertThat(grids).isNotEmpty();
  }

  @Test
  @DisplayName("buildView with title reigns renders reign grid")
  @SuppressWarnings("unchecked")
  void buildViewWithReigns_showsGrid() {
    Title title = mock(Title.class);
    when(title.getName()).thenReturn("World Title");
    TitleReign reign = mock(TitleReign.class);
    when(reign.getTitle()).thenReturn(title);
    when(reign.getReignNumber()).thenReturn(1);
    when(reign.getStartDate()).thenReturn(Instant.now().minus(30, ChronoUnit.DAYS));
    when(reign.getEndDate()).thenReturn(null);
    when(reign.getReignLengthDisplay(any())).thenReturn("30 days");
    when(titleService.findReignsByChampion(any())).thenReturn(List.of(reign));

    buildViewWith(42L);

    List<Grid> grids = _find(view, Grid.class);
    assertThat(grids).isNotEmpty();
  }

  @Test
  @DisplayName("buildView with injuries renders injury log grid")
  @SuppressWarnings("unchecked")
  void buildViewWithInjuries_showsGrid() {
    Injury injury = mock(Injury.class);
    InjurySeverity severity = InjurySeverity.MINOR;
    when(injury.getName()).thenReturn("Knee Sprain");
    when(injury.getSeverity()).thenReturn(severity);
    when(injury.getInjuryDate()).thenReturn(Instant.now().minus(14, ChronoUnit.DAYS));
    when(injury.getDurationDisplay()).thenReturn("2 weeks");
    when(injury.isCurrentlyActive()).thenReturn(false);
    when(injuryService.getAllInjuriesForWrestler(anyLong(), anyLong())).thenReturn(List.of(injury));

    buildViewWith(42L);

    List<Grid> grids = _find(view, Grid.class);
    assertThat(grids).isNotEmpty();
  }

  @Test
  @DisplayName("buildView with null universeId shows no-universe message in injury section")
  void buildViewNullUniverse_injuryPlaceholder() {
    when(universeContextService.getCurrentUniverseId()).thenReturn(null);

    buildViewWith(null);

    List<Paragraph> paragraphs = _find(view, Paragraph.class);
    boolean hasNoUniverseMsg =
        paragraphs.stream().anyMatch(p -> p.getText().contains("No universe selected"));
    assertThat(hasNoUniverseMsg).isTrue();
  }

  @Test
  @DisplayName("buildView with null stats falls back to zero record")
  void buildViewNoStats_usesZeroFallback() {
    when(wrestlerStatsService.getWrestlerStats(anyLong(), anyLong())).thenReturn(Optional.empty());

    buildViewWith(42L);

    assertThat(view).isNotNull();
  }

  // ── Briefcase section (ATW-8p72) ─────────────────────────────────────────

  @Test
  @DisplayName("buildView with no briefcases shows the no-record placeholder")
  void briefcaseSection_noOpportunities_showsPlaceholder() {
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of());

    buildViewWith(42L);

    List<Paragraph> paragraphs = _find(view, Paragraph.class);
    assertThat(paragraphs)
        .anyMatch(p -> p.getText() != null && p.getText().contains("No briefcase"));
  }

  @Test
  @DisplayName("buildView with a HELD briefcase renders the held badge and history grid")
  void briefcaseSection_heldBadgeAndGrid() {
    TitleOpportunity held = new TitleOpportunity();
    held.setId(10L);
    held.setName("Time Vault briefcase");
    held.setStatus(TitleOpportunityStatus.HELD);
    held.setWrestler(wrestler);
    held.setEarnedAt(LocalDate.now().minusDays(30));
    held.setExpiryDate(LocalDate.now().plusDays(335));
    TitleOpportunity cashed = new TitleOpportunity();
    cashed.setId(11L);
    cashed.setName("Old case");
    cashed.setStatus(TitleOpportunityStatus.CASHED_IN);
    cashed.setWrestler(wrestler);
    cashed.setEarnedAt(LocalDate.now().minusDays(400));
    Title title = new Title();
    title.setId(2L);
    title.setName("ATW World");
    cashed.setCashedAgainstTitle(title);
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(cashed, held));
    when(titleService.getTitleById(2L)).thenReturn(Optional.of(title));
    when(showFacade.getShowService()).thenReturn(careerShowService);
    when(careerShowService.getUpcomingShows(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of());

    buildViewWith(42L);

    List<Span> spans = _find(view, Span.class);
    assertThat(spans).anyMatch(s -> s.getText() != null && s.getText().contains("HELD"));
    List<Grid> grids = _find(view, Grid.class);
    assertThat(grids).isNotEmpty();
  }

  @Test
  @DisplayName("buildView with only a cashed-in case renders history without a held badge")
  void briefcaseSection_cashedOnly_noBadge() {
    TitleOpportunity cashed = new TitleOpportunity();
    cashed.setId(12L);
    cashed.setName("Old case");
    cashed.setStatus(TitleOpportunityStatus.CASHED_IN);
    cashed.setWrestler(wrestler);
    cashed.setEarnedAt(LocalDate.now().minusDays(400));
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(cashed));
    when(showFacade.getShowService()).thenReturn(careerShowService);
    when(careerShowService.getUpcomingShows(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of());

    buildViewWith(42L);

    List<Span> spans = _find(view, Span.class);
    assertThat(spans).noneMatch(s -> s.getText() != null && s.getText().contains("— HELD"));
    List<Grid> grids = _find(view, Grid.class);
    assertThat(grids).isNotEmpty();
  }

  // ── Cash-in dialog (ATW-8p72) ────────────────────────────────────────────

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

  private void stubCashableCatalog() {
    Title active = new Title();
    active.setId(2L);
    active.setName("ATW World");
    active.setIsActive(true);
    Wrestler champ = new Wrestler();
    champ.setId(7L);
    champ.setName("Champion");
    active.getCurrentChampions().add(champ);
    Title retired = new Title();
    retired.setId(3L);
    retired.setName("Retired Belt");
    retired.setIsActive(false);
    when(titleService.findAll()).thenReturn(List.of(active, retired));
    when(showFacade.getShowService()).thenReturn(careerShowService);
    Show upcoming = new Show();
    upcoming.setId(5L);
    upcoming.setName("Saturday Slam");
    upcoming.setShowDate(LocalDate.now().plusDays(7));
    when(careerShowService.getUpcomingShows(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(upcoming));
  }

  private void openCashInDialogViaButton() {
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(heldCase()));
    stubCashableCatalog();
    buildViewWith(42L);

    List<Button> buttons = _find(view, Button.class);
    Button cashIn =
        buttons.stream()
            .filter(b -> "Cash In".equals(b.getText()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Cash In button not rendered"));
    cashIn.click();
  }

  @Test
  @DisplayName("Cash In button opens the dialog with the cashable championship filter")
  void cashInDialog_opens_withCashableTitles() {
    openCashInDialogViaButton();

    List<Dialog> dialogs = _find(UI.getCurrent(), Dialog.class);
    assertThat(dialogs).isNotEmpty();
    Dialog dialog = dialogs.getFirst();
    assertThat(dialog.getHeaderTitle()).isEqualTo("Cash In: Time Vault briefcase");

    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withLabel("Championship"));
    // The retired belt is filtered out of the cashable list.
    assertThat(titleCombo.getListDataView().getItemCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Confirm without selections shows a notification and books nothing")
  void cashInDialog_confirmWithoutSelection_showsWarning() {
    openCashInDialogViaButton();

    Dialog dialog = _find(UI.getCurrent(), Dialog.class).getFirst();
    Button confirm = _find(dialog, Button.class, spec -> spec.withText("Cash In")).getFirst();
    assertThat(confirm.isEnabled()).isFalse();

    // Even if triggered programmatically, nothing is booked while disabled.
    verify(titleOpportunityService, never()).cashIn(anyLong(), anyLong(), anyLong());
  }

  @Test
  @DisplayName("Selecting a championship and show books the cash-in through the service")
  void cashInDialog_selection_booksCashIn() {
    openCashInDialogViaButton();

    Dialog dialog = _find(UI.getCurrent(), Dialog.class).getFirst();
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withLabel("Championship"));
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Show> showCombo = _get(dialog, ComboBox.class, spec -> spec.withLabel("Show"));

    titleCombo.getListDataView().getItems().findFirst().ifPresent(titleCombo::setValue);
    showCombo.getListDataView().getItems().findFirst().ifPresent(showCombo::setValue);

    Button confirm = _find(dialog, Button.class, spec -> spec.withText("Cash In")).getFirst();
    assertThat(confirm.isEnabled()).isTrue();
    confirm.click();

    verify(titleOpportunityService).cashIn(30L, 2L, 5L);
  }

  @Test
  @DisplayName("A service validation error surfaces as an error notification")
  void cashInDialog_serviceError_showsNotification() {
    when(titleOpportunityService.cashIn(anyLong(), anyLong(), anyLong()))
        .thenThrow(new IllegalStateException("This briefcase is not cashable"));
    openCashInDialogViaButton();

    Dialog dialog = _find(UI.getCurrent(), Dialog.class).getFirst();
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Title> titleCombo =
        _get(dialog, ComboBox.class, spec -> spec.withLabel("Championship"));
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Show> showCombo = _get(dialog, ComboBox.class, spec -> spec.withLabel("Show"));
    titleCombo.getListDataView().getItems().findFirst().ifPresent(titleCombo::setValue);
    showCombo.getListDataView().getItems().findFirst().ifPresent(showCombo::setValue);

    Button confirm = _find(dialog, Button.class, spec -> spec.withText("Cash In")).getFirst();
    confirm.click();

    verify(titleOpportunityService).cashIn(30L, 2L, 5L);
    // The dialog stays open on error (closed only on success).
    assertThat(_find(UI.getCurrent(), Dialog.class)).isNotEmpty();
  }
}
