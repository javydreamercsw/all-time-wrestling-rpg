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
package com.github.javydreamercsw.management.domain.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for Role entity. */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * Find a role by its name.
   *
   * @param name the role name
   * @return the role if found
   */
  Optional<Role> findByName(RoleName name);

  /**
   * Check if a role exists by name.
   *
   * @param name the role name
   * @return true if the role exists
   */
  boolean existsByName(RoleName name);
}
