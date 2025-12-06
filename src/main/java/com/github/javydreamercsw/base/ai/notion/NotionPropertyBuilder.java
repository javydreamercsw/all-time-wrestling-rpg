/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.ai.notion;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.common.RichTextType;
import notion.api.v1.model.databases.DatabaseProperty;
import notion.api.v1.model.pages.PageProperty;

public class NotionPropertyBuilder {

  public static PageProperty createTitleProperty(@NonNull String content) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.Title);
    property.setTitle(
        Collections.singletonList(
            new PageProperty.RichText(
                RichTextType.Text,
                new PageProperty.RichText.Text(content),
                null,
                null,
                null,
                null,
                null)));
    return property;
  }

  public static PageProperty createRichTextProperty(@NonNull String content) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.RichText);
    property.setRichText(
        Collections.singletonList(
            new PageProperty.RichText(
                RichTextType.Text,
                new PageProperty.RichText.Text(content),
                null,
                null,
                null,
                null,
                null)));
    return property;
  }

  public static PageProperty createNumberProperty(@NonNull Double value) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.Number);
    property.setNumber(value);
    return property;
  }

  public static PageProperty createSelectProperty(@NonNull String name) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.Select);
    property.setSelect(new DatabaseProperty.Select.Option(null, name, null, null));
    return property;
  }

  public static PageProperty createRelationProperty(@NonNull String externalId) {
    return createRelationProperty(Collections.singletonList(externalId));
  }

  public static PageProperty createRelationProperty(@NonNull List<String> externalIds) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.Relation);
    property.setRelation(
        externalIds.stream().map(PageProperty.PageReference::new).collect(Collectors.toList()));
    return property;
  }

  public static PageProperty createCheckboxProperty(@NonNull Boolean value) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.Checkbox);
    property.setCheckbox(value);
    return property;
  }

  public static PageProperty createDateProperty(@NonNull String dateString) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.Date);
    property.setDate(new PageProperty.Date(dateString, null));
    return property;
  }

  public static PageProperty createUrlProperty(@NonNull String urlString) {
    PageProperty property = new PageProperty();
    property.setType(PropertyType.Url);
    property.setUrl(urlString);
    return property;
  }
}
