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
package com.github.javydreamercsw.management.service.card;

import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CardSetService {

  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private Clock clock;

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public CardSet createCardSet(@NonNull final String name, @NonNull final String setCode) {
    CardSet card = new CardSet();
    card.setName(name);
    card.setCode(setCode);
    return save(card);
  }

  @PreAuthorize("isAuthenticated()")
  public List<CardSet> list(@NonNull final Pageable pageable) {
    return cardSetRepository.findAll(pageable).toList();
  }

  public long count() {
    return cardSetRepository.count();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public CardSet save(@NonNull final CardSet card) {
    card.setCreationDate(clock.instant());
    return cardSetRepository.saveAndFlush(card);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<CardSet> saveAll(@NonNull final List<CardSet> cardSets) {
    cardSets.forEach(cardSet -> cardSet.setCreationDate(clock.instant()));
    return cardSetRepository.saveAll(cardSets);
  }

  @PreAuthorize("isAuthenticated()")
  public List<CardSet> findAll() {
    return cardSetRepository.findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<CardSet> findBySetCode(@NonNull final String setCode) {
    return cardSetRepository.findByCode(setCode);
  }
}
