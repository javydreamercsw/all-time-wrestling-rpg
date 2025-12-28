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

import static com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import static com.vaadin.flow.theme.lumo.LumoUtility.Display;
import static com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import static com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import static com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import static com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import static com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import static com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import static com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import static com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import static com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import static com.vaadin.flow.theme.lumo.LumoUtility.Width;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;

@Layout
@PermitAll // When security is enabled, allow all authenticated users
public class MainLayout extends AppLayout {

  private MenuService menuService;
  private BuildProperties buildProperties;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private Registration inboxUpdateBroadcasterRegistration;
  private @Nullable SecurityUtils securityUtils;

  /** For testing purposes. */
  public MainLayout() {}

  @Autowired
  public MainLayout(
      MenuService menuService,
      InboxUpdateBroadcaster inboxUpdateBroadcaster,
      Optional<BuildProperties> buildProperties,
      SecurityUtils securityUtils) {
    this.menuService = menuService;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
    this.buildProperties = buildProperties.orElse(null);
    this.securityUtils = securityUtils;
    setPrimarySection(Section.DRAWER);

    SideNav sideNav = createSideNav();
    Div footer = createFooter();
    Div content = new Div(sideNav, footer);
    content.setSizeFull(); // Ensure content takes full size for proper scrolling

    addToDrawer(createHeader(), new Scroller(content));
    addToNavbar(createNavbar());
  }

  private Div createHeader() {
    // TODO Replace with real application logo and name
    Icon appLogo = VaadinIcon.CUBES.create();
    appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

    Span appName = new Span("All Time Wrestling RPG");
    appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

    Div header = new Div(appLogo, appName);
    header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
    return header;
  }

  private Div createFooter() {
    Span versionSpan;
    if (buildProperties != null) { // Needed for tests
      versionSpan = new Span("Version: " + buildProperties.getVersion());
    } else {
      versionSpan = new Span("Version: N/A");
    }
    versionSpan.addClassNames(
        FontSize.XSMALL, TextColor.SECONDARY, Padding.Top.SMALL, Padding.Bottom.SMALL);
    Anchor githubLink =
        new Anchor("https://github.com/javydreamercsw/all-time-wrestling-rpg", "Source Code");
    githubLink.addClassNames(
        FontSize.XSMALL, TextColor.SECONDARY, Padding.Top.SMALL, Padding.Bottom.SMALL);
    Div footer = new Div(versionSpan, githubLink);
    footer.addClassNames(
        Display.FLEX, JustifyContent.CENTER, Width.FULL, FlexDirection.COLUMN, AlignItems.CENTER);
    return footer;
  }

  private SideNav createSideNav() {
    SideNav nav = new SideNav();
    nav.addClassNames(Margin.Horizontal.MEDIUM);
    menuService.getMenuItems().forEach(menuItem -> nav.addItem(createSideNavItem(menuItem)));
    return nav;
  }

  private SideNavItem createSideNavItem(MenuItem menuItem) {
    SideNavItem item = new SideNavItem(menuItem.getTitle());
    item.setPrefixComponent(menuItem.getIcon().create());
    item.setPath(menuItem.getPath());
    if (!menuItem.getChildren().isEmpty()) {
      menuItem.getChildren().forEach(child -> item.addItem(createSideNavItem(child)));
    }
    return item;
  }

  private Div createNavbar() {
    Div navbar = new Div();
    navbar.addClassNames(
        Display.FLEX,
        AlignItems.CENTER,
        JustifyContent.END,
        Padding.Horizontal.MEDIUM,
        Padding.Vertical.SMALL,
        Gap.MEDIUM,
        Width.FULL);

    if (securityUtils != null && securityUtils.isAuthenticated()) {
      String username = securityUtils.getCurrentUsername();

      // User avatar
      Avatar avatar = new Avatar(username);
      avatar.setTooltipEnabled(true);

      // Username label
      Span usernameLabel = new Span(username);
      usernameLabel.addClassNames(FontWeight.SEMIBOLD, FontSize.SMALL);

      // Profile link
      Anchor profileLink = new Anchor("profile", "Profile");

      // Logout button
      Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
      logoutButton.setId("logout-button");
      logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      logoutButton.addClickListener(e -> securityUtils.logout());

      navbar.add(avatar, usernameLabel, profileLink, logoutButton);
    }

    return navbar;
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    UI ui = attachEvent.getUI();
    if (inboxUpdateBroadcaster != null) { // Needed for tests
      inboxUpdateBroadcasterRegistration =
          inboxUpdateBroadcaster.register(
              event -> {
                if (ui.isAttached()) {
                  ui.access(
                      () -> {
                        Notification.show("New inbox item!", 3000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                      });
                }
              });
    }
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    if (inboxUpdateBroadcasterRegistration != null) { // Needed for tests
      inboxUpdateBroadcasterRegistration.remove();
    }
  }
}
