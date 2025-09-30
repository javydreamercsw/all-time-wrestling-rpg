package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@Route("wrestler-rankings")
@PageTitle("Wrestler Rankings")
@Menu(order = 4, icon = "vaadin:trophy", title = "Wrestler Rankings")
@PermitAll
public class WrestlerRankingsView extends Main {

  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final Grid<Wrestler> grid = new Grid<>(Wrestler.class, false);

  public WrestlerRankingsView(WrestlerService wrestlerService, TitleService titleService) {
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);
    setHeightFull();

    add(new ViewToolbar("Wrestler Rankings"));
    configureGrid();
    VerticalLayout content = new VerticalLayout(grid);
    content.setSizeFull();
    content.setFlexGrow(1, grid);
    content.setFlexGrow(1); // Make the VerticalLayout itself grow
    add(content);
    updateList();
  }

  private void configureGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setSizeFull();
    grid.setHeightFull();

    grid.addComponentColumn(
            wrestler -> {
              HorizontalLayout layout = new HorizontalLayout();
              layout.setAlignItems(FlexComponent.Alignment.CENTER);

              Span nameSpan = new Span(wrestler.getName());
              nameSpan.addClassNames(
                  "wrestler-tier-" + wrestler.getTier().name().toLowerCase(),
                  "wrestler-tier-badge");
              layout.add(nameSpan);

              // Add a star icon for champions
              if (titleService.isChampion(wrestler)) {
                Icon starIcon = VaadinIcon.STAR.create();
                starIcon.addClickListener(
                    event -> {
                      // Show title details in a notification or dialog
                      Notification.show(
                          "Champion: "
                              + wrestler.getName()
                              + " holds "
                              + titleService.findTitlesByChampion(wrestler).size()
                              + " titles.");
                    });
                layout.add(starIcon);
              }
              return layout;
            })
        .setHeader("Wrestler")
        .setSortable(true)
        .setComparator(Wrestler::getName);

    grid.addColumn(Wrestler::getFans).setHeader("Fans").setSortable(true);
    grid.addColumn(wrestler -> wrestler.getTier().getDisplayWithEmoji())
        .setHeader("Tier")
        .setSortable(true);
  }

  private void updateList() {
    grid.setItems(wrestlerService.findAll());
  }
}
