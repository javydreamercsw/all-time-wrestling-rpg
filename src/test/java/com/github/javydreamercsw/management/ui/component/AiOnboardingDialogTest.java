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
package com.github.javydreamercsw.management.ui.component;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.ai.AiOnboardingService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class AiOnboardingDialogTest extends AbstractViewTest {

  @Mock private AiOnboardingService aiOnboardingService;
  @Mock private SecurityUtils securityUtils;

  private AiOnboardingDialog dialog;

  @BeforeEach
  void setUp() {
    // Default gate: CAN_CONFIGURE. Individual tests override.
    when(aiOnboardingService.evaluateGate())
        .thenReturn(AiOnboardingService.GateState.CAN_CONFIGURE);
    when(securityUtils.isAdmin()).thenReturn(true);
  }

  private void openDialog() {
    dialog = new AiOnboardingDialog(aiOnboardingService, securityUtils);
    UI.getCurrent().add(dialog);
    dialog.open();
  }

  @Test
  @DisplayName("CAN_CONFIGURE: full layout with key field and save")
  void canConfigure_fullLayout() {
    openDialog();

    _get(dialog, PasswordField.class, spec -> spec.withId("gemini-key-field"));
    _get(dialog, Button.class, spec -> spec.withId("save-button"));
    _get(dialog, Button.class, spec -> spec.withId("open-key-page-button"));
    TextField link = _get(dialog, TextField.class, spec -> spec.withId("key-link-field"));
    assertEquals(AiOnboardingDialog.GEMINI_KEY_URL, link.getValue());
  }

  @Test
  @DisplayName("NO_UNIVERSE_SELECTED: key field and save hidden, guidance shown")
  void preUniverse_saveDisabled() {
    when(aiOnboardingService.evaluateGate())
        .thenReturn(AiOnboardingService.GateState.NO_UNIVERSE_SELECTED);
    openDialog();

    assertTrue(_find(dialog, PasswordField.class).isEmpty());
    assertTrue(_find(dialog, Button.class, spec -> spec.withId("save-button")).isEmpty());
    Span status = _get(dialog, Span.class, spec -> spec.withId("status-line"));
    assertThat(status.getText()).contains("Create your universe first");
  }

  @Test
  @DisplayName("ALREADY_CONFIGURED: informational setup state, no key field")
  void alreadyConfigured_showsSetupState() {
    when(aiOnboardingService.evaluateGate())
        .thenReturn(AiOnboardingService.GateState.ALREADY_CONFIGURED);
    openDialog();

    assertTrue(
        _find(dialog, PasswordField.class, spec -> spec.withId("gemini-key-field")).isEmpty());
    assertTrue(_find(dialog, Button.class, spec -> spec.withId("save-button")).isEmpty());
    Span status = _get(dialog, Span.class, spec -> spec.withId("status-line"));
    assertThat(status.getText()).contains("already set up");
  }

  @Test
  @DisplayName("NOT_UNIVERSE_OWNER: read-only guidance")
  void nonOwner_readOnly() {
    when(aiOnboardingService.evaluateGate())
        .thenReturn(AiOnboardingService.GateState.NOT_UNIVERSE_OWNER);
    openDialog();

    assertTrue(_find(dialog, PasswordField.class).isEmpty());
    Span status = _get(dialog, Span.class, spec -> spec.withId("status-line"));
    assertThat(status.getText()).contains("universe owner");
  }

  @Test
  @DisplayName("Link fallback is always present")
  void linkFallback_present() {
    openDialog();

    TextField link = _get(dialog, TextField.class, spec -> spec.withId("key-link-field"));
    assertThat(link.getValue()).isNotBlank();
  }

  @Test
  @DisplayName("Save success: status flips, field cleared, button becomes Close")
  void saveButton_roundTrip_success() {
    openDialog();
    when(aiOnboardingService.saveGeminiKey("AIza-key")).thenReturn(true);

    PasswordField keyField =
        _get(dialog, PasswordField.class, spec -> spec.withId("gemini-key-field"));
    keyField.setValue("AIza-key");
    Button save = _get(dialog, Button.class, spec -> spec.withId("save-button"));
    save.click();

    Span status = _get(dialog, Span.class, spec -> spec.withId("status-line"));
    assertThat(status.getText()).contains("✓");
    assertThat(keyField.getValue()).isEmpty();
    assertEquals("Close", save.getText());
  }

  @Test
  @DisplayName("Save failure: status shows could-not-verify, field kept")
  void saveButton_roundTrip_failure() {
    openDialog();
    when(aiOnboardingService.saveGeminiKey("bad-key")).thenReturn(false);

    PasswordField keyField =
        _get(dialog, PasswordField.class, spec -> spec.withId("gemini-key-field"));
    keyField.setValue("bad-key");
    Button save = _get(dialog, Button.class, spec -> spec.withId("save-button"));
    save.click();

    Span status = _get(dialog, Span.class, spec -> spec.withId("status-line"));
    assertThat(status.getText()).contains("Could not verify");
    assertThat(keyField.getValue()).isEqualTo("bad-key");
  }

  @Test
  @DisplayName("Advanced button visible only to admins")
  void advancedButton_adminOnly() {
    openDialog();
    _get(dialog, Button.class, spec -> spec.withId("advanced-button"));

    when(securityUtils.isAdmin()).thenReturn(false);
    openDialog();
    assertThat(_find(dialog, Button.class, spec -> spec.withId("advanced-button"))).isEmpty();
  }

  @Test
  @DisplayName("Cancel closes the dialog")
  void cancel_closes() {
    openDialog();
    Button cancel = _get(dialog, Button.class, spec -> spec.withId("cancel-button"));
    cancel.click();
    assertThat(dialog.isOpened()).isFalse();
  }

  @Test
  @DisplayName("Save blank key surfaces the message")
  void saveBlankKey_showsMessage() {
    openDialog();
    when(aiOnboardingService.saveGeminiKey(""))
        .thenThrow(new IllegalArgumentException("API key is required"));

    Button save = _get(dialog, Button.class, spec -> spec.withId("save-button"));
    save.click();

    Span status = _get(dialog, Span.class, spec -> spec.withId("status-line"));
    assertThat(status.getText()).contains("API key is required");
  }

  @Test
  @DisplayName("All gate states open the dialog — ids stable")
  void dialogIds_stable() {
    openDialog();
    _get(dialog, Button.class, spec -> spec.withId("open-key-page-button"));
    assertThat(
            _find(dialog, TextField.class).stream()
                .anyMatch(t -> AiOnboardingDialog.GEMINI_KEY_URL.equals(t.getValue())))
        .isTrue();
    List<Dialog> dialogs = _find(Dialog.class);
    assertThat(dialogs).isNotEmpty();
  }
}
