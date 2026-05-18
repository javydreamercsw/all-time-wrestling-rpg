/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.SecurityUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MenuServiceTest {

  @Mock private SecurityUtils securityUtils;

  private MenuService menuService;

  @BeforeEach
  void setUp() {
    menuService = new MenuService(securityUtils);
  }

  @Test
  void getMenuItems_asAdmin_returnsAllTopLevelItems() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(true);

    List<MenuItem> items = menuService.getMenuItems();

    assertThat(items).isNotEmpty();
    // Admin should see all top-level items: Dashboards, Booker Dashboard, General Manager,
    // Player Dashboard, Campaign, Multiplayer, Entities, Content Generation, Card Game,
    // Configuration, Help
    assertThat(items.size()).isGreaterThanOrEqualTo(10);
  }

  @Test
  void getMenuItems_asViewer_returnsLimitedItems() {
    // Viewer only has VIEWER role
    when(securityUtils.hasRole(RoleName.VIEWER)).thenReturn(true);
    when(securityUtils.hasRole(RoleName.ADMIN)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.BOOKER)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.PLAYER)).thenReturn(false);

    List<MenuItem> viewerItems = menuService.getMenuItems();

    // Now get admin count for comparison
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(true);
    List<MenuItem> adminItems = menuService.getMenuItems();

    assertThat(viewerItems.size()).isLessThan(adminItems.size());
  }

  @Test
  void getMenuItems_noRole_returnsOnlyPublicItems() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(false);

    List<MenuItem> items = menuService.getMenuItems();

    // Items with no required roles should still appear: Dashboards, Card Game, Help
    assertThat(items).isNotEmpty();
    // Role-restricted items (Configuration, Entities, etc.) should NOT appear
    assertThat(items.stream().map(MenuItem::getTitle))
        .doesNotContain("Configuration", "Entities", "Booker Dashboard", "General Manager");
  }

  @Test
  void getMenuItems_itemsAreSortedAlphabetically() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(true);

    List<MenuItem> items = menuService.getMenuItems();

    List<String> titles = items.stream().map(MenuItem::getTitle).toList();
    List<String> sortedTitles = titles.stream().sorted().toList();
    assertThat(titles).isEqualTo(sortedTitles);
  }

  @Test
  void getMenuItems_adminSeesConfigurationMenu() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(true);

    List<MenuItem> items = menuService.getMenuItems();

    assertThat(items.stream().map(MenuItem::getTitle)).contains("Configuration");
  }

  @Test
  void getMenuItems_adminSeesEntitiesMenu() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(true);

    List<MenuItem> items = menuService.getMenuItems();

    assertThat(items.stream().map(MenuItem::getTitle)).contains("Entities");
  }

  @Test
  void getMenuItems_nonAdminDoesNotSeeConfigurationMenu() {
    when(securityUtils.hasRole(RoleName.ADMIN)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.BOOKER)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.PLAYER)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.VIEWER)).thenReturn(true);

    List<MenuItem> items = menuService.getMenuItems();

    assertThat(items.stream().map(MenuItem::getTitle)).doesNotContain("Configuration");
  }

  @Test
  void getMenuItems_subMenusAreSortedAlphabetically() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(true);

    List<MenuItem> items = menuService.getMenuItems();

    // Find the Entities menu item and verify its children are sorted
    items.stream()
        .filter(item -> "Entities".equals(item.getTitle()))
        .findFirst()
        .ifPresent(
            entitiesItem -> {
              List<String> childTitles =
                  entitiesItem.getChildren().stream().map(MenuItem::getTitle).toList();
              List<String> sortedChildTitles = childTitles.stream().sorted().toList();
              assertThat(childTitles).isEqualTo(sortedChildTitles);
            });
  }

  @Test
  void getMenuItems_playerRoleSeesPlayerDashboard() {
    when(securityUtils.hasRole(RoleName.PLAYER)).thenReturn(true);
    when(securityUtils.hasRole(RoleName.ADMIN)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.BOOKER)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.VIEWER)).thenReturn(false);

    List<MenuItem> items = menuService.getMenuItems();

    assertThat(items.stream().map(MenuItem::getTitle)).contains("Player Dashboard");
  }

  @Test
  void getMenuItems_bookerRoleSeesBookerDashboardAndGeneralManager() {
    when(securityUtils.hasRole(RoleName.BOOKER)).thenReturn(true);
    when(securityUtils.hasRole(RoleName.ADMIN)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.PLAYER)).thenReturn(false);
    when(securityUtils.hasRole(RoleName.VIEWER)).thenReturn(false);

    List<MenuItem> items = menuService.getMenuItems();

    List<String> titles = items.stream().map(MenuItem::getTitle).toList();
    assertThat(titles).contains("Booker Dashboard", "General Manager");
  }

  @Test
  void getMenuItems_helpMenuVisibleToAllUsers() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(false);

    List<MenuItem> items = menuService.getMenuItems();

    assertThat(items.stream().map(MenuItem::getTitle)).contains("Help");
  }

  @Test
  void getMenuItems_helpMenuContainsGameGuide() {
    when(securityUtils.hasRole(any(RoleName.class))).thenReturn(false);

    List<MenuItem> items = menuService.getMenuItems();

    MenuItem helpMenu =
        items.stream()
            .filter(item -> "Help".equals(item.getTitle()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Help menu not found"));

    assertThat(helpMenu.getChildren().stream().map(MenuItem::getTitle)).contains("Game Guide");
  }
}
