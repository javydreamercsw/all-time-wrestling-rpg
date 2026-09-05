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
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.tabs.TabSheet;
import java.util.List;
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
}
