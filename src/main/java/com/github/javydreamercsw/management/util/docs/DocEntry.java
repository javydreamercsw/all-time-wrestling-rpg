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
package com.github.javydreamercsw.management.util.docs;

import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class DocEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  @Getter @Setter private String id;
  @Getter @Setter private String category;
  @Getter @Setter private String title;
  @Getter @Setter private String description;
  @Getter @Setter private String imagePath;
  @Getter @Setter private int order;

  public DocEntry(
      final String id,
      final String category,
      final String title,
      final String description,
      final String imagePath,
      final int order) {
    this.id = id;
    this.category = category;
    this.title = title;
    this.description = description;
    this.imagePath = imagePath;
    this.order = order;
  }
}
