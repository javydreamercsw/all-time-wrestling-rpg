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
package com.github.javydreamercsw.management.ui.view.news;

import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import lombok.NonNull;

public class NewsDialog extends Dialog {

  public NewsDialog(@NonNull NewsService newsService, @NonNull Runnable onSave) {
    setHeaderTitle("Create News Item");

    TextField headline = new TextField("Headline");
    headline.setWidthFull();
    headline.setRequired(true);

    TextArea content = new TextArea("Content");
    content.setWidthFull();
    content.setRequired(true);
    content.setMinHeight("150px");

    ComboBox<NewsCategory> category = new ComboBox<>("Category");
    category.setItems(NewsCategory.values());
    category.setItemLabelGenerator(NewsCategory::getDisplayName);
    category.setValue(NewsCategory.BREAKING);
    category.setRequired(true);

    Checkbox isRumor = new Checkbox("Is Rumor?");

    IntegerField importance = new IntegerField("Importance (1-5)");
    importance.setMin(1);
    importance.setMax(5);
    importance.setValue(3);
    importance.setStepButtonsVisible(true);

    VerticalLayout layout = new VerticalLayout(headline, content, category, isRumor, importance);
    layout.setPadding(false);
    layout.setSpacing(true);
    add(layout);

    Button saveButton =
        new Button(
            "Save",
            e -> {
              if (headline.isEmpty() || content.isEmpty() || category.getValue() == null) {
                return;
              }
              newsService.createNewsItem(
                  headline.getValue(),
                  content.getValue(),
                  category.getValue(),
                  isRumor.getValue(),
                  importance.getValue());
              onSave.run();
              close();
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(cancelButton, saveButton);
  }
}
