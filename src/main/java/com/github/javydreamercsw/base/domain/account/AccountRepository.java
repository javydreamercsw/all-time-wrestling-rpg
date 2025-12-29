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
package com.github.javydreamercsw.base.domain.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for Account entity. */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

  /**
   * Find an account by username.
   *
   * @param username the username
   * @return the account if found
   */
  Optional<Account> findByUsername(String username);

  /**
   * Find an account by email.
   *
   * @param email the email address
   * @return the account if found
   */
  Optional<Account> findByEmail(String email);

  /**
   * Check if an account exists with the given username.
   *
   * @param username the username
   * @return true if an account exists
   */
  boolean existsByUsername(String username);

  /**
   * Check if an account exists with the given email.
   *
   * @param email the email address
   * @return true if an account exists
   */
  boolean existsByEmail(String email);

  /**
   * Find an account by username with roles eagerly loaded.
   *
   * @param username the username
   * @return the account if found
   */
  @Query("SELECT a FROM Account a LEFT JOIN FETCH a.roles WHERE a.username = :username")
  Optional<Account> findByUsernameWithRoles(@Param("username") String username);
}
