package com.github.javydreamercsw.management.ui.view.faction;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Route("faction-rivalry-list")
@PageTitle("Faction Rivalry List")
@Menu(order = 2, icon = "vaadin:group", title = "Faction Rivalry List")
@PermitAll
@Transactional(readOnly = true)
public class FactionRivalryListView extends Main {

  private final FactionRivalryService factionRivalryService;
  private final FactionRivalryRepository factionRivalryRepository;

  final Grid<FactionRivalry> factionRivalryGrid;

  public FactionRivalryListView(
      FactionRivalryService factionRivalryService,
      FactionRivalryRepository factionRivalryRepository,
      FactionService factionService) {
    this.factionRivalryService = factionRivalryService;
    this.factionRivalryRepository = factionRivalryRepository;

    ComboBox<Faction> faction1ComboBox = new ComboBox<>("Faction 1");
    faction1ComboBox.setItems(
        factionService.getAllFactions(Pageable.unpaged()).stream()
            .sorted(Comparator.comparing(Faction::getName))
            .collect(Collectors.toList()));
    faction1ComboBox.setItemLabelGenerator(Faction::getName);

    ComboBox<Faction> faction2ComboBox = new ComboBox<>("Faction 2");
    faction2ComboBox.setItems(
        factionService.getAllFactions(Pageable.unpaged()).stream()
            .sorted(Comparator.comparing(Faction::getName))
            .collect(Collectors.toList()));
    faction2ComboBox.setItemLabelGenerator(Faction::getName);

    TextField storylineNotes = new TextField("Storyline Notes");
    factionRivalryGrid = new Grid<>();

    Button createButton =
        new Button(
            "Create",
            event -> {
              factionRivalryService.createFactionRivalry(
                  faction1ComboBox.getValue().getId(),
                  faction2ComboBox.getValue().getId(),
                  storylineNotes.getValue());
              factionRivalryGrid.getDataProvider().refreshAll();
              faction1ComboBox.clear();
              faction2ComboBox.clear();
              storylineNotes.clear();
              Notification.show("Faction Rivalry created", 2000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    factionRivalryGrid.setItems(
        query ->
            factionRivalryService
                .getAllFactionRivalriesWithFactions(toSpringPageRequest(query))
                .stream());
    factionRivalryGrid
        .addColumn(rivalry -> rivalry.getFaction1().getName())
        .setHeader("Faction 1")
        .setSortable(true);
    factionRivalryGrid
        .addColumn(rivalry -> rivalry.getFaction2().getName())
        .setHeader("Faction 2")
        .setSortable(true);
    factionRivalryGrid.addColumn(FactionRivalry::getHeat).setHeader("Heat").setSortable(true);
    factionRivalryGrid
        .addComponentColumn(
            rivalry -> {
              Button addHeatButton = new Button("Add Heat");
              addHeatButton.addClickListener(
                  e -> {
                    Dialog dialog = new Dialog();
                    NumberField heatField = new NumberField("Heat to Add");
                    Button saveButton =
                        new Button(
                            "Save",
                            event -> {
                              factionRivalryService.addHeat(
                                  rivalry.getId(), heatField.getValue().intValue(), "UI Edit");
                              dialog.close();
                              factionRivalryGrid.getDataProvider().refreshAll();
                            });
                    dialog.add(new VerticalLayout(heatField, saveButton));
                    dialog.open();
                  });
              return addHeatButton;
            })
        .setHeader("Actions");

    factionRivalryGrid
        .addComponentColumn(
            rivalry -> {
              Button deleteButton = new Button("Delete");
              deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
              deleteButton.addClickListener(
                  e -> {
                    factionRivalryRepository.deleteById(rivalry.getId());
                    factionRivalryGrid.getDataProvider().refreshAll();
                    Notification.show(
                            "Faction Rivalry deleted", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                  });
              return deleteButton;
            })
        .setHeader("Delete");

    factionRivalryGrid.setSizeFull();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(
        new ViewToolbar(
            "Faction Rivalry List",
            ViewToolbar.group(faction1ComboBox, faction2ComboBox, storylineNotes, createButton)));
    add(factionRivalryGrid);
  }
}
