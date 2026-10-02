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
package com.github.javydreamercsw.management.ui.view.tournament;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.tournament.QualifierGroupsFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Karibu tests for the tournament list view (ATW-oahn): grid render, creation wizard gating and
 * seeding modes, and CRUD actions. Dialogs are driven through the views' test hooks; ConfirmDialog
 * lives on the UI (not the view subtree) and its buttons are shadow-DOM, so confirm is fired via
 * the event.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentListViewTest extends AbstractViewTest {

  @Mock private TournamentService tournamentService;
  @Mock private WrestlerFacade wrestlerFacade;
  @Mock private ShowFacade showFacade;
  @Mock private ViewContext viewContext;
  @Mock private SegmentRuleService segmentRuleService;

  @Mock private SegmentTypeService segmentTypeService;

  @Mock private ShowService showService;
  @Mock private UniverseContextService universeContextService;
  @Mock private SecurityUtils securityUtils;
  @Mock private TitleService titleService;
  @Mock private WrestlerService wrestlerService;
  @Mock private TournamentFormat format;
  @Mock private TournamentFormat qualifierFormat;

  private TournamentListView view;
  private Show upcomingShow;
  private SegmentType payoffType;
  private SegmentRule payoffRule;

  @BeforeEach
  void setup() {
    lenient().when(format.getFormatId()).thenReturn("SINGLE_ELIMINATION");
    lenient().when(format.getDisplayName()).thenReturn("Single Elimination");
    lenient().when(format.getMinEntrants()).thenReturn(4);
    lenient().when(format.getMaxEntrants()).thenReturn(8);
    lenient().when(tournamentService.getAvailableFormats()).thenReturn(List.of(format));
    lenient().when(tournamentService.findFormat(any())).thenReturn(Optional.of(format));
    lenient().when(segmentRuleService.findAll()).thenReturn(List.of());
    lenient().when(titleService.findAll()).thenReturn(List.of());
    lenient().when(wrestlerService.getAllWrestlers()).thenReturn(List.of());
    lenient().when(wrestlerFacade.getTitleService()).thenReturn(titleService);
    lenient().when(wrestlerFacade.getWrestlerService()).thenReturn(wrestlerService);
    lenient().when(showFacade.getSegmentRuleService()).thenReturn(segmentRuleService);
    lenient().when(showFacade.getSegmentTypeService()).thenReturn(segmentTypeService);
    lenient().when(showFacade.getShowService()).thenReturn(showService);
    upcomingShow = new Show();
    upcomingShow.setId(3L);
    upcomingShow.setName("Crown Cup Final");
    upcomingShow.setShowDate(LocalDate.now().plusDays(14));
    payoffType = new SegmentType();
    payoffType.setId(10L);
    payoffType.setName("Free-for-All");
    payoffRule = new SegmentRule();
    payoffRule.setId(20L);
    payoffRule.setName("Tables, Ladders and Chairs (TLC)");
    lenient().when(showService.getUpcomingShows(50)).thenReturn(List.of(upcomingShow));
    lenient().when(segmentTypeService.findAll()).thenReturn(List.of(payoffType));
    lenient().when(segmentRuleService.findAll()).thenReturn(List.of(payoffRule));
    lenient().when(viewContext.getUniverseContextService()).thenReturn(universeContextService);
    lenient().when(viewContext.getSecurityUtils()).thenReturn(securityUtils);
    lenient().when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    lenient().when(securityUtils.canEdit()).thenReturn(true);
    lenient().when(securityUtils.canDelete()).thenReturn(true);
    lenient().when(tournamentService.findAll()).thenReturn(List.of(tournament()));
    lenient().when(tournamentService.countEntries(any(Tournament.class))).thenReturn(8L);
    lenient().when(tournamentService.countEligibleEntrants(any(), any())).thenReturn(20);
    lenient()
        .when(tournamentService.findEligibleWrestlersSortedByFans(any(), any(), anyLong()))
        .thenReturn(List.of());
    lenient()
        .when(tournamentService.findByIdWithDetails(anyLong()))
        .thenAnswer(inv -> Optional.of(tournament()));
    lenient()
        .when(
            tournamentService.createTournament(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Tournament t = tournament();
              return t;
            });
    lenient().when(tournamentService.deleteTournament(anyLong())).thenReturn(true);

    view = new TournamentListView(tournamentService, wrestlerFacade, showFacade, viewContext);
    UI.getCurrent().add(view);
  }

  private Tournament tournament() {
    Tournament t = new Tournament();
    t.setId(1L);
    t.setName("Crown Cup");
    t.setFormatId("SINGLE_ELIMINATION");
    t.setStartDate(LocalDate.now().plusDays(7));
    return t;
  }

  /** The wizard gate needs BOTH name and format filled before Next/Create accept. */
  private void fillDetailsStep() {
    _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Tournament Name"))
        .setValue("Fed Cup");
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<TournamentFormat> formatBox =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Format"));
    formatBox.setValue(format);
  }

  @Test
  @DisplayName("Grid renders the universe's tournaments")
  void gridRendersTournaments() {
    Grid<Tournament> grid = _get(view, Grid.class);
    assertEquals(1, grid.getListDataView().getItemCount());
  }

  @Test
  @DisplayName("Toolbar offers New Tournament which opens the creation wizard")
  void newTournamentButton_opensWizard() {
    _get(view, Button.class, spec -> spec.withText("New Tournament")).click();

    Dialog wizard = _get(UI.getCurrent(), Dialog.class);
    assertTrue(wizard.isOpened());
  }

  @Test
  @DisplayName("Wizard starts on the Details tab with Create hidden")
  void wizardDetailsTab_hidesCreateButton() {
    view.openCreationWizardForTest();

    // _find only sees what a user could interact with: Create must be unreachable on Details.
    assertEquals(
        0,
        _find(UI.getCurrent(), Button.class, spec -> spec.withText("Create Tournament")).size(),
        "Create must be unreachable from the Details tab");
    assertNotNull(_get(UI.getCurrent(), Button.class, spec -> spec.withText("Next")));
  }

  @Test
  @DisplayName("Wizard requires name and format before moving to the seeding step")
  void wizardNext_gating() {
    view.openCreationWizardForTest();

    // Leave name and format empty — Next must refuse to advance.
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Next")).click();
    TabSheet tabs = _get(UI.getCurrent(), TabSheet.class);
    assertEquals(0, tabs.getSelectedIndex(), "Empty details must keep the wizard on Details");

    fillDetailsStep();
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Next")).click();
    assertEquals(1, tabs.getSelectedIndex(), "Filled details advance to the seeding step");

    Button create = _get(UI.getCurrent(), Button.class, spec -> spec.withText("Create Tournament"));
    assertTrue(create.isVisible());
  }

  @Test
  @DisplayName("Create with Auto seeding passes the entrant count to seedAuto")
  void wizardCreate_autoSeed() {
    view.openCreationWizardForTest();
    fillDetailsStep();
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Next")).click();
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Create Tournament")).click();

    verify(tournamentService)
        .createTournament(
            eq("Fed Cup"),
            eq("SINGLE_ELIMINATION"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any());
    verify(tournamentService).seedAuto(any(Tournament.class), anyInt(), anyLong());
  }

  @Test
  @DisplayName("Qualifier group-size field appears only for QUALIFIER_GROUPS and saves its value")
  void wizardGroupSize_visibleForQualifierGroups_andSaved() {
    lenient().when(qualifierFormat.getFormatId()).thenReturn(QualifierGroupsFormat.FORMAT_ID);
    lenient().when(qualifierFormat.getDisplayName()).thenReturn(QualifierGroupsFormat.FORMAT_ID);
    lenient().when(qualifierFormat.getMinEntrants()).thenReturn(4);
    lenient().when(qualifierFormat.getMaxEntrants()).thenReturn(25);
    lenient()
        .when(tournamentService.getAvailableFormats())
        .thenReturn(List.of(format, qualifierFormat));

    view.openCreationWizardForTest();
    _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Tournament Name"))
        .setValue("Fed Cup");
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<TournamentFormat> formatBox =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Format"));

    // Selecting the qualifier-groups format reveals the field — it lives on the seeding tab,
    // so advance there before probing (TabSheet renders tab content lazily).
    formatBox.setValue(qualifierFormat);
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Next")).click();
    IntegerField groupSize =
        _get(
            UI.getCurrent(),
            IntegerField.class,
            spec -> spec.withLabel("Wrestlers per Qualifier Group (optional)"));
    assertTrue(groupSize.isVisible());

    groupSize.setValue(3);
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Create Tournament")).click();

    // The created tournament carries the pinned group size.
    ArgumentCaptor<Tournament> created = ArgumentCaptor.forClass(Tournament.class);
    verify(tournamentService).save(created.capture());
    assertEquals(3, created.getValue().getQualifierGroupSize());
  }

  @Test
  @DisplayName("Edit action re-reads the tournament and opens the edit dialog")
  void editAction_opensDialogWithManagedTournament() {
    view.openEditDialogForTest(tournament());

    verify(tournamentService).findByIdWithDetails(1L);
    Dialog editDialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(editDialog.isOpened());
    assertNotNull(
        _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Tournament Name")));
  }

  @Test
  @DisplayName("Saving the edit dialog routes through updateTournament")
  void editDialog_saveUpdates() {
    view.openEditDialogForTest(tournament());

    _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Tournament Name"))
        .setValue("Renamed Cup");
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(tournamentService)
        .updateTournament(
            eq(1L),
            eq("Renamed Cup"),
            eq("SINGLE_ELIMINATION"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(true));
  }

  @Test
  @DisplayName("Delete action opens a confirmation and deletes on confirm")
  void deleteAction_confirmDeletes() {
    view.confirmDeleteForTest(tournament());

    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    assertNotNull(confirm);
    fireConfirm(confirm);

    verify(tournamentService).deleteTournament(1L);
  }

  @Test
  @DisplayName("Clicking the row's Delete button opens the confirmation (row-click must not win)")
  void deleteAction_rowButtonClick_opensConfirmDialog() {
    // Regression: a click on the Delete button inside the Actions cell did nothing — the
    // grid's row-click navigation swallowed it. Karibu's _getCellComponent reaches inside the
    // rendered row (tree-wide search cannot), so this drives the real component a user clicks.
    Grid<Tournament> grid = _get(view, Grid.class);
    Component actionsCell = _getCellComponent(grid, 0, "actions");

    Button deleteBtn = _get(actionsCell, Button.class, spec -> spec.withText("Delete"));
    assertTrue(deleteBtn.isVisible(), "Delete must render for a booker");
    deleteBtn.click();

    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    assertNotNull(confirm, "Row Delete click must open the confirmation dialog");
    fireConfirm(confirm);
    verify(tournamentService).deleteTournament(1L);
  }

  @Test
  @DisplayName("Delete of a started tournament surfaces the error without deleting")
  void deleteAction_startedTournament_showsError() {
    lenient()
        .when(tournamentService.deleteTournament(anyLong()))
        .thenThrow(new IllegalStateException("Only SCHEDULED tournaments can be deleted"));

    view.confirmDeleteForTest(tournament());
    fireConfirm(_get(UI.getCurrent(), ConfirmDialog.class));

    verify(tournamentService).deleteTournament(1L);
  }

  @Test
  @DisplayName("Wizard offers the host show and payoff pickers; payoff stays disabled until host")
  void wizard_hostShowPickers() {
    view.openCreationWizardForTest();

    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Show> hostCombo =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Host Show (optional)"));
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<SegmentType> payoffType =
        _get(
            UI.getCurrent(),
            ComboBox.class,
            spec -> spec.withLabel("Payoff Match Type (optional)"));
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<SegmentRule> payoffRule =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Payoff Rule (optional)"));

    assertFalse(payoffType.isEnabled(), "Payoff pickers stay disabled until a host show is picked");
    assertFalse(payoffRule.isEnabled());
    assertTrue(hostCombo.isEnabled());

    // Choosing a host show enables the payoff pickers.
    hostCombo.setValue(upcomingShow);

    assertTrue(payoffType.isEnabled(), "Picking a host show enables the payoff pickers");
    assertTrue(payoffRule.isEnabled());

    // Clearing the host disables them again and wipes the payoff selections.
    payoffType.setValue(this.payoffType);
    hostCombo.clear();
    assertFalse(payoffType.isEnabled());
    assertFalse(payoffRule.isEnabled());
    assertNull(payoffType.getValue(), "A cleared host show must clear the payoff type");
  }

  @Test
  @DisplayName("Wizard create with a host show routes the payoff fields to createTournament")
  void wizardCreate_withHostShow_passesPayoffFields() {
    view.openCreationWizardForTest();
    fillDetailsStep();
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Show> hostCombo =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Host Show (optional)"));
    hostCombo.setValue(upcomingShow);
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<SegmentType> payoffType =
        _get(
            UI.getCurrent(),
            ComboBox.class,
            spec -> spec.withLabel("Payoff Match Type (optional)"));
    payoffType.setValue(this.payoffType);
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Next")).click();
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Create Tournament")).click();

    verify(tournamentService)
        .createTournament(
            eq("Fed Cup"),
            eq("SINGLE_ELIMINATION"),
            any(),
            any(),
            any(),
            any(),
            eq(upcomingShow),
            eq(this.payoffType),
            any(),
            any());
  }

  @Test
  @DisplayName("Grid shows the host show name on hosted tournaments")
  void grid_hostShowColumnRendersPayoffShow() {
    Tournament hosted = tournament();
    hosted.setPayoffShow(upcomingShow);
    lenient().when(tournamentService.findAll()).thenReturn(List.of(hosted));
    view.refreshGridForTest();

    Grid<Tournament> grid = _get(view, Grid.class);
    assertEquals(1, grid.getListDataView().getItemCount());
  }

  @Test
  @DisplayName("Edit dialog: clearing the host show clears and disables the payoff pickers")
  void editDialog_clearingHostShow_disablesPayoffPickers() {
    Tournament hosted = tournament();
    hosted.setPayoffShow(upcomingShow);
    hosted.setPayoffSegmentType(payoffType);
    lenient().when(tournamentService.findByIdWithDetails(1L)).thenReturn(Optional.of(hosted));

    view.openEditDialogForTest(hosted);

    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Show> hostCombo =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Host Show (optional)"));
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<SegmentType> typeCombo =
        _get(
            UI.getCurrent(),
            ComboBox.class,
            spec -> spec.withLabel("Payoff Match Type (optional)"));

    assertTrue(typeCombo.isEnabled(), "Prefilled host show enables the payoff picker");
    hostCombo.clear();
    assertFalse(typeCombo.isEnabled(), "Clearing the host disables the payoff pickers");
    assertNull(typeCombo.getValue(), "Clearing the host clears the payoff type");
  }

  @Test
  @DisplayName("Edit dialog prefills the host show and payoff fields")
  void editDialog_prefillsPayoffFields() {
    Tournament hosted = tournament();
    hosted.setPayoffShow(upcomingShow);
    hosted.setPayoffSegmentType(payoffType);
    hosted.setPayoffSegmentRule(payoffRule);
    lenient().when(tournamentService.findByIdWithDetails(1L)).thenReturn(Optional.of(hosted));

    view.openEditDialogForTest(hosted);

    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<Show> hostCombo =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Host Show (optional)"));
    assertEquals(upcomingShow, hostCombo.getValue());
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<SegmentType> typeCombo =
        _get(
            UI.getCurrent(),
            ComboBox.class,
            spec -> spec.withLabel("Payoff Match Type (optional)"));
    assertEquals(payoffType, typeCombo.getValue());
  }

  /** Fires ConfirmDialog's confirm action via reflection (fireEvent is protected). */
  private static void fireConfirm(ConfirmDialog dialog) {
    try {
      var event = new ConfirmDialog.ConfirmEvent(dialog, true);
      var fireEvent = Component.class.getDeclaredMethod("fireEvent", ComponentEvent.class);
      fireEvent.setAccessible(true);
      fireEvent.invoke(dialog, event);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to fire confirm event", e);
    }
  }
}
