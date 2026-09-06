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
package com.github.javydreamercsw.management.ui.view.faction;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class FactionListViewTest extends AbstractViewTest {

  @Mock private FactionService factionService;
  @Mock private WrestlerService wrestlerService;
  @Mock private NpcService npcService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageStorageService imageStorageService;

  private FactionListView view;

  @BeforeEach
  void setup() {
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    when(factionService.findAllByUniverse(anyLong())).thenReturn(Collections.emptyList());
    when(wrestlerService.findAllIncludingInactive()).thenReturn(Collections.emptyList());
    when(npcService.findAllIncludingInactive()).thenReturn(Collections.emptyList());
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);
    when(factionService.resolveFactionImage(any())).thenReturn("");

    view =
        new FactionListView(
            factionService,
            wrestlerService,
            npcService,
            securityUtils,
            universeContextService,
            imageStorageService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the faction grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
    assertFalse(grid.getColumns().isEmpty());
  }

  @Test
  @DisplayName("Grid should show Art and Active columns")
  void gridShouldHaveArtAndActiveColumns() {
    Grid<?> grid = _get(view, Grid.class);
    List<String> headers =
        grid.getColumns().stream()
            .map(Grid.Column::getHeaderText)
            .filter(h -> h != null && !h.isEmpty())
            .collect(Collectors.toList());
    assertTrue(headers.contains("Art"));
    assertTrue(headers.contains("Active"));
  }

  @Test
  @DisplayName("Members dialog Fans column should sort numerically, not lexicographically")
  void membersGridFansColumnShouldSortNumerically() {
    Wrestler lowFansWrestler = Wrestler.builder().id(1L).name("Low Fans").build();
    Wrestler midFansWrestler = Wrestler.builder().id(2L).name("Mid Fans").build();
    Wrestler highFansWrestler = Wrestler.builder().id(3L).name("High Fans").build();

    WrestlerState lowFans = WrestlerState.builder().wrestler(lowFansWrestler).fans(6_720L).build();
    WrestlerState midFans = WrestlerState.builder().wrestler(midFansWrestler).fans(56_100L).build();
    WrestlerState highFans =
        WrestlerState.builder().wrestler(highFansWrestler).fans(57_630L).build();

    Faction faction =
        Faction.builder()
            .id(1L)
            .name("Test Faction")
            .members(Set.of(lowFans, midFans, highFans))
            .build();

    when(factionService.findAllByUniverse(anyLong())).thenReturn(List.of(faction));
    when(factionService.getFactionByIdWithMembers(1L)).thenReturn(Optional.of(faction));

    view =
        new FactionListView(
            factionService,
            wrestlerService,
            npcService,
            securityUtils,
            universeContextService,
            imageStorageService);
    UI.getCurrent().add(view);

    @SuppressWarnings("unchecked")
    Grid<Faction> factionGrid = _get(view, Grid.class);
    Component actionsCell = _getCellComponent(factionGrid, 0, "actions");
    Button membersButton = _get(actionsCell, Button.class, spec -> spec.withId("members-1"));
    _click(membersButton);

    @SuppressWarnings("unchecked")
    Grid<WrestlerState> membersGrid = _get(Grid.class, spec -> spec.withId("members-grid"));
    Grid.Column<WrestlerState> fansColumn =
        membersGrid.getColumns().stream()
            .filter(col -> "Fans".equals(col.getHeaderText()))
            .findFirst()
            .orElseThrow();

    membersGrid.sort(GridSortOrder.desc(fansColumn).build());

    List<WrestlerState> items = membersGrid.getListDataView().getItems().toList();
    assertEquals(
        "High Fans", items.get(0).getWrestler().getName(), "Highest fan count should sort first");
    assertEquals(
        "Low Fans",
        items.get(items.size() - 1).getWrestler().getName(),
        "If sorting were lexicographic, '56,100' and '57,630' would order before '6,720'");
  }

  /** Builds one faction with the given id/name and registers the list stub. */
  private Faction faction(final long id, final String name) {
    Faction f = Faction.builder().id(id).name(name).build();
    when(factionService.findAllByUniverse(anyLong())).thenReturn(List.of(f));
    when(factionService.resolveFactionImage(f)).thenReturn("");
    return f;
  }

  @Test
  @DisplayName("Delete button opens a confirm dialog; confirming deletes the faction")
  void deleteButtonConfirmsThenDeletes() {
    Faction f = faction(5L, "Doomed Faction");
    when(factionService.findAllByUniverse(anyLong())).thenReturn(List.of(f));
    view.refreshGridForTest();

    @SuppressWarnings("unchecked")
    Grid<Faction> factionGrid = _get(view, Grid.class);
    Component actionsCell = _getCellComponent(factionGrid, 0, "actions");
    Button deleteButton = _get(actionsCell, Button.class, spec -> spec.withId("delete-5"));
    _click(deleteButton);

    // ConfirmDialog attaches to the UI, not the view.
    ConfirmDialog confirm = _get(ConfirmDialog.class);

    fireConfirm(confirm);
    verify(factionService).deleteById(5L);
  }

  /** Fires the confirm event on a ConfirmDialog (the click path is client-side only). */
  private void fireConfirm(final ConfirmDialog dialog) {
    try {
      Class<?> eventClass = Class.forName(ConfirmDialog.class.getName() + "$ConfirmEvent");
      var ctor = eventClass.getDeclaredConstructor(ConfirmDialog.class, boolean.class);
      ctor.setAccessible(true);
      var event = ctor.newInstance(dialog, true);
      var method =
          Component.class.getDeclaredMethod("fireEvent", new Class<?>[] {ComponentEvent.class});
      method.setAccessible(true);
      method.invoke(dialog, event);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to fire confirm event", e);
    }
  }

  @Test
  @DisplayName("Toggle button flips the active flag through the service")
  void toggleButtonFlipsActive() {
    Faction f = faction(6L, "Switchable");
    when(factionService.findAllByUniverse(anyLong())).thenReturn(List.of(f));
    view.refreshGridForTest();

    @SuppressWarnings("unchecked")
    Grid<Faction> factionGrid = _get(view, Grid.class);
    Component actionsCell = _getCellComponent(factionGrid, 0, "actions");
    Button toggleButton = _get(actionsCell, Button.class, spec -> spec.withId("toggle-6"));
    _click(toggleButton);

    verify(factionService).setActive(6L, false);
  }

  @Test
  @DisplayName("Search term filters the faction grid by name")
  void searchFiltersByName() {
    Faction rey = faction(7L, "Monday Night Rew");
    Faction usos = faction(8L, "Uso Crazy");
    when(factionService.findAllByUniverse(anyLong())).thenReturn(List.of(rey, usos));
    view.refreshGridForTest();

    view.searchForTest("rew");

    @SuppressWarnings("unchecked")
    Grid<Faction> factionGrid = _get(view, Grid.class);
    List<Faction> items = factionGrid.getListDataView().getItems().toList();
    assertEquals(1, items.size(), "Only the name-matching faction should remain");
    assertEquals("Monday Night Rew", items.get(0).getName());
  }
}
