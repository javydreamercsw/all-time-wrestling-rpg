package com.github.javydreamercsw.management.ui.view.title;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Height;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Route("title-list")
@PageTitle("Titles")
@PermitAll
@Menu(order = 4, icon = "vaadin:trophy", title = "Titles")
@Slf4j
public class TitleListView extends Main {

  private final TitleService titleService;
  private final WrestlerService wrestlerService;
  public final Grid<Title> grid = new Grid<>(Title.class, false);

  public TitleListView(
      @NonNull TitleService titleService, @NonNull WrestlerService wrestlerService) {
    this.titleService = titleService;
    this.wrestlerService = wrestlerService;

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL,
        Height.FULL, // Use Height.FULL
        Width.FULL); // Use Width.FULL

    Button createButton = new Button("Create Title", new Icon(VaadinIcon.PLUS));
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> openCreateDialog());

    add(new ViewToolbar("Title List", ViewToolbar.group(createButton)));

    setupGrid();
    grid.addClassNames(LumoUtility.Flex.GROW); // Add this to make the grid grow
    add(grid);
    refreshGrid();
  }

  private void setupGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.addColumn(Title::getName).setHeader("Name").setSortable(true);
    grid.addColumn(Title::getTier).setHeader("Tier").setSortable(true);
    grid.addColumn(Title::getChampionNames).setHeader("Champion(s)").setSortable(true);
    grid.addColumn(Title::getIsActive).setHeader("Active").setSortable(true);

    // Add ComboBox for #1 Contender
    grid.addComponentColumn(
            title -> {
              ComboBox<Wrestler> contenderComboBox = new ComboBox<>("Contender");
              assert title.getId() != null;
              contenderComboBox.setItems(
                  titleService.getEligibleChallengers(title.getId()).stream()
                      .sorted(Comparator.comparing(Wrestler::getName))
                      .collect(Collectors.toList()));
              contenderComboBox.setItemLabelGenerator(Wrestler::getName);
              contenderComboBox.setWidthFull();
              contenderComboBox.setClearButtonVisible(true); // Allow clearing the field

              // Set initial value if a contender exists
              title.getContender().stream().findFirst().ifPresent(contenderComboBox::setValue);

              contenderComboBox.addValueChangeListener(
                  event -> {
                    if (event.getValue() != null) {
                      assert event.getValue().getId() != null;
                      titleService
                          .updateNumberOneContender(title.getId(), event.getValue().getId())
                          .ifPresentOrElse(
                              updatedTitle -> {
                                Notification.show(
                                        "Contender updated for " + title.getName(),
                                        3000,
                                        Notification.Position.BOTTOM_END)
                                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                refreshGrid(); // Refresh grid to reflect changes
                              },
                              () ->
                                  Notification.show(
                                          "Failed to update contender",
                                          5000,
                                          Notification.Position.BOTTOM_END)
                                      .addThemeVariants(NotificationVariant.LUMO_ERROR));
                    } else {
                      // Handle clearing the contender
                      titleService
                          .clearNumberOneContender(title.getId())
                          .ifPresentOrElse(
                              updatedTitle -> {
                                Notification.show(
                                        "Contender cleared for " + title.getName(),
                                        3000,
                                        Notification.Position.BOTTOM_END)
                                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                refreshGrid(); // Refresh grid to reflect changes
                              },
                              () ->
                                  Notification.show(
                                          "Failed to clear contender",
                                          5000,
                                          Notification.Position.BOTTOM_END)
                                      .addThemeVariants(NotificationVariant.LUMO_ERROR));
                    }
                  });
              return contenderComboBox;
            })
        .setHeader("Contender");

    grid.addComponentColumn(
            title -> {
              Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
              editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
              editButton.addClickListener(e -> openEditDialog(title));

              Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
              deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteButton.addClickListener(e -> deleteTitle(title));

              return new HorizontalLayout(editButton, deleteButton);
            })
        .setHeader("Actions");
  }

  public void refreshGrid() {
    List<Title> titles = titleService.findAll();
    grid.setItems(titles);
  }

  private void openCreateDialog() {
    Title newTitle = new Title();
    newTitle.setIsActive(true);
    TitleFormDialog dialog =
        new TitleFormDialog(titleService, wrestlerService, newTitle, this::refreshGrid);
    dialog.setHeaderTitle("Create New Title");
    dialog.open();
  }

  private void openEditDialog(@NonNull Title title) {
    TitleFormDialog dialog =
        new TitleFormDialog(titleService, wrestlerService, title, this::refreshGrid);
    dialog.setHeaderTitle("Edit Title: " + title.getName());
    dialog.open();
  }

  private void deleteTitle(@NonNull Title title) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setHeader("Delete Title");
    confirmDialog.setText("Are you sure you want to delete the title '" + title.getName() + "'?");
    confirmDialog.setCancelable(true);
    confirmDialog.setConfirmText("Delete");
    confirmDialog.setConfirmButtonTheme("error primary");

    confirmDialog.addConfirmListener(
        e -> {
          try {
            assert title.getId() != null;
            boolean deleted = titleService.deleteTitle(title.getId());
            if (deleted) {
              refreshGrid();
              Notification.show(
                      "Title deleted successfully", 3000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
              Notification.show(
                      "Title cannot be deleted. It must be inactive and vacant.",
                      5000,
                      Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
          } catch (Exception ex) {
            log.error("Error deleting title", ex);
            Notification.show(
                    "Error deleting title: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    confirmDialog.open();
  }
}
