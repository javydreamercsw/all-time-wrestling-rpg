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

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.service.inbox.DirectMessageService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ComposeMessageDialogTest extends AbstractViewTest {

  @Mock private AccountRepository accountRepository;
  @Mock private DirectMessageService directMessageService;

  private Account alice;
  private Account bob;

  @BeforeEach
  void setupAccounts() {
    alice = mock(Account.class);
    when(alice.getId()).thenReturn(1L);
    when(alice.getUsername()).thenReturn("alice");

    bob = mock(Account.class);
    when(bob.getId()).thenReturn(2L);
    when(bob.getUsername()).thenReturn("bob");

    when(accountRepository.findAll()).thenReturn(List.of(alice, bob));
  }

  @Test
  @DisplayName("Send button does not call service when fields are empty")
  void send_withEmptyFields_doesNotCallService() {
    ComposeMessageDialog dialog =
        new ComposeMessageDialog(accountRepository, directMessageService, 99L, null);
    dialog.open();

    Button sendBtn = _get(dialog, Button.class, spec -> spec.withText("Send"));
    _click(sendBtn);

    verifyNoInteractions(directMessageService);
  }

  @Test
  @DisplayName("Send button calls directMessageService.send() with correct arguments")
  @SuppressWarnings("unchecked")
  void send_withValidFields_callsService() {
    ComposeMessageDialog dialog =
        new ComposeMessageDialog(accountRepository, directMessageService, 99L, null);
    dialog.open();

    ComboBox<Account> recipientPicker = (ComboBox<Account>) _get(dialog, ComboBox.class);
    recipientPicker.setValue(alice);

    _get(dialog, TextField.class).setValue("Hello");
    _get(dialog, TextArea.class).setValue("Body of message");

    Button sendBtn = _get(dialog, Button.class, spec -> spec.withText("Send"));
    _click(sendBtn);

    verify(directMessageService).send(eq(1L), eq("Hello"), eq("Body of message"));
  }

  @Test
  @DisplayName("Prefilled recipient is pre-selected when prefilledRecipientId matches an account")
  @SuppressWarnings("unchecked")
  void prefilledRecipient_isSelected() {
    // sender is alice (id=1), prefilled recipient is bob (id=2)
    ComposeMessageDialog dialog =
        new ComposeMessageDialog(accountRepository, directMessageService, 1L, 2L);
    dialog.open();

    ComboBox<Account> recipientPicker = (ComboBox<Account>) _get(dialog, ComboBox.class);
    assertThat(recipientPicker.getValue()).isNotNull();
    assertThat(recipientPicker.getValue().getUsername()).isEqualTo("bob");
  }

  @Test
  @DisplayName("Cancel button closes the dialog")
  void cancelButton_closesDialog() {
    ComposeMessageDialog dialog =
        new ComposeMessageDialog(accountRepository, directMessageService, 99L, null);
    dialog.open();
    assertThat(dialog.isOpened()).isTrue();

    Button cancelBtn = _get(dialog, Button.class, spec -> spec.withText("Cancel"));
    _click(cancelBtn);

    assertThat(dialog.isOpened()).isFalse();
  }
}
