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
package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.export.ShowExportService;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.List;
import lombok.NonNull;

/** Dialog for exporting a show card. */
public class ShowExportDialog extends Dialog {

  private final ShowExportService exportService;
  private final Show show;

  private final ComboBox<String> formatSelector = new ComboBox<>("Export Format");
  private final TextArea previewArea = new TextArea("Preview");
  private final Button copyButton = new Button("Copy to Clipboard", VaadinIcon.COPY.create());

  public ShowExportDialog(@NonNull ShowExportService exportService, @NonNull Show show) {
    this.exportService = exportService;
    this.show = show;

    setHeaderTitle("Export Show Card: " + show.getName());
    setModality(ModalityMode.VISUAL);
    setDraggable(true);
    setResizable(true);
    setWidth("700px");
    setHeight("600px");

    formatSelector.setWidthFull();
    List<String> formats = exportService.getAvailableFormats();
    formatSelector.setItems(formats);
    formatSelector.addValueChangeListener(event -> updatePreview());

    previewArea.setWidthFull();
    previewArea.setHeight("400px");
    previewArea.setReadOnly(true);

    copyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    copyButton.setEnabled(false);
    copyButton.addClickListener(event -> copyToClipboard());

    Button closeButton = new Button("Close", e -> close());

    HorizontalLayout footer = new HorizontalLayout(copyButton, closeButton);
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    footer.setWidthFull();

    VerticalLayout layout = new VerticalLayout(formatSelector, previewArea, footer);
    layout.setPadding(true);
    layout.setSpacing(true);
    layout.setSizeFull();

    add(layout);

    // Select first format by default
    if (!formats.isEmpty()) {
      formatSelector.setValue(formats.get(0));
    }
  }

  public ComboBox<String> getFormatSelector() {
    return formatSelector;
  }

  public TextArea getPreviewArea() {
    return previewArea;
  }

  public Button getCopyButton() {
    return copyButton;
  }

  private void updatePreview() {
    String format = formatSelector.getValue();
    if (format != null) {
      try {
        String content = exportService.export(show, format);
        previewArea.setValue(content);
        copyButton.setEnabled(true);
      } catch (Exception e) {
        Notification.show(
                "Error generating export: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
        previewArea.setValue("");
        copyButton.setEnabled(false);
      }
    } else {
      previewArea.setValue("");
      copyButton.setEnabled(false);
    }
  }

  private void copyToClipboard() {
    String content = previewArea.getValue();
    if (content != null && !content.isEmpty()) {
      // Escape special characters for JS string
      String escapedContent =
          content.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$").replace("'", "\\'");

      getElement().executeJs("navigator.clipboard.writeText($0)", content);

      Notification.show("Copied to clipboard!", 2000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
  }
}
