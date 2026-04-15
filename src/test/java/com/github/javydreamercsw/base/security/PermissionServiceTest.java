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
package com.github.javydreamercsw.base.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class PermissionServiceTest {

  private WrestlerRepository wrestlerRepository;
  private AccountRepository accountRepository;
  private DeckRepository deckRepository;
  private PermissionService permissionService;

  @BeforeEach
  void setUp() {
    wrestlerRepository = mock(WrestlerRepository.class);
    accountRepository = mock(AccountRepository.class);
    deckRepository = mock(DeckRepository.class);
    permissionService =
        new PermissionService(wrestlerRepository, accountRepository, deckRepository);

    UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
    var auth = new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void testIsOwnerWrestler() {
    Account account = new Account("testuser", "password", "test@example.com");
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);

    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(wrestler));

    assertThat(permissionService.isOwner(wrestler)).isTrue();

    Wrestler otherWrestler = new Wrestler();
    otherWrestler.setId(2L);
    assertThat(permissionService.isOwner(otherWrestler)).isFalse();
  }

  @Test
  void testIsOwnerDeck() {
    Account account = new Account("testuser", "password", "test@example.com");
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    Deck deck = new Deck();
    deck.setWrestler(wrestler);

    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(wrestler));

    assertThat(permissionService.isOwner(deck)).isTrue();

    Wrestler otherWrestler = new Wrestler();
    otherWrestler.setId(2L);
    Deck otherDeck = new Deck();
    otherDeck.setWrestler(otherWrestler);
    assertThat(permissionService.isOwner(otherDeck)).isFalse();
  }

  @Test
  void testIsOwnerDeckCard() {
    Account account = new Account("testuser", "password", "test@example.com");
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    Deck deck = new Deck();
    deck.setWrestler(wrestler);
    com.github.javydreamercsw.management.domain.deck.DeckCard card =
        new com.github.javydreamercsw.management.domain.deck.DeckCard();
    card.setDeck(deck);

    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(wrestler));

    assertThat(permissionService.isOwner(card)).isTrue();
  }

  @Test
  void testIsOwnerInboxItem() {
    Account account = new Account("testuser", "password", "test@example.com");
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);

    com.github.javydreamercsw.management.domain.inbox.InboxItem item =
        new com.github.javydreamercsw.management.domain.inbox.InboxItem();
    com.github.javydreamercsw.management.domain.inbox.InboxItemTarget target =
        new com.github.javydreamercsw.management.domain.inbox.InboxItemTarget();
    target.setTargetId("1");
    item.getTargets().add(target);

    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(wrestler));

    assertThat(permissionService.isOwner(item)).isTrue();
  }

  @Test
  void testIsOwnerCollection() {
    Account account = new Account("testuser", "password", "test@example.com");
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);

    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(w1, w2));

    assertThat(permissionService.isOwner(List.of(w1, w2))).isTrue();

    Wrestler w3 = new Wrestler();
    w3.setId(3L);
    assertThat(permissionService.isOwner(List.of(w1, w3))).isFalse();
  }

  @Test
  void testIsOwnerByTypeId() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    Account account = new Account("testuser", "password", "test@example.com");

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(wrestler));

    assertThat(permissionService.isOwner(1L, "Wrestler")).isTrue();

    Deck deck = new Deck();
    deck.setWrestler(wrestler);
    when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
    assertThat(permissionService.isOwner(10L, "Deck")).isTrue();

    assertThat(permissionService.isOwner(1L, "Unknown")).isFalse();
  }
}
