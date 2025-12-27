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

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;

@Route("npc-list")
@PageTitle("NPC List")
@Menu(order = 1, icon = "vaadin:users", title = "NPC List")
@PermitAll
public class NpcListView extends Main {

  private final NpcService npcService;

  final TextField name;
  final TextField npcType;
  final Button createBtn;
  final Grid<Npc> npcGrid;

  public NpcListView(@NonNull NpcService npcService, @NonNull SecurityUtils securityUtils) {
    this.npcService = npcService;

    name = new TextField();
    name.setPlaceholder("NPC Name");
    name.setAriaLabel("NPC Name");
    name.setMinWidth("15em");

    npcType = new TextField();
    npcType.setPlaceholder("NPC Type");
    npcType.setAriaLabel("NPC Type");
    npcType.setMinWidth("10em");

    createBtn = new Button("Create", event -> createNpc());
    createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createBtn.setVisible(securityUtils.canCreate());

    npcGrid = new Grid<>();
    Editor<Npc> editor = npcGrid.getEditor();
    editor.setBuffered(true);
    Binder<Npc> binder = new Binder<>(Npc.class);
    if (securityUtils.canEdit()) {
      editor.setBinder(binder);
    }

    TextField nameField = new TextField();
    TextField npcTypeField = new TextField();

    npcGrid.setItems(query -> npcService.findAll(toSpringPageRequest(query)).stream());
    npcGrid
        .addColumn(Npc::getName)
        .setHeader("Name")
        .setEditorComponent(nameField)
        .setSortable(true);
    npcGrid
        .addColumn(Npc::getNpcType)
        .setHeader("Type")
        .setEditorComponent(npcTypeField)
        .setSortable(true);

    npcGrid
        .addComponentColumn(
            npc -> {
              Button editButton = new Button("Edit");
              editButton.addClickListener(e -> npcGrid.getEditor().editItem(npc));
              editButton.setVisible(securityUtils.canEdit());
              return editButton;
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
                    npcGrid.getDataProvider().refreshAll();
                    Notification.show("NPC deleted", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                  });
              deleteButton.setVisible(securityUtils.canDelete());
              return deleteButton;
            })
        .setHeader("Delete");

    npcGrid.setSizeFull();

    binder.forField(nameField).bind("name");
    binder.forField(npcTypeField).bind("npcType");

    editor.addSaveListener(
        event -> {
          npcService.save(event.getItem());
          npcGrid.getDataProvider().refreshAll();
          Notification.show("NPC updated", 2000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    Button saveButton = new Button("Save", e -> editor.save());
    Button cancelButton = new Button("Cancel", e -> editor.cancel());
    HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
    actions.setVisible(securityUtils.canEdit());
    npcGrid.getElement().appendChild(actions.getElement());

    if (securityUtils.canCreate()) {
      add(new ViewToolbar("NPC List", ViewToolbar.group(name, npcType, createBtn)));
    } else {
      add(new ViewToolbar("NPC List"));
    }
    add(npcGrid, actions);
  }

  private void createNpc() {
    Npc npc = new Npc();
    npc.setName(name.getValue());
    npc.setNpcType(npcType.getValue());
    npcService.save(npc);
    npcGrid.getDataProvider().refreshAll();
    name.clear();
    npcType.clear();
    Notification.show("NPC added", 3_000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
