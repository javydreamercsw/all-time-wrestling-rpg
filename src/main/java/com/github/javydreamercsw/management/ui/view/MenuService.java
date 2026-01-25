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

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {

  private final SecurityUtils securityUtils;

  public List<MenuItem> getMenuItems() {
    List<MenuItem> menuItems = new ArrayList<>();

    // Manually define the menu structure with role requirements
    MenuItem dashboards = new MenuItem("Dashboards", VaadinIcon.DASHBOARD, null);
    dashboards.addChild(new MenuItem("Inbox", VaadinIcon.INBOX, "inbox"));
    dashboards.addChild(new MenuItem("Show Calendar", VaadinIcon.CALENDAR, "show-calendar"));
    dashboards.addChild(new MenuItem("Wrestler Rankings", VaadinIcon.STAR, "wrestler-rankings"));
    dashboards.addChild(
        new MenuItem("Championship Rankings", VaadinIcon.TROPHY, "championship-rankings"));

    // Booker Dashboard: Only BOOKER and ADMIN
    MenuItem bookerDashboard =
        new MenuItem(
            "Booker Dashboard", VaadinIcon.NOTEBOOK, "booker", RoleName.ADMIN, RoleName.BOOKER);

    // Player Dashboard: Only PLAYER, BOOKER, and ADMIN
    MenuItem playerDashboard =
        new MenuItem(
            "Player Dashboard",
            VaadinIcon.USER_CARD,
            "player",
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER);

    // Campaign: Only PLAYER, BOOKER, and ADMIN
    MenuItem campaignMenu =
        new MenuItem(
            "Campaign", VaadinIcon.GAMEPAD, null, RoleName.ADMIN, RoleName.BOOKER, RoleName.PLAYER);
    campaignMenu.addChild(new MenuItem("Dashboard", VaadinIcon.DASHBOARD, "campaign"));

    // Entities menu: Only ADMIN can access
    // BOOKER, PLAYER, and VIEWER have their own dedicated views
    MenuItem entities = new MenuItem("Entities", VaadinIcon.DATABASE, null, RoleName.ADMIN);
    entities.addChild(new MenuItem("Faction Rivalries", VaadinIcon.GROUP, "faction-rivalry-list"));
    entities.addChild(new MenuItem("Factions", VaadinIcon.GROUP, "faction-list"));
    entities.addChild(new MenuItem("Accounts", VaadinIcon.USERS, "account-list"));
    entities.addChild(new MenuItem("Injury Types", VaadinIcon.PLUS_CIRCLE, "injury-types"));
    entities.addChild(new MenuItem("NPCs", VaadinIcon.USERS, "npc-list"));
    entities.addChild(new MenuItem("Rivalries", VaadinIcon.FIRE, "rivalry-list"));
    entities.addChild(new MenuItem("Seasons", VaadinIcon.CALENDAR_CLOCK, "season-list"));
    entities.addChild(new MenuItem("Segment Rules", VaadinIcon.LIST_OL, "segment-rule-list"));
    entities.addChild(new MenuItem("Segment Types", VaadinIcon.PUZZLE_PIECE, "segment-type-list"));
    entities.addChild(
        new MenuItem("Show Templates", VaadinIcon.CLIPBOARD_TEXT, "show-template-list"));
    entities.addChild(new MenuItem("Shows", VaadinIcon.CALENDAR_O, "show-list"));
    entities.addChild(new MenuItem("Teams", VaadinIcon.USERS, "teams"));
    entities.addChild(new MenuItem("Titles", VaadinIcon.TROPHY, "title-list"));
    entities.addChild(new MenuItem("Wrestlers", VaadinIcon.USER, "wrestler-list"));

    // Content Generation: Only ADMIN and BOOKER
    MenuItem contentGeneration =
        new MenuItem(
            "Content Generation", VaadinIcon.AUTOMATION, null, RoleName.ADMIN, RoleName.BOOKER);
    contentGeneration.addChild(
        new MenuItem(
            "Show Planning",
            VaadinIcon.CALENDAR,
            "show-planning",
            RoleName.ADMIN,
            RoleName.BOOKER));

    MenuItem cardGame = new MenuItem("Card Game", VaadinIcon.RECORDS, null);
    cardGame.addChild(new MenuItem("Cards", VaadinIcon.CREDIT_CARD, "card-list"));
    cardGame.addChild(new MenuItem("Decks", VaadinIcon.RECORDS, "deck-list"));

    // Configuration: Only ADMIN
    MenuItem configuration = new MenuItem("Configuration", VaadinIcon.COG, null, RoleName.ADMIN);
    configuration.addChild(
        new MenuItem("Sync Dashboard", VaadinIcon.REFRESH, "notion-sync", RoleName.ADMIN));
    configuration.addChild(
        new MenuItem("Data Transfer", VaadinIcon.EXCHANGE, "data-transfer", RoleName.ADMIN));
    configuration.addChild(new MenuItem("Admin", VaadinIcon.TOOLS, "admin", RoleName.ADMIN));

    // Help menu: accessible to everyone
    MenuItem help = new MenuItem("Help", VaadinIcon.QUESTION_CIRCLE, null);
    help.addChild(new MenuItem("Game Guide", VaadinIcon.BOOK, "docs/index.html", true));

    menuItems.add(dashboards);
    menuItems.add(bookerDashboard);
    menuItems.add(playerDashboard);
    menuItems.add(campaignMenu);
    menuItems.add(entities);
    menuItems.add(contentGeneration);
    menuItems.add(cardGame);
    menuItems.add(configuration);
    menuItems.add(help);

    // Filter menu items based on user roles
    List<MenuItem> filteredMenuItems = filterMenuItems(menuItems);

    // Sort top-level menu items
    filteredMenuItems.sort(Comparator.comparing(MenuItem::getTitle));

    // Recursively sort sub-menus
    filteredMenuItems.forEach(this::sortSubMenus);

    return filteredMenuItems;
  }

  /**
   * Filter menu items based on current user's roles. Removes items the user doesn't have permission
   * to access.
   */
  private List<MenuItem> filterMenuItems(List<MenuItem> menuItems) {
    return menuItems.stream()
        .map(this::filterMenuItem)
        .filter(menuItem -> menuItem != null)
        .collect(Collectors.toList());
  }

  /**
   * Filter a single menu item and its children based on user roles. Returns null if the user
   * doesn't have access and the item has no accessible children.
   */
  private MenuItem filterMenuItem(MenuItem menuItem) {
    // Check if user has required role for this item FIRST
    boolean hasAccess =
        !menuItem.hasRequiredRoles()
            || menuItem.getRequiredRoles().stream().anyMatch(securityUtils::hasRole);

    // If user doesn't have access to this item at all, return null immediately
    if (!hasAccess) {
      return null;
    }

    // User has access - now filter children and remove nulls
    List<MenuItem> filteredChildren =
        menuItem.getChildren().stream()
            .map(this::filterMenuItem)
            .filter(child -> child != null)
            .toList();

    // For parent menus (no path), only show if they have accessible children
    // For leaf items (with path), show if user has access
    boolean shouldShow = false;
    if (menuItem.getPath() == null) {
      // Parent menu - show only if it has accessible children
      shouldShow = !filteredChildren.isEmpty();
    } else {
      // Leaf item - show if user has access (which we already verified)
      shouldShow = true;
    }

    if (shouldShow) {
      MenuItem filtered = new MenuItem(menuItem.getTitle(), menuItem.getIcon(), menuItem.getPath());
      filtered.setExternal(menuItem.isExternal());
      filtered.setRequiredRoles(menuItem.getRequiredRoles());
      filteredChildren.forEach(filtered::addChild);
      return filtered;
    }

    // No accessible children for parent menu
    return null;
  }

  private void sortSubMenus(MenuItem menuItem) {
    if (!menuItem.getChildren().isEmpty()) {
      menuItem.getChildren().sort(Comparator.comparing(MenuItem::getTitle));
      menuItem.getChildren().forEach(this::sortSubMenus);
    }
  }
}
