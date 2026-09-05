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
package com.github.javydreamercsw.management.ui.view.season;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Exercises {@link SeasonListView}'s CRUD paths: grid population with and without a search term,
 * the edit dialog binder round-trip (create vs update), delete confirmation, and the
 * error-notification paths for failed loads.
 */
class SeasonListViewCrudTest extends AbstractViewTest {

  @Mock private SeasonService seasonService;
  @Mock private SecurityUtils securityUtils;
  @Mock private NotificationService notificationService;

  private SeasonListView view;

  @BeforeEach
  void setup() {
    when(seasonService.getAllSeasons(any(Pageable.class))).thenReturn(Page.empty());
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);
    view = new SeasonListView(seasonService, securityUtils, notificationService);
    UI.getCurrent().add(view);
  }

  private Season season(String name) {
    Season s = new Season();
    s.setId(1L);
    s.setName(name);
    s.setDescription("Test season");
    s.setShowsPerPpv(5);
    s.setIsActive(true);
    return s;
  }

  @Test
  @DisplayName("Grid populates from service results")
  void gridPopulates() {
    when(seasonService.getAllSeasons(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(season("Season 1"), season("Season 2"))));

    view.updateGridForTest();

    Grid<?> grid = _get(view, Grid.class);
    @SuppressWarnings("unchecked")
    List<Season> items = (List<Season>) ((Grid<Season>) grid).getListDataView().getItems().toList();
    assertEquals(2, items.size());
    assertEquals("Season 1", items.get(0).getName());
  }

  @Test
  @DisplayName("Search term routes through searchSeasons")
  void searchUsesSearchEndpoint() {
    when(seasonService.searchSeasons(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(season("Spring"))));

    TextField search = _get(view, TextField.class);
    search.setValue("Spring");
    view.updateGridForTest(); // LAZY value-change mode: trigger the load explicitly

    verify(seasonService, Mockito.atLeastOnce()).searchSeasons("Spring", Pageable.unpaged());
  }

  @Test
  @DisplayName("Grid load failure shows an error notification")
  void loadFailureShowsError() {
    when(seasonService.getAllSeasons(any(Pageable.class)))
        .thenThrow(new RuntimeException("DB down"));

    view.updateGridForTest();

    verify(notificationService).showError(any());
  }

  @Test
  @DisplayName("Edit dialog opens for a new season and Save calls createSeason")
  void createSeasonViaDialog() {
    // The view sets start/end/active on the returned season; a null would NPE.
    when(seasonService.createSeason(any(), any(), any()))
        .thenAnswer(
            inv -> {
              Season created = new Season();
              created.setId(2L);
              created.setName(inv.getArgument(0));
              return created;
            });
    // No rows: dialog opens in create mode.
    view.openEditDialogForTest(null);

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened());

    TextField name = _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Name"));
    name.setValue("Brand New Season");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(seasonService).createSeason(any(), any(), any());
    assertFalse(dialog.isOpened(), "Dialog should close after save");
  }

  @Test
  @DisplayName("Edit dialog with an existing season updates it")
  void updateSeasonViaDialog() {
    Season existing = season("Season One");
    view.openEditDialogForTest(existing);

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened());

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(seasonService).updateSeason(existing);
    assertFalse(dialog.isOpened(), "Dialog should close after update");
  }

  @Test
  @DisplayName("Save with a blank name shows the validation error")
  void saveWithBlankNameShowsError() {
    view.openEditDialogForTest(null);

    TextField name = _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Name"));
    name.setValue("");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(notificationService).showError(any());
  }

  @Test
  @DisplayName("Delete flow confirms and calls deleteSeason")
  void deleteSeasonConfirmed() {
    // deleteSeason is private; drive it through the grid's confirm by invoking
    // the public behavior: the confirm dialog opens, confirming calls the service.
    view.deleteSeasonForTest(season("Doomed"));

    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    assertTrue(confirm.isOpened(), "Confirm dialog should open");
    // ConfirmDialog's buttons live in shadow DOM; fire the confirm event via a
    // same-package subclass so the protected fireEvent is reachable.
    fireConfirm(confirm);

    verify(seasonService).deleteSeason(1L);
  }

  @Test
  @DisplayName("Delete failure shows an error notification")
  void deleteFailureShowsError() {
    when(seasonService.deleteSeason(1L)).thenThrow(new RuntimeException("FK violation"));

    view.deleteSeasonForTest(season("Doomed"));
    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    fireConfirm(confirm);

    verify(notificationService).showError(any());
  }

  @Test
  @DisplayName("Cancel closes the edit dialog without saving")
  void cancelClosesWithoutSaving() {
    view.openEditDialogForTest(null);
    Dialog dialog = _get(UI.getCurrent(), Dialog.class);

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Cancel")).click();

    assertFalse(dialog.isOpened());
    verify(seasonService, Mockito.never()).createSeason(any(), any(), any());
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
