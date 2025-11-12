package com.github.javydreamercsw.management.ui.view;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

import com.github.javydreamercsw.management.event.FanChangeBroadcaster;
import com.github.javydreamercsw.management.event.WrestlerInjuryHealedBroadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;

@Layout
@PermitAll // When security is enabled, allow all authenticated users
public final class MainLayout extends AppLayout {

  private final MenuService menuService;
  private final WrestlerInjuryHealedBroadcaster injuryBroadcaster;
  private final BuildProperties buildProperties;
  private Registration broadcasterRegistration;
  private Registration injuryBroadcasterRegistration;

  @Autowired
  public MainLayout(
      MenuService menuService,
      WrestlerInjuryHealedBroadcaster injuryBroadcaster,
      BuildProperties buildProperties) {
    this.menuService = menuService;
    this.injuryBroadcaster = injuryBroadcaster;
    this.buildProperties = buildProperties;
    setPrimarySection(Section.DRAWER);

    var sideNav = createSideNav();
    var footer = createFooter();
    var content = new Div(sideNav, footer);
    content.setSizeFull(); // Ensure content takes full size for proper scrolling

    addToDrawer(createHeader(), new Scroller(content));
  }

  private Div createHeader() {
    // TODO Replace with real application logo and name
    var appLogo = VaadinIcon.CUBES.create();
    appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

    var appName = new Span("All Time Wrestling RPG");
    appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

    var header = new Div(appLogo, appName);
    header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
    return header;
  }

  private Div createFooter() {
    var versionSpan = new Span("Version: " + buildProperties.getVersion());
    versionSpan.addClassNames(
        FontSize.XSMALL, TextColor.SECONDARY, Padding.Top.SMALL, Padding.Bottom.SMALL);
    var footer = new Div(versionSpan);
    footer.addClassNames(Display.FLEX, JustifyContent.CENTER, Width.FULL);
    return footer;
  }

  private SideNav createSideNav() {
    var nav = new SideNav();
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

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    UI ui = attachEvent.getUI();
    broadcasterRegistration =
        FanChangeBroadcaster.register(
            event -> {
              if (ui.isAttached()) {
                ui.access(
                    () -> {
                      String message =
                          String.format(
                              "%s %s %d fans!",
                              event.getWrestler().getName(),
                              event.getFanChange() > 0 ? "gained" : "lost",
                              Math.abs(event.getFanChange()));
                      Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    });
              }
            });
    injuryBroadcasterRegistration =
        injuryBroadcaster.register(
            event -> {
              if (ui.isAttached()) {
                ui.access(
                    () -> {
                      String message =
                          String.format(
                              "%s's injury (%s) has been healed!",
                              event.getWrestler().getName(), event.getInjury().getName());
                      Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    });
              }
            });
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    broadcasterRegistration.remove();
    injuryBroadcasterRegistration.remove();
  }
}
