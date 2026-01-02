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
package com.github.javydreamercsw.base.ai.notion.handler;

import com.github.javydreamercsw.base.ai.notion.NotionPage;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;

/**
 * A generic handler for a Notion entity.
 *
 * @param <T> The type of the entity page.
 */
public interface NotionEntityHandler<T extends NotionPage> {

  /**
   * Gets the name of the database.
   *
   * @return The name of the database.
   */
  String getDatabaseName();

  /**
   * Loads an entity by its ID.
   *
   * @param id The ID of the entity.
   * @return An optional containing the entity page if found, empty otherwise.
   */
  Optional<T> loadById(@NonNull String id);

  /**
   * Loads an entity by its name.
   *
   * @param name The name of the entity.
   * @return An optional containing the entity page if found, empty otherwise.
   */
  Optional<T> loadByName(@NonNull String name);

  /**
   * Loads all entities.
   *
   * @return A list of all entity pages.
   */
  List<T> loadAll();

  /**
   * Loads all entities.
   *
   * @param syncMode sync mode
   * @return A list of all entity pages.
   */
  List<T> loadAll(boolean syncMode);
}
