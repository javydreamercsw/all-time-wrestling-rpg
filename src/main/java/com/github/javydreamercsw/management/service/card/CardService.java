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

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardService {

  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private Clock clock;
  @Autowired private Validator validator;

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public Card createCard(@NonNull String name) {
    Card card = new Card();
    card.setName(name);
    // Set default values
    card.setDamage(1);
    card.setMomentum(1);
    card.setTarget(1);
    card.setStamina(1);
    card.setSignature(false);
    card.setFinisher(false);
    card.setType("Strike");

    // Set default CardSet (use the first available one)
    List<CardSet> sets = new ArrayList<>();
    cardSetRepository.findAll().forEach(sets::add);
    CardSet defaultSet =
        sets.stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No CardSet available for default"));
    card.setSet(defaultSet);

    Integer maxCardNumber = cardRepository.findMaxCardNumberBySet(defaultSet.getId());
    card.setNumber(maxCardNumber == null ? 1 : maxCardNumber + 1);

    return save(card);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Card> list(Pageable pageable) {
    return cardRepository.findAll(pageable).toList();
  }

  @PreAuthorize("isAuthenticated()")
  public long count() {
    return cardRepository.count();
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public Card save(@NonNull Card card) {
    card.setCreationDate(clock.instant());
    var violations = validator.validate(card);
    if (!violations.isEmpty()) {
      throw new ValidationException(violations.toString());
    }
    return cardRepository.saveAndFlush(card);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Card> findAll() {
    return cardRepository.findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Card> findByNumberAndSet(Integer number, String setCode) {
    return cardRepository.findByNumberAndSetCode(number, setCode);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public void delete(Long id) {
    cardRepository.deleteById(id);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Card> findById(Long id) {
    return cardRepository.findById(id);
  }
}
