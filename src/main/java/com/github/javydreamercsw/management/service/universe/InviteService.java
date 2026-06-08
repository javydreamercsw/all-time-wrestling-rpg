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
package com.github.javydreamercsw.management.service.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite.InviteType;
import com.github.javydreamercsw.management.domain.universe.UniverseInviteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the lifecycle of universe invite links. */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class InviteService {

  /** Default expiry for TARGETED invites. */
  static final int TARGETED_EXPIRY_DAYS = 7;

  private final UniverseInviteRepository inviteRepository;

  /**
   * Generates a new invite link for the given universe.
   *
   * @param universe the target universe
   * @param type TARGETED (single-use, expires in 7 days) or COMMUNITY (multi-use, no expiry)
   * @param createdBy the admin creating the link
   * @return the persisted invite
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public UniverseInvite generateInvite(
      @NonNull final Universe universe,
      @NonNull final InviteType type,
      @NonNull final Account createdBy) {
    UniverseInvite invite = new UniverseInvite();
    invite.setId(UUID.randomUUID().toString());
    invite.setUniverse(universe);
    invite.setType(type);
    invite.setCreatedBy(createdBy);
    invite.setCreatedAt(Instant.now());

    if (type == InviteType.TARGETED) {
      invite.setExpiresAt(Instant.now().plus(TARGETED_EXPIRY_DAYS, ChronoUnit.DAYS));
      invite.setMaxUses(1);
    }
    // COMMUNITY: no expiry, no max_uses limit

    UniverseInvite saved = inviteRepository.save(invite);
    log.info(
        "Generated {} invite {} for universe {} by {}",
        type,
        saved.getId(),
        universe.getName(),
        createdBy.getUsername());
    return saved;
  }

  /**
   * Validates that an invite token is usable. Returns the invite or throws with a clear message.
   *
   * @param token the UUID token from the invite link
   * @return the valid, active invite
   * @throws IllegalArgumentException if the token does not exist
   * @throws IllegalStateException if the invite is expired, revoked, or exhausted
   */
  @Transactional(readOnly = true)
  public UniverseInvite validateInvite(@NonNull final String token) {
    UniverseInvite invite =
        inviteRepository
            .findById(token)
            .orElseThrow(() -> new IllegalArgumentException("Invite link not found or invalid."));

    if (invite.getRevokedAt() != null) {
      throw new IllegalStateException("This invite link has been revoked.");
    }
    if (invite.getExpiresAt() != null && Instant.now().isAfter(invite.getExpiresAt())) {
      throw new IllegalStateException("This invite link has expired.");
    }
    if (invite.getMaxUses() != null && invite.getUseCount() >= invite.getMaxUses()) {
      throw new IllegalStateException("This invite link has already been used.");
    }
    return invite;
  }

  /**
   * Revokes an invite immediately, preventing further use.
   *
   * @param inviteId the UUID of the invite to revoke
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void revokeInvite(@NonNull final String inviteId) {
    UniverseInvite invite =
        inviteRepository
            .findById(inviteId)
            .orElseThrow(() -> new IllegalArgumentException("Invite not found: " + inviteId));
    invite.setRevokedAt(Instant.now());
    inviteRepository.save(invite);
    log.info("Revoked invite {}", inviteId);
  }

  /**
   * Records one use of the invite. Auto-revokes TARGETED invites that have reached max_uses.
   *
   * @param invite the invite to record a use against
   */
  public void recordUse(@NonNull final UniverseInvite invite) {
    invite.setUseCount(invite.getUseCount() + 1);
    if (invite.getMaxUses() != null && invite.getUseCount() >= invite.getMaxUses()) {
      invite.setRevokedAt(Instant.now());
      log.debug("Auto-revoked invite {} after reaching max uses", invite.getId());
    }
    inviteRepository.save(invite);
  }

  /**
   * Returns all non-revoked invites for a universe, ordered newest first.
   *
   * @param universe the universe whose invites to list
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Transactional(readOnly = true)
  public List<UniverseInvite> listActiveInvites(@NonNull final Universe universe) {
    return inviteRepository.findActiveByUniverse(universe);
  }

  /**
   * Looks up an invite by token without validating it (used to display invite info on join pages).
   */
  @Transactional(readOnly = true)
  public Optional<UniverseInvite> findById(@NonNull final String token) {
    return inviteRepository.findById(token);
  }
}
