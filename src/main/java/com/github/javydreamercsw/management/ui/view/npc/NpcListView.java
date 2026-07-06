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
package com.github.javydreamercsw.management.ui.view.npc;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;

@Route("npc-list")
@PageTitle("NPC List")
@Menu(order = 1, icon = "vaadin:users", title = "NPC List")
@PermitAll
public class NpcListView extends Main {

  private final NpcService npcService;
  private final ImageGenerationServiceFactory imageFactory;
  private final ImageStorageService storageService;
  private final AiSettingsService aiSettingsService;

  final Grid<Npc> npcGrid;

  public NpcListView(
      @NonNull final NpcService npcService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final ImageGenerationServiceFactory imageFactory,
      @NonNull final ImageStorageService storageService,
      @NonNull final AiSettingsService aiSettingsService) {
    this.npcService = npcService;
    this.imageFactory = imageFactory;
    this.storageService = storageService;
    this.aiSettingsService = aiSettingsService;

    npcGrid = new Grid<>();
    npcGrid.setId("npc-grid");

    Button createBtn =
        new Button(
            "Create NPC",
            event -> {
              new NpcFormDialog(npcService, storageService, new Npc(), () -> refreshGrid()).open();
            });
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createBtn.setVisible(securityUtils.canCreate());

    refreshGrid();
    npcGrid
        .addComponentColumn(
            npc -> {
              HorizontalLayout nameLayout = new HorizontalLayout();
              nameLayout.setAlignItems(
                  com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
              nameLayout.setSpacing(false);
              nameLayout.setPadding(false);
              if (!npc.isActive()) {
                Icon inactiveIcon = VaadinIcon.EYE_SLASH.create();
                inactiveIcon.setColor("var(--lumo-disabled-text-color)");
                inactiveIcon.getStyle().set("margin-right", "6px");
                nameLayout.add(inactiveIcon);
              }
              nameLayout.add(new com.vaadin.flow.component.html.Span(npc.getName()));
              return nameLayout;
            })
        .setHeader("Name")
        .setComparator(java.util.Comparator.comparing(Npc::getName))
        .setSortable(true);
    npcGrid.addColumn(Npc::getNpcType).setHeader("Type").setSortable(true);

    npcGrid
        .addColumn(
            npc ->
                "Referee".equalsIgnoreCase(npc.getNpcType()) ? npcService.getAwareness(npc) : null)
        .setHeader("Awareness")
        .setSortable(true);

    npcGrid.addColumn(Npc::getImageUrl).setHeader("Image URL").setSortable(true);

    npcGrid
        .addComponentColumn(
            npc -> {
              HorizontalLayout buttons = new HorizontalLayout();

              Button viewProfileButton = new Button("View Profile");
              viewProfileButton.addClickListener(
                  e -> {
                    getUI()
                        .ifPresent(
                            ui ->
                                ui.navigate(
                                    NpcProfileView.class,
                                    new RouteParameters("npcId", String.valueOf(npc.getId()))));
                  });
              viewProfileButton.setId("view-profile-btn-" + npc.getId());
              buttons.add(viewProfileButton);

              Button editButton = new Button("Edit");
              editButton.addClickListener(
                  e -> {
                    new NpcFormDialog(npcService, storageService, npc, () -> refreshGrid()).open();
                  });
              editButton.setVisible(securityUtils.canEdit());
              buttons.add(editButton);

              if (securityUtils.canEdit()) {
                Icon toggleIcon =
                    npc.isActive() ? VaadinIcon.EYE.create() : VaadinIcon.EYE_SLASH.create();
                toggleIcon.setColor(
                    npc.isActive()
                        ? "var(--lumo-success-color)"
                        : "var(--lumo-disabled-text-color)");
                Button toggleButton = new Button(toggleIcon);
                toggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                toggleButton.setTooltipText(npc.isActive() ? "Deactivate" : "Activate");
                toggleButton.addClickListener(
                    e -> {
                      npcService.setActive(npc.getId(), !npc.isActive());
                      refreshGrid();
                    });
                buttons.add(toggleButton);
              }

              Button generateImageButton = new Button("Generate Image");
              generateImageButton.setId("generate-image-btn-" + npc.getId());
              generateImageButton.addClickListener(
                  e -> {
                    new NpcImageGenerationDialog(
                            npc,
                            npcService,
                            imageFactory,
                            storageService,
                            aiSettingsService,
                            () -> npcGrid.getDataProvider().refreshAll())
                        .open();
                  });
              generateImageButton.setVisible(securityUtils.canEdit());
              buttons.add(generateImageButton);

              return buttons;
            })
        .setHeader("Actions");

    npcGrid
        .addComponentColumn(
            npc -> {
              Button deleteButton = new Button("Delete");
              deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
              deleteButton.addClickListener(
                  e -> {
                    npcService.delete(npc);
                    refreshGrid();
                    Notification.show("NPC deleted", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                  });
              deleteButton.setVisible(securityUtils.canDelete());
              return deleteButton;
            })
        .setHeader("Delete");

    npcGrid.setSizeFull();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(new ViewToolbar("NPC List", ViewToolbar.group(createBtn)));
    add(npcGrid);
  }

  private void refreshGrid() {
    npcGrid.setItems(npcService.findAllIncludingInactive());
  }
}
