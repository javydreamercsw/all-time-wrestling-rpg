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

  DeckService(DeckRepository deckRepository, Clock clock) {
    this.deckRepository = deckRepository;
    this.clock = clock;
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER') or @permissionService.isOwner(#wrestler)")
  public Deck createDeck(@NonNull Wrestler wrestler) {

    Deck deck = new Deck();

    deck.setWrestler(wrestler);

    save(deck);

    return deck;
  }

  @PreAuthorize("isAuthenticated()")
  public List<Deck> list(Pageable pageable) {

    return deckRepository.findAllBy(pageable).toList();
  }

  @PreAuthorize("isAuthenticated()")
  public long count() {

    return deckRepository.count();
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER') or @permissionService.isOwner(#deck)")
  public Deck save(@NonNull Deck deck) {

    deck.setCreationDate(clock.instant());

    return deckRepository.saveAndFlush(deck);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Deck> findAll() {

    return deckRepository.findAll();
  }

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER') or @permissionService.isOwner(#deck)")
  public void delete(Deck deck) {

    deckRepository.delete(deck);
  }

  @PreAuthorize("isAuthenticated()")
  public Deck findById(@NonNull Long id) {
    return deckRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Deck with id " + id + " not found"));
  }

  @PreAuthorize("isAuthenticated()")
  public List<Deck> findByWrestler(Wrestler wrestler) {
    return deckRepository.findByWrestler(wrestler);
  }
}
