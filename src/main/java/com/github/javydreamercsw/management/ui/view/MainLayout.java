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
import static com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import static com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import static com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import static com.vaadin.flow.theme.lumo.LumoUtility.TextColor;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.OpenProfileDrawerBroadcaster;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.ui.view.account.ProfileDrawer;
import com.github.javydreamercsw.management.ui.view.tutorial.TutorialStepOverlay;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.PermitAll;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.crypto.password.PasswordEncoder;

@Layout
@NoArgsConstructor
@PermitAll
@AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver {

  private MenuService menuService;
  private BuildProperties buildProperties;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private Registration inboxUpdateBroadcasterRegistration;
  private OpenProfileDrawerBroadcaster openProfileDrawerBroadcaster;
  private Registration openProfileDrawerRegistration;
  private @Nullable SecurityUtils securityUtils;
  private AccountService accountService;
  private PasswordEncoder passwordEncoder;
  private ThemeService themeService;
  private UniverseContextService universeContextService;
  private UniverseRepository universeRepository;
  private UniverseMembershipService universeMembershipService;
  private InboxService inboxService;
  private Span inboxBadge;
  private TutorialService tutorialService;
  private TutorialStepOverlay tutorialOverlay;

  @Autowired
  public MainLayout(
      final MenuService menuService,
      final InboxUpdateBroadcaster inboxUpdateBroadcaster,
      final Optional<BuildProperties> buildProperties,
      final SecurityUtils securityUtils,
      final AccountService accountService,
      final PasswordEncoder passwordEncoder,
      final ThemeService themeService,
      final UniverseContextService universeContextService,
      final UniverseRepository universeRepository,
      final UniverseMembershipService universeMembershipService,
      final InboxService inboxService,
      final TutorialService tutorialService,
      final OpenProfileDrawerBroadcaster openProfileDrawerBroadcaster) {
    this.menuService = menuService;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
    this.buildProperties = buildProperties.orElse(null);
    this.securityUtils = securityUtils;
    this.accountService = accountService;
    this.passwordEncoder = passwordEncoder;
    this.themeService = themeService;
    this.universeContextService = universeContextService;
    this.universeRepository = universeRepository;
    this.universeMembershipService = universeMembershipService;
    this.inboxService = inboxService;
    this.tutorialService = tutorialService;
    this.openProfileDrawerBroadcaster = openProfileDrawerBroadcaster;
    setPrimarySection(Section.DRAWER);

    SideNav sideNav = createSideNav();
    Scroller navScroller = new Scroller(sideNav);
    navScroller.addClassNames(Margin.Bottom.AUTO);

    com.vaadin.flow.component.html.Section drawerContainer =
        new com.vaadin.flow.component.html.Section(
            createHeader(), createUniverseSelector(), navScroller, createFooter());
    drawerContainer.setSizeFull();
    drawerContainer.addClassNames(Display.FLEX, FlexDirection.COLUMN, AlignItems.STRETCH);

    addToDrawer(drawerContainer);
    addToNavbar(new DrawerToggle(), createNavbar());
  }

  private Div createUniverseSelector() {
    List<Universe> accessible = resolveAccessibleUniverses();

    ComboBox<Universe> universeSelector = new ComboBox<>("Active Universe");
    universeSelector.setItemLabelGenerator(Universe::getName);
    universeSelector.setWidthFull();
    universeSelector.addClassNames(Padding.Horizontal.MEDIUM, Padding.Bottom.SMALL);

    if (accessible.isEmpty()) {
      universeSelector.setPlaceholder("No universes assigned");
      universeSelector.setEnabled(false);
    } else {
      universeSelector.setItems(accessible);

      // If the current universe is not in the accessible list, switch to the first available.
      Universe current = universeContextService.getCurrentUniverse().orElse(null);
      if (current != null && accessible.contains(current)) {
        universeSelector.setValue(current);
      } else {
        Universe fallback = accessible.get(0);
        universeContextService.setCurrentUniverse(fallback);
        universeSelector.setValue(fallback);
      }

      universeSelector.addValueChangeListener(
          event -> {
            if (event.getValue() != null) {
              universeContextService.setCurrentUniverse(event.getValue());
              UI.getCurrent().getPage().reload();
            }
          });
    }

    Div container = new Div(universeSelector);
    container.addClassName("universe-selector-container");
    return container;
  }

  private List<Universe> resolveAccessibleUniverses() {
    if (securityUtils.isAdmin()) {
      return universeRepository.findAll();
    }
    return securityUtils
        .getAuthenticatedUser()
        .map(user -> universeMembershipService.getUniversesForAccount(user.getAccount()))
        .orElseGet(Collections::emptyList);
  }

  private Div createHeader() {
    Image appLogo = new Image("images/logo.png", "ATW Logo");
    appLogo.setWidth("32px");
    appLogo.setHeight("32px");
    appLogo.addClassNames(LumoUtility.BorderRadius.SMALL);

    Span appName = new Span("All Time Wrestling RPG");
    appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

    Div header = new Div(appLogo, appName);
    header.addClassNames(
        Display.FLEX, JustifyContent.CENTER, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
    header.addClassName("drawer-header");
    return header;
  }

  private com.vaadin.flow.component.html.Footer createFooter() {
    Span versionSpan = new Span();
    versionSpan.setId("version-span");
    if (buildProperties != null) { // Needed for tests
      versionSpan.setText("Version: " + buildProperties.getVersion());
    } else {
      versionSpan.setText("Version: N/A");
    }

    Anchor githubLink =
        new Anchor("https://github.com/javydreamercsw/all-time-wrestling-rpg", "Source Code");
    githubLink.addClassNames(FontSize.XSMALL, TextColor.SECONDARY);

    com.vaadin.flow.component.html.Footer footer =
        new com.vaadin.flow.component.html.Footer(versionSpan, githubLink);
    footer.addClassNames(Display.FLEX, FlexDirection.COLUMN, AlignItems.CENTER, Padding.MEDIUM);
    footer.addClassName("drawer-footer");
    return footer;
  }

  private SideNav createSideNav() {
    SideNav nav = new SideNav();
    nav.addClassNames(Margin.Horizontal.MEDIUM);
    menuService.getMenuItems().forEach(menuItem -> nav.addItem(createSideNavItem(menuItem)));
    return nav;
  }

  private SideNavItem createSideNavItem(final MenuItem menuItem) {
    SideNavItem item = new SideNavItem(menuItem.getTitle());
    item.setPrefixComponent(menuItem.getIcon().create());
    String path = menuItem.getPath();
    if (menuItem.isExternal() && path != null) {
      item.getElement().setAttribute("router-ignore", "");
      item.getElement().setAttribute("target", "_blank");
    }
    item.setPath(path);
    if (!menuItem.getChildren().isEmpty()) {
      menuItem.getChildren().forEach(child -> item.addItem(createSideNavItem(child)));
    }
    return item;
  }

  private Div createNavbar() {
    Div navbar = new Div();
    navbar.addClassNames(
        Display.FLEX, AlignItems.CENTER, Gap.MEDIUM, Margin.Left.AUTO, Padding.Horizontal.MEDIUM);

    if (securityUtils != null && securityUtils.isAuthenticated()) {
      String username = securityUtils.getCurrentUsername();

      // User avatar
      Avatar avatar = new Avatar(username);
      avatar.setTooltipEnabled(true);

      // Username label
      Span usernameLabel = new Span(username);
      usernameLabel.addClassNames(FontWeight.SEMIBOLD, FontSize.SMALL);

      // Inbox badge (pill showing unread count)
      inboxBadge = new Span();
      inboxBadge.setId("inbox-unread-badge");
      inboxBadge.getElement().getThemeList().add("badge pill");
      inboxBadge.setVisible(false);

      // Inbox button with badge
      Span inboxLabel = new Span("Inbox");
      Div inboxContent = new Div(inboxLabel, inboxBadge);
      inboxContent.addClassNames(Display.FLEX, AlignItems.CENTER, Gap.SMALL);
      Button inboxButton = new Button(inboxContent);
      inboxButton.setId("inbox-button");
      inboxButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      inboxButton.addClickListener(e -> UI.getCurrent().navigate("inbox"));

      // Initialise the badge count
      refreshInboxBadge();

      // Profile button (opens drawer)
      Button profileButton = new Button("Profile", VaadinIcon.USER.create());
      profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      profileButton.addClickListener(
          e ->
              securityUtils
                  .getAuthenticatedUser()
                  .flatMap(user -> accountService.findByUsername(user.getUsername()))
                  .ifPresent(
                      account ->
                          new ProfileDrawer(
                                  account,
                                  accountService,
                                  passwordEncoder,
                                  themeService,
                                  tutorialService,
                                  universeContextService)
                              .open()));

      // Logout button
      Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
      logoutButton.setId("logout-button");
      logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      logoutButton.addClickListener(e -> securityUtils.logout());

      navbar.add(avatar, usernameLabel, inboxButton, profileButton, logoutButton);
    }

    return navbar;
  }

  private void refreshInboxBadge() {
    if (inboxBadge == null || securityUtils == null || !securityUtils.isAuthenticated()) {
      return;
    }
    Long accountId =
        securityUtils.getAuthenticatedUser().map(u -> u.getAccount().getId()).orElse(null);
    long unread = inboxService != null ? inboxService.countUnread(accountId) : 0L;
    if (unread > 0) {
      inboxBadge.setText(String.valueOf(unread));
      inboxBadge.setVisible(true);
    } else {
      inboxBadge.setVisible(false);
    }
  }

  private void openProfileDrawer() {
    if (securityUtils == null) {
      return;
    }
    securityUtils
        .getAuthenticatedUser()
        .flatMap(user -> accountService.findByUsername(user.getUsername()))
        .ifPresent(
            account ->
                new com.github.javydreamercsw.management.ui.view.account.ProfileDrawer(
                        account,
                        accountService,
                        passwordEncoder,
                        themeService,
                        tutorialService,
                        universeContextService)
                    .open());
  }

  @Override
  protected void onAttach(final AttachEvent attachEvent) {
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
                        refreshInboxBadge();
                      });
                }
              });
    }
    if (openProfileDrawerBroadcaster != null) {
      openProfileDrawerRegistration =
          openProfileDrawerBroadcaster.register(
              ignored -> {
                if (ui.isAttached()) {
                  ui.access(this::openProfileDrawer);
                }
              });
    }
  }

  @Override
  protected void onDetach(final DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    if (inboxUpdateBroadcasterRegistration != null) { // Needed for tests
      inboxUpdateBroadcasterRegistration.remove();
    }
    if (openProfileDrawerRegistration != null) {
      openProfileDrawerRegistration.remove();
    }
  }

  @Override
  public void afterNavigation(final AfterNavigationEvent event) {
    refreshInboxBadge();
    manageTutorialOverlay(event);
  }

  private void manageTutorialOverlay(final AfterNavigationEvent event) {
    if (securityUtils == null || !securityUtils.isAuthenticated() || tutorialService == null) {
      return;
    }

    // Never show the overlay while the user is on the tutorial setup/inline pages
    String location = event.getLocation().getPath();
    if ("tutorial".equals(location)) {
      if (tutorialOverlay != null && tutorialOverlay.isOpened()) {
        tutorialOverlay.close();
      }
      return;
    }

    Long accountId =
        securityUtils.getAuthenticatedUser().map(u -> u.getAccount().getId()).orElse(null);
    if (accountId == null) {
      return;
    }

    com.github.javydreamercsw.base.domain.account.Account account =
        accountService.get(accountId).orElse(null);
    if (account == null) {
      return;
    }

    com.github.javydreamercsw.management.domain.universe.Universe tutorialUniverse =
        tutorialService.findTutorialUniverse(account.getUsername()).orElse(null);
    if (tutorialUniverse == null) {
      closeTutorialOverlay();
      return;
    }

    // Keep the universe context pointed at the tutorial universe while the overlay is active so
    // that beforeStep/validateStep calls (which use getCurrentUniverse) use the correct scope.
    universeContextService.setCurrentUniverse(tutorialUniverse);

    com.github.javydreamercsw.management.domain.universe.Universe.UniverseType universeType =
        tutorialUniverse.getType();
    com.github.javydreamercsw.management.service.tutorial.TutorialDefinition definition =
        tutorialService.getDefinition(universeType);
    int totalSteps = definition.getSteps().size();
    int stepIndex = tutorialService.getCurrentStep(accountId, universeType);

    if (stepIndex >= totalSteps) {
      closeTutorialOverlay();
      return;
    }

    TutorialStep step = definition.getSteps().get(stepIndex);
    if (step.getInteractionMode() == TutorialStep.InteractionMode.INLINE) {
      // Inline steps are handled inside TutorialView itself
      closeTutorialOverlay();
      return;
    }

    if (tutorialOverlay == null) {
      tutorialOverlay = new TutorialStepOverlay(tutorialService, accountService);
    }
    tutorialOverlay.updateStep(account, universeType, stepIndex, totalSteps);
  }

  private void closeTutorialOverlay() {
    if (tutorialOverlay != null && tutorialOverlay.isOpened()) {
      tutorialOverlay.close();
    }
  }
}
