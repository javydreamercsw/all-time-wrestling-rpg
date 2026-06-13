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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {

  private final SecurityUtils securityUtils;
  private final RouteRoleResolver routeRoleResolver;

  public List<MenuItem> getMenuItems() {
    List<MenuItem> menuItems = new ArrayList<>();

    // Manually define the menu structure with role requirements
    MenuItem dashboards = new MenuItem("Dashboards", VaadinIcon.DASHBOARD, null);
    dashboards.addChild(new MenuItem("Inbox", VaadinIcon.INBOX, "inbox"));
    dashboards.addChild(new MenuItem("Show Calendar", VaadinIcon.CALENDAR, "show-calendar"));
    dashboards.addChild(new MenuItem("Wrestler Rankings", VaadinIcon.STAR, "wrestler-rankings"));
    dashboards.addChild(
        new MenuItem("Championship Rankings", VaadinIcon.TROPHY, "championship-rankings"));
    dashboards.addChild(new MenuItem("News & Rumors", VaadinIcon.NEWSPAPER, "news"));
    dashboards.addChild(new MenuItem("Wrestling World Feed", VaadinIcon.CHAT, "news/feed"));
    dashboards.addChild(new MenuItem("Hall of Fame", VaadinIcon.ACADEMY_CAP, "hall-of-fame"));

    // Booker Dashboard: Only BOOKER and ADMIN
    MenuItem bookerDashboard =
        new MenuItem(
            "Booker Dashboard", VaadinIcon.NOTEBOOK, "booker", RoleName.ADMIN, RoleName.BOOKER);

    // General Manager: Only BOOKER and ADMIN
    MenuItem gmMenu =
        new MenuItem(
            "General Manager",
            VaadinIcon.OFFICE,
            null,
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER);
    gmMenu.addChild(new MenuItem("GM Dashboard", VaadinIcon.DASHBOARD, "gm-dashboard"));
    gmMenu.addChild(new MenuItem("Contract Management", VaadinIcon.CLIPBOARD_CHECK, "contracts"));

    // Player Dashboard: Only PLAYER, BOOKER, and ADMIN
    MenuItem playerDashboard =
        new MenuItem(
            "Player Dashboard",
            VaadinIcon.USER_CARD,
            "player",
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER);

    // Campaign: Dashboard open to ADMIN/BOOKER/PLAYER; admin-only sub-views restricted at route
    // level
    MenuItem campaignMenu =
        new MenuItem(
            "Campaign", VaadinIcon.GAMEPAD, null, RoleName.ADMIN, RoleName.BOOKER, RoleName.PLAYER);
    campaignMenu.addChild(
        new MenuItem("Campaigns", VaadinIcon.FILM, "campaign-list", RoleName.ADMIN));
    campaignMenu.addChild(
        new MenuItem(
            "Dashboard",
            VaadinIcon.DASHBOARD,
            "campaign",
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER));

    // Entities menu: Only ADMIN can access
    // BOOKER, PLAYER, and VIEWER have their own dedicated views
    MenuItem entities = new MenuItem("Entities", VaadinIcon.DATABASE, null, RoleName.ADMIN);
    entities.addChild(new MenuItem("Arenas", VaadinIcon.BUILDING, "arena-list"));
    entities.addChild(new MenuItem("Locations", VaadinIcon.GLOBE, "location-list"));
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
    entities.addChild(new MenuItem("Universes", VaadinIcon.GLOBE_WIRE, "universe-list"));
    entities.addChild(new MenuItem("Wrestlers", VaadinIcon.USER, "wrestler-list"));

    // Content Generation: Only ADMIN and BOOKER
    MenuItem contentGeneration =
        new MenuItem(
            "Content Generation", VaadinIcon.AUTOMATION, null, RoleName.ADMIN, RoleName.BOOKER);

    MenuItem cardGame = new MenuItem("Card Game", VaadinIcon.RECORDS, null);
    cardGame.addChild(new MenuItem("Cards", VaadinIcon.CREDIT_CARD, "card-list"));
    cardGame.addChild(
        new MenuItem(
            "Decks",
            VaadinIcon.RECORDS,
            "deck-list",
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER));

    // Configuration: Only ADMIN
    MenuItem configuration = new MenuItem("Configuration", VaadinIcon.COG, null, RoleName.ADMIN);
    configuration.addChild(
        new MenuItem("Sync Dashboard", VaadinIcon.REFRESH, "notion-sync", RoleName.ADMIN));
    configuration.addChild(
        new MenuItem("Data Transfer", VaadinIcon.EXCHANGE, "data-transfer", RoleName.ADMIN));
    configuration.addChild(new MenuItem("Admin", VaadinIcon.TOOLS, "admin", RoleName.ADMIN));

    // Help menu: accessible to everyone
    MenuItem help = new MenuItem("Help", VaadinIcon.QUESTION_CIRCLE, null);
    help.addChild(
        new MenuItem(
            "Game Guide",
            VaadinIcon.BOOK,
            "https://javydreamercsw.github.io/all-time-wrestling-rpg/",
            true));
    help.addChild(
        new MenuItem(
            "Tutorial",
            VaadinIcon.ACADEMY_CAP,
            "tutorial",
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER));

    // Multiplayer menu: Only PLAYER, BOOKER, and ADMIN
    MenuItem multiplayer =
        new MenuItem(
            "Multiplayer",
            VaadinIcon.USERS,
            null,
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER);
    multiplayer.addChild(
        new MenuItem(
            "My Leagues",
            VaadinIcon.LIST,
            "leagues",
            RoleName.ADMIN,
            RoleName.BOOKER,
            RoleName.PLAYER));

    menuItems.add(dashboards);
    menuItems.add(bookerDashboard);
    menuItems.add(gmMenu);
    menuItems.add(playerDashboard);
    menuItems.add(campaignMenu);
    menuItems.add(multiplayer);
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
  private List<MenuItem> filterMenuItems(final List<MenuItem> menuItems) {
    return menuItems.stream()
        .map(this::filterMenuItem)
        .filter(menuItem -> menuItem != null)
        .collect(Collectors.toList());
  }

  /**
   * Filter a single menu item and its children based on user roles.
   *
   * <p>Access is determined in this order:
   *
   * <ol>
   *   <li>If the item has a non-null, non-external path, the {@link RouteRoleResolver} is consulted
   *       first. Its result (derived from {@code @RolesAllowed} / {@code @PermitAll} /
   *       {@code @DenyAll} on the view class) takes precedence over any explicit roles on the
   *       {@link MenuItem}.
   *   <li>If the resolver has no information for the path (view is unannotated or path is unknown),
   *       the item's own {@link MenuItem#getRequiredRoles()} list is used as the fallback.
   *   <li>Items with a {@code null} path (group headers) and external links always use the item's
   *       explicit role list.
   * </ol>
   *
   * <p>Group headers (null path) with no accessible children are hidden to avoid empty menu
   * sections.
   *
   * @return a filtered copy of the item, or {@code null} if the user has no access
   */
  private MenuItem filterMenuItem(final MenuItem menuItem) {
    boolean hasAccess;

    if (menuItem.getPath() != null && !menuItem.isExternal()) {
      // @DenyAll → always hidden
      if (routeRoleResolver.isDeniedAll(menuItem.getPath())) {
        return null;
      }

      Optional<Set<RoleName>> resolved = routeRoleResolver.resolveRoles(menuItem.getPath());
      if (resolved.isPresent()) {
        // View annotation is authoritative: empty set = @PermitAll, non-empty = role check
        Set<RoleName> roles = resolved.get();
        hasAccess = roles.isEmpty() || roles.stream().anyMatch(securityUtils::hasRole);
      } else {
        // No annotation found — fall back to MenuItem's explicit roles
        hasAccess =
            !menuItem.hasRequiredRoles()
                || menuItem.getRequiredRoles().stream().anyMatch(securityUtils::hasRole);
      }
    } else {
      // null path (group header) or external link — use MenuItem's explicit roles
      hasAccess =
          !menuItem.hasRequiredRoles()
              || menuItem.getRequiredRoles().stream().anyMatch(securityUtils::hasRole);
    }

    if (!hasAccess) {
      return null;
    }

    // User has access — now filter children and remove nulls
    List<MenuItem> filteredChildren =
        menuItem.getChildren().stream()
            .map(this::filterMenuItem)
            .filter(child -> child != null)
            .toList();

    // Hide group headers (null path) that have no accessible children
    if (menuItem.getPath() == null && filteredChildren.isEmpty()) {
      return null;
    }

    MenuItem filtered = new MenuItem(menuItem.getTitle(), menuItem.getIcon(), menuItem.getPath());
    filtered.setExternal(menuItem.isExternal());
    filtered.setRequiredRoles(menuItem.getRequiredRoles());
    filteredChildren.forEach(filtered::addChild);
    return filtered;
  }

  private void sortSubMenus(final MenuItem menuItem) {
    if (!menuItem.getChildren().isEmpty()) {
      menuItem.getChildren().sort(Comparator.comparing(MenuItem::getTitle));
      menuItem.getChildren().forEach(this::sortSubMenus);
    }
  }
}
