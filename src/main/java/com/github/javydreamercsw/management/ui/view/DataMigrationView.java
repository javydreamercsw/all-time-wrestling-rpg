package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.service.DataMigrationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        Anchor downloadLink = new Anchor();
        downloadLink.setVisible(false);

        exportButton.addClickListener(e -> {
            try {
                byte[] data = dataMigrationService.exportData(formatComboBox.getValue());
                StreamResource resource = new StreamResource("export.zip", () -> new ByteArrayInputStream(data));
                downloadLink.setHref(resource);
                downloadLink.getElement().setAttribute("download", true);
                downloadLink.setText("Download Export");
                downloadLink.setVisible(true);
            } catch (IOException ex) {
                Notification.show("Error exporting data: " + ex.getMessage());
            }
        });

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".zip");

        Button importButton = new Button("Import");
        importButton.addClickListener(e -> {
            try {
                InputStream inputStream = buffer.getInputStream();
                dataMigrationService.importData(formatComboBox.getValue(), inputStream.readAllBytes());
                Notification.show("Data imported successfully!");
            } catch (IOException ex) {
                Notification.show("Error importing data: " + ex.getMessage());
            }
        });

        add(new VerticalLayout(formatComboBox, exportButton, downloadLink, upload, importButton));
    }
}
