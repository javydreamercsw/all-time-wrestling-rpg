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
package com.github.javydreamercsw.management.ui.view.inbox;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.service.inbox.DirectMessageService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;

public class ComposeMessageDialog extends Dialog {

  private final ComboBox<Account> recipientPicker = new ComboBox<>("To");
  private final TextField subjectField = new TextField("Subject");
  private final TextArea bodyField = new TextArea("Message");

  public ComposeMessageDialog(
      final AccountRepository accountRepository,
      final DirectMessageService directMessageService,
      final Long senderAccountId,
      final Long prefilledRecipientId) {

    setWidth("560px");
    setMaxWidth("95vw");

    List<Account> recipients =
        accountRepository.findAll().stream()
            .filter(a -> !a.getId().equals(senderAccountId))
            .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
            .toList();

    recipientPicker.setItems(recipients);
    recipientPicker.setItemLabelGenerator(Account::getUsername);
    recipientPicker.setWidthFull();
    recipientPicker.setRequired(true);

    if (prefilledRecipientId != null) {
      recipients.stream()
          .filter(a -> a.getId().equals(prefilledRecipientId))
          .findFirst()
          .ifPresent(recipientPicker::setValue);
    }

    subjectField.setWidthFull();
    subjectField.setMaxLength(255);
    subjectField.setRequired(true);

    bodyField.setWidthFull();
    bodyField.setMaxLength(1024);
    bodyField.setMinHeight("120px");
    bodyField.setRequired(true);

    Button sendBtn = new Button("Send");
    sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendBtn.addClickListener(
        e -> {
          if (!isValid()) {
            Notification.show("Please fill in all required fields.")
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
          }
          directMessageService.send(
              recipientPicker.getValue().getId(), subjectField.getValue(), bodyField.getValue());
          Notification.show("Message sent to " + recipientPicker.getValue().getUsername() + ".")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          close();
        });

    Button cancelBtn = new Button("Cancel", e -> close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    HorizontalLayout footer = new HorizontalLayout(sendBtn, cancelBtn);
    footer.setAlignItems(Alignment.CENTER);

    VerticalLayout content =
        new VerticalLayout(
            new H3("Compose Message"), recipientPicker, subjectField, bodyField, footer);
    content.setPadding(false);
    content.setSpacing(true);
    add(content);

    if (prefilledRecipientId != null) {
      subjectField.focus();
    } else {
      recipientPicker.focus();
    }
  }

  private boolean isValid() {
    return recipientPicker.getValue() != null
        && !subjectField.getValue().isBlank()
        && !bodyField.getValue().isBlank();
  }
}
