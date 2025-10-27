package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.service.DataMigrationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Route("data-migration")
@PageTitle("Data Migration")
@Menu(order = 99, icon = "vaadin:database", title = "Data Migration")
@PermitAll
public class DataMigrationView extends Main {

  private final DataMigrationService dataMigrationService;

  public DataMigrationView(DataMigrationService dataMigrationService) {
    this.dataMigrationService = dataMigrationService;

    ComboBox<String> formatComboBox = new ComboBox<>("Select Format");
    formatComboBox.setItems("JSON", "CSV");

    Button exportButton = new Button("Export");
    VerticalLayout downloadContainer = new VerticalLayout();

    exportButton.addClickListener(
        e -> {
          try {
            String format = formatComboBox.getValue();
            if (format == null) {
              Notification.show("Please select a format.");
              return;
            }
            byte[] data = dataMigrationService.exportData(format);
            StreamResource resource =
                new StreamResource("export.zip", () -> new ByteArrayInputStream(data));
            Anchor downloadLink = new Anchor(resource, "Download Export");
            downloadLink.getElement().setAttribute("download", true);
            downloadContainer.removeAll();
            downloadContainer.add(downloadLink);
          } catch (IOException ex) {
            Notification.show("Error exporting data: " + ex.getMessage());
          }
        });

    Upload upload = new Upload();
    upload.setAcceptedFileTypes(".zip");
    upload.setUploadHandler(
        UploadHandler.inMemory(
            (uploadMetadata, bytes) -> {
              try {
                String format = formatComboBox.getValue();
                if (format == null) {
                  Notification.show("Please select a format.");
                  return;
                }
                dataMigrationService.importData(format, bytes);
                Notification.show("Data imported successfully!");
              } catch (IOException ex) {
                Notification.show("Error importing data: " + ex.getMessage());
              }
            }));

    add(new VerticalLayout(formatComboBox, exportButton, downloadContainer, upload));
  }
}
