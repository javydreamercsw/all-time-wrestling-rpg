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
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "deck_card",
    uniqueConstraints = @UniqueConstraint(columnNames = {"deck_id", "card_id", "set_id"}))
@Getter
@Setter
public class DeckCard extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "deck_id")
  private Deck deck;

  @ManyToOne(optional = false)
  @JoinColumn(name = "card_id")
  private Card card;

  @ManyToOne(optional = false)
  @JoinColumn(name = "set_id")
  private CardSet set;

  @Column(nullable = false, name = "amount")
  private int amount;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    DeckCard deckCard = (DeckCard) o;
    return Objects.equals(deck, deckCard.deck)
        && Objects.equals(card, deckCard.card)
        && Objects.equals(set, deckCard.set);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), deck, card, set);
  }
}
