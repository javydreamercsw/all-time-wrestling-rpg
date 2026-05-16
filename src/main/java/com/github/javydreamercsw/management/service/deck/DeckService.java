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
package com.github.javydreamercsw.management.service.deck;

import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class DeckService {

  private final DeckRepository deckRepository;
  private final Clock clock;

  DeckService(final DeckRepository deckRepository, final Clock clock) {
    this.deckRepository = deckRepository;
    this.clock = clock;
  }

  public long count() {
    return deckRepository.count();
  }

  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or\
       @permissionService.isOwner(#deck.wrestler)\
      """)
  public void delete(@NonNull final Deck deck) {
    deckRepository.delete(deck);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public List<Deck> findAll() {
    return deckRepository.findAll();
  }

  public Deck findById(@NonNull final Long id) {
    return deckRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Deck with id " + id + " not found"));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public List<Deck> list(@NonNull final Pageable pageable) {
    return deckRepository.findAll(pageable).getContent();
  }

  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or\
       @permissionService.isOwner(#deck.wrestler)\
      """)
  public Deck save(@NonNull final Deck deck) {
    deck.setCreationDate(clock.instant());
    return deckRepository.saveAndFlush(deck);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public List<Deck> saveAll(@NonNull final List<Deck> decks) {
    decks.forEach(deck -> deck.setCreationDate(clock.instant()));
    return deckRepository.saveAll(decks);
  }

  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or\
       @permissionService.isOwner(#wrestler)\
      """)
  public Deck createDeck(@NonNull final Wrestler wrestler) {
    Deck deck = new Deck();
    deck.setWrestler(wrestler);
    save(deck);
    return deck;
  }

  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_VIEWER') or\
       @permissionService.isOwner(#wrestler)\
      """)
  public List<Deck> findByWrestler(final Wrestler wrestler) {
    return deckRepository.findByWrestler(wrestler);
  }
}
