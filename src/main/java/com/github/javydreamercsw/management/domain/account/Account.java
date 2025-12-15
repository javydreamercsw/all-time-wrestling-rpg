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

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Entity representing a user account in the system. */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false, length = 50)
  @NotNull @Size(min = 3, max = 50) private String username;

  @Column(nullable = false)
  @NotNull @Size(min = 60, max = 100) // BCrypt encoded passwords are 60 chars
  private String password;

  @Column(unique = true, nullable = false, length = 100)
  @NotNull @Email private String email;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "account_roles",
      joinColumns = @JoinColumn(name = "account_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false)
  private boolean accountNonExpired = true;

  @Column(nullable = false)
  private boolean accountNonLocked = true;

  @Column(nullable = false)
  private boolean credentialsNonExpired = true;

  @Column(nullable = false)
  private int failedLoginAttempts = 0;

  @Column private LocalDateTime lockedUntil;

  @Column private LocalDateTime lastLogin;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedDate;

  public Account(String username, String password, String email) {
    this.username = username;
    this.password = password;
    this.email = email;
  }

  /**
   * Add a role to this account.
   *
   * @param role the role to add
   */
  public void addRole(Role role) {
    roles.add(role);
  }

  /**
   * Remove a role from this account.
   *
   * @param role the role to remove
   */
  public void removeRole(Role role) {
    roles.remove(role);
  }

  /**
   * Check if this account has a specific role.
   *
   * @param roleName the role name to check
   * @return true if the account has the role
   */
  public boolean hasRole(RoleName roleName) {
    return roles.stream().anyMatch(role -> role.getName() == roleName);
  }

  /** Increment failed login attempts counter. */
  public void incrementFailedAttempts() {
    this.failedLoginAttempts++;
  }

  /** Reset failed login attempts counter. */
  public void resetFailedAttempts() {
    this.failedLoginAttempts = 0;
    this.lockedUntil = null;
    this.accountNonLocked = true;
  }

  /**
   * Lock the account until a specific time.
   *
   * @param until the time until which the account is locked
   */
  public void lockUntil(LocalDateTime until) {
    this.accountNonLocked = false;
    this.lockedUntil = until;
  }

  /**
   * Check if the account lock has expired.
   *
   * @return true if the account is unlocked or lock has expired
   */
  public boolean isLockExpired() {
    if (lockedUntil == null) {
      return true;
    }
    return LocalDateTime.now().isAfter(lockedUntil);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Account)) {
      return false;
    }
    Account account = (Account) o;
    return Objects.equals(username, account.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username);
  }

  @Override
  public String toString() {
    return "Account{"
        + "id="
        + id
        + ", username='"
        + username
        + '\''
        + ", email='"
        + email
        + '\''
        + ", enabled="
        + enabled
        + ", roles="
        + roles
        + '}';
  }
}
