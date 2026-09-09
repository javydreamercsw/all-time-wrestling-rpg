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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;

/**
 * Shared responsive base for form dialogs. Standardizes what every form dialog in the app repeats
 * by hand:
 *
 * <ul>
 *   <li><b>Width cap:</b> a fixed design width clamped to the viewport via {@code min(Npx, 95vw)},
 *       so wide dialogs never overflow a phone screen.
 *   <li><b>Responsive form steps:</b> a single column below 600px viewport, two columns above
 *       (Vaadin FormLayout responsive steps).
 *   <li><b>Footer buttons:</b> a right-aligned button row with the app's Save/Delete/Cancel
 *       conventions — delete (error variant) on the left, cancel and primary action on the right.
 * </ul>
 *
 * <p>Subclasses add their fields to {@link #getFormLayout()} and call the button factory methods;
 * the base wires the common enable/disable and close behavior.
 */
public class AtwDialog extends Dialog {

  /** Standard design widths for common dialog shapes (all clamped to 95vw). */
  public static final String WIDTH_NARROW = "min(560px, 95vw)";

  public static final String WIDTH_FORM = "min(800px, 95vw)";
  public static final String WIDTH_LARGE = "min(900px, 95vw)";

  @Getter private final FormLayout formLayout = new FormLayout();
  @Getter private final HorizontalLayout buttonRow = new HorizontalLayout();
  private final VerticalLayout content = new VerticalLayout(formLayout);

  public AtwDialog() {
    this(WIDTH_FORM);
  }

  public AtwDialog(final String width) {
    setWidth(width);
    setMaxWidth("95vw");

    // One column below 600px viewport; two columns with side labels above.
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
        new FormLayout.ResponsiveStep("600px", 2, FormLayout.ResponsiveStep.LabelsPosition.ASIDE));

    buttonRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttonRow.setWidthFull();

    content.setPadding(false);
    content.setSpacing(false);
    content.add(buttonRow);
    add(content);
  }

  /** Adds arbitrary components to the content area above the form (e.g. an image preview block). */
  public void addAboveForm(final Component... components) {
    content.addComponentAtIndex(0, wrapper(components));
  }

  /** Adds arbitrary components between the form and the button row. */
  public void addBelowForm(final Component... components) {
    content.addComponentAtIndex(content.getComponentCount() - 1, wrapper(components));
  }

  private static Component wrapper(final Component... components) {
    VerticalLayout wrapper = new VerticalLayout(components);
    wrapper.setPadding(false);
    wrapper.setSpacing(true);
    return wrapper;
  }

  /**
   * Creates the primary (save/confirm) button with the app's standard styling, placed at the end of
   * the button row.
   */
  public Button createPrimaryButton(final String id, final String label, final Runnable onClick) {
    Button button = new Button(label, e -> onClick.run());
    button.setId(id);
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    buttonRow.add(button);
    return button;
  }

  /**
   * Creates the destructive (delete) button with error styling, placed at the start of the button
   * row (left), separated from the confirm actions.
   */
  public Button createDeleteButton(final String id, final String label, final Runnable onClick) {
    Button button = new Button(label, e -> onClick.run());
    button.setId(id);
    button.addThemeVariants(ButtonVariant.LUMO_ERROR);
    button.getStyle().set("margin-inline-end", "auto");
    buttonRow.addComponentAsFirst(button);
    return button;
  }

  /** Creates a cancel button that closes the dialog. */
  public Button createCancelButton(final String id) {
    Button button = new Button("Cancel", e -> close());
    button.setId(id);
    buttonRow.add(button);
    return button;
  }

  /**
   * Wires the common binder affordance: the given primary button stays disabled until the binder
   * reports a valid form.
   */
  public void bindSaveEnabled(final Button saveButton, final Binder<?> binder) {
    saveButton.setEnabled(false);
    binder.addStatusChangeListener(e -> saveButton.setEnabled(binder.isValid()));
  }

  /** Convenience for muted helper text above the form (e.g. field hints). */
  public void addHelperText(final String text) {
    Span helper = new Span(text);
    helper.getStyle().set("color", "var(--lumo-secondary-text-color)");
    helper.getStyle().set("font-size", "var(--lumo-font-size-s)");
    addAboveForm(helper);
  }
}
