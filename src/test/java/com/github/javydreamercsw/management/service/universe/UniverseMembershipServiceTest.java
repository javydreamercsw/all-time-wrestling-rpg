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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UniverseMembershipServiceTest {

  @Mock private UniverseMembershipRepository membershipRepository;

  @InjectMocks private UniverseMembershipService universeMembershipService;

  private Universe universe;
  private Account account;
  private UniverseMembership membership;

  @BeforeEach
  void setUp() {
    universe = new Universe();
    universe.setName("Global Universe");

    account = new Account("booker1", "password1234", "booker1@example.com");

    membership = new UniverseMembership();
    membership.setUniverse(universe);
    membership.setAccount(account);
    membership.setRole(UniverseMemberRole.MEMBER);
  }

  // ==================== getUniversesForAccount ====================

  @Test
  void getUniversesForAccount_returnsMappedUniverses() {
    when(membershipRepository.findByAccount(account)).thenReturn(List.of(membership));

    List<Universe> result = universeMembershipService.getUniversesForAccount(account);

    assertEquals(1, result.size());
    assertSame(universe, result.get(0));
  }

  @Test
  void getUniversesForAccount_noMemberships_returnsEmptyList() {
    when(membershipRepository.findByAccount(account)).thenReturn(List.of());

    List<Universe> result = universeMembershipService.getUniversesForAccount(account);

    assertTrue(result.isEmpty());
  }

  // ==================== getMembersForUniverse ====================

  @Test
  void getMembersForUniverse_returnsMembershipList() {
    when(membershipRepository.findByUniverse(universe)).thenReturn(List.of(membership));

    List<UniverseMembership> result = universeMembershipService.getMembersForUniverse(universe);

    assertEquals(1, result.size());
    assertSame(membership, result.get(0));
  }

  @Test
  void getMembersForUniverse_noMembers_returnsEmptyList() {
    when(membershipRepository.findByUniverse(universe)).thenReturn(List.of());

    List<UniverseMembership> result = universeMembershipService.getMembersForUniverse(universe);

    assertTrue(result.isEmpty());
  }

  // ==================== isMember ====================

  @Test
  void isMember_accountIsMember_returnsTrue() {
    when(membershipRepository.existsByAccountAndUniverse(account, universe)).thenReturn(true);

    boolean result = universeMembershipService.isMember(universe, account);

    assertTrue(result);
  }

  @Test
  void isMember_accountIsNotMember_returnsFalse() {
    when(membershipRepository.existsByAccountAndUniverse(account, universe)).thenReturn(false);

    boolean result = universeMembershipService.isMember(universe, account);

    assertFalse(result);
  }

  // ==================== addMember ====================

  @Test
  void addMember_notYetMember_savesAndReturnsMembership() {
    when(membershipRepository.existsByAccountAndUniverse(account, universe)).thenReturn(false);
    when(membershipRepository.save(any(UniverseMembership.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    UniverseMembership result =
        universeMembershipService.addMember(universe, account, UniverseMemberRole.MEMBER);

    assertSame(universe, result.getUniverse());
    assertSame(account, result.getAccount());
    assertEquals(UniverseMemberRole.MEMBER, result.getRole());
    verify(membershipRepository).save(any(UniverseMembership.class));
  }

  @Test
  void addMember_alreadyMember_throwsIllegalStateException() {
    when(membershipRepository.existsByAccountAndUniverse(account, universe)).thenReturn(true);

    assertThrows(
        IllegalStateException.class,
        () -> universeMembershipService.addMember(universe, account, UniverseMemberRole.MEMBER));
  }

  // ==================== removeMember ====================

  @Test
  void removeMember_memberExists_deletesMembership() {
    when(membershipRepository.findByAccountAndUniverse(account, universe))
        .thenReturn(Optional.of(membership));

    universeMembershipService.removeMember(universe, account);

    verify(membershipRepository).delete(membership);
  }

  @Test
  void removeMember_notMember_throwsIllegalStateException() {
    when(membershipRepository.findByAccountAndUniverse(account, universe))
        .thenReturn(Optional.empty());

    assertThrows(
        IllegalStateException.class,
        () -> universeMembershipService.removeMember(universe, account));
  }
}
