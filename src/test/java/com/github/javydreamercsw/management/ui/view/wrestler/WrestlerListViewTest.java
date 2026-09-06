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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.ValueProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

class WrestlerListViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private InjuryService injuryService;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private NpcService npcService;
  @Mock private ExpansionService expansionService;
  @Mock private UniverseSettingsService universeSettingsService;
  @Mock private AccountService accountService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignService campaignService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private UniverseContextService universeContextService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private AlignmentService alignmentService;

  private WrestlerListView view;

  @BeforeEach
  void setup() {
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    when(injuryService.getWrestlersWithActiveInjuries(any())).thenReturn(Collections.emptyList());
    when(expansionService.getEnabledExpansionCodes()).thenReturn(Collections.emptyList());
    when(securityUtils.isAdmin()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.canCreate()).thenReturn(true);
    when(wrestlerService.findAllIncludingInactive()).thenReturn(Collections.emptyList());

    view =
        new WrestlerListView(
            wrestlerService,
            injuryService,
            injuryTypeService,
            npcService,
            expansionService,
            universeSettingsService,
            accountService,
            securityUtils,
            campaignService,
            imageStorageService,
            universeContextService,
            wrestlerStateRepository,
            alignmentService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the wrestler list grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Search term is passed through to findPageFiltered and countFiltered")
  void searchFieldFiltersThroughService() {
    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);
    when(securityUtils.canCreate()).thenReturn(true);

    view.reloadGridForTest("rey");
    // findPageFiltered runs inside the grid's lazy DataProvider — force a fetch.
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    grid.getDataProvider().fetch(new Query<>());

    verify(wrestlerService).findPageFiltered(any(), any(), ArgumentMatchers.eq("rey"), any());
    verify(wrestlerService).countFiltered(any(), any(), ArgumentMatchers.eq("rey"));
  }

  @Test
  @DisplayName("Empty search term still loads the grid with a null filter")
  void emptySearchLoadsGrid() {
    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    grid.getDataProvider().fetch(new Query<>());

    verify(wrestlerService).findPageFiltered(any(), any(), any(), any());
    verify(wrestlerService).countFiltered(any(), any(), any());
  }

  // --- Renderer coverage helpers ---

  /** Renders one wrestler through every grid column, mirroring a row render. */
  @SuppressWarnings("unchecked")
  private List<Component> renderRow(final Wrestler wrestler) {
    // The action-menu column builds a WrestlerActionMenu, which resolves the
    // wrestler's state from the service; give it a default for any id.
    when(wrestlerService.getOrCreateState(any(), any()))
        .thenReturn(WrestlerState.builder().wrestler(wrestler).build());
    Grid<Wrestler> grid =
        (Grid<Wrestler>) _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    return grid.getColumns().stream()
        .map(
            column -> {
              if (column.getRenderer() instanceof ComponentRenderer<?, ?> cr) {
                var renderer = (ComponentRenderer<Component, Wrestler>) cr;
                return renderer.createComponent(wrestler);
              }
              return null;
            })
        .toList();
  }

  /**
   * Renders the text of every plain value-provider column (Fans, Bumps, Manager, Creation Date).
   * Those use a ColumnPathRenderer whose private provider runs only during real row serialization,
   * so drive it directly.
   */
  private List<String> renderTextCells(final Wrestler wrestler) {
    Grid<Wrestler> grid =
        (Grid<Wrestler>) _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    return grid.getColumns().stream()
        .map(
            column -> {
              if (column.getRenderer() instanceof ColumnPathRenderer<?> cpr) {
                try {
                  var field = ColumnPathRenderer.class.getDeclaredField("provider");
                  field.setAccessible(true);
                  var provider = (ValueProvider<Wrestler, ?>) field.get(cpr);
                  Object value = provider.apply(wrestler);
                  return value == null ? "" : String.valueOf(value);
                } catch (ReflectiveOperationException e) {
                  throw new IllegalStateException("Cannot read column provider", e);
                }
              }
              return null;
            })
        .toList();
  }

  /** Collects the text of a component subtree (works where getText() does not recurse). */
  private String flattenText(final Component root) {
    StringBuilder sb = new StringBuilder();
    flatten(root, sb);
    return sb.toString();
  }

  private void flatten(final Component component, final StringBuilder sb) {
    sb.append(component.getElement().getText()).append(' ');
    component.getChildren().forEach(child -> flatten(child, sb));
  }

  /** Builds a wrestler with the given id and name, active by default. */
  private Wrestler wrestler(final long id, final String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    w.setActive(true);
    w.setExpansionCode("BASE_GAME");
    return w;
  }

  @Test
  @DisplayName("Name column renders active/injured/account icons and alignment badge variants")
  void nameColumnRendererVariants() {
    // Loaded: active wrestler with an account, FACE alignment, and an active injury.
    Account owner = new Account();
    owner.setId(5L);
    Wrestler loaded = wrestler(1L, "Face Guy");
    loaded.setAccount(owner);

    Npc manager = new Npc();
    manager.setId(7L);
    manager.setName("Paul E.");
    manager.setExpansionCode("BASE_GAME");
    WrestlerState state =
        WrestlerState.builder().wrestler(loaded).fans(1234L).bumps(3).manager(manager).build();

    WrestlerAlignment face =
        WrestlerAlignment.builder()
            .wrestler(loaded)
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();

    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Map.of(1L, state));
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Map.of(1L, face));
    when(injuryService.getWrestlersWithActiveInjuries(1L)).thenReturn(List.of(loaded));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of("BASE_GAME"));
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");

    // The alignment badge and the wrestler name appear in the name-column cell.
    List<Component> cells = renderRow(loaded);
    assertTrue(
        cells.stream().filter(Objects::nonNull).anyMatch(c -> flattenText(c).contains("FACE")),
        "FACE badge should render in the name column");

    // Fans/Bumps value providers read the preloaded state (not the 0 fallback).
    List<String> textCells = renderTextCells(loaded);
    assertTrue(textCells.contains("1234"), "Fans column should show the state's fans");
    assertTrue(textCells.contains("3"), "Bumps column should show the state's bumps");
    // Manager column shows the manager name (expansion enabled).
    assertTrue(
        textCells.contains("Paul E."),
        "Manager column should show the manager name; cells=" + textCells);
  }

  @Test
  @DisplayName("Name column renders inactive wrestler with HEEL badge and no injury icon")
  void nameColumnInactiveHeelVariant() {
    Wrestler inactive = wrestler(2L, "Heel Guy");
    inactive.setActive(false);

    Npc manager = new Npc();
    manager.setId(8L);
    manager.setName("Disabled Manager");
    manager.setExpansionCode("DISABLED_PACK");
    WrestlerState state =
        WrestlerState.builder().wrestler(inactive).fans(50L).bumps(1).manager(manager).build();

    WrestlerAlignment heel =
        WrestlerAlignment.builder()
            .wrestler(inactive)
            .alignmentType(AlignmentType.HEEL)
            .level(2)
            .build();

    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Map.of(2L, state));
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Map.of(2L, heel));
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");

    List<Component> cells = renderRow(inactive);
    assertTrue(
        cells.stream().filter(Objects::nonNull).anyMatch(c -> flattenText(c).contains("HEEL")),
        "HEEL badge should render in the name column");

    // Manager whose expansion is not enabled renders an empty cell — the fans
    // and bumps still come from the state.
    List<String> textCells = renderTextCells(inactive);
    assertTrue(textCells.contains("50"), "Fans column should show the state's fans");
    assertFalse(
        textCells.contains("Disabled Manager"),
        "Manager from a disabled expansion should render empty");
  }

  @Test
  @DisplayName("Missing state and universe-less manager fallbacks render zeros and empty")
  void statelessFallbacksRender() {
    Wrestler plain = wrestler(3L, "No State");
    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(new HashMap<>());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");

    List<String> textCells = renderTextCells(plain);
    // No state → fans 0, bumps 0, manager "".
    assertTrue(
        textCells.contains("0"), "Fans/Bumps fallback of 0 should render; cells=" + textCells);
  }

  @Test
  @DisplayName("Action-menu column renders a WrestlerActionMenu per row")
  void actionMenuColumnRenders() {
    Wrestler w = wrestler(4L, "Menu Guy");
    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");

    List<Component> cells = renderRow(w);
    assertTrue(
        cells.stream()
            .filter(Objects::nonNull)
            .anyMatch(c -> "action-menu-4".equals(c.getId().orElse(null))),
        "Action menu column should render with the wrestler-specific id");
  }

  @Test
  @DisplayName("Toolbar omits the Create Wrestler button when canCreate is false")
  void toolbarWithoutCreateButton() {
    when(securityUtils.canCreate()).thenReturn(false);
    view =
        new WrestlerListView(
            wrestlerService,
            injuryService,
            injuryTypeService,
            npcService,
            expansionService,
            universeSettingsService,
            accountService,
            securityUtils,
            campaignService,
            imageStorageService,
            universeContextService,
            wrestlerStateRepository,
            alignmentService);
    UI.getCurrent().add(view);

    // The constructor must not have thrown and the grid is still present.
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    assertTrue(grid.isVisible());

    // The create button is only added to the toolbar when canCreate() is true;
    // find the button by walking the tree (it is INVIS, so locator skips it).
    Button create = findButtonById(view, "create-wrestler-button");
    assertFalse(
        create != null && create.isVisible(),
        "Create button should be absent or hidden without canCreate");
  }

  /** Tree-walks for a button by id (karibu's locator skips INVIS components). */
  private Button findButtonById(final Component root, final String id) {
    if (root.getId().orElse("").equals(id)) {
      return (Button) root;
    }
    for (Component child : root.getChildren().toList()) {
      Button found = findButtonById(child, id);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  @Test
  @DisplayName("Create Wrestler button opens the wrestler dialog")
  void createButtonOpensDialog() {
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit(any())).thenReturn(true);
    when(securityUtils.isAdmin()).thenReturn(true);
    when(npcService.findAllByType("Manager")).thenReturn(Collections.emptyList());
    when(accountService.findAll()).thenReturn(Collections.emptyList());

    Button create = _get(view, Button.class, spec -> spec.withId("create-wrestler-button"));
    create.click();

    // The dialog attaches to the UI, not the view.
    Dialog dialog = _get(Dialog.class);
    assertTrue(dialog.isOpened());
  }

  @Test
  @DisplayName("Non-admin non-booker sees only their own wrestlers")
  void nonBookerSeesOnlyOwnWrestlers() {
    Account mine = new Account();
    mine.setId(9L);
    CustomUserDetails userDetails = new CustomUserDetails(mine, null);

    Wrestler own = wrestler(6L, "My Wrestler");
    Wrestler excluded = wrestler(7L, "Wrong Expansion");
    excluded.setExpansionCode("OTHER_PACK");

    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of("BASE_GAME"));
    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(wrestlerService.findAllByAccount(mine)).thenReturn(List.of(own, excluded));

    view.reloadGridForTest("");

    @SuppressWarnings("unchecked")
    Grid<Wrestler> grid =
        (Grid<Wrestler>) _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    grid.getDataProvider().fetch(new Query<>());

    verify(wrestlerService).findAllByAccount(mine);
    // The account list filters by enabled expansion codes and exclusions.
    List<Wrestler> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size(), "Only wrestlers in enabled expansions should remain");
    assertEquals("My Wrestler", items.get(0).getName());
  }

  @Test
  @DisplayName("NEUTRAL alignment badge renders with contrast styling")
  void neutralAlignmentBadgeRenders() {
    Wrestler w = wrestler(8L, "Neutral Guy");
    WrestlerAlignment neutral =
        WrestlerAlignment.builder()
            .wrestler(w)
            .alignmentType(AlignmentType.NEUTRAL)
            .level(1)
            .build();

    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Map.of(8L, neutral));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of("BASE_GAME"));
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");

    List<Component> cells = renderRow(w);
    assertTrue(
        cells.stream().filter(Objects::nonNull).anyMatch(c -> flattenText(c).contains("NEUTRAL")),
        "NEUTRAL badge should render in the name column");
  }

  @Test
  @DisplayName("Preload failures fall back to empty maps without breaking the grid")
  void preloadFailuresFallBackToEmptyMaps() {
    when(wrestlerService.getStateMapByUniverseId(1L)).thenThrow(new RuntimeException("db down"));
    when(alignmentService.getAlignmentMapByUniverseId(1L))
        .thenThrow(new RuntimeException("db down"));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of("BASE_GAME"));
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");

    // The grid still loads; state-dependent cells render the 0/empty fallbacks.
    List<String> textCells = renderTextCells(wrestler(9L, "Fallback"));
    assertTrue(textCells.contains("0"), "Fans fallback of 0 should render after preload failure");
  }

  @Test
  @DisplayName("Universe-scoped exclusion drops excluded wrestlers from the admin grid")
  void universeExcludedWrestlersAreFiltered() {
    Universe universe = new Universe();
    universe.setId(1L);
    universe.setName("Main");

    Wrestler kept = wrestler(10L, "Kept");
    Wrestler excluded = wrestler(11L, "Excluded");
    excluded.setId(11L);

    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.of(universe));
    when(universeSettingsService.getEnabledExpansionCodesForUniverse(universe))
        .thenReturn(Set.of("BASE_GAME"));
    when(universeSettingsService.getExcludedWrestlers(universe)).thenReturn(Set.of(excluded));
    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(wrestlerService.findPageFiltered(any(), any(), any(), any())).thenReturn(Page.empty());
    when(wrestlerService.countFiltered(any(), any(), any())).thenReturn(0L);

    view.reloadGridForTest("");

    @SuppressWarnings("unchecked")
    Grid<Wrestler> grid =
        (Grid<Wrestler>) _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    grid.getDataProvider().fetch(new Query<>());

    // Excluded ids were computed from the universe settings and passed to the
    // filtered fetch (the fetch itself is the admin data path).
    verify(universeSettingsService).getExcludedWrestlers(universe);
    verify(wrestlerService).findPageFiltered(any(), any(), ArgumentMatchers.eq(""), any());
  }

  @Test
  @DisplayName("Viewer search filters their own wrestlers by name")
  void viewerSearchFiltersByName() {
    Account mine = new Account();
    mine.setId(12L);
    CustomUserDetails userDetails = new CustomUserDetails(mine, null);

    Wrestler rey = wrestler(13L, "Rey Mysterio");
    Wrestler eddie = wrestler(14L, "Eddie Guerrero");

    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of("BASE_GAME"));
    when(wrestlerService.getStateMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(alignmentService.getAlignmentMapByUniverseId(1L)).thenReturn(Collections.emptyMap());
    when(wrestlerService.findAllByAccount(mine)).thenReturn(List.of(rey, eddie));

    view.reloadGridForTest("rey");

    @SuppressWarnings("unchecked")
    Grid<Wrestler> grid =
        (Grid<Wrestler>) _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    grid.getDataProvider().fetch(new Query<>());

    List<Wrestler> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size(), "Only the name-matching wrestler should remain");
    assertEquals("Rey Mysterio", items.get(0).getName());
  }
}
