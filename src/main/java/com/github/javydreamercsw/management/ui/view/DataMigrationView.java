package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.service.DataMigrationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

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
        exportButton.addClickListener(e -> {
            dataMigrationService.exportData(formatComboBox.getValue());
        });

        Upload upload = new Upload();
        Button importButton = new Button("Import");
        importButton.addClickListener(e -> {
            // How to get the file from the upload component?
            // dataMigrationService.importData(formatComboBox.getValue(), file);
        });

        add(new VerticalLayout(formatComboBox, exportButton, upload, importButton));
    }
}
