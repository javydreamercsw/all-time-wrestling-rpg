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
package com.github.javydreamercsw.management.domain.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "universe_join_request")
@Getter
@Setter
public class UniverseJoinRequest {

  public enum RequestStatus {
    /** Awaiting admin decision. */
    PENDING,
    /** Admin approved — account has been added as a member. */
    APPROVED,
    /** Admin rejected — requester may re-request later. */
    REJECTED,
    /** Admin blocked — requester cannot submit further requests to this universe. */
    BLOCKED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "universe_id", nullable = false)
  private Universe universe;

  /** The invite link used to access the join form. Nullable if the link was later deleted. */
  @ManyToOne
  @JoinColumn(name = "invite_id")
  private UniverseInvite invite;

  /** Set after self-registration completes, or immediately for existing logged-in users. */
  @ManyToOne
  @JoinColumn(name = "account_id")
  private Account account;

  @Column(name = "requester_name", nullable = false)
  private String requesterName;

  @Column(name = "requester_email")
  private String requesterEmail;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RequestStatus status = RequestStatus.PENDING;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt = Instant.now();

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @ManyToOne
  @JoinColumn(name = "resolved_by")
  private Account resolvedBy;

  @Column(name = "notes", length = 1000)
  private String notes;
}
