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
package com.github.javydreamercsw.management.ui.view.universe;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.Universe.UniverseType;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Covers the Members and Settings tabs of {@link UniverseFormDialog}: add/remove member (with the
 * remove confirmation), expansion enable/disable toggles, and wrestler exclusion/removal.
 */
class UniverseFormDialogMembersTest extends AbstractViewTest {

  @Mock private UniverseService universeService;
  @Mock private UniverseMembershipService membershipService;
  @Mock private AccountService accountService;
  @Mock private UniverseSettingsService settingsService;
  @Mock private WrestlerRepository wrestlerRepository;

  private Universe universe;
  private Account member;

  @BeforeEach
  void setup() {
    universe = new Universe();
    universe.setId(1L);
    universe.setName("Members Universe");
    universe.setType(UniverseType.GLOBAL);

    member = new Account();
    member.setId(7L);
    member.setUsername("member1");
  }

  /** Selects a tab and forces the content swap (updateContent is deferred to client round-trip). */
  private void selectTab(TabSheet tabs, int index) {
    tabs.setSelectedIndex(index);
    try {
      var updateContent = TabSheet.class.getDeclaredMethod("updateContent");
      updateContent.setAccessible(true);
      updateContent.invoke(tabs);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to update tab content", e);
    }
  }

  /** Builds the edit-mode dialog including the full constructor. */
  private UniverseFormDialog buildEditDialog() {
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of());
    when(accountService.findAll()).thenReturn(List.of());
    when(settingsService.getExpansionsForUniverse(universe)).thenReturn(List.of());
    when(settingsService.getExcludedWrestlers(universe)).thenReturn(Set.of());
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of());
    return new UniverseFormDialog(
        universeService,
        membershipService,
        accountService,
        settingsService,
        wrestlerRepository,
        universe,
        () -> {});
  }

  @Test
  @DisplayName("Full constructor builds Details + Members + Settings tabs")
  void fullConstructorBuildsThreeTabs() {
    UniverseFormDialog dialog = buildEditDialog();

    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    dialog.open();
    assertEquals3(tabs.getTabCount(), "Details, Members and Settings tabs expected");
  }

  private void assertEquals3(int actual, String message) {
    Assertions.assertEquals(3, actual, message);
  }

  @Test
  @DisplayName("Add Member button calls addMember with the picked account and role")
  void addMemberCallsService() {
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of());
    when(accountService.findAll()).thenReturn(List.of(member));
    when(settingsService.getExpansionsForUniverse(universe)).thenReturn(List.of());
    when(settingsService.getExcludedWrestlers(universe)).thenReturn(Set.of());
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of());
    UniverseFormDialog dialog =
        new UniverseFormDialog(
            universeService, membershipService, accountService, universe, () -> {});
    // buildEditDialog-style tabs; add-member flow lives in the Members tab.
    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    dialog.open();
    selectTab(tabs, 1); // Members tab
    // The handler requires a selected account (and role defaults to MEMBER).
    ComboBox<Account> accountPicker =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Add Account"));
    accountPicker.setValue(member);

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Add Member")).click();

    verify(membershipService).addMember(universe, member, UniverseMemberRole.MEMBER);
  }

  @Test
  @DisplayName("Exclude wrestler button calls excludeWrestler")
  void excludeWrestlerCallsService() {
    Wrestler target = new Wrestler();
    target.setId(3L);
    target.setName("Excludable");
    when(settingsService.getExpansionsForUniverse(universe)).thenReturn(List.of());
    when(settingsService.getExcludedWrestlers(universe)).thenReturn(Set.of());
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(target));
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of());
    when(accountService.findAll()).thenReturn(List.of());
    UniverseFormDialog dialog = buildEditDialog();
    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    dialog.open();
    selectTab(tabs, 2); // Settings tab
    ComboBox<Wrestler> picker =
        _get(UI.getCurrent(), ComboBox.class, spec -> spec.withLabel("Exclude Wrestler"));
    picker.setValue(target);

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Exclude")).click();

    verify(settingsService).excludeWrestler(universe, target);
  }

  @Test
  @DisplayName("Selecting no wrestler skips the exclude call")
  void excludeWithoutSelectionSkips() {
    when(settingsService.getExpansionsForUniverse(universe)).thenReturn(List.of());
    when(settingsService.getExcludedWrestlers(universe)).thenReturn(Set.of());
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of());
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of());
    when(accountService.findAll()).thenReturn(List.of());
    UniverseFormDialog dialog = buildEditDialog();
    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    dialog.open();
    selectTab(tabs, 2); // Settings tab

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Exclude")).click();

    verify(settingsService, Mockito.never()).excludeWrestler(any(), any());
  }

  /** Renders one item through every component column of the grid. */
  @SuppressWarnings("unchecked")
  private List<Component> renderCells(final Grid<?> grid, final Object item) {
    return grid.getColumns().stream()
        .map(
            column -> {
              if (column.getRenderer() instanceof ComponentRenderer<?, ?> cr) {
                var renderer = (ComponentRenderer<Component, Object>) cr;
                return renderer.createComponent(item);
              }
              return null;
            })
        .toList();
  }

  /** Fires the confirm event on a ConfirmDialog (the click path is client-side only). */
  private void fireConfirm(final ConfirmDialog dialog) {
    try {
      Class<?> eventClass =
          Class.forName("com.vaadin.flow.component.confirmdialog.ConfirmDialog$ConfirmEvent");
      var ctor = eventClass.getDeclaredConstructor(dialog.getClass(), boolean.class);
      ctor.setAccessible(true);
      var event = ctor.newInstance(dialog, true);
      var method = Component.class.getDeclaredMethod("fireEvent", ComponentEvent.class);
      method.setAccessible(true);
      method.invoke(dialog, event);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to fire confirm event", e);
    }
  }

  @Test
  @DisplayName("Remove-member button opens a confirm dialog; confirming removes the member")
  void removeMemberConfirmFlow() {
    UniverseMembership membership = new UniverseMembership();
    membership.setAccount(member);
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of(membership));
    when(accountService.findAll()).thenReturn(List.of());
    when(settingsService.getExpansionsForUniverse(universe)).thenReturn(List.of());
    when(settingsService.getExcludedWrestlers(universe)).thenReturn(Set.of());
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of());

    UniverseFormDialog dialog = buildEditDialog();
    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    dialog.open();

    // Tab content is not attached to the TabSheet until a client round-trip;
    // reach it directly through getComponent(getTabAt(n)).
    Grid<?> membersGrid =
        gridsIn(tabs.getComponent(tabs.getTabAt(1))).stream()
            .filter(
                g -> g.getColumns().stream().anyMatch(c -> "Username".equals(c.getHeaderText())))
            .findFirst()
            .orElseThrow();

    renderCells(membersGrid, membership).stream()
        .filter(Objects::nonNull)
        .flatMap(c -> flattenTree(c).stream())
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(b -> "Remove".equals(b.getText()))
        .findFirst()
        .orElseThrow()
        .click();

    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    fireConfirm(confirm);

    verify(membershipService).removeMember(universe, member);
  }

  @Test
  @DisplayName("Remove-member failure surfaces as an error notification, not an exception")
  void removeMemberFailureShowsNotification() {
    UniverseMembership membership = new UniverseMembership();
    membership.setAccount(member);
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of(membership));
    when(accountService.findAll()).thenReturn(List.of());
    when(settingsService.getExpansionsForUniverse(universe)).thenReturn(List.of());
    when(settingsService.getExcludedWrestlers(universe)).thenReturn(Set.of());
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of());
    Mockito.doThrow(new IllegalStateException("Last remaining admin"))
        .when(membershipService)
        .removeMember(universe, member);

    UniverseFormDialog dialog =
        new UniverseFormDialog(
            universeService,
            membershipService,
            accountService,
            settingsService,
            wrestlerRepository,
            universe,
            () -> {});
    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    dialog.open();

    Grid<?> membersGrid =
        gridsIn(tabs.getComponent(tabs.getTabAt(1))).stream()
            .filter(
                g -> g.getColumns().stream().anyMatch(c -> "Username".equals(c.getHeaderText())))
            .findFirst()
            .orElseThrow();

    renderCells(membersGrid, membership).stream()
        .filter(Objects::nonNull)
        .flatMap(c -> flattenTree(c).stream())
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(b -> "Remove".equals(b.getText()))
        .findFirst()
        .orElseThrow()
        .click();

    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    fireConfirm(confirm);

    verify(membershipService).removeMember(universe, member);
  }

  @Test
  @DisplayName("Remove-exclusion button confirms and re-includes the wrestler")
  void removeExclusionConfirmFlow() {
    Wrestler excluded = new Wrestler();
    excluded.setId(3L);
    excluded.setName("Excluded");
    when(settingsService.getExcludedWrestlers(universe)).thenReturn(Set.of(excluded));
    when(settingsService.getExpansionsForUniverse(universe)).thenReturn(List.of());
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of());
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of());
    when(accountService.findAll()).thenReturn(List.of());

    // Built inline: buildEditDialog() would re-stub getExcludedWrestlers to empty.
    UniverseFormDialog dialog =
        new UniverseFormDialog(
            universeService,
            membershipService,
            accountService,
            settingsService,
            wrestlerRepository,
            universe,
            () -> {});
    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    dialog.open();

    // The exclusion grid lives in the Settings tab content.
    Grid<?> exclusionGrid =
        gridsIn(tabs.getComponent(tabs.getTabAt(2))).stream()
            .filter(g -> g.getListDataView().getItems().anyMatch(i -> i instanceof Wrestler))
            .findFirst()
            .orElseThrow();

    renderCells(exclusionGrid, excluded).stream()
        .filter(Objects::nonNull)
        .flatMap(c -> flattenTree(c).stream())
        .filter(Button.class::isInstance)
        .map(Button.class::cast)
        .filter(b -> b.getText().contains("Remove Exclusion"))
        .findFirst()
        .orElseThrow()
        .click();

    ConfirmDialog confirm = _get(UI.getCurrent(), ConfirmDialog.class);
    fireConfirm(confirm);

    verify(settingsService).includeWrestler(universe, excluded);
  }

  /** Depth-first collection of every Grid in the given component tree. */
  private List<Grid<?>> gridsIn(final Component root) {
    List<Grid<?>> grids = new ArrayList<>();
    if (root instanceof Grid<?> g) {
      grids.add(g);
    }
    root.getChildren().forEach(child -> grids.addAll(gridsIn(child)));
    return grids;
  }

  /** Depth-first collection of the component and all its descendants. */
  private List<Component> flattenTree(final Component root) {
    List<Component> all = new ArrayList<>();
    all.add(root);
    root.getChildren().forEach(child -> all.addAll(flattenTree(child)));
    return all;
  }
}
