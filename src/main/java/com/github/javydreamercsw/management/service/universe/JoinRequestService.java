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
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest.RequestStatus;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequestRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.event.JoinRequestSubmittedEvent;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the lifecycle of universe membership requests. */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class JoinRequestService {

  private final UniverseJoinRequestRepository requestRepository;
  private final UniverseMembershipService membershipService;
  private final InviteService inviteService;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Submits a join request for a universe.
   *
   * <p>Guards:
   *
   * <ul>
   *   <li>Validates the invite is still active
   *   <li>Throws if the account is BLOCKED from this universe
   *   <li>Deduplicates: throws if a PENDING request already exists for this account
   * </ul>
   *
   * @param invite the validated invite link
   * @param requesterName display name provided by the requester
   * @param requesterEmail optional email
   * @param account null for anonymous (set after self-registration completes)
   * @return the new PENDING request
   */
  public UniverseJoinRequest submitRequest(
      @NonNull final UniverseInvite invite,
      @NonNull final String requesterName,
      final String requesterEmail,
      final Account account) {

    Universe universe = invite.getUniverse();

    if (account != null) {
      // Reject if blocked
      requestRepository
          .findByUniverseAndAccountAndStatus(universe, account, RequestStatus.BLOCKED)
          .ifPresent(
              b -> {
                throw new IllegalStateException(
                    "Your account has been blocked from joining this universe.");
              });

      // For community invites: if a pending request already exists, return it silently rather
      // than blocking the user. Community invites are multi-use and one pending request should
      // not prevent additional uses of the same link by the same account.
      if (invite.getType() == InviteType.COMMUNITY) {
        var existing =
            requestRepository.findByUniverseAndAccountAndStatusIn(
                universe, account, List.of(RequestStatus.PENDING));
        if (existing.isPresent()) {
          log.info(
              "Community invite used by '{}' — returning existing pending request {} for universe"
                  + " {}",
              requesterName,
              existing.get().getId(),
              universe.getName());
          inviteService.recordUse(invite);
          return existing.get();
        }
      } else {
        // For targeted invites: deduplicate strictly
        requestRepository
            .findByUniverseAndAccountAndStatusIn(universe, account, List.of(RequestStatus.PENDING))
            .ifPresent(
                p -> {
                  throw new IllegalStateException(
                      "A join request for this universe is already pending.");
                });
      }
    }

    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setUniverse(universe);
    request.setInvite(invite);
    request.setAccount(account);
    request.setRequesterName(requesterName);
    request.setRequesterEmail(requesterEmail);
    request.setStatus(RequestStatus.PENDING);
    request.setRequestedAt(Instant.now());

    inviteService.recordUse(invite);
    UniverseJoinRequest saved = requestRepository.save(request);
    log.info(
        "New join request {} for universe {} from '{}'",
        saved.getId(),
        universe.getName(),
        requesterName);
    eventPublisher.publishEvent(new JoinRequestSubmittedEvent(this, saved));
    return saved;
  }

  /**
   * Approves a pending request, adding the requester as a universe member.
   *
   * @param requestId the ID of the PENDING request
   * @param resolvedBy the admin approving
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.isOwnerOfRequest(#requestId)")
  public void approveRequest(final long requestId, @NonNull final Account resolvedBy) {
    UniverseJoinRequest request = getRequestOrThrow(requestId);
    ensurePending(request);

    request.setStatus(RequestStatus.APPROVED);
    request.setResolvedAt(Instant.now());
    request.setResolvedBy(resolvedBy);
    requestRepository.save(request);

    if (request.getAccount() == null) {
      throw new IllegalStateException(
          "Cannot approve: no account is linked to this request. "
              + "The requester must complete registration first.");
    }
    try {
      membershipService.addMember(
          request.getUniverse(), request.getAccount(), UniverseMemberRole.MEMBER);
      log.info(
          "Approved join request {} — {} added to universe {}",
          requestId,
          request.getAccount().getUsername(),
          request.getUniverse().getName());
    } catch (IllegalStateException alreadyMember) {
      log.warn(
          "Approved request {} but {} was already a member of {} — skipping addMember",
          requestId,
          request.getAccount().getUsername(),
          request.getUniverse().getName());
    }
  }

  /**
   * Rejects a pending request. The requester may submit a new request later.
   *
   * @param requestId the ID of the PENDING request
   * @param resolvedBy the admin rejecting
   * @param notes optional reason shown to the admin (not exposed to the requester)
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.isOwnerOfRequest(#requestId)")
  public void rejectRequest(
      final long requestId, @NonNull final Account resolvedBy, final String notes) {
    UniverseJoinRequest request = getRequestOrThrow(requestId);
    ensurePending(request);

    request.setStatus(RequestStatus.REJECTED);
    request.setResolvedAt(Instant.now());
    request.setResolvedBy(resolvedBy);
    request.setNotes(notes);
    requestRepository.save(request);
    log.info(
        "Rejected join request {} for universe {}", requestId, request.getUniverse().getName());
  }

  /**
   * Blocks a requester from submitting further requests to this universe.
   *
   * @param requestId the ID of the request being acted on
   * @param resolvedBy the admin blocking
   * @param notes optional reason
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.isOwnerOfRequest(#requestId)")
  public void blockRequester(
      final long requestId, @NonNull final Account resolvedBy, final String notes) {
    UniverseJoinRequest request = getRequestOrThrow(requestId);

    request.setStatus(RequestStatus.BLOCKED);
    request.setResolvedAt(Instant.now());
    request.setResolvedBy(resolvedBy);
    request.setNotes(notes);
    requestRepository.save(request);
    log.info(
        "Blocked requester on request {} for universe {}",
        requestId,
        request.getUniverse().getName());
  }

  /**
   * Returns all PENDING requests for a universe, ordered by submission time ascending.
   *
   * @param universe the universe whose requests to list
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.isOwner(#universe)")
  @Transactional(readOnly = true)
  public List<UniverseJoinRequest> getPendingRequests(@NonNull final Universe universe) {
    return requestRepository.findPendingByUniverse(universe);
  }

  /**
   * Returns all requests (any status) for a universe — for the full history view.
   *
   * @param universe the universe
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.isOwner(#universe)")
  @Transactional(readOnly = true)
  public List<UniverseJoinRequest> getAllRequests(@NonNull final Universe universe) {
    return requestRepository.findAllByUniverse(universe);
  }

  /** Returns the count of PENDING requests across all universes the admin manages. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Transactional(readOnly = true)
  public long countPendingForAdmin(@NonNull final Account admin) {
    return requestRepository.countPendingForAdmin(admin);
  }

  /**
   * Links an account to a previously anonymous request (called after self-registration completes).
   *
   * @param requestId the ID of the request
   * @param account the newly created account
   */
  public void linkAccount(final long requestId, @NonNull final Account account) {
    UniverseJoinRequest request = getRequestOrThrow(requestId);
    request.setAccount(account);
    requestRepository.save(request);
  }

  private UniverseJoinRequest getRequestOrThrow(final long requestId) {
    return requestRepository
        .findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Join request not found: " + requestId));
  }

  private void ensurePending(final UniverseJoinRequest request) {
    if (request.getStatus() != RequestStatus.PENDING) {
      throw new IllegalStateException(
          "Request " + request.getId() + " is not PENDING (status: " + request.getStatus() + ")");
    }
  }
}
