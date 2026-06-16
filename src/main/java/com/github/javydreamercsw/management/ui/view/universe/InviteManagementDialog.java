/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite.InviteType;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Admin dialog for generating new invite links and managing existing ones. */
@Slf4j
public class InviteManagementDialog extends Dialog {

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

  private final Universe universe;
  private final InviteService inviteService;
  private final Account admin;
  private final Grid<UniverseInvite> activeInvitesGrid;

  public InviteManagementDialog(
      final Universe universe, final InviteService inviteService, final Account admin) {
    this.universe = universe;
    this.inviteService = inviteService;
    this.admin = admin;
    this.activeInvitesGrid = new Grid<>(UniverseInvite.class, false);

    setHeaderTitle("Manage Invites — " + universe.getName());
    setWidth("min(750px, 95vw)");
    setResizable(true);

    add(buildGenerateSection(), buildActiveSection());
    getFooter().add(new Button("Close", e -> close()));
  }

  private VerticalLayout buildGenerateSection() {
    H3 header = new H3("Generate New Link");

    RadioButtonGroup<InviteType> typeGroup = new RadioButtonGroup<>("Invite type");
    typeGroup.setId("invite-type-group");
    typeGroup.setItems(InviteType.TARGETED, InviteType.COMMUNITY);
    typeGroup.setValue(InviteType.COMMUNITY);
    typeGroup.setItemLabelGenerator(
        t ->
            t == InviteType.TARGETED
                ? "Targeted (single-use, 7-day expiry)"
                : "Community (multi-use, no expiry)");

    TextField generatedLink = new TextField("Generated link");
    generatedLink.setId("generated-invite-link");
    generatedLink.setReadOnly(true);
    generatedLink.setWidthFull();
    generatedLink.setVisible(false);

    Button copyBtn = new Button("Copy");
    copyBtn.setId("copy-invite-link");
    copyBtn.setVisible(false);
    copyBtn.addClickListener(
        e -> {
          UI.getCurrent()
              .getPage()
              .executeJs("navigator.clipboard.writeText($0)", generatedLink.getValue());
          Notification.show("Link copied to clipboard!", 2000, Notification.Position.BOTTOM_END)
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Button generateBtn = new Button("Generate Link");
    generateBtn.setId("generate-invite-button");
    generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generateBtn.addClickListener(
        e -> {
          try {
            UniverseInvite invite =
                GeneralSecurityUtils.runAsAdmin(
                    () -> inviteService.generateInvite(universe, typeGroup.getValue(), admin));
            String token = invite.getId();
            // Resolve the absolute URL client-side to handle reverse proxies correctly
            UI.getCurrent()
                .getPage()
                .executeJs("return window.location.origin + '/join/' + $0", token)
                .then(
                    String.class,
                    url -> {
                      generatedLink.setValue(url);
                      generatedLink.setVisible(true);
                      copyBtn.setVisible(true);
                    });
            refreshGrid();
            Notification.show("Link generated!", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } catch (Exception ex) {
            log.error("Failed to generate invite", ex);
            Notification.show("Failed: " + ex.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    HorizontalLayout linkRow = new HorizontalLayout(generatedLink, copyBtn);
    linkRow.setWidthFull();
    linkRow.setFlexGrow(1, generatedLink);

    VerticalLayout section = new VerticalLayout(header, typeGroup, generateBtn, linkRow);
    section.setPadding(false);
    return section;
  }

  private VerticalLayout buildActiveSection() {
    H3 header = new H3("Active Invite Links");

    activeInvitesGrid.addColumn(UniverseInvite::getId).setHeader("Token").setFlexGrow(1);
    activeInvitesGrid
        .addColumn(i -> i.getType().name())
        .setHeader("Type")
        .setWidth("110px")
        .setFlexGrow(0);
    activeInvitesGrid
        .addColumn(i -> i.getExpiresAt() != null ? FMT.format(i.getExpiresAt()) : "Never")
        .setHeader("Expires")
        .setWidth("140px")
        .setFlexGrow(0);
    activeInvitesGrid
        .addColumn(i -> i.getUseCount() + (i.getMaxUses() != null ? "/" + i.getMaxUses() : ""))
        .setHeader("Uses")
        .setWidth("80px")
        .setFlexGrow(0);
    activeInvitesGrid
        .addComponentColumn(
            invite -> {
              Button copy = new Button("Copy");
              copy.addThemeVariants(ButtonVariant.LUMO_SMALL);
              copy.addClickListener(
                  e ->
                      UI.getCurrent()
                          .getPage()
                          .executeJs(
                              "return window.location.origin + '/join/' + $0", invite.getId())
                          .then(
                              String.class,
                              url -> {
                                UI.getCurrent()
                                    .getPage()
                                    .executeJs("navigator.clipboard.writeText($0)", url);
                                Notification.show(
                                        "Link copied!", 2000, Notification.Position.BOTTOM_END)
                                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                              }));

              Button revoke = new Button("Revoke");
              revoke.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
              revoke.addClickListener(
                  e -> {
                    GeneralSecurityUtils.runAsAdmin(
                        () -> inviteService.revokeInvite(invite.getId()));
                    refreshGrid();
                    Notification.show("Invite revoked.", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                  });

              return new HorizontalLayout(copy, revoke);
            })
        .setHeader("Actions")
        .setWidth("160px")
        .setFlexGrow(0);

    refreshGrid();

    VerticalLayout section = new VerticalLayout(header, activeInvitesGrid);
    section.setPadding(false);
    return section;
  }

  private void refreshGrid() {
    List<UniverseInvite> active =
        GeneralSecurityUtils.runAsAdmin(() -> inviteService.listActiveInvites(universe));
    activeInvitesGrid.setItems(active);
    activeInvitesGrid.setVisible(!active.isEmpty());
    if (active.isEmpty()) {
      activeInvitesGrid.setVisible(false);
    }
  }
}
