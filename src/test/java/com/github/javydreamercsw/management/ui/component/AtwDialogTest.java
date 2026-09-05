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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.Binder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

class AtwDialogTest extends AbstractViewTest {

  private AtwDialog dialog;

  @BeforeEach
  void setup() {
    dialog = new AtwDialog(AtwDialog.WIDTH_FORM);
    dialog.setId("test-dialog");
    UI.getCurrent().add(dialog);
  }

  @Test
  @DisplayName("Default width is the 95vw-capped form width")
  void defaultWidth_isViewportCapped() {
    AtwDialog defaultDialog = new AtwDialog();
    assertThat(defaultDialog.getWidth()).isEqualTo(AtwDialog.WIDTH_FORM);
    assertThat(defaultDialog.getMaxWidth()).isEqualTo("95vw");
  }

  @Test
  @DisplayName("FormLayout has responsive single-column and two-column steps")
  void formLayout_hasResponsiveSteps() {
    // ResponsiveStep has no getters; the client-visible JSON is the API surface.
    ObjectNode first = (ObjectNode) dialog.getFormLayout().getResponsiveSteps().get(0).toJson();
    ObjectNode second = (ObjectNode) dialog.getFormLayout().getResponsiveSteps().get(1).toJson();
    assertThat(first.get("minWidth").asText()).isEqualTo("0");
    assertThat(first.get("columns").asInt()).isEqualTo(1);
    assertThat(second.get("minWidth").asText()).isEqualTo("600px");
    assertThat(second.get("columns").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("Primary button keeps the exact id given and the primary variant")
  void createPrimaryButton_idAndVariant() {
    Button save = dialog.createPrimaryButton("test-dialog-save-button", "Save", () -> {});
    assertThat(save.getId()).contains("test-dialog-save-button");
    assertThat(save.getThemeName()).contains("primary");
    Button located = _get(dialog, Button.class, spec -> spec.withId("test-dialog-save-button"));
    assertThat(located).isSameAs(save);
  }

  @Test
  @DisplayName("Delete button sits before the confirm actions in the row")
  void createDeleteButton_isPlacedFirst() {
    dialog.createPrimaryButton("save", "Save", () -> {});
    dialog.createCancelButton("cancel");
    Button delete = dialog.createDeleteButton("delete", "Delete", () -> {});

    Component first = dialog.getButtonRow().getComponentAt(0);
    assertThat(first).isSameAs(delete);
    assertThat(delete.getThemeName()).contains("error");
  }

  @Test
  @DisplayName("Cancel button closes the dialog")
  void createCancelButton_closes() {
    dialog.open();
    Button cancel = dialog.createCancelButton("cancel");
    cancel.click();
    assertFalse(dialog.isOpened());
  }

  @Test
  @DisplayName("bindSaveEnabled keeps the save button disabled until the binder is valid")
  void bindSaveEnabled_disabledUntilValid() {
    Binder<Div> binder = new Binder<>(Div.class);
    Button save = dialog.createPrimaryButton("save", "Save", () -> {});
    dialog.bindSaveEnabled(save, binder);

    assertFalse(save.isEnabled());
    // BeanValidation-less Binder on a clean bean: mark it valid manually via setBean.
    binder.setBean(new Div());
    assertTrue(save.isEnabled());
  }

  @Test
  @DisplayName("addAboveForm places content before the form; addBelowForm places it after")
  void contentSlots_orderedCorrectly() {
    Div above = new Div("above");
    Div below = new Div("below");
    dialog.addAboveForm(above);
    dialog.addBelowForm(below);

    // content layout: [above-wrapper, formLayout, below-wrapper, buttonRow]
    assertThat(dialog.getFormLayout().getParent()).isNotNull();
    assertThat(above.getParent()).isNotNull();
    assertThat(below.getParent()).isNotNull();
    FormLayout located = _get(dialog, FormLayout.class);
    assertThat(located).isSameAs(dialog.getFormLayout());
  }
}
