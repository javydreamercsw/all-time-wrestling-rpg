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

import com.github.javydreamercsw.base.domain.account.Account;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

  private static final int EXPIRATION_MINUTES = 60 * 24; // 24 hours

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String token;

  @OneToOne(targetEntity = Account.class, fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = "account_id")
  private Account account;

  @NotNull private LocalDateTime expiryDate;

  @NotNull private LocalDateTime createdDate;

  public PasswordResetToken(final String token, final Account account) {
    this.token = token;
    this.account = account;
    this.expiryDate = calculateExpiryDate();
    this.createdDate = LocalDateTime.now();
  }

  private LocalDateTime calculateExpiryDate() {
    return LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
  }

  public boolean isExpired() {
    return getExpiryDate().isBefore(LocalDateTime.now());
  }
}
