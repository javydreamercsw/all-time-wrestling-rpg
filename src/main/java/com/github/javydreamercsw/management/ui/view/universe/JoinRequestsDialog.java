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
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest;
import com.github.javydreamercsw.management.service.universe.JoinRequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Admin dialog showing pending join requests for a universe with approve/reject/block actions. */
@Slf4j
public class JoinRequestsDialog extends Dialog {

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

  private final Universe universe;
  private final JoinRequestService joinRequestService;
  private final Account admin;
  private final Grid<UniverseJoinRequest> grid;

  public JoinRequestsDialog(
      final Universe universe, final JoinRequestService joinRequestService, final Account admin) {
    this.universe = universe;
    this.joinRequestService = joinRequestService;
    this.admin = admin;
    this.grid = new Grid<>(UniverseJoinRequest.class, false);

    setHeaderTitle("Join Requests — " + universe.getName());
    setWidth("min(900px, 95vw)");
    setResizable(true);

    buildGrid();
    refreshGrid();
    add(grid);
    getFooter().add(new Button("Close", e -> close()));
  }

  private void buildGrid() {
    grid.addColumn(UniverseJoinRequest::getRequesterName).setHeader("Name").setFlexGrow(1);
    grid.addColumn(UniverseJoinRequest::getRequesterEmail).setHeader("Email").setFlexGrow(1);
    grid.addColumn(r -> r.getRequestedAt() != null ? FMT.format(r.getRequestedAt()) : "")
        .setHeader("Requested")
        .setWidth("140px")
        .setFlexGrow(0);
    grid.addColumn(r -> r.getInvite() != null ? r.getInvite().getType().name() : "—")
        .setHeader("Via")
        .setWidth("110px")
        .setFlexGrow(0);
    grid.addComponentColumn(
            request -> {
              Button approveBtn = new Button("Approve");
              approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
              approveBtn.addClickListener(e -> handleApprove(request));

              Button rejectBtn = new Button("Reject");
              rejectBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
              rejectBtn.addClickListener(e -> handleRejectOrBlock(request, false));

              Button blockBtn = new Button("Block");
              blockBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
              blockBtn.addClickListener(e -> handleRejectOrBlock(request, true));

              return new HorizontalLayout(approveBtn, rejectBtn, blockBtn);
            })
        .setHeader("Actions")
        .setWidth("230px")
        .setFlexGrow(0);
  }

  private void handleApprove(final UniverseJoinRequest request) {
    try {
      joinRequestService.approveRequest(request.getId(), admin);
      refreshGrid();
      Notification.show(
              request.getRequesterName() + " has been approved and added as a member.",
              3000,
              Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    } catch (Exception ex) {
      log.error("Approve failed", ex);
      Notification.show("Failed: " + ex.getMessage())
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void handleRejectOrBlock(final UniverseJoinRequest request, final boolean block) {
    ConfirmDialog confirm = new ConfirmDialog();
    confirm.setHeader(
        block ? "Block " + request.getRequesterName() : "Reject " + request.getRequesterName());

    TextArea notesField = new TextArea("Reason (optional)");
    notesField.setWidthFull();
    notesField.setPlaceholder("Internal notes, not shown to the requester");

    Span warning =
        new Span(
            block
                ? "This will permanently prevent this account from requesting to join this"
                    + " universe."
                : "The requester may submit a new request in the future.");
    confirm.add(notesField, warning);

    confirm.setCancelable(true);
    confirm.setConfirmText(block ? "Block" : "Reject");
    confirm.setConfirmButtonTheme(block ? "error primary" : "primary");
    confirm.addConfirmListener(
        e -> {
          try {
            String notes = notesField.getValue().trim();
            if (block) {
              joinRequestService.blockRequester(
                  request.getId(), admin, notes.isEmpty() ? null : notes);
            } else {
              joinRequestService.rejectRequest(
                  request.getId(), admin, notes.isEmpty() ? null : notes);
            }
            refreshGrid();
            Notification.show(
                    request.getRequesterName() + " has been " + (block ? "blocked." : "rejected."),
                    3000,
                    Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } catch (Exception ex) {
            log.error("Reject/block failed", ex);
            Notification.show("Failed: " + ex.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });
    confirm.open();
  }

  private void refreshGrid() {
    List<UniverseJoinRequest> pending = joinRequestService.getPendingRequests(universe);
    grid.setItems(pending);
    if (pending.isEmpty()) {
      grid.setVisible(false);
      if (getChildren().noneMatch(c -> c instanceof Paragraph)) {
        add(new Paragraph("No pending requests."));
      }
    } else {
      grid.setVisible(true);
    }
  }
}
