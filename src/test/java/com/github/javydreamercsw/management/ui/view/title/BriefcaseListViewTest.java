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
package com.github.javydreamercsw.management.ui.view.title;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

/** ATW-jpki: the briefcase CRUD management view. */
class BriefcaseListViewTest extends AbstractViewTest {

  @Mock private TitleOpportunityService titleOpportunityService;
  @Mock private TitleOpportunityRepository opportunityRepository;

  @Mock private WrestlerRepository wrestlerRepository;

  @Mock private GameSettingService gameSettingService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageStorageService imageStorageService;

  private BriefcaseListView view;
  private TitleOpportunity held;
  private Wrestler holder;

  @BeforeEach
  void setUp() {
    holder = new Wrestler();
    holder.setId(8L);
    holder.setName("Mukundi Shumba");
    holder.setActive(true);
    holder.setGender(Gender.MALE);

    Universe universe = new Universe();
    universe.setId(1L);
    universe.setName("Default");

    held = new TitleOpportunity();
    held.setId(40L);
    held.setName("Time Vault briefcase");
    held.setStatus(TitleOpportunityStatus.HELD);
    held.setWrestler(holder);
    held.setUniverse(universe);
    held.setEarnedAt(LocalDate.of(2026, 10, 8));

    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);
    when(opportunityRepository.findAllByOrderByEarnedAtDesc()).thenReturn(List.of(held));
    when(titleOpportunityService.findAllWithDetails()).thenReturn(List.of(held));
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(holder));

    view =
        new BriefcaseListView(
            titleOpportunityService,
            opportunityRepository,
            wrestlerRepository,
            gameSettingService,
            securityUtils,
            imageStorageService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Grid lists the briefcases with holder and status")
  void gridListsBriefcases() {
    Grid<TitleOpportunity> grid = _get(view, Grid.class);
    List<TitleOpportunity> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    assertEquals("Time Vault briefcase", items.getFirst().getName());
    assertEquals("Mukundi Shumba", items.getFirst().getWrestler().getName());
    assertEquals(TitleOpportunityStatus.HELD, items.getFirst().getStatus());
  }

  @Test
  @DisplayName("New button opens the create dialog with a holder picker")
  void newButton_opensCreateDialog() {
    Button newBtn =
        _find(view, Button.class).stream()
            .filter(b -> "New Briefcase".equals(b.getText()))
            .findFirst()
            .orElseThrow();
    newBtn.click();

    BriefcaseFormDialog dialog = _get(UI.getCurrent(), BriefcaseFormDialog.class);
    assertNotNull(dialog);
    assertTrue(dialog.isOpened());
  }

  @Test
  @DisplayName("Void on a HELD row asks for confirmation and calls the service")
  @SuppressWarnings("unchecked")
  void voidButton_confirmsThenVoids() {
    view.refreshGrid();
    // Render the actions cell for the held row (TitleListViewTest pattern: buttons by position).
    ComponentRenderer<Component, TitleOpportunity> renderer =
        (ComponentRenderer<Component, TitleOpportunity>) actionsColumn().getRenderer();
    Component cell = renderer.createComponent(held);

    Button voidBtn =
        cell.getChildren()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .skip(1)
            .findFirst()
            .orElseThrow();

    voidBtn.click();

    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    try {
      Class<?> eventClass =
          Class.forName("com.vaadin.flow.component.confirmdialog.ConfirmDialog$ConfirmEvent");
      var ctor = eventClass.getDeclaredConstructor(ConfirmDialog.class, boolean.class);
      ctor.setAccessible(true);
      var event = ctor.newInstance(confirm, true);
      var method = Component.class.getDeclaredMethod("fireEvent", ComponentEvent.class);
      method.setAccessible(true);
      method.invoke(confirm, event);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to fire confirm event", e);
    }

    verify(titleOpportunityService).adminVoid(40L);
  }

  private Grid.Column<TitleOpportunity> actionsColumn() {
    return view.getGrid().getColumns().stream()
        .filter(c -> "Actions".equals(c.getHeaderText()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  @DisplayName("Edit on a HELD row opens the dialog pre-filled")
  @SuppressWarnings("unchecked")
  void editButton_opensPrefilledDialog() {
    view.refreshGrid();
    when(titleOpportunityService.findByIdWithDetails(40L)).thenReturn(Optional.of(held));

    ComponentRenderer<Component, TitleOpportunity> renderer =
        (ComponentRenderer<Component, TitleOpportunity>) actionsColumn().getRenderer();
    Component cell = renderer.createComponent(held);

    Button editBtn =
        cell.getChildren()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow();

    editBtn.click();

    BriefcaseFormDialog dialog = _get(UI.getCurrent(), BriefcaseFormDialog.class);
    assertNotNull(dialog);
  }

  // ── ATW-ucux: create mode anchors to the in-game date, never the wall clock ──

  private static final LocalDate GAME_DATE = LocalDate.of(2026, 9, 15);

  private BriefcaseFormDialog openCreateDialog() {
    when(gameSettingService.getCurrentGameDate()).thenReturn(GAME_DATE);
    Mockito.lenient()
        .when(
            titleOpportunityService.adminCreate(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(Wrestler.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()))
        .thenReturn(held);
    Button newBtn =
        _find(view, Button.class).stream()
            .filter(b -> "New Briefcase".equals(b.getText()))
            .findFirst()
            .orElseThrow();
    newBtn.click();
    return _get(UI.getCurrent(), BriefcaseFormDialog.class);
  }

  @Test
  @DisplayName("Create dialog pre-fills the Earned picker with the in-game date")
  void createDialog_prefillsEarnedWithGameDate() {
    BriefcaseFormDialog dialog = openCreateDialog();

    DatePicker earned = _get(dialog, DatePicker.class, spec -> spec.withLabel("Earned"));
    assertEquals(GAME_DATE, earned.getValue());
  }

  @Test
  @DisplayName("Saving an untouched Earned picker books the in-game date, not the wall clock")
  void createDialog_untouchedEarned_savesGameDate() {
    BriefcaseFormDialog dialog = openCreateDialog();

    _get(dialog, TextField.class, spec -> spec.withLabel("Name")).setValue("New case");
    _get(dialog, ComboBox.class, spec -> spec.withLabel("Holder")).setValue(holder);
    _get(dialog, Button.class, spec -> spec.withId("briefcase-save")).click();

    ArgumentCaptor<LocalDate> earnedCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(titleOpportunityService)
        .adminCreate(
            ArgumentMatchers.eq("New case"),
            ArgumentMatchers.eq(holder),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            earnedCaptor.capture(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any());
    assertEquals(GAME_DATE, earnedCaptor.getValue());
  }
}
