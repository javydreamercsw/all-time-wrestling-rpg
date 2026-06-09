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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite.InviteType;
import com.github.javydreamercsw.management.domain.universe.UniverseInviteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InviteServiceTest {

  @Mock private UniverseInviteRepository inviteRepository;
  @InjectMocks private InviteService service;

  private Universe universe;
  private Account admin;

  @BeforeEach
  void setUp() {
    universe = new Universe();
    universe.setId(1L);
    universe.setName("Test Universe");

    admin = new Account();
    admin.setId(1L);
    admin.setUsername("admin");

    when(inviteRepository.save(any(UniverseInvite.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void generateInvite_targeted_setsExpiryAndMaxUses() {
    UniverseInvite invite = service.generateInvite(universe, InviteType.TARGETED, admin);

    assertThat(invite.getId()).isNotNull().hasSize(36);
    assertThat(invite.getType()).isEqualTo(InviteType.TARGETED);
    assertThat(invite.getMaxUses()).isEqualTo(1);
    assertThat(invite.getExpiresAt()).isNotNull();
    assertThat(invite.getExpiresAt())
        .isAfter(Instant.now().plus(InviteService.TARGETED_EXPIRY_DAYS - 1, ChronoUnit.DAYS));
    assertThat(invite.getRevokedAt()).isNull();
    assertThat(invite.getUseCount()).isZero();
  }

  @Test
  void generateInvite_community_noExpiryNoMaxUses() {
    UniverseInvite invite = service.generateInvite(universe, InviteType.COMMUNITY, admin);

    assertThat(invite.getType()).isEqualTo(InviteType.COMMUNITY);
    assertThat(invite.getExpiresAt()).isNull();
    assertThat(invite.getMaxUses()).isNull();
  }

  @Test
  void validateInvite_validToken_returnsInvite() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("abc-123");
    invite.setUseCount(0);
    when(inviteRepository.findById("abc-123")).thenReturn(Optional.of(invite));

    UniverseInvite result = service.validateInvite("abc-123");

    assertThat(result).isSameAs(invite);
  }

  @Test
  void validateInvite_unknownToken_throwsIllegalArgument() {
    when(inviteRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.validateInvite("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void validateInvite_revoked_throwsIllegalState() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("tok");
    invite.setRevokedAt(Instant.now().minusSeconds(1));
    when(inviteRepository.findById("tok")).thenReturn(Optional.of(invite));

    assertThatThrownBy(() -> service.validateInvite("tok"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("revoked");
  }

  @Test
  void validateInvite_expired_throwsIllegalState() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("tok");
    invite.setExpiresAt(Instant.now().minusSeconds(1));
    when(inviteRepository.findById("tok")).thenReturn(Optional.of(invite));

    assertThatThrownBy(() -> service.validateInvite("tok"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void validateInvite_maxUsesReached_throwsIllegalState() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("tok");
    invite.setMaxUses(1);
    invite.setUseCount(1);
    when(inviteRepository.findById("tok")).thenReturn(Optional.of(invite));

    assertThatThrownBy(() -> service.validateInvite("tok"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already been used");
  }

  @Test
  void revokeInvite_setsRevokedAt() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("tok");
    when(inviteRepository.findById("tok")).thenReturn(Optional.of(invite));

    service.revokeInvite("tok");

    ArgumentCaptor<UniverseInvite> captor = ArgumentCaptor.forClass(UniverseInvite.class);
    verify(inviteRepository).save(captor.capture());
    assertThat(captor.getValue().getRevokedAt()).isNotNull();
  }

  @Test
  void revokeInvite_unknownId_throwsIllegalArgument() {
    when(inviteRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.revokeInvite("unknown"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recordUse_incrementsCount() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("tok");
    invite.setUseCount(0);

    service.recordUse(invite);

    assertThat(invite.getUseCount()).isEqualTo(1);
    assertThat(invite.getRevokedAt()).isNull();
  }

  @Test
  void recordUse_reachesMaxUses_autoRevokes() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("tok");
    invite.setMaxUses(1);
    invite.setUseCount(0);

    service.recordUse(invite);

    assertThat(invite.getUseCount()).isEqualTo(1);
    assertThat(invite.getRevokedAt()).isNotNull();
  }

  @Test
  void recordUse_communityInvite_doesNotAutoRevoke() {
    UniverseInvite invite = new UniverseInvite();
    invite.setId("tok");
    invite.setType(InviteType.COMMUNITY);
    invite.setMaxUses(null);
    invite.setUseCount(99);

    service.recordUse(invite);

    assertThat(invite.getUseCount()).isEqualTo(100);
    assertThat(invite.getRevokedAt()).isNull();
  }

  @Test
  void listActiveInvites_delegatesToRepository() {
    UniverseInvite invite = new UniverseInvite();
    when(inviteRepository.findActiveByUniverse(universe)).thenReturn(List.of(invite));

    List<UniverseInvite> result = service.listActiveInvites(universe);

    assertThat(result).containsExactly(invite);
  }
}
