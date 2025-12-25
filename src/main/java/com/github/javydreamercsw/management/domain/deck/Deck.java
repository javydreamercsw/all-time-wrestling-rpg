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
package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "deck")
@Getter
@Setter
public class Deck extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "deck_id")
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  @OneToMany(
      mappedBy = "deck",
      cascade = CascadeType.ALL,
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  private Set<DeckCard> cards = new HashSet<>();

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  public void addCard(@NonNull Card card, @NonNull CardSet set, int amount) {
    DeckCard newDeckCard = new DeckCard();
    newDeckCard.setCard(card);
    newDeckCard.setSet(set);
    newDeckCard.setAmount(amount);
    newDeckCard.setDeck(this);

    // Remove the old one if it exists, then add the new one.
    // This ensures the amount is updated.
    getCards().remove(newDeckCard);
    getCards().add(newDeckCard);
  }
}
